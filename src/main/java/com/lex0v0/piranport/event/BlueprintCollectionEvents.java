package com.lex0v0.piranport.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// 监听玩家登录，补判已有图纸的累计收集进度。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class BlueprintCollectionEvents {
    private BlueprintCollectionEvents() {}

    // 登录时仅补扫直接物品栏和副手，不逐 tick 扫描或递归容器。
    public static void checkExistingInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        for (var stack : inventory.items) {
            if (BlueprintRules.isValid(stack)) CriteriaTriggers.INVENTORY_CHANGED.trigger(player, inventory, stack);
        }
        for (var stack : inventory.offhand) {
            if (BlueprintRules.isValid(stack)) CriteriaTriggers.INVENTORY_CHANGED.trigger(player, inventory, stack);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) checkExistingInventory(player);
    }
}
