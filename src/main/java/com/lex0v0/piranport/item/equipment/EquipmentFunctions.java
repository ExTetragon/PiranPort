package com.lex0v0.piranport.item.equipment;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;

// 作战与设备功能只提供查询边界，空值明确表示尚无实现或参数。
public interface EquipmentFunctions {
    interface CombatProfile {}
    interface DeviceProfile {}
    default Optional<CombatProfile> combatProfile(ItemStack stack) { return Optional.empty(); }
    default Optional<DeviceProfile> deviceProfile(ItemStack stack) { return Optional.empty(); }
}
