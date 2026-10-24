package com.lex0v0.piranport.item.equipment;

import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.text.ClassificationText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// 普通提示显示分类和单件状态，Shift 详情显示调试属性。
public final class EquipmentTooltips {
    private EquipmentTooltips() {}
    private static Component line(String key, Object... args) { return Component.translatable("tooltip.piranport.equipment." + key, args).withStyle(ChatFormatting.GRAY); }
    public static List<Component> summary(ItemStack stack) {
        var result = new ArrayList<Component>();
        if (!(stack.getItem() instanceof EquipmentItem item)) return result;
        var definition = EquipmentRules.definition(stack);
        result.add(line("classification", definition.map(ClassificationText::equipment).orElseGet(() -> ClassificationText.equipment(item.category()))));
        if (definition.isEmpty()) return result;
        var value = definition.get();
        result.add(line("durability", EquipmentRules.remainingDurability(stack), stack.getMaxDamage()));
        if (EquipmentRules.broken(stack)) result.add(Component.translatable("tooltip.piranport.equipment.broken").withStyle(ChatFormatting.RED));
        if (value.category().group() == EquipmentCategory.Group.AIRCRAFT) result.add(line("fuel", EquipmentRules.fuel(stack).getFluid().getAmount(), value.fuelCapacity()));
        result.add(line("hold_shift"));
        return result;
    }
    public static List<Component> details(ItemStack stack) {
        var definition = EquipmentRules.definition(stack);
        if (definition.isEmpty()) return List.of();
        var value = definition.get();
        var stats = value.modifiers();
        return List.of(line("modifiers", stats.durability(), stats.armor(), stats.speed()), line("functions_pending"));
    }
}
