package com.lex0v0.piranport.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * 硝土块生成配置。
 *
 * @param block     放置的方块
 * @param tries     每次调用尝试的候选点数量
 * @param minCount  单个候选点最少生成数量
 * @param maxCount  单个候选点最多生成数量
 * @param xzSpread  水平随机偏移
 * @param ySpread   垂直随机偏移
 */
public record SaltpeterPatchConfiguration(
        BlockStateProvider block,
        int tries,
        int minCount,
        int maxCount,
        int xzSpread,
        int ySpread
) implements FeatureConfiguration {
    public static final Codec<SaltpeterPatchConfiguration> CODEC = RecordCodecBuilder.<SaltpeterPatchConfiguration>create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(SaltpeterPatchConfiguration::block),
            Codec.intRange(1, 64).fieldOf("tries").orElse(8).forGetter(SaltpeterPatchConfiguration::tries),
            Codec.intRange(1, 16).fieldOf("min_count").orElse(1).forGetter(SaltpeterPatchConfiguration::minCount),
            Codec.intRange(1, 16).fieldOf("max_count").orElse(3).forGetter(SaltpeterPatchConfiguration::maxCount),
            Codec.intRange(1, 16).fieldOf("xz_spread").orElse(4).forGetter(SaltpeterPatchConfiguration::xzSpread),
            Codec.intRange(0, 16).fieldOf("y_spread").orElse(2).forGetter(SaltpeterPatchConfiguration::ySpread)
    ).apply(instance, SaltpeterPatchConfiguration::new)).validate(config -> config.minCount() <= config.maxCount()
            ? DataResult.success(config) : DataResult.error(() -> "minimum must not exceed maximum"));
}
