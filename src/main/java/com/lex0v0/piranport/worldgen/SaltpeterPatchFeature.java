package com.lex0v0.piranport.worldgen;

import com.lex0v0.piranport.logging.ModLog;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

// 在遮蔽、无流体且紧贴固体面的洞穴空气中生成一簇硝土。
public class SaltpeterPatchFeature extends Feature<SaltpeterPatchConfiguration> {
    public SaltpeterPatchFeature(Codec<SaltpeterPatchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SaltpeterPatchConfiguration> context) {
        boolean placed = placeFeature(context);
        ModLog.aggregate(ModLog.Event.SALTPETER, 0, placed);
        return placed;
    }

    private boolean placeFeature(FeaturePlaceContext<SaltpeterPatchConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        SaltpeterPatchConfiguration config = context.config();
        boolean placed = false;

        for (int attempt = 0; attempt < config.tries(); attempt++) {
            int dx = random.nextInt(config.xzSpread() * 2 + 1) - config.xzSpread();
            int dz = random.nextInt(config.xzSpread() * 2 + 1) - config.xzSpread();
            int dy = random.nextInt(config.ySpread() * 2 + 1) - config.ySpread();
            BlockPos candidate = origin.offset(dx, dy, dz);

            if (!canPlaceAt(level, candidate)) {
                continue;
            }

            int count = config.minCount() + random.nextInt(config.maxCount() - config.minCount() + 1);
            for (int i = 0; i < count; i++) {
                BlockPos target = candidate.offset(
                        random.nextInt(3) - 1,
                        random.nextInt(3) - 1,
                        random.nextInt(3) - 1);
                if (canPlaceAt(level, target)) {
                    level.setBlock(target, config.block().getState(random, target), 2);
                    placed = true;
                }
            }
            if (placed) return true; // 单次调用最多生成一簇。
        }

        return placed;
    }

    private static boolean canPlaceAt(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos) || level.canSeeSky(pos)
                || !level.isEmptyBlock(pos) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }
}
