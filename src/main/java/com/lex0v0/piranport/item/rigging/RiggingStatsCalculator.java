package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.domain.ship.ShipStats;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

// 只汇总当前装配区域，备用库存和熟练度不产生属性收益。
public final class RiggingStatsCalculator {
    private RiggingStatsCalculator() {}
    public static Optional<ShipStats> calculate(ItemStack core) {
        return RiggingCoreRules.definition(core).map(ship -> {
            var stats = ship.baseStats();
            for (var region : new RiggingRegion[]{RiggingRegion.WEAPONS, RiggingRegion.EQUIPMENT}) {
                var items = RiggingCoreRules.inventory(core, region);
                for (int slot = 0; slot < region.capacity(ship.size()); slot++) {
                    var stack = items.getStackInSlot(slot);
                    if (!stack.isEmpty() && stack.getItem() instanceof RiggingEquipment equipment)
                        stats = stats.add(equipment.riggingStats(stack));
                }
            }
            return stats;
        });
    }
}
