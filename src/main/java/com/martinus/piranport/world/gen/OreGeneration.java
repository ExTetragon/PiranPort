package com.martinus.piranport.world.gen;

import com.martinus.piranport.registry.ModBlocks;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.*;
import net.minecraftforge.event.world.BiomeLoadingEvent;

public class OreGeneration {

    public static void GenerateOre(final BiomeLoadingEvent event){

        event.getGeneration().addFeature(GenerationStage.Decoration.UNDERGROUND_ORES,
                Feature.ORE.configured(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.NATURAL_STONE, ModBlocks.BAUXITE_ORE.get().defaultBlockState(),9))
                        .range(64)
                        .squared()
                        .count(20)
        );

        event.getGeneration().addFeature(GenerationStage.Decoration.SURFACE_STRUCTURES,
                Feature.DISK.configured(new SphereReplaceConfig(ModBlocks.SALT_BLOCK.get().defaultBlockState(),
                        FeatureSpread.of(1,2),1,
                        ImmutableList.of(Blocks.CLAY.defaultBlockState(),Blocks.DIRT.defaultBlockState())))
                        .decorated(Features.Placements.TOP_SOLID_HEIGHTMAP_SQUARE)
        );
    }
}
