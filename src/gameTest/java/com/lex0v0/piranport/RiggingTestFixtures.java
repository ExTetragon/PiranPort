package com.lex0v0.piranport;

import com.lex0v0.piranport.item.rigging.RiggingEquipment;
import com.lex0v0.piranport.domain.ship.ShipStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

// 装备属性夹具只存在于测试源集，不进入生产目录或发行包。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class RiggingTestFixtures {
    public static Item positive;
    public static Item negative;
    private RiggingTestFixtures() {}
    private static final class Equipment extends Item implements RiggingEquipment {
        private final ShipStats stats;
        Equipment(ShipStats stats) { super(new Item.Properties().stacksTo(1)); this.stats = stats; }
        @Override public ShipStats riggingStats(ItemStack stack) { return stats; }
    }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            positive = new Equipment(new ShipStats(8, 2, 0.5));
            negative = new Equipment(new ShipStats(-2, -1, -0.75));
            helper.register(ResourceLocation.fromNamespaceAndPath("piranport_test", "stat_positive"), positive);
            helper.register(ResourceLocation.fromNamespaceAndPath("piranport_test", "stat_negative"), negative);
        });
    }
}
