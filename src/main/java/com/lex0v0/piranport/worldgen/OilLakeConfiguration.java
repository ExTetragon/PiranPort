package com.lex0v0.piranport.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

// 定义油湖流体、围壳、半径与深度，并校验半径上下界。
public record OilLakeConfiguration(
        BlockStateProvider fluid,
        BlockStateProvider barrier,
        int minRadius,
        int maxRadius,
        int depth
) implements FeatureConfiguration {
    public static final Codec<OilLakeConfiguration> CODEC = RecordCodecBuilder.<OilLakeConfiguration>create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("fluid").forGetter(OilLakeConfiguration::fluid),
            BlockStateProvider.CODEC.fieldOf("barrier").forGetter(OilLakeConfiguration::barrier),
            Codec.intRange(1, 32).fieldOf("min_radius").orElse(4).forGetter(OilLakeConfiguration::minRadius),
            Codec.intRange(1, 32).fieldOf("max_radius").orElse(8).forGetter(OilLakeConfiguration::maxRadius),
            Codec.intRange(1, 32).fieldOf("depth").orElse(4).forGetter(OilLakeConfiguration::depth)
    ).apply(instance, OilLakeConfiguration::new)).validate(config -> config.minRadius() <= config.maxRadius()
            ? DataResult.success(config) : DataResult.error(() -> "minimum must not exceed maximum"));
}
