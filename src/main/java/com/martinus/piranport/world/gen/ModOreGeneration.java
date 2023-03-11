package com.martinus.piranport.world.gen;

import com.martinus.piranport.world.feature.ModPlacedFeatures;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import java.util.List;

public class ModOreGeneration {
    public static void generateOres(final BiomeLoadingEvent event){
        List<Holder<PlacedFeature>> bauxite = event.getGeneration().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);
        List<Holder<PlacedFeature>> salt = event.getGeneration().getFeatures(GenerationStep.Decoration.SURFACE_STRUCTURES);

        bauxite.add(ModPlacedFeatures.BAUXITE_ORE_PLACED);
        salt.add(ModPlacedFeatures.DISK_SALT_PLACED);
    }

}
