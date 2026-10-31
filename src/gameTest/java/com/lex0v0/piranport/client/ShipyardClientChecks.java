package com.lex0v0.piranport.client;

import com.lex0v0.piranport.block.ShipyardBlock;
import com.lex0v0.piranport.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;

// 在实际模型烘焙后核对贴图、物品变换和四向可见面与服务端轮廓。
final class ShipyardClientChecks {
    private ShipyardClientChecks() {}

    static boolean verify(Minecraft minecraft) {
        var atlas = ResourceLocation.fromNamespaceAndPath("piranport", "block/shipyard_atlas");
        var casing = ResourceLocation.fromNamespaceAndPath("piranport", "block/shipyard_casing");
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            var state = ModBlocks.SHIPYARD.get().defaultBlockState().setValue(ShipyardBlock.FACING, facing);
            var model = minecraft.getBlockRenderer().getBlockModel(state);
            if (model == minecraft.getModelManager().getMissingModel()) return false;
            if (!model.getParticleIcon().contents().name().equals(casing)) return false;
            var shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, RandomSource.create(0)));
            for (Direction side : Direction.values()) quads.addAll(model.getQuads(state, side, RandomSource.create(0)));
            if (quads.size() <= 6) return false;
            for (var quad : quads) {
                var sprite = quad.getSprite().contents();
                if (!sprite.name().equals(atlas) || sprite.width() != 128 || sprite.height() != 128) return false;
                int[] vertices = quad.getVertices(); int stride = vertices.length/4;
                double x = 0, y = 0, z = 0;
                for (int i = 0; i < 4; i++) {
                    x += Float.intBitsToFloat(vertices[i*stride])/4;
                    y += Float.intBitsToFloat(vertices[i*stride+1])/4;
                    z += Float.intBitsToFloat(vertices[i*stride+2])/4;
                }
                var normal = quad.getDirection();
                double px = x-normal.getStepX()*1e-4, py = y-normal.getStepY()*1e-4, pz = z-normal.getStepZ()*1e-4;
                if (shape.toAabbs().stream().noneMatch(b -> b.contains(px, py, pz))) return false;
            }
        }
        var item = minecraft.getItemRenderer().getModel(new ItemStack(ModBlocks.SHIPYARD.get()), null, null, 0);
        if (item == minecraft.getModelManager().getMissingModel()) return false;
        var transforms = item.getTransforms();
        return Math.abs(transforms.getTransform(ItemDisplayContext.GUI).scale.x()-.6) < 1e-6
                && Math.abs(transforms.getTransform(ItemDisplayContext.GROUND).scale.x()-.25) < 1e-6
                && Math.abs(transforms.getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).scale.x()-.4) < 1e-6;
    }
}
