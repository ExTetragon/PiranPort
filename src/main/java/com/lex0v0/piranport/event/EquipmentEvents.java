package com.lex0v0.piranport.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.EquipmentItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

// 维修另行设计，先禁止两件装备在铁砧中混修，单件改名仍走原版。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class EquipmentEvents {
    private EquipmentEvents() {}
    @SubscribeEvent public static void anvil(AnvilUpdateEvent event) {
        if (event.getLeft().getItem() instanceof EquipmentItem && event.getRight().getItem() instanceof EquipmentItem) event.setCanceled(true);
    }
}
