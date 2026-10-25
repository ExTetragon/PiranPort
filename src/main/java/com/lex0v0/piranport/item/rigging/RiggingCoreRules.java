package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.item.RiggingCoreItem;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.domain.ship.ShipDefinition;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.lex0v0.piranport.domain.ship.ShipType;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// 集中创建、校验和查询核心，读取不会初始化或清除存档。
public final class RiggingCoreRules {
    public enum IdentityStatus { VALID, MISSING, UNKNOWN_SHIP, UNKNOWN_TYPE, MISMATCH }
    private RiggingCoreRules() {}
    public static boolean isCore(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof RiggingCoreItem; }
    public static IdentityStatus status(ItemStack stack) {
        var identity = isCore(stack) ? stack.get(ModDataComponents.RIGGING_IDENTITY) : null;
        if (identity == null) return IdentityStatus.MISSING;
        var ship = ShipDefinitions.byId(identity.shipId());
        if (ship == null) return IdentityStatus.UNKNOWN_SHIP;
        var type = ShipType.byId(identity.typeId());
        if (type == null) return IdentityStatus.UNKNOWN_TYPE;
        return ship.type() == type ? IdentityStatus.VALID : IdentityStatus.MISMATCH;
    }
    public static Optional<ShipDefinition> definition(ItemStack stack) {
        return status(stack) == IdentityStatus.VALID
                ? Optional.of(ShipDefinitions.byId(stack.get(ModDataComponents.RIGGING_IDENTITY).shipId())) : Optional.empty();
    }
    public static ItemStack create(ResourceLocation shipId) {
        var ship = ShipDefinitions.byId(shipId);
        if (ship == null) throw new IllegalArgumentException("Unknown ship: " + shipId);
        var stack = new ItemStack(ModItems.RIGGING_CORE.get());
        stack.set(ModDataComponents.RIGGING_IDENTITY, new RiggingIdentity(ship.id(), ship.type().id()));
        stack.set(ModDataComponents.RIGGING_PROFICIENCY, RiggingProficiency.ZERO);
        return stack;
    }
    public static int capacity(ItemStack stack, RiggingRegion region) {
        return definition(stack).map(ship -> region.capacity(ship.size())).orElse(0);
    }
    public static int fuelCapacity(ItemStack stack) { return definition(stack).map(ship -> ship.size().fuel()).orElse(0); }
    public static int getProficiency(ItemStack stack) {
        return isCore(stack) ? stack.getOrDefault(ModDataComponents.RIGGING_PROFICIENCY, RiggingProficiency.ZERO).value() : 0;
    }
    public static void setProficiency(ItemStack stack, int value) {
        if (!isCore(stack)) throw new IllegalArgumentException("Not a rigging core");
        var proficiency = new RiggingProficiency(value);
        int before = getProficiency(stack);
        if (before != value) {
            stack.set(ModDataComponents.RIGGING_PROFICIENCY, proficiency);
            ModLog.debug(ModLog.Event.PROFICIENCY, "core", stack.get(ModDataComponents.RIGGING_IDENTITY), "from", before, "to", value);
        }
    }
    public static int addProficiency(ItemStack stack, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative proficiency gain");
        int current = getProficiency(stack);
        int next = (int)Math.min(RiggingProficiency.MAX, (long)current + amount);
        setProficiency(stack, next);
        return next - current;
    }
    public static RiggingItemHandler inventory(ItemStack stack, RiggingRegion region) { return new RiggingItemHandler(stack, region); }
    public static RiggingInventory inventory(ItemStack stack) { return new RiggingInventory(stack); }
    public static RiggingFluidHandler fuel(ItemStack stack) { return new RiggingFluidHandler(stack); }
}
