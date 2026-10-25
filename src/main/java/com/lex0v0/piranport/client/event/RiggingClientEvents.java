package com.lex0v0.piranport.client.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.RiggingCoreItem;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.item.rigging.RiggingTooltips;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// 客户端改写核心标题颜色并切换详情，归属行由通用提示事件处理。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class RiggingClientEvents {
    private RiggingClientEvents() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        applyTooltip(event.getItemStack(), event.getToolTip(), Screen.hasShiftDown());
    }
    public static void applyTooltip(ItemStack stack, List<Component> lines, boolean shift) {
        if (!RiggingCoreRules.isCore(stack) || lines.isEmpty()) return;
        lines.set(0, lines.getFirst().copy().withStyle(style -> style.withColor(RiggingCoreItem.NAME_COLOR)));
        if (!shift) return;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).getContents() instanceof TranslatableContents text && text.getKey().equals("item.piranport.rigging_core")) {
                lines.remove(i);
                lines.addAll(i, RiggingTooltips.details(stack));
                return;
            }
        }
    }
}
