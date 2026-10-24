package com.lex0v0.piranport.client;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.item.equipment.EquipmentTooltips;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// 仅在客户端读取 Shift，不改写标题及原版归属行。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class EquipmentClientEvents {
    private EquipmentClientEvents() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) { applyTooltip(event.getItemStack(), event.getToolTip(), Screen.hasShiftDown()); }
    public static void applyTooltip(ItemStack stack, List<Component> lines, boolean shift) {
        if (!shift || !(stack.getItem() instanceof EquipmentItem)) return;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).getContents() instanceof TranslatableContents text && text.getKey().equals("tooltip.piranport.equipment.hold_shift")) {
                lines.remove(i); lines.addAll(i, EquipmentTooltips.details(stack)); return;
            }
        }
    }
}
