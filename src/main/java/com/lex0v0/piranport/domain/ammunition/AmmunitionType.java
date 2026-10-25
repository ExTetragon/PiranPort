package com.lex0v0.piranport.domain.ammunition;

import net.minecraft.resources.ResourceLocation;

// 弹种身份和制造点数固定在定义中，不写入单枚组件或随配方重算。
public enum AmmunitionType {
    HIGH_EXPLOSIVE_SHELL("high_explosive_shell", AmmunitionCategory.SHELLS, 2),
    ARMOR_PIERCING_SHELL("armor_piercing_shell", AmmunitionCategory.SHELLS, 3),
    ANTI_AIRCRAFT_SHELL("anti_aircraft_shell", AmmunitionCategory.SHELLS, 3),
    DEPTH_CHARGE("depth_charge", AmmunitionCategory.DEPTH_CHARGES, 1),
    STRAIGHT_RUNNING_TORPEDO("straight_running_torpedo", AmmunitionCategory.TORPEDOES, 3),
    GUIDED_TORPEDO("guided_torpedo", AmmunitionCategory.TORPEDOES, 4),
    ANTI_SHIP_MISSILE("anti_ship_missile", AmmunitionCategory.MISSILES, 8),
    ANTI_AIRCRAFT_MISSILE("anti_aircraft_missile", AmmunitionCategory.MISSILES, 6);

    private final ResourceLocation id;
    private final AmmunitionCategory category;
    private final int materialPoints;

    AmmunitionType(String path, AmmunitionCategory category, int materialPoints) {
        this.id = ResourceLocation.fromNamespaceAndPath("piranport", path);
        this.category = category;
        this.materialPoints = materialPoints;
    }

    public ResourceLocation id() { return id; }
    public AmmunitionCategory category() { return category; }
    public int materialPoints() { return materialPoints; }
    public String translationKey() { return "item.piranport." + id.getPath(); }
}
