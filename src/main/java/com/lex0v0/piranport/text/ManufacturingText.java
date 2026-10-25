package com.lex0v0.piranport.text;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import com.lex0v0.piranport.item.AmmunitionItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// 制造界面统一显示粒折锭数与向上取整到秒的任务时长。
public final class ManufacturingText {
    private ManufacturingText() {}

    // 只由制造页面的材料槽调用，保留原版标题、归属和高级提示的相对顺序。
    public static List<Component> ammunitionTooltip(List<Component> original, ItemStack stack) {
        int points = AmmunitionItem.points(stack);
        if (points == 0) return original;
        List<Component> lines = new ArrayList<>(original);
        lines.addAll(Math.min(1, lines.size()), List.of(
                Component.translatable("tooltip.piranport.manufacturing.ammunition_points", points).withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.piranport.manufacturing.ammunition_total", (long) points * stack.getCount()).withStyle(ChatFormatting.DARK_GRAY)));
        return lines;
    }
    public static String metal(long units) {
        return units % 9 == 0 ? Long.toString(units / 9) : String.format(Locale.ROOT, "%.2f", units / 9.0);
    }
    public static String metalParts(long units,String ingot,String nugget){
        if(units<=0)return "0";
        return (units/9==0?"":units/9+ingot)+(units%9==0?"":units%9+nugget);
    }
    public static String time(int ticks) {
        int seconds = Math.max(0, (int) ((ticks + 19L) / 20));
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
