package com.lex0v0.piranport.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 提供四种弹药的船坞材料量，战斗与合成另行接入。
public final class AmmunitionItem extends Item {
    private final int materialPoints;

    public AmmunitionItem(int materialPoints, Properties properties) {
        super(properties);
        this.materialPoints = materialPoints;
    }

    public int materialPoints() { return materialPoints; }

    public static int points(ItemStack stack) {
        return stack.getItem() instanceof AmmunitionItem item ? item.materialPoints : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.piranport.ammunition.points", materialPoints).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.piranport.ammunition.total", (long) materialPoints * stack.getCount()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
