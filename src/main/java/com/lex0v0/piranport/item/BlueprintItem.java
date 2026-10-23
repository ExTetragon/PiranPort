package com.lex0v0.piranport.item;

import com.lex0v0.piranport.item.blueprint.BlueprintFamily;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 按许可集合生成图纸名称和分组悬浮说明。
public final class BlueprintItem extends Item {
    private final BlueprintFamily family;

    public BlueprintItem(BlueprintFamily family, Properties properties) {
        super(properties);
        this.family = family;
    }

    public BlueprintFamily family() { return family; }

    @Override
    public Component getName(ItemStack stack) {
        var types = BlueprintRules.getAllowedTypes(stack);
        if (!types.isEmpty()) {
            var subclass = types.iterator().next().subclass();
            if (types.stream().allMatch(type -> type.subclass() == subclass)) return Component.translatable(subclass.itemNameKey());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        for (var subclass : BlueprintSubclass.values()) {
            var types = BlueprintRules.getAllowedTypes(stack, subclass);
            if (types.isEmpty()) continue;
            MutableComponent names = Component.empty();
            boolean first = true;
            for (var type : types) {
                if (!first) names.append(Component.translatable("tooltip.piranport.blueprint.separator"));
                names.append(Component.translatable(type.translationKey()));
                first = false;
            }
            lines.add(Component.translatable("tooltip.piranport.blueprint.group", Component.translatable(subclass.translationKey()), names)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
