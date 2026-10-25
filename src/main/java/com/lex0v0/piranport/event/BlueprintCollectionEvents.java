package com.lex0v0.piranport.event;

import com.lex0v0.piranport.logging.ModLog;
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
        int valid = 0;
        for (var stack : inventory.items) {
            if (BlueprintRules.isValid(stack)) { valid++; CriteriaTriggers.INVENTORY_CHANGED.trigger(player, inventory, stack); }
        }
        for (var stack : inventory.offhand) {
            if (BlueprintRules.isValid(stack)) { valid++; CriteriaTriggers.INVENTORY_CHANGED.trigger(player, inventory, stack); }
        }
        ModLog.debug(ModLog.Event.BLUEPRINT_LOGIN, "player", player.getUUID(), "slots", inventory.items.size()+inventory.offhand.size(), "valid", valid);
    }

    @SubscribeEvent
    public static void onCraft(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide || !BlueprintRules.isValid(event.getCrafting())) return;
        ModLog.debug(ModLog.Event.BLUEPRINT_CRAFT, "player", event.getEntity().getUUID(),
                "item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()), "count", event.getCrafting().getCount());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) checkExistingInventory(player);
    }
}
