package com.lex0v0.piranport.client;

import com.lex0v0.piranport.domain.ammunition.AmmunitionCategory;
import com.lex0v0.piranport.domain.ammunition.AmmunitionType;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.text.ManufacturingText;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 主菜单核对八个真实模型、双语名称及原版提示保留，不载入玩家存档。
final class AmmunitionClientChecks {
    private AmmunitionClientChecks() {}

    static boolean verify(Minecraft minecraft) {
        var previous = Language.getInstance();
        try {
            for (String code : List.of("en_us", "zh_cn")) {
                var language = ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of("en_us", code), false);
                Language.inject(language);
                for(var item:List.of(ModItems.AMMUNITION_FACTORY.get(),ModItems.COPPER_NUGGET.get())){
                    var stack=new ItemStack(item);var model=minecraft.getItemRenderer().getModel(stack,null,null,0);
                    if(model==minecraft.getModelManager().getMissingModel()||stack.getHoverName().getString().startsWith("item.")||stack.getHoverName().getString().startsWith("block."))return false;
                }
                for (var type : AmmunitionType.values()) {
                    var stack = new ItemStack(ModItems.ammunition(type), 64);
                    var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                    if (model == minecraft.getModelManager().getMissingModel()
                            || !model.getParticleIcon().contents().name().equals(type.id().withPrefix("item/"))
                            || !language.has(type.translationKey()) || !language.has(type.category().translationKey())) return false;
                    if (!stack.getHoverName().getString().equals(language.getOrDefault(type.translationKey()))) return false;
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("My ammunition"));
                    for (var flag : List.of(TooltipFlag.NORMAL, TooltipFlag.ADVANCED)) {
                        var original = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, flag);
                        if (!original.getFirst().getString().equals("My ammunition")) return false;
                        if (original.stream().anyMatch(line -> line.getString().contains(code.equals("zh_cn") ? "制造材料量" : "Manufacturing value"))) return false;
                        var cache = ManufacturingText.ammunitionTooltip(original, stack);
                        if (cache.size() != original.size() + 2 || !cache.getFirst().equals(original.getFirst())) return false;
                        for (int i = 1; i < original.size(); i++) if (cache.get(i + 2) != original.get(i)) return false;
                        if (!cache.get(1).getString().equals(Component.translatable("tooltip.piranport.manufacturing.ammunition_points", type.materialPoints()).getString())
                                || !cache.get(2).getString().equals(Component.translatable("tooltip.piranport.manufacturing.ammunition_total", type.materialPoints() * 64L).getString())) return false;
                        for (var category : AmmunitionCategory.values())
                            if (cache.subList(1, 3).stream().anyMatch(line -> line.getString().equals(language.getOrDefault(category.translationKey())))) return false;
                    }
                }
            }
            return true;
        } finally { Language.inject(previous); }
    }
}
