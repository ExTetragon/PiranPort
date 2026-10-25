package com.lex0v0.piranport;

import net.neoforged.neoforge.common.ModConfigSpec;

// 集中定义可在模组配置界面调整的公共选项。
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("是否输出本模组的额外调试日志；记录实际状态变化，高频操作每5秒汇总，不影响其他模组日志级别",
                    "Enable additional PiranPort debug logs for actual state changes, with frequent operations summarized every 5 seconds. Other mods' logging levels are unchanged.")
            .define("debugLogging", false);

    public static final ModConfigSpec.IntValue FUEL_BARREL_CAPACITY = BUILDER
            .comment("单个燃料桶的容量（mB）；降容保留已有超额流体，双桶容量为此值的两倍",
                    "Capacity of one fuel barrel in mB. Lowering the capacity preserves any existing excess fluid; a connected pair has twice this capacity.")
            .defineInRange("fuelBarrelCapacity", 16_000, 1, Integer.MAX_VALUE / 2);

    public static final ModConfigSpec.BooleanValue FUEL_BARREL_AXIAL_CONNECTION = BUILDER
            .comment("是否允许新放置的燃料桶轴向连接；放置时记录，已有桶不受开关变化影响",
                    "Allow newly placed fuel barrels to connect along their axis. This setting is recorded on placement; changing it does not affect existing barrels.")
            .define("fuelBarrelAxialConnection", true);

    public static final ModConfigSpec.BooleanValue RETAIN_BROKEN_EQUIPMENT = BUILDER
            .comment("是否保留耐久归零的损坏装备；仅影响后续耗损结算，不追溯删除已有损坏件",
                    "Keep equipment when its durability reaches zero. Only future wear is affected; existing broken items are not removed retroactively.")
            .define("retainBrokenEquipment", true);

    public static final ModConfigSpec.BooleanValue ALLOW_QUICK_REPAIR_FROM_PLAYER_INVENTORY = BUILDER
            .comment("是否允许直接消耗玩家背包中的快修；设备栏优先，自动回复触发后续实现",
                    "Allow quick repair consumption from the player inventory after equipment slots. Automatic healing is not implemented yet.")
            .define("allowQuickRepairFromPlayerInventory", false);

    public static final ModConfigSpec.BooleanValue ALLOW_DAMAGE_CONTROL_FROM_PLAYER_INVENTORY = BUILDER
            .comment("是否允许直接消耗玩家背包中的损管；设备栏优先，抵消死亡触发后续实现",
                    "Allow damage control consumption from the player inventory after equipment slots. Death prevention is not implemented yet.")
            .define("allowDamageControlFromPlayerInventory", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
