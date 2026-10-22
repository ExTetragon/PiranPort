package com.lex0v0.piranport.worldgen;

import java.util.LinkedHashSet;
import java.util.Set;
import com.lex0v0.piranport.PiranPort;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** 先构建并验证整个油湖，再统一写入。只替换明确允许的天然地形。 */
public class OilLakeFeature extends Feature<OilLakeConfiguration> {
    private static final int FLOOR_SEARCH_DISTANCE = 24;
    private static final TagKey<Block> REPLACEABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "oil_lake_replaceable"));

    public OilLakeFeature(Codec<OilLakeConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<OilLakeConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OilLakeConfiguration config = context.config();
        BlockPos floor = findFloor(level, context.origin());
        if (floor == null) return false;
        int radius = random.nextIntBetweenInclusive(config.minRadius(), config.maxRadius());
        Set<BlockPos> fluid = new LinkedHashSet<>();
        Set<BlockPos> shell = new LinkedHashSet<>();
        Set<BlockPos> clearance = new LinkedHashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = (double) (dx * dx + dz * dz) / (radius * radius);
                if (distance >= 1.0) continue;
                int depth = Math.max(1, (int) Math.round(config.depth() * (1.0 - distance)));
                BlockPos surface = floor.offset(dx, 0, dz);
                // 湖面略低于中心地面，允许清除少量天然起伏，避免只在完全平坦地形生成。
                for (int up = 1; up <= 3; up++) {
                    BlockPos above = surface.above(up);
                    if (level.isOutsideBuildHeight(above) || !level.ensureCanWrite(above)) return false;
                    BlockState cover = level.getBlockState(above);
                    if (cover.hasBlockEntity() || !cover.getFluidState().isEmpty()
                            || !(cover.isAir() || cover.is(REPLACEABLE) || cover.canBeReplaced())) return false;
                    if (!cover.isAir()) clearance.add(above);
                }
                if (!level.isEmptyBlock(surface.above(4))) return false;
                for (int dy = 0; dy < depth; dy++) fluid.add(surface.below(dy));
            }
        }
        for (BlockPos pos : fluid) {
            for (Direction side : Direction.values()) {
                if (side == Direction.UP) continue;
                BlockPos edge = pos.relative(side);
                if (!fluid.contains(edge)) shell.add(edge);
            }
        }
        for (BlockPos pos : fluid) if (!safeTerrain(level, pos)) return false;
        for (BlockPos pos : shell) if (!safeTerrain(level, pos)) return false;
        // 此前没有任何世界写入。失败时整座湖不生成，不留下部分流体或破坏结构。
        for (BlockPos pos : clearance) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos pos : shell) level.setBlock(pos, config.barrier().getState(random, pos), Block.UPDATE_CLIENTS);
        for (BlockPos pos : fluid) level.setBlock(pos, config.fluid().getState(random, pos), Block.UPDATE_CLIENTS);
        return !fluid.isEmpty();
    }

    private static boolean safeTerrain(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return !state.hasBlockEntity() && state.getFluidState().isEmpty() && state.is(REPLACEABLE);
    }

    private static BlockPos findFloor(WorldGenLevel level, BlockPos origin) {
        for (int i = 0; i <= FLOOR_SEARCH_DISTANCE; i++) {
            BlockPos candidate = origin.below(i);
            if (level.isOutsideBuildHeight(candidate)) continue;
            if (level.getBlockState(candidate).isFaceSturdy(level, candidate, Direction.UP)
                    && level.isEmptyBlock(candidate.above())) return candidate.below();
        }
        return null;
    }
}
