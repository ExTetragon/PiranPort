package com.lex0v0.piranport.item.equipment;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.Config;
import com.lex0v0.piranport.domain.equipment.EquipmentDefinition;
import com.lex0v0.piranport.domain.equipment.EquipmentDefinitions;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModItems;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// 创建与纯查询不随机补身份，耗损只修改调用方持有的单件物品。
public final class EquipmentRules {
    private EquipmentRules() {}
    public enum IdentityStatus { MISSING, INVALID, VALID }
    public static Optional<EquipmentDefinition> definition(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EquipmentItem item)) return Optional.empty();
        var identity = stack.get(ModDataComponents.EQUIPMENT_IDENTITY);
        return identity == null ? Optional.empty() : EquipmentDefinitions.find(identity.model()).filter(value -> value.category() == item.category());
    }
    public static IdentityStatus status(ItemStack stack) {
        return definition(stack).isPresent() ? IdentityStatus.VALID
                : stack.has(ModDataComponents.EQUIPMENT_IDENTITY) ? IdentityStatus.INVALID : IdentityStatus.MISSING;
    }
    public static ItemStack create(ResourceLocation model) {
        var definition = EquipmentDefinitions.find(model).orElseThrow(() -> new IllegalArgumentException("Unknown equipment: " + model));
        var stack = new ItemStack(ModItems.equipment(definition.category()));
        stack.set(ModDataComponents.EQUIPMENT_IDENTITY, new EquipmentIdentity(model));
        stack.set(DataComponents.MAX_DAMAGE, definition.maxDurability());
        stack.set(DataComponents.DAMAGE, 0);
        return stack;
    }
    public static boolean broken(ItemStack stack) {
        return definition(stack).isPresent() && stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage();
    }
    public static boolean operational(ItemStack stack) {
        return definition(stack).isPresent() && stack.isDamageableItem() && !broken(stack);
    }
    public static int remainingDurability(ItemStack stack) { return Math.max(0, stack.getMaxDamage() - stack.getDamageValue()); }
    public static int fuelCapacity(ItemStack stack) { return definition(stack).map(EquipmentDefinition::fuelCapacity).orElse(0); }
    public static AircraftFluidHandler fuel(ItemStack stack) { return new AircraftFluidHandler(stack); }
    public static void damage(ItemStack stack, int amount) {
        if (amount <= 0 || definition(stack).isEmpty() || !stack.isDamageableItem()) return;
        var model = definition(stack).orElseThrow().id();
        int before = remainingDurability(stack);
        int damage = (int)Math.min(stack.getMaxDamage(), (long)stack.getDamageValue() + amount);
        if (damage >= stack.getMaxDamage() && !Config.RETAIN_BROKEN_EQUIPMENT.get()) stack.shrink(1);
        else stack.set(DataComponents.DAMAGE, damage);
        if (before != remainingDurability(stack) || stack.isEmpty())
            ModLog.debug(ModLog.Event.WEAR, "model", model, "from", before, "to", stack.isEmpty() ? 0 : remainingDurability(stack), "destroyed", stack.isEmpty());
    }
}
