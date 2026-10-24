package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.domain.ship.ShipStats;
import net.minecraft.world.item.ItemStack;

// 后续装备提供属性修正，物品标签本身不隐式产生属性。
public interface RiggingEquipment {
    ShipStats riggingStats(ItemStack stack);
}
