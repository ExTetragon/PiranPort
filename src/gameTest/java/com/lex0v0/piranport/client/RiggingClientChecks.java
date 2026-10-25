package com.lex0v0.piranport.client;

import com.lex0v0.piranport.client.event.RiggingClientEvents;

import com.lex0v0.piranport.item.RiggingCoreItem;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 主菜单验证逐舰模型、实际标题颜色和双语详情，不代替世界内人工操作。
final class RiggingClientChecks {
    private RiggingClientChecks() {}
    static boolean verify(Minecraft minecraft) {
        var models = new HashSet<net.minecraft.client.resources.model.BakedModel>();
        for (var ship : ShipDefinitions.ALL) {
            var stack = RiggingCoreRules.create(ship.id());
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()) return false;
            if (!model.getParticleIcon().contents().name().equals(ResourceLocation.fromNamespaceAndPath("piranport", "item/rigging_core/" + ship.id().getPath()))) return false;
            models.add(model);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"));
            RiggingCoreRules.setProficiency(stack, 200);
            if (minecraft.getItemRenderer().getModel(stack, null, null, 0) != model) return false;
        }
        if (models.size() != 10) return false;
        var fallback = minecraft.getItemRenderer().getModel(new ItemStack(ModItems.RIGGING_CORE.get()), null, null, 0);
        if (fallback == minecraft.getModelManager().getMissingModel() || models.contains(fallback)) return false;
        var previous = Language.getInstance();
        try {
            for (String code : List.of("en_us", "zh_cn")) {
                var language = ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of("en_us", code), false);
                Language.inject(language);
                for (var ship : ShipDefinitions.ALL) {
                    if (!language.has(ship.translationKey()) || !language.has(ship.type().translationKey()) || !language.has(ship.size().translationKey())) return false;
                    var stack = RiggingCoreRules.create(ship.id());
                    var simple = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL);
                    RiggingClientEvents.applyTooltip(stack, simple, false);
                    if (simple.size() != 2 || !simple.get(1).getString().equals(code.equals("zh_cn") ? "舰装核心" : "Rigging Core")) return false;
                    if (simple.getFirst().getStyle().getColor().getValue() != RiggingCoreItem.NAME_COLOR) return false;
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Alias"));
                    RiggingCoreRules.setProficiency(stack, 200);
                    var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);
                    RiggingClientEvents.applyTooltip(stack, lines, true);
                    if (!lines.getFirst().getString().equals("Alias") || lines.getFirst().getStyle().getColor().getValue() != RiggingCoreItem.NAME_COLOR) return false;
                    if (!lines.get(1).getString().equals(language.getOrDefault(ship.translationKey()))) return false;
                    String separator = code.equals("zh_cn") ? "·" : " · ";
                    String expectedClassification = language.getOrDefault(ship.size().translationKey()) + separator
                            + language.getOrDefault(ship.type().translationKey()) + separator + language.getOrDefault(ship.nationality().translationKey());
                    if (!lines.get(2).getString().equals(expectedClassification)) return false;
                    if (!lines.get(3).getString().equals(code.equals("zh_cn") ? "熟练度：200 / 200" : "Proficiency: 200 / 200")) return false;
                    if (lines.stream().anyMatch(line -> line.getString().equals(code.equals("zh_cn") ? "舰装核心" : "Rigging Core"))) return false;
                    if (lines.stream().noneMatch(line -> line.getString().equals("piranport:rigging_core"))) return false;
                }
            }
            return true;
        } finally { Language.inject(previous); }
    }

}
