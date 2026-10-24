package com.lex0v0.piranport.item;

import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// 核心物品的默认名称、简洁说明及身份无关的掉落物保护。
public final class RiggingCoreItem extends Item {
    public static final int NAME_COLOR = 0x6699FF;
    public RiggingCoreItem(Properties properties) { super(properties); }
    @Override public Component getName(ItemStack stack) {
        var name = RiggingCoreRules.definition(stack).map(ship -> Component.translatable(ship.translationKey()))
                .orElseGet(() -> Component.translatable(RiggingCoreRules.status(stack) == RiggingCoreRules.IdentityStatus.MISSING
                        ? "item.piranport.rigging_core.uninitialized" : "item.piranport.rigging_core.unknown"));
        return name.withStyle(style -> style.withColor(NAME_COLOR));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.piranport.rigging_core").withStyle(ChatFormatting.GRAY));
    }
    @Override public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL);
    }
    @Override public int getEntityLifespan(ItemStack stack, Level level) { return Integer.MAX_VALUE; }
    @Override public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.setUnlimitedLifetime();
        return false;
    }
}
