package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

// 舰装区域按物品标签分类，后续装备和数据包可补充成员。
public final class ModItemTags {
    public static final TagKey<Item> WEAPONS = tag("weapons");
    public static final TagKey<Item> AIRCRAFT = tag("aircraft");
    public static final TagKey<Item> DEVICES = tag("devices");
    public static final TagKey<Item> EQUIPMENT_CONSUMABLES = tag("equipment_consumables");
    public static final TagKey<Item> AMMUNITION = tag("ammunition");
    public static final TagKey<Item> SHELLS = tag("ammunition/shells");
    public static final TagKey<Item> DEPTH_CHARGES = tag("ammunition/depth_charges");
    public static final TagKey<Item> TORPEDOES = tag("ammunition/torpedoes");
    public static final TagKey<Item> MISSILES = tag("ammunition/missiles");

    public static TagKey<Item> ammunition(com.lex0v0.piranport.domain.ammunition.AmmunitionCategory category) {
        return switch (category) {
            case SHELLS -> SHELLS;
            case DEPTH_CHARGES -> DEPTH_CHARGES;
            case TORPEDOES -> TORPEDOES;
            case MISSILES -> MISSILES;
        };
    }
    private ModItemTags() {}
    private static TagKey<Item> tag(String path) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path)); }
}
