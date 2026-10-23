package com.lex0v0.piranport;

import com.lex0v0.piranport.registry.*;
import com.lex0v0.piranport.worldgen.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 验证硝土生成边界及固定种子油湖形状。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void fixedSeedOilShapes(GameTestHelper h) {
        var results = new java.util.LinkedHashMap<String, Object>();
        for (var material : java.util.List.of("sandstone", "red_sandstone", "stone")) {
            for (boolean underground : new boolean[]{false, true}) {
                String id = "oil_lake_" + material + (underground ? "_underground" : "");
                var configured = h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .get(ResourceLocation.parse("piranport:" + id));
                var config = (OilLakeConfiguration) configured.config();
                int minVolume = Integer.MAX_VALUE, maxVolume = 0;
                for (int seed = 0; seed < 16; seed++) {
                    for (var p : BlockPos.betweenClosed(1, 0, 1, 23, 9, 23))
                        h.getLevel().setBlock(h.absolutePos(p), (p.getY() <= 5 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
                    h.assertTrue(configured.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(),
                            RandomSource.create(20261022L + seed), h.absolutePos(new BlockPos(12, 6, 12))), "flat terrain succeeds: " + id);
                    int volume = 0;
                    for (var p : BlockPos.betweenClosed(1, 0, 1, 23, 5, 23)) if (h.getBlockState(p).is(ModBlocks.CRUDE_OIL_BLOCK.get())) {
                        volume++;
                        h.assertTrue(Math.abs(p.getX() - 12) < config.maxRadius() && Math.abs(p.getZ() - 12) < config.maxRadius(), "radius bound");
                        for (var side : Direction.values()) if (side != Direction.UP) {
                            var edgePos = p.relative(side);
                            var edge = h.getBlockState(edgePos);
                            h.assertTrue(edge.is(ModBlocks.CRUDE_OIL_BLOCK.get()) || edge.equals(config.barrier().getState(RandomSource.create(0), h.absolutePos(edgePos))),
                                    "every oil block has continuous side/floor: " + id);
                        }
                    }
                    h.assertTrue(volume > 0, "nonempty lake");
                    minVolume = Math.min(minVolume, volume);
                    maxVolume = Math.max(maxVolume, volume);
                }
                results.put(id, java.util.Map.of("seeds", 16, "minVolume", minVolume, "maxVolume", maxVolume,
                        "minRadius", config.minRadius(), "maxRadius", config.maxRadius()));
            }
        }
        System.out.println("PIRANPORT_WORLDGEN_SHAPES=" + new com.google.gson.Gson().toJson(results));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void saltpeterSingleClusterUnderRoof(GameTestHelper h) {
        var feature = h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.parse("piranport:saltpeter_patch"));
        int successes = 0, maximum = 0;
        for (int seed = 0; seed < 64; seed++) {
            for (var p : BlockPos.betweenClosed(1, 1, 1, 21, 7, 21))
                h.getLevel().setBlock(h.absolutePos(p), (p.getY() == 1 || p.getY() >= 6 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
            boolean placed = feature.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(),
                    RandomSource.create(seed), h.absolutePos(new BlockPos(11, 3, 11)));
            int count = 0;
            for (var p : BlockPos.betweenClosed(1, 1, 1, 21, 7, 21)) if (h.getBlockState(p).is(ModBlocks.SALTPETER_BLOCK.get())) {
                count++;
                h.assertTrue(!h.getLevel().canSeeSky(h.absolutePos(p)), "saltpeter remains under roof");
            }
            h.assertTrue(count <= 3 && placed == (count > 0), "one call creates at most 3 blocks");
            if (placed) successes++;
            maximum = Math.max(maximum, count);
        }
        h.assertTrue(successes > 0, "valid cave must produce at least one successful sample");
        System.out.println("PIRANPORT_SALTPETER_SAMPLES=" + new com.google.gson.Gson().toJson(java.util.Map.of("seeds", 64, "successes", successes, "maxBlocks", maximum)));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void worldHeightAndInvalidRanges(GameTestHelper h) {
        var oilConfig = new OilLakeConfiguration(BlockStateProvider.simple(ModBlocks.CRUDE_OIL_BLOCK.get()),
                BlockStateProvider.simple(Blocks.SANDSTONE), 4, 8, 4);
        var json = OilLakeConfiguration.CODEC.encodeStart(JsonOps.INSTANCE, oilConfig).getOrThrow().getAsJsonObject();
        json.addProperty("min_radius", 9);
        h.assertTrue(OilLakeConfiguration.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), "reject inverted oil radius");
        var salt = new SaltpeterPatchConfiguration(BlockStateProvider.simple(ModBlocks.SALTPETER_BLOCK.get()), 8, 1, 3, 4, 2);
        json = SaltpeterPatchConfiguration.CODEC.encodeStart(JsonOps.INSTANCE, salt).getOrThrow().getAsJsonObject();
        json.addProperty("min_count", 4);
        h.assertTrue(SaltpeterPatchConfiguration.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(), "reject inverted salt count");
        for (int y : new int[]{h.getLevel().getMinBuildHeight() - 100, h.getLevel().getMaxBuildHeight() + 100}) {
            var pos = new BlockPos(h.absolutePos(new BlockPos(12, 2, 12)).getX(), y, h.absolutePos(new BlockPos(12, 2, 12)).getZ());
            h.assertTrue(!new ConfiguredFeature<>(ModFeatures.OIL_LAKE.get(), oilConfig).place(h.getLevel(),
                    h.getLevel().getChunkSource().getGenerator(), RandomSource.create(1), pos), "outside height oil is rejected");
            h.assertTrue(!new ConfiguredFeature<>(ModFeatures.SALTPETER_PATCH.get(), salt).place(h.getLevel(),
                    h.getLevel().getChunkSource().getGenerator(), RandomSource.create(1), pos), "outside height salt is rejected");
        }
        h.succeed();
    }
}
