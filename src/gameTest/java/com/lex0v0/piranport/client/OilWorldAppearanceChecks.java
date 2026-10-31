package com.lex0v0.piranport.client;

import com.google.gson.GsonBuilder;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModFluids;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// 只在新建隔离世界检查油品实景，截图沿用统一渲染阶段入口。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class OilWorldAppearanceChecks {
    private static final Path OUTPUT = Path.of(System.getProperty("piranport.acceptance.oilWorldOutput", "build/oil-world"));
    private static final Map<String, Object> REPORT = new LinkedHashMap<>();
    private static final List<String> SHOTS = List.of("overview", "waterfalls-a", "waterfalls-b", "floating", "pools");
    private static final double[][] CAMERAS = {{22, 104, 28, 0, 82, 1}, {16, 89, -1, -1, 83, -10}, {16, 89, -1, -1, 83, -10}, {16, 91, 24, 0, 82, 12}, {12, 88, 12, 0, 81, 2}};
    private static volatile boolean seeded, checked;
    private static int stage, ticks, delay, shot;
    private static float yaw, pitch;
    private static Screen captureScreen;

    private OilWorldAppearanceChecks() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("piranport.acceptance.oilWorld") || stage == 100) return;
        var mc = Minecraft.getInstance();
        try {
            if (++ticks > 6000) throw new IllegalStateException("Oil world check timed out at stage " + stage);
            if (mc.player != null) {
                mc.player.setYRot(yaw);
                mc.player.setXRot(pitch);
                mc.player.yRotO = yaw;
                mc.player.xRotO = pitch;
            }
            if (UiTooltipCapture.pending()) return;
            if (delay > 0) { delay--; return; }
            if (stage == 0 && AcceptanceClientStartup.titleScreenReady(mc)) {
                Files.createDirectories(OUTPUT);
                String world = "oil-visual-" + System.currentTimeMillis();
                REPORT.put("world", world);
                mc.createWorldOpenFlows().createFreshLevel(world,
                        new LevelSettings(world, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261031L, false, false), registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
                stage = 1;
            } else if (stage == 1 && mc.player != null && mc.level != null && mc.getSingleplayerServer() != null) {
                var server = mc.getSingleplayerServer();
                var uuid = mc.player.getUUID();
                server.execute(() -> {
                    var level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    for (int x = -17; x <= 17; x++) for (int z = -16; z <= 19; z++) {
                        level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
                    }
                    Fluid[] fluids = {ModFluids.CRUDE_OIL.get(), ModFluids.FUEL_OIL.get(), Fluids.WATER};
                    int[] centers = {-9, -1, 7};
                    for (int i = 0; i < fluids.length; i++) {
                        int cx = centers[i];
                        for (int x = cx - 3; x <= cx + 3; x++) for (int z = -1; z <= 5; z++) {
                            boolean rim = x == cx - 3 || x == cx + 3 || z == -1 || z == 5;
                            level.setBlock(new BlockPos(x, 81, z), rim ? Blocks.QUARTZ_BLOCK.defaultBlockState() : fluids[i].defaultFluidState().createLegacyBlock(), 3);
                        }
                        for (int x = cx - 1; x <= cx + 1; x++) for (int z = -11; z <= -9; z++) {
                            for (int y = 81; y <= 85; y++) level.setBlock(new BlockPos(x, y, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
                        }
                        level.setBlock(new BlockPos(cx, 86, -10), fluids[i].defaultFluidState().createLegacyBlock(), 3);
                    }
                    for (int cx : new int[]{-7, 7}) {
                        for (int x = cx - 5; x <= cx + 5; x++) for (int z = 8; z <= 16; z++) {
                            boolean rim = x == cx - 5 || x == cx + 5 || z == 8 || z == 16;
                            level.setBlock(new BlockPos(x, 81, z), rim ? Blocks.QUARTZ_BLOCK.defaultBlockState() : Blocks.WATER.defaultBlockState(), 3);
                        }
                        var oil = cx < 0 ? ModFluids.CRUDE_OIL.get() : ModFluids.FUEL_OIL.get();
                        level.setBlock(new BlockPos(cx, 82, 12), oil.defaultFluidState().createLegacyBlock(), 3);
                    }
                    var player = server.getPlayerList().getPlayer(uuid);
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(22, 104, 28);
                    seeded = true;
                });
                stage = 2;
            } else if (stage == 2 && seeded) {
                mc.options.hideGui = true;
                stage = 3;
                delay = 240;
            } else if (stage == 3) {
                var server = mc.getSingleplayerServer();
                server.execute(() -> {
                    var level = server.overworld();
                    int water = 0;
                    for (int cx : new int[]{-7, 7}) for (int x = cx - 4; x <= cx + 4; x++) for (int z = 9; z <= 15; z++) {
                        if (level.getBlockState(new BlockPos(x, 81, z)).is(Blocks.WATER)) water++;
                    }
                    REPORT.put("waterBlocksPreserved", water == 126);
                    REPORT.put("waterBlockCount", water);
                    checked = true;
                });
                stage = 4;
            } else if (stage == 4 && checked) {
                if (!Boolean.TRUE.equals(REPORT.get("waterBlocksPreserved"))) throw new IllegalStateException("Floating oil changed water blocks");
                captureScreen = new CaptureScreen();
                mc.setScreen(captureScreen);
                setCamera(mc, 0);
                stage = 5;
                delay = 40;
            } else if (stage == 5) {
                UiTooltipCapture.requestScreen(captureScreen, OUTPUT, "oil-world-" + SHOTS.get(shot) + ".png");
                stage = 6;
                delay = 8;
            } else if (stage == 6) {
                if (++shot < SHOTS.size()) {
                    setCamera(mc, shot);
                    stage = 5;
                    delay = 32;
                } else {
                    for (String name : SHOTS) {
                        if (!Files.isRegularFile(OUTPUT.resolve("screenshots/oil-world-" + name + ".png"))) throw new IllegalStateException("Missing screenshot: " + name);
                    }
                    REPORT.put("screenshots", SHOTS);
                    REPORT.put("crudeOilAlpha", 242);
                    REPORT.put("passed", true);
                    finish(mc);
                }
            }
        } catch (Exception error) {
            REPORT.put("passed", false);
            REPORT.put("error", error.toString());
            finish(mc);
        }
    }

    private static void setCamera(Minecraft mc, int index) {
        double[] view = CAMERAS[index];
        double dx = view[3] - view[0], dy = view[4] - (view[1] + mc.player.getEyeHeight()), dz = view[5] - view[2];
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        var uuid = mc.player.getUUID();
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayer(uuid);
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.teleportTo(view[0], view[1], view[2]);
        });
    }

    private static void finish(Minecraft mc) {
        stage = 100;
        try {
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve("world-validation.json"), new GsonBuilder().setPrettyPrinting().create().toJson(REPORT));
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        mc.stop();
    }

    private static final class CaptureScreen extends Screen {
        private CaptureScreen() { super(Component.literal("Oil world capture")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(4, 4, width - 4, 19, 0xa0000000);
            graphics.drawCenteredString(font, "Crude / Fuel / Water | alpha 242 / 180 | " + SHOTS.get(Math.min(shot, SHOTS.size() - 1)), width / 2, 8, 0xffffff);
        }
    }
}
