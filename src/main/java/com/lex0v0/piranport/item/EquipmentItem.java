package com.lex0v0.piranport.item;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.Config;
import com.lex0v0.piranport.domain.equipment.EquipmentCategory;
import com.lex0v0.piranport.domain.ship.ShipStats;
import com.lex0v0.piranport.item.equipment.EquipmentFunctions;
import com.lex0v0.piranport.item.equipment.EquipmentRules;
import com.lex0v0.piranport.item.equipment.EquipmentTooltips;
import com.lex0v0.piranport.item.rigging.RiggingEquipment;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

// 一类注册物品对应多个型号，耐久和燃油随具体物品保存。
public final class EquipmentItem extends Item implements RiggingEquipment, EquipmentFunctions {
    private final EquipmentCategory category;
    public EquipmentItem(EquipmentCategory category, Properties properties) { super(properties.stacksTo(1).setNoRepair()); this.category = category; }
    public EquipmentCategory category() { return category; }
    @Override public Component getName(ItemStack stack) {
        return EquipmentRules.definition(stack).map(value -> Component.translatable(value.translationKey()))
                .orElseGet(() -> Component.translatable("equipment.piranport." + (EquipmentRules.status(stack) == EquipmentRules.IdentityStatus.MISSING ? "uninitialized" : "invalid"),
                        Component.translatable(category.translationKey())));
    }
    @Override public ShipStats riggingStats(ItemStack stack) {
        return EquipmentRules.operational(stack) ? EquipmentRules.definition(stack).orElseThrow().modifiers() : ShipStats.ZERO;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.addAll(EquipmentTooltips.summary(stack));
    }
    @Override public boolean canGrindstoneRepair(ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return false; }
    @Override public void setDamage(ItemStack stack, int damage) {
        // 原版耗损继续结算时不得把刚保留的损坏件回退一格；维修将使用独立接口。
        if (stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage() && damage < stack.getMaxDamage()) return;
        int before = EquipmentRules.remainingDurability(stack);
        super.setDamage(stack, damage);
        int after = EquipmentRules.remainingDurability(stack);
        if (before != after && EquipmentRules.definition(stack).isPresent())
            ModLog.debug(ModLog.Event.WEAR, "model", EquipmentRules.definition(stack).orElseThrow().id(),
                    "from", before, "to", after, "destroyed", after == 0 && !Config.RETAIN_BROKEN_EQUIPMENT.get());
    }
    @Override public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        if (entity != null && entity.hasInfiniteMaterials()) return 0;
        if (EquipmentRules.definition(stack).isEmpty()) return stack.getDamageValue() >= stack.getMaxDamage() ? -1 : 0;
        if (amount <= 0) return EquipmentRules.broken(stack) ? -1 : 0;
        int remaining = EquipmentRules.remainingDurability(stack);
        if (Config.RETAIN_BROKEN_EQUIPMENT.get() && amount >= remaining) {
            EquipmentRules.damage(stack, amount);
            // 原版随后以 max-1 进行耗尽判定，setDamage 保留 max，避免缩减堆叠。
            return -1;
        }
        return Math.min(amount, remaining);
    }
}
