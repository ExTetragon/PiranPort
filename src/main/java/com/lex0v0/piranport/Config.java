package com.lex0v0.piranport;

import net.neoforged.neoforge.common.ModConfigSpec;

// 集中定义可在模组配置界面调整的公共选项。
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("是否输出额外的调试日志",
                    "Whether to output additional debug logs.")
            .define("debugLogging", false);

    public static final ModConfigSpec.IntValue FUEL_BARREL_CAPACITY = BUILDER
            .comment("单个燃料桶的容量（mB）；降容保留已有超额流体，双桶容量为此值的两倍",
                    "Capacity of one fuel barrel in mB. Lowering the capacity preserves any existing excess fluid; a connected pair has twice this capacity.")
            .defineInRange("fuelBarrelCapacity", 16_000, 1, Integer.MAX_VALUE / 2);

    public static final ModConfigSpec.BooleanValue FUEL_BARREL_AXIAL_CONNECTION = BUILDER
            .comment("是否允许新放置的燃料桶轴向连接；放置时记录，已有桶不受开关变化影响",
                    "Allow newly placed fuel barrels to connect along their axis. This setting is recorded on placement; changing it does not affect existing barrels.")
            .define("fuelBarrelAxialConnection", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
