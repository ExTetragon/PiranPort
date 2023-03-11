package com.martinus.piranport.world.feature;

import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ModPlacedFeatures {

    public static final Holder<PlacedFeature> BAUXITE_ORE_PLACED = PlacementUtils.register("bauxite_ore_placed",
            ModConfiguredFeatures.BAUXITE_ORE, ModOrePlacement.commonOrePlacement(16,
                    HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(40),VerticalAnchor.aboveBottom(128))));

    public static final Holder<PlacedFeature> DISK_SALT_PLACED = PlacementUtils.register("disk_salt_placed",
            ModConfiguredFeatures.DISK_SALT, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome());
}
