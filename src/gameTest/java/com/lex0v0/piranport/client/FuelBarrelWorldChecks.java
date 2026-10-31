package com.lex0v0.piranport.client;

import com.google.gson.GsonBuilder;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.client.renderer.FuelBarrelRenderer;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModFluids;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

// 显式开关下使用全新隔离世界核对网络显示，并在正常渲染阶段截图。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class FuelBarrelWorldChecks {
    private record Expected(BlockPos pos, int amount, int capacity, FuelBarrelBlock.Part part) {}
    private static final List<Expected> EXPECTED = new ArrayList<>();
    private static final Map<String, Object> REPORT = new LinkedHashMap<>();
    private static final Path OUTPUT = Path.of(System.getProperty("piranport.acceptance.barrelOutput", "build/fuel-barrel-client"));
    private static final boolean SEAM = Boolean.getBoolean("piranport.acceptance.barrelSeam");
    private static final String[] SHOTS = SEAM ? new String[]{"seam-front","seam-left","seam-right","seam-above","seam-empty","seam-full"}
            : new String[]{"overview", "horizontal", "vertical", "singles"};
    private static final double[][] CAMERAS = SEAM
            ? new double[][]{{1,81,3.5,1,82,.75},{-1,81.2,3,.7,82,.5},{3,81.2,3,1.3,82,.5},{1,82,3,1,82,.5},{1,81,3.5,1,82,.75},{1,81,3.5,1,82,.75}}
            : new double[][]{{19,89,23,0,82,3},{-1,83.2,9,-.5,81.6,5},{3,84,0,0,82,-5},{4,83,5,0,81.5,0}};
    private static volatile boolean seeded, transferred;
    private static int stage, ticks, delay, shot;
    private static float yaw, pitch;
    private static Screen capture;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("piranport.acceptance.barrelWorld") || stage == 100) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (++ticks > 6000) throw new IllegalStateException("Fuel barrel check timed out at stage " + stage);
            if (mc.player != null) {
                mc.player.setYRot(yaw); mc.player.setXRot(pitch);
                mc.player.yRotO = yaw; mc.player.xRotO = pitch;
            }
            if (UiTooltipCapture.pending()) return;
            if (delay > 0) { delay--; return; }
            if (stage == 0 && AcceptanceClientStartup.titleScreenReady(mc)) {
                Files.createDirectories(OUTPUT);
                String name = "fuel-barrel-visual-" + System.currentTimeMillis();
                REPORT.put("world", name);
                mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261031L, false, false), r -> r.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
                stage = 1;
            } else if (stage == 1 && mc.player != null && mc.getSingleplayerServer() != null) {
                var server = mc.getSingleplayerServer(); var uuid = mc.player.getUUID();
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    for (int x=-10;x<=13;x++) for(int z=-9;z<=14;z++) level.setBlock(new BlockPos(x,80,z), Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);
                    if (SEAM) {
                        pair(level,new BlockPos(0,81,0),Direction.Axis.Y,12000,false);
                        pair(level,new BlockPos(1,81,0),Direction.Axis.Y,4000,true);
                    } else {
                    for(int i=0;i<5;i++) single(level,new BlockPos(-6+i*3,81,0),Direction.Axis.Y,i*4000, i==3);
                    for(int i=0;i<3;i++) {
                        pair(level,new BlockPos(-6+i*6,81,-5),Direction.Axis.Y,(i+1)*8000,false);
                        pair(level,new BlockPos(-6+i*5,81,5),Direction.Axis.X,(i+1)*8000,false);
                        pair(level,new BlockPos(-6+i*6,81,10),Direction.Axis.Z,16000,true);
                    }
                    single(level,new BlockPos(10,81,0),Direction.Axis.X,8000,false);
                    single(level,new BlockPos(10,81,4),Direction.Axis.Z,8000,true);
                    }
                    var player=server.getPlayerList().getPlayer(uuid);
                    player.setGameMode(GameType.SPECTATOR); player.teleportTo(19,89,23);
                    seeded=true;
                });
                stage=2;
            } else if(stage==2 && seeded) {
                mc.options.hideGui=true; delay=100; stage=3;
            } else if(stage==3) {
                checkClient(mc);
                REPORT.put("initialSnapshots",EXPECTED.size());
                var server=mc.getSingleplayerServer();
                server.execute(() -> {
                    if (!SEAM) {
                        var barrel=(FuelBarrelBlockEntity)server.overworld().getBlockEntity(new BlockPos(-1,81,5));
                        barrel.getFluidHandler(null).drain(4000,FluidAction.EXECUTE);
                        EXPECTED.replaceAll(e -> e.pos().equals(new BlockPos(-1,81,5)) || e.pos().equals(new BlockPos(0,81,5))
                                ? new Expected(e.pos(),12000,32000,e.part()) : e);
                    }
                    transferred=true;
                });
                stage=4;delay=30;
            } else if(stage==4 && transferred) {
                checkClient(mc); REPORT.put(SEAM ? "seamSnapshotsSynchronized" : "transferUpdatedBothMembers",true);
                for(Direction.Axis axis:Direction.Axis.values()) {
                    float previous=0;
                    for(int i=0;i<=100;i++) {
                        float y=FuelBarrelRenderer.liquidHeight(axis,FuelBarrelBlock.Part.SINGLE,i/100.0);
                        if(y<previous)throw new IllegalStateException("Liquid height reversed");
                        previous=y;
                    }
                }
                REPORT.put("bottomToTopAllAxes",true);
                capture=new CaptureScreen();mc.setScreen(capture);setCamera(mc,0);stage=5;delay=30;
            } else if(stage==5) {
                checkClient(mc);
                UiTooltipCapture.requestScreen(capture,OUTPUT,"fuel-barrel-"+SHOTS[shot]+".png");stage=6;delay=8;
            } else if(stage==6) {
                if(++shot<SHOTS.length){
                    if (SEAM && shot >= 4) {
                        final int amount = shot == 4 ? 0 : 32000;
                        var server=mc.getSingleplayerServer();
                        server.execute(() -> {
                            for(int x=0;x<2;x++) {
                                var b=(FuelBarrelBlockEntity)server.overworld().getBlockEntity(new BlockPos(x,81,0));
                                b.getFluidHandler(null).drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
                                if(amount>0)b.getFluidHandler(null).fill(new FluidStack(x==0?ModFluids.FUEL_OIL.get():ModFluids.CRUDE_OIL.get(),amount),FluidAction.EXECUTE);
                            }
                            EXPECTED.replaceAll(e -> new Expected(e.pos(),amount,32000,e.part()));
                        });
                    }
                    setCamera(mc,shot);stage=5;delay=25;
                }
                else {
                    for(String name:SHOTS)if(!Files.isRegularFile(OUTPUT.resolve("screenshots/fuel-barrel-"+name+".png")))throw new IllegalStateException("Missing screenshot "+name);
                    REPORT.put("screenshots",SHOTS);REPORT.put("passed",true);finish(mc);
                }
            }
        } catch(Exception e){REPORT.put("passed",false);REPORT.put("error",e.toString());finish(mc);}
    }

    private static FuelBarrelBlockEntity place(ServerLevel level,BlockPos pos,Direction.Axis axis){
        level.setBlock(pos,ModBlocks.FUEL_BARREL.get().defaultBlockState().setValue(FuelBarrelBlock.AXIS,axis),3);
        return (FuelBarrelBlockEntity)level.getBlockEntity(pos);
    }

    private static void single(ServerLevel level,BlockPos p,Direction.Axis axis,int amount,boolean crude){
        var barrel=place(level,p,axis);barrel.refreshConnection();
        if(amount>0)barrel.getFluidHandler(null).fill(new FluidStack(crude?ModFluids.CRUDE_OIL.get():ModFluids.FUEL_OIL.get(),amount),FluidAction.EXECUTE);
        EXPECTED.add(new Expected(p,amount,16000,FuelBarrelBlock.Part.SINGLE));
    }

    private static void pair(ServerLevel level,BlockPos p,Direction.Axis axis,int amount,boolean crude){
        var next=p.relative(Direction.get(Direction.AxisDirection.POSITIVE,axis));
        var high=place(level,next,axis);high.refreshConnection();var low=place(level,p,axis);low.refreshConnection();
        high.getFluidHandler(null).fill(new FluidStack(crude?ModFluids.CRUDE_OIL.get():ModFluids.FUEL_OIL.get(),amount),FluidAction.EXECUTE);
        EXPECTED.add(new Expected(p,amount,32000,FuelBarrelBlock.Part.LOWER));
        EXPECTED.add(new Expected(next,amount,32000,FuelBarrelBlock.Part.UPPER));
    }

    private static void checkClient(Minecraft mc){
        checkLiningSeams(mc);
        checkFluidUvCropping(mc);
        for(var e:EXPECTED){
            if(!(mc.level.getBlockEntity(e.pos()) instanceof FuelBarrelBlockEntity b)
                    || b.getDisplayFluid().getAmount()!=e.amount() || b.getDisplayCapacity()!=e.capacity()
                    || b.getBlockState().getValue(FuelBarrelBlock.PART)!=e.part())throw new IllegalStateException("Snapshot mismatch at "+e);
            if(!(mc.getBlockEntityRenderDispatcher().getRenderer(b) instanceof FuelBarrelRenderer))throw new IllegalStateException("Missing renderer");
        }
    }

    // 从实际输出的顶点核对纹理密度及底边锚点，捕捉低液位把整图压扁的回归。
    private static void checkFluidUvCropping(Minecraft mc) {
        if (REPORT.containsKey("fluidUvMeshCases")) return;
        var fluid = new FluidStack(ModFluids.FUEL_OIL.get(), 1000);
        var extension = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite = mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(extension.getStillTexture(fluid));
        float uSize = sprite.getU1() - sprite.getU0(), vSize = sprite.getV1() - sprite.getV0();
        int cases = 0;
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (FuelBarrelBlock.Part part : FuelBarrelBlock.Part.values()) {
                float[][] anchors = new float[4][];
                for (double fill : new double[]{.125, .25, .375, .5, .625, .75, 1}) {
                    var state = ModBlocks.FUEL_BARREL.get().defaultBlockState().setValue(FuelBarrelBlock.AXIS, axis).setValue(FuelBarrelBlock.PART, part);
                    var sample = new FuelBarrelBlockEntity(BlockPos.ZERO, state) {
                        @Override public FluidStack getDisplayFluid() { return fluid.copy(); }
                        @Override public double getDisplayFill() { return fill; }
                    };
                    var captured = new CapturedVertices();
                    new FuelBarrelRenderer().render(sample, 0, new com.mojang.blaze3d.vertex.PoseStack(),
                            type -> captured, 0x00f000f0, 0);
                    if (FuelBarrelRenderer.liquidHeight(axis, part, fill) <= .002F) {
                        if (!captured.vertices.isEmpty()) throw new IllegalStateException("Empty segment emitted fluid vertices");
                        continue;
                    }
                    if (captured.vertices.size() != 24) throw new IllegalStateException("Missing fluid faces");
                    for (float[] v : captured.vertices) {
                        v[3] = (v[3] - sprite.getU0()) / uSize;
                        v[4] = (v[4] - sprite.getV0()) / vSize;
                    }
                    for (int face = 0; face < 6; face++) {
                        for (int edge = 0; edge < 2; edge++) {
                            float[] a = captured.vertices.get(face * 4 + edge), b = captured.vertices.get(face * 4 + edge + 1);
                            double physical = Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
                            double texture = Math.hypot(a[3] - b[3], a[4] - b[4]);
                            if (Math.abs(physical - texture) > .00003) throw new IllegalStateException("Fluid texture stretched at " + axis + "/" + part + "/" + fill);
                        }
                        if (face < 4) {
                            float[] bottom = captured.vertices.get(face * 4 + 1);
                            if (anchors[face] != null && (Math.abs(bottom[3] - anchors[face][3]) > .00003 || Math.abs(bottom[4] - anchors[face][4]) > .00003)) {
                                throw new IllegalStateException("Existing texture shifted after liquid level change");
                            }
                            anchors[face] = bottom.clone();
                        }
                    }
                    cases++;
                }
            }
        }
        REPORT.put("fluidUvMeshCases", cases);
        REPORT.put("fluidTextureDensityPerBlock", 1);
        REPORT.put("fluidUvAnchorsStable", true);
    }

    private static final class CapturedVertices implements com.mojang.blaze3d.vertex.VertexConsumer {
        private final List<float[]> vertices = new ArrayList<>();
        @Override public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) { vertices.add(new float[]{x, y, z, 0, 0}); return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) { float[] last = vertices.getLast(); last[3] = u; last[4] = v; return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) { return this; }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) { return this; }
    }

    // 检查真实烘焙后的四个内衬侧面，避免生成模型仍在共用端内缩。
    private static void checkLiningSeams(Minecraft mc) {
        int checked = 0;
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (var part : new FuelBarrelBlock.Part[]{FuelBarrelBlock.Part.LOWER, FuelBarrelBlock.Part.UPPER}) {
                var state = ModBlocks.FUEL_BARREL.get().defaultBlockState().setValue(FuelBarrelBlock.AXIS, axis).setValue(FuelBarrelBlock.PART, part);
                var model = mc.getBlockRenderer().getBlockModel(state);
                var quads = model.getQuads(state, null, net.minecraft.util.RandomSource.create(42));
                int sides = 0;
                for (var quad : quads) {
                    if (quad.getDirection().getAxis() == axis) continue;
                    int coordinate = axis == Direction.Axis.X ? 0 : axis == Direction.Axis.Y ? 1 : 2;
                    int[] vertices = quad.getVertices();
                    int stride = vertices.length / 4;
                    float edge = part == FuelBarrelBlock.Part.LOWER ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
                    for (int i = 0; i < 4; i++) {
                        float value = Float.intBitsToFloat(vertices[i * stride + coordinate]);
                        edge = part == FuelBarrelBlock.Part.LOWER ? Math.max(edge, value) : Math.min(edge, value);
                    }
                    if (Math.abs(edge - (part == FuelBarrelBlock.Part.LOWER ? 1 : 0)) > .000001F) {
                        throw new IllegalStateException("Lining gap at " + axis + "/" + part + "/" + quad.getDirection());
                    }
                    sides++;
                }
                if (sides != 4) throw new IllegalStateException("Missing lining side at " + axis + "/" + part);
                checked += sides;
            }
        }
        REPORT.put("bakedLiningSidesContinuous", checked);
    }

    private static void setCamera(Minecraft mc,int index){
        var v=CAMERAS[index];double dx=v[3]-v[0],dy=v[4]-(v[1]+mc.player.getEyeHeight()),dz=v[5]-v[2];
        yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
        var uuid=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->server.getPlayerList().getPlayer(uuid).teleportTo(v[0],v[1],v[2]));
    }

    private static void finish(Minecraft mc){
        stage=100;
        try{Files.createDirectories(OUTPUT);Files.writeString(OUTPUT.resolve("world-validation.json"),new GsonBuilder().setPrettyPrinting().create().toJson(REPORT));}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
        mc.stop();
    }

    private static final class CaptureScreen extends Screen {
        private CaptureScreen(){super(Component.literal("Fuel barrel visual check"));}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics graphics,int x,int y,float partialTick){}
    }
}
