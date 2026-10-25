package com.lex0v0.piranport;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.registry.ModBlocks;
import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

// 独立采样自然生成，仅显式启用且不进入发行包。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class WorldgenAcceptance {
    private static final java.util.Map<ResourceKey<Level>, java.util.Set<Long>> VISITED = new java.util.HashMap<>();
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("piranport.acceptance.worldgen")) return;
        var server = event.getServer();
        VISITED.clear();
        var report = new java.util.LinkedHashMap<String, Object>();
        var regions = new java.util.ArrayList<java.util.Map<String, Object>>();
        long started = System.nanoTime();
        try {
            report.put("seed", server.overworld().getSeed());
            report.put("scope", "Natural FULL chunks; sampled regions only. Counts are blocks/chunks, not independent lakes or rarity attempts.");
            regions.add(sample(server.overworld(), "overworld-origin", BlockPos.ZERO, 16));
            for (var biome : java.util.List.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.SAVANNA)) {
                var found = server.overworld().findClosestBiome3d(h -> h.is(biome), new BlockPos(0, 64, 0), 6400, 32, 64);
                if (found == null) regions.add(java.util.Map.of("name", biome.location().toString(), "notFoundWithinRadius", 6400));
                else regions.add(sample(server.overworld(), biome.location().toString(), found.getFirst(), 8));
            }
            regions.add(sample(server.getLevel(Level.NETHER), "nether-origin", BlockPos.ZERO, 8));
            regions.add(sample(server.getLevel(Level.END), "end-origin", BlockPos.ZERO, 8));
            boolean valid = ((Number) regions.getFirst().get("bauxiteBlocks")).longValue() > 0;
            for (var region : regions) {
                if (!"minecraft:overworld".equals(region.get("dimension")) && region.containsKey("bauxiteBlocks"))
                    valid &= ((Number) region.get("bauxiteBlocks")).longValue() == 0;
                if ("minecraft:the_end".equals(region.get("dimension")))
                    valid &= ((Number) region.get("saltpeterBlocks")).longValue() == 0 && ((Number) region.get("oilBlocks")).longValue() == 0;
            }
            report.put("uniqueSampledChunks", regions.stream().filter(r -> r.containsKey("sampledChunks"))
                    .mapToInt(r -> ((Number) r.get("sampledChunks")).intValue()).sum());
            report.put("basicDimensionChecksPassed", valid);
            report.put("completed", true);
        } catch (Throwable error) {
            ModLog.failure(ModLog.Failure.WORLDGEN, error);
            report.put("completed", false);
            report.put("error", error.toString());
        } finally {
            report.put("regions", regions);
            report.put("elapsedSeconds", (System.nanoTime() - started) / 1_000_000_000.0);
            try {
                var path = java.nio.file.Path.of(System.getProperty("piranport.acceptance.worldgenReport"));
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
                ModLog.evidence(ModLog.Evidence.NATURAL_REPORT, path);
            } catch (Exception error) { ModLog.failure(ModLog.Failure.REPORT, error); }
            server.halt(false);
        }
    }

    private static java.util.Map<String, Object> sample(ServerLevel level, String name, BlockPos center, int width) {
        var result = new java.util.LinkedHashMap<String, Object>();
        var biomes = new java.util.TreeMap<String, Long>();
        long bauxite = 0, saltpeter = 0, oil = 0, oilChunks = 0;
        int sampled = 0;
        var visited = VISITED.computeIfAbsent(level.dimension(), key -> new java.util.HashSet<>());
        int minBauxiteY = Integer.MAX_VALUE, maxBauxiteY = Integer.MIN_VALUE;
        int minSaltY = Integer.MAX_VALUE, maxSaltY = Integer.MIN_VALUE;
        long started = System.nanoTime();
        int startX = (center.getX() >> 4) - width / 2, startZ = (center.getZ() >> 4) - width / 2;
        for (int x = startX; x < startX + width; x++) for (int z = startZ; z < startZ + width; z++) {
            if (!visited.add(net.minecraft.world.level.ChunkPos.asLong(x, z))) continue;
            sampled++;
            var chunk = level.getChunk(x, z);
            boolean hasOil = false;
            for (int section = 0; section < chunk.getSections().length; section++) {
                var data = chunk.getSections()[section];
                if (data.hasOnlyAir()) continue;
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) for (int dy = 0; dy < 16; dy++) {
                    var state = data.getBlockState(dx, dy, dz);
                    int y = level.getMinBuildHeight() + section * 16 + dy;
                    if (state.is(ModBlocks.BAUXITE_ORE.get())) {
                        bauxite++;
                        minBauxiteY = Math.min(minBauxiteY, y); maxBauxiteY = Math.max(maxBauxiteY, y);
                    } else if (state.is(ModBlocks.SALTPETER_BLOCK.get())) {
                        saltpeter++;
                        minSaltY = Math.min(minSaltY, y); maxSaltY = Math.max(maxSaltY, y);
                    } else if (state.is(ModBlocks.CRUDE_OIL_BLOCK.get())) { oil++; hasOil = true; }
                }
            }
            if (hasOil) oilChunks++;
            String biome = level.getBiome(new BlockPos(x * 16 + 8, 64, z * 16 + 8)).unwrapKey().orElseThrow().location().toString();
            biomes.merge(biome, 1L, Long::sum);
        }
        result.put("name", name); result.put("dimension", level.dimension().location().toString());
        result.put("startChunkX", startX); result.put("startChunkZ", startZ); result.put("width", width);
        result.put("requestedChunks", width * width); result.put("sampledChunks", sampled); result.put("biomesAtY64", biomes);
        result.put("bauxiteBlocks", bauxite); result.put("saltpeterBlocks", saltpeter); result.put("oilBlocks", oil);
        result.put("chunksContainingOil", oilChunks);
        if (bauxite > 0) { result.put("bauxiteMinY", minBauxiteY); result.put("bauxiteMaxY", maxBauxiteY); }
        if (saltpeter > 0) { result.put("saltpeterMinY", minSaltY); result.put("saltpeterMaxY", maxSaltY); }
        result.put("generationAndScanSeconds", (System.nanoTime() - started) / 1_000_000_000.0);
        ModLog.evidence(ModLog.Evidence.NATURAL_REGION, new com.google.gson.Gson().toJson(result));
        return result;
    }
}
