package com.lex0v0.piranport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lex0v0.piranport.block.ShipyardBlock;
import com.lex0v0.piranport.registry.ModBlocks;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 从发行模型和方块状态独立核对四向碰撞，防止美术与服务端轮廓漂移。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardModelGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void collisionAndSelectionFollowModelInAllFacings(GameTestHelper h) {
        var model = resource("models/block/shipyard.json");
        var variants = resource("blockstates/shipyard.json").getAsJsonObject("variants");
        var relative = new BlockPos(2, 2, 2);
        var pos = h.absolutePos(relative);
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            var state = ModBlocks.SHIPYARD.get().defaultBlockState().setValue(ShipyardBlock.FACING, facing);
            h.setBlock(relative, state);
            int degrees = variants.getAsJsonObject("facing=" + facing.getName()).get("y").getAsInt();
            VoxelShape expected = Shapes.empty();
            for (var value : model.getAsJsonArray("elements")) {
                var e = value.getAsJsonObject();
                var from = e.getAsJsonArray("from");
                var to = e.getAsJsonArray("to");
                double x0 = 1, z0 = 1, x1 = 0, z1 = 0;
                for (int x : new int[]{0, 1}) for (int z : new int[]{0, 1}) {
                    Vec3 point = rotate(new Vec3((x == 0 ? from : to).get(0).getAsDouble()/16, 0,
                            (z == 0 ? from : to).get(2).getAsDouble()/16), degrees);
                    x0 = Math.min(x0, point.x); x1 = Math.max(x1, point.x);
                    z0 = Math.min(z0, point.z); z1 = Math.max(z1, point.z);
                }
                expected = Shapes.or(expected, Shapes.box(x0, from.get(1).getAsDouble()/16, z0,
                        x1, to.get(1).getAsDouble()/16, z1));
            }
            var collision = state.getCollisionShape(h.getLevel(), pos, CollisionContext.empty());
            var selection = state.getShape(h.getLevel(), pos);
            h.assertTrue(volume(Shapes.join(collision, expected, BooleanOp.NOT_SAME)) < 1e-8,
                    facing + " collision matches rendered model");
            h.assertTrue(volume(Shapes.join(selection, expected, BooleanOp.NOT_SAME)) < 1e-8,
                    facing + " selection follows model");
            h.assertTrue(!contains(collision, rotate(new Vec3(.05, .8, .05), degrees)), "upper outside corner remains empty");
            h.assertTrue(contains(collision, new Vec3(.5, .05, .5)), "foundation remains solid");
            h.assertTrue(Math.abs(collision.max(Direction.Axis.Y)-15.5/16) < 1e-8, "top bound reaches model edge");
            h.assertTrue(!state.canOcclude(), "open model does not occlude like a full cube");
        }
        h.succeed();
    }

    private static Vec3 rotate(Vec3 p, int degrees) {
        double angle = Math.toRadians(degrees), x = p.x-.5, z = p.z-.5;
        return new Vec3(.5+x*Math.cos(angle)-z*Math.sin(angle), p.y,
                .5+x*Math.sin(angle)+z*Math.cos(angle));
    }

    private static double volume(VoxelShape shape) {
        return shape.toAabbs().stream().mapToDouble(b -> b.getXsize()*b.getYsize()*b.getZsize()).sum();
    }

    private static boolean contains(VoxelShape shape, Vec3 point) {
        return shape.toAabbs().stream().anyMatch(b -> b.contains(point));
    }

    private static JsonObject resource(String path) {
        try (var input = ShipyardModelGameTests.class.getResourceAsStream("/assets/piranport/" + path)) {
            if (input == null) throw new IllegalStateException("Missing resource: " + path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException error) { throw new IllegalStateException(path, error); }
    }
}
