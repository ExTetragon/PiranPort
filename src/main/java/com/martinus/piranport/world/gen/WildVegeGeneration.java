package com.martinus.piranport.world.gen;

import com.martinus.piranport.world.feature.ModPlacedFeatures;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.world.BiomeLoadingEvent;

import java.util.List;
import java.util.Set;

public class WildVegeGeneration {
    public static void generateWildVege(final BiomeLoadingEvent event){
        ResourceKey<Biome> key = ResourceKey.create(Registry.BIOME_REGISTRY, event.getName());
        Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(key);

        if(types.contains(BiomeDictionary.Type.PLAINS)){
            List<Holder<PlacedFeature>> base = event.getGeneration().getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION);

            base.add(ModPlacedFeatures.WILD_VEGE_PLACED);
        }
    }
}
