package com.lex0v0.piranport.client;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

// 仅将原版已有的创造栏归属行移至本模组说明下方。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class ModTooltipEvents {
    private ModTooltipEvents() {}

    @SubscribeEvent
    public static void gatherTooltip(RenderTooltipEvent.GatherComponents event) {
        var stack = event.getItemStack();
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var elements = event.getTooltipElements();
        if (!itemId.getNamespace().equals(PiranPort.MODID) || elements.isEmpty()) return;

        int categoryIndex = -1;
        int lastModLine = 0;
        for (int i = 1; i < elements.size(); i++) {
            if (!(elements.get(i).left().orElse(null) instanceof Component component
                    && component.getContents() instanceof TranslatableContents text)) continue;
            String key = text.getKey();
            if (categoryIndex < 0 && key.equals("itemGroup.piranport.main")) categoryIndex = i;
            else if (key.startsWith("tooltip.piranport.") || key.equals("item.piranport.rigging_core")) lastModLine = i;
        }
        if (categoryIndex >= 0 && categoryIndex < lastModLine)
            elements.add(lastModLine, elements.remove(categoryIndex));
    }
}
