package com.lex0v0.piranport.domain.ship;

// 共用舰船属性值，承载基础值、装备修正与汇总结果，允许负修正。
public record ShipStats(double durability, double armor, double speed) {
    public static final ShipStats ZERO = new ShipStats(0, 0, 0);
    public ShipStats {
        if (!Double.isFinite(durability) || !Double.isFinite(armor) || !Double.isFinite(speed))
            throw new IllegalArgumentException("Non-finite ship stats");
    }
    public ShipStats add(ShipStats other) {
        return new ShipStats(durability + other.durability, armor + other.armor, speed + other.speed);
    }
}
