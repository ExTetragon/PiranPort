package com.martinus.piranport.world.feature;

import com.martinus.piranport.registry.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import java.util.List;

public class ModConfiguredFeatures {

    public static final List<OreConfiguration.TargetBlockState> OVERWORLD_BAUXITE_ORES = List.of(
            OreConfiguration.target(OreFeatures.STONE_ORE_REPLACEABLES, ModBlocks.BAUXITE_ORE.get().defaultBlockState()),
            OreConfiguration.target(OreFeatures.DEEPSLATE_ORE_REPLACEABLES, ModBlocks.DEEPSLATE_BAUXITE_ORE.get().defaultBlockState())
    );

    public static final Holder<ConfiguredFeature<OreConfiguration, ?>> BAUXITE_ORE = FeatureUtils.register("bauxite_ore",
            Feature.ORE, new OreConfiguration(OVERWORLD_BAUXITE_ORES,9));

    public static final Holder<ConfiguredFeature<DiskConfiguration, ?>> DISK_SALT = FeatureUtils.register("disk_salt",
            Feature.DISK, new DiskConfiguration(ModBlocks.SALT_BLOCK.get().defaultBlockState(), UniformInt.of(1, 2), 1,
                    List.of(Blocks.DIRT.defaultBlockState(), Blocks.CLAY.defaultBlockState())));
}
