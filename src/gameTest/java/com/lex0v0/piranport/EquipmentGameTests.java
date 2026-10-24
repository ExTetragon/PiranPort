package com.lex0v0.piranport;

import com.lex0v0.piranport.domain.equipment.*;
import com.lex0v0.piranport.domain.ship.ShipStats;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.item.blueprint.*;
import com.lex0v0.piranport.item.equipment.*;
import com.lex0v0.piranport.item.rigging.*;
import com.lex0v0.piranport.registry.*;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 验证真实物品、原版耗损、核心写回和飞机储油，不模拟尚未设计的作战行为。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path); }
    private static ItemStack equipment(String path) { return EquipmentRules.create(id(path)); }
    private static FluidStack fuel(int amount) { return new FluidStack(ModFluids.FUEL_OIL.get(), amount); }
    private static ItemStack roundtrip(GameTestHelper h, ItemStack stack) { return ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)stack.save(h.getLevel().registryAccess())); }
    private static void nativeDamage(GameTestHelper h, ItemStack stack, int amount, AtomicInteger broken) {
        stack.hurtAndBreak(amount, h.getLevel(), (net.minecraft.server.level.ServerPlayer)null, item -> broken.incrementAndGet());
    }

    @GameTest(template = "empty")
    public static void catalogueAndRegistrations(GameTestHelper h) {
        h.assertTrue(EquipmentDefinitions.ALL.size() == 21 && EquipmentCategory.values().length == 15, "21 durable models and two consumables across 15 registered classes");
        var items = new HashSet<net.minecraft.world.item.Item>();
        for (var category : EquipmentCategory.values()) items.add(ModItems.equipment(category));
        h.assertTrue(items.size() == 15, "categories have distinct registered items");
        for (var definition : EquipmentDefinitions.ALL) {
            var stack = EquipmentRules.create(definition.id());
            h.assertTrue(stack.getMaxStackSize() == 1 && stack.getMaxDamage() == definition.maxDurability()
                    && EquipmentRules.remainingDurability(stack) == definition.maxDurability() && EquipmentRules.operational(stack), "full durability and nonstackable");
            h.assertTrue(definition.modifiers().equals(ShipStats.ZERO), "all provisional ship modifiers are zero");
            h.assertTrue(((EquipmentItem)stack.getItem()).combatProfile(stack).isEmpty() && ((EquipmentItem)stack.getItem()).deviceProfile(stack).isEmpty(), "no fabricated functional configuration");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void subtypeProperties(GameTestHelper h) {
        h.assertTrue(EquipmentRules.definition(equipment("experimental_coilgun")).orElseThrow().gunClass().orElseThrow() == GunClass.SMALL, "coilgun remains small");
        h.assertTrue(EquipmentRules.definition(equipment("us_triple_mk16_8in")).orElseThrow().gunClass().orElseThrow() == GunClass.MEDIUM_HEAVY, "Mk16 is medium heavy");
        h.assertTrue(EquipmentRules.definition(equipment("g7e_t10_submarine")).orElseThrow().torpedoUse().orElseThrow() == TorpedoUse.SUBMARINE, "submarine torpedo property");
        h.assertTrue(EquipmentRules.definition(equipment("additional_armor_large")).orElseThrow().armorClass().orElseThrow() == ArmorClass.LARGE, "large armor property");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void identityPersistenceAndNetwork(GameTestHelper h) {
        for (var definition : EquipmentDefinitions.ALL) {
            var stack = EquipmentRules.create(definition.id()); EquipmentRules.damage(stack, 7);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("custom"));
            var restored = roundtrip(h, stack);
            h.assertTrue(ItemStack.isSameItemSameComponents(stack, restored), "identity, custom name and damage persist");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, stack);
                h.assertTrue(ItemStack.isSameItemSameComponents(stack, ItemStack.STREAM_CODEC.decode(buffer)), "network stack roundtrip");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void missingUnknownAndMismatchedIdentity(GameTestHelper h) {
        var blank = new ItemStack(ModItems.MAIN_GUN.get());
        h.assertTrue(EquipmentRules.status(blank) == EquipmentRules.IdentityStatus.MISSING && !EquipmentRules.operational(blank), "ordinary give does not initialize");
        blank.set(ModDataComponents.EQUIPMENT_IDENTITY, new EquipmentIdentity(id("future_model")));
        var saved = roundtrip(h, blank);
        h.assertTrue(EquipmentRules.status(saved) == EquipmentRules.IdentityStatus.INVALID && saved.get(ModDataComponents.EQUIPMENT_IDENTITY).model().equals(id("future_model")), "unknown ID survives and stays inert");
        saved.set(DataComponents.MAX_DAMAGE,250);saved.set(DataComponents.DAMAGE,250);var before=saved.copy();
        nativeDamage(h,saved,Integer.MAX_VALUE,new AtomicInteger());
        h.assertTrue(ItemStack.isSameItemSameComponents(saved,before), "native wear leaves invalid model data unchanged");
        blank.set(ModDataComponents.EQUIPMENT_IDENTITY, new EquipmentIdentity(id("advanced_radar")));
        h.assertTrue(EquipmentRules.status(blank) == EquipmentRules.IdentityStatus.INVALID && ((EquipmentItem)blank.getItem()).riggingStats(blank).equals(ShipStats.ZERO), "cross-category identity rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pureQueriesAndIndependentCopies(GameTestHelper h) {
        var plane = equipment("phantom_fg1"); var before = plane.copy();
        EquipmentRules.definition(plane); EquipmentRules.broken(plane); EquipmentRules.fuelCapacity(plane);
        EquipmentTooltips.summary(plane); EquipmentTooltips.details(plane);
        h.assertTrue(ItemStack.isSameItemSameComponents(plane, before), "queries do not add components");
        var copy = plane.copy(); EquipmentRules.damage(copy, 100); EquipmentRules.fuel(copy).fill(fuel(500), FluidAction.EXECUTE);
        h.assertTrue(EquipmentRules.remainingDurability(plane) == plane.getMaxDamage() && EquipmentRules.fuel(plane).getFluid().isEmpty(), "copy mutations remain isolated");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void retainedBreakKeepsIdentityAndFuel(GameTestHelper h) {
        boolean previous = Config.RETAIN_BROKEN_EQUIPMENT.get();
        try {
            Config.RETAIN_BROKEN_EQUIPMENT.set(true);
            var stack = equipment("tu91"); EquipmentRules.fuel(stack).fill(fuel(777), FluidAction.EXECUTE);
            EquipmentRules.damage(stack, Integer.MAX_VALUE);
            h.assertTrue(!stack.isEmpty() && EquipmentRules.broken(stack) && EquipmentRules.remainingDurability(stack) == 0 && !EquipmentRules.operational(stack), "broken item kept and inert");
            h.assertTrue(EquipmentRules.fuel(stack).getFluid().getAmount() == 777 && EquipmentRules.definition(roundtrip(h,stack)).isPresent(), "identity and fuel remain");
            h.assertTrue(((EquipmentItem)stack.getItem()).riggingStats(stack).equals(ShipStats.ZERO), "broken equipment gives no modifier");
        } finally { Config.RETAIN_BROKEN_EQUIPMENT.set(previous); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeWearRetainsAtZero(GameTestHelper h) {
        boolean previous = Config.RETAIN_BROKEN_EQUIPMENT.get();
        try {
            Config.RETAIN_BROKEN_EQUIPMENT.set(true);
            var stack = equipment("experimental_coilgun"); var broken = new AtomicInteger();
            nativeDamage(h, stack, 249, broken); h.assertTrue(EquipmentRules.remainingDurability(stack) == 1, "native wear until last point");
            nativeDamage(h, stack, Integer.MAX_VALUE, broken);
            for (int value : new int[]{0, 1, -1, Integer.MAX_VALUE}) nativeDamage(h, stack, value, broken);
            h.assertTrue(!stack.isEmpty() && stack.getDamageValue() == 250 && broken.get() == 0, "native path never deletes retained item or repairs it accidentally");
        } finally { Config.RETAIN_BROKEN_EQUIPMENT.set(previous); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void destroySettingAndNoRetroactiveRemoval(GameTestHelper h) {
        boolean previous = Config.RETAIN_BROKEN_EQUIPMENT.get();
        try {
            Config.RETAIN_BROKEN_EQUIPMENT.set(true);
            var old = equipment("b25_doolittle"); EquipmentRules.damage(old, Integer.MAX_VALUE);
            Config.RETAIN_BROKEN_EQUIPMENT.set(false);
            h.assertTrue(EquipmentRules.broken(old) && !roundtrip(h, old).isEmpty(), "config and readback do not delete existing broken equipment");
            var nativeOld=old.copy();nativeOld.set(DataComponents.MAX_DAMAGE,Integer.MAX_VALUE);nativeOld.set(DataComponents.DAMAGE,Integer.MAX_VALUE);
            var nativeBreaks=new AtomicInteger();nativeDamage(h,nativeOld,0,nativeBreaks);
            h.assertTrue(!nativeOld.isEmpty(), "native zero wear preserves broken equipment after config switch");
            nativeDamage(h,nativeOld,1,nativeBreaks);
            h.assertTrue(nativeOld.isEmpty() && nativeBreaks.get()==1, "native next wear destroys once without integer overflow");
            EquipmentRules.damage(old, 0); h.assertTrue(!old.isEmpty(), "zero wear is a no-op");
            EquipmentRules.damage(old, 1); h.assertTrue(old.isEmpty(), "future wear uses current setting");
            var fresh = equipment("experimental_coilgun");var broken = new AtomicInteger(); nativeDamage(h, fresh, Integer.MAX_VALUE, broken);
            h.assertTrue(fresh.isEmpty() && broken.get() == 1, "native wear destroys once when configured");
        } finally { Config.RETAIN_BROKEN_EQUIPMENT.set(previous); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeCreativeDoesNotWear(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        h.assertTrue(player.hasInfiniteMaterials(), "creative fixture includes native ability flags");
        var stack = equipment("experimental_coilgun");
        stack.hurtAndBreak(Integer.MAX_VALUE, h.getLevel(), player, item -> {});
        h.assertTrue(!stack.isEmpty() && stack.getDamageValue() == 0, "native creative immunity precedes wear side effects");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aircraftFuelCapabilityAndSimulation(GameTestHelper h) {
        for (var definition : EquipmentDefinitions.ALL) {
            var stack = EquipmentRules.create(definition.id());var handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (definition.category().group() != EquipmentCategory.Group.AIRCRAFT) { h.assertTrue(handler == null, "nonaircraft has no fuel capability");continue; }
            h.assertTrue(handler != null && handler.getTankCapacity(0) == 2000 && handler.getFluidInTank(0).isEmpty(), "four aircraft begin empty with 2000mB capacity");
            h.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 0, "reject water");
            h.assertTrue(handler.fill(fuel(3000), FluidAction.SIMULATE) == 2000 && handler.getFluidInTank(0).isEmpty(), "simulate leaves item unchanged");
            h.assertTrue(handler.fill(fuel(3000), FluidAction.EXECUTE) == 2000 && handler.fill(fuel(1), FluidAction.EXECUTE) == 0, "capacity enforced");
            h.assertTrue(handler.drain(500, FluidAction.SIMULATE).getAmount() == 500 && handler.getFluidInTank(0).getAmount() == 2000, "drain simulation pure");
            h.assertTrue(EquipmentRules.fuel(roundtrip(h, stack)).getFluid().getAmount() == 2000, "fuel persists");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fuelComponentsAndAbnormalIdentityRecovery(GameTestHelper h) {
        var plane = equipment("c6n_saiun");var tagged = fuel(1500);tagged.set(DataComponents.CUSTOM_NAME, Component.literal("batch A"));
        var handler = EquipmentRules.fuel(plane); handler.fill(tagged, FluidAction.EXECUTE);
        h.assertTrue(handler.fill(fuel(100), FluidAction.EXECUTE) == 0, "same fluid with different components does not merge");
        var drained = handler.drain(200, FluidAction.EXECUTE);
        h.assertTrue(drained.get(DataComponents.CUSTOM_NAME).getString().equals("batch A"), "draining preserves fluid components");
        plane.set(ModDataComponents.FLUID_CONTENT, SimpleFluidContent.copyOf(tagged.copyWithAmount(5000)));
        h.assertTrue(handler.fill(tagged, FluidAction.EXECUTE) == 0 && handler.getFluid().getAmount() == 5000, "overfill remains drainable without truncation");
        plane.set(ModDataComponents.EQUIPMENT_IDENTITY, new EquipmentIdentity(id("future_plane")));
        h.assertTrue(handler.getTankCapacity(0) == 0 && handler.fill(tagged, FluidAction.EXECUTE) == 0 && handler.drain(6000, FluidAction.EXECUTE).getAmount() == 5000, "invalid identity cannot fill but preserves recoverable stored fuel");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void coreSlotsAcceptActualEquipment(GameTestHelper h) {
        var core = RiggingCoreRules.create(id("lexington_cv2"));
        var weapons = RiggingCoreRules.inventory(core, RiggingRegion.WEAPONS);var devices = RiggingCoreRules.inventory(core, RiggingRegion.EQUIPMENT);
        for (var definition : EquipmentDefinitions.ALL) {
            var stack = EquipmentRules.create(definition.id()); var target = definition.category().group() == EquipmentCategory.Group.DEVICE ? devices : weapons;
            var other = target == devices ? weapons : devices;
            h.assertTrue(target.isItemValid(0,stack) && !other.isItemValid(0,stack), "production tags place category in correct region");
            h.assertTrue(target.insertItem(0,stack,true).isEmpty() && target.getStackInSlot(0).isEmpty(), "insertion simulation pure");
            target.setStackInSlot(0, stack);h.assertTrue(target.extractItem(0,1,false).getCount() == 1, "one equipment per slot and removable");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void coreDuplicatesAndExplicitWriteback(GameTestHelper h) {
        var core = RiggingCoreRules.create(id("lexington_cv2"));var weapons = RiggingCoreRules.inventory(core, RiggingRegion.WEAPONS);
        var plane = equipment("phantom_fg1"); EquipmentRules.fuel(plane).fill(fuel(333), FluidAction.EXECUTE);
        weapons.setStackInSlot(0,plane);weapons.setStackInSlot(1,plane);
        var copy = weapons.getStackInSlot(0);EquipmentRules.damage(copy, 100);
        h.assertTrue(weapons.getStackInSlot(0).getDamageValue() == 0, "retrieving core content returns a copy");
        weapons.setStackInSlot(0,copy);
        var restored = RiggingCoreRules.inventory(roundtrip(h,core), RiggingRegion.WEAPONS);
        h.assertTrue(restored.getStackInSlot(0).getDamageValue() == 100 && restored.getStackInSlot(1).getDamageValue() == 0 && EquipmentRules.fuel(restored.getStackInSlot(0)).getFluid().getAmount() == 333, "duplicates preserve independent wear and oil after explicit writeback");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaRepairBlockedRenamePreserved(GameTestHelper h) {
        var first = equipment("experimental_coilgun");var second = equipment("us_triple_mk6_16in");EquipmentRules.damage(first,100);EquipmentRules.damage(second,100);
        h.assertTrue(!first.isRepairable() && !first.getItem().canGrindstoneRepair(first), "crafting and grindstone repairs disabled");
        var player = h.makeMockPlayer(GameType.SURVIVAL);var menu = new AnvilMenu(1,player.getInventory(),ContainerLevelAccess.NULL);
        menu.getSlot(0).set(first);menu.getSlot(1).set(second);menu.createResult();
        h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "two models sharing registered item cannot mix in anvil");
        menu.getSlot(1).set(ItemStack.EMPTY);menu.setItemName("My turret");menu.createResult();
        h.assertTrue(!menu.getSlot(2).getItem().isEmpty() && EquipmentRules.definition(menu.getSlot(2).getItem()).orElseThrow().id().equals(id("experimental_coilgun"))
                && menu.getSlot(2).getItem().getHoverName().getString().equals("My turret"), "renaming preserves real identity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeIncludesEveryModelOnce(GameTestHelper h) {
        var tab = ModCreativeTabs.MAIN.get();tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess()));
        var models = tab.getDisplayItems().stream().filter(stack -> stack.getItem() instanceof EquipmentItem).toList();
        h.assertTrue(models.size() == 21 && models.stream().map(stack -> stack.get(ModDataComponents.EQUIPMENT_IDENTITY)).distinct().count() == 21, "every durable model appears exactly once, no raw category items");
        h.assertTrue(tab.getDisplayItems().stream().filter(stack -> stack.is(ModItems.QUICK_REPAIR.get()) || stack.is(ModItems.DAMAGE_CONTROL.get())).count() == 2, "both ordinary consumables appear once");
        h.assertTrue(models.stream().allMatch(stack -> EquipmentRules.operational(stack) && stack.getDamageValue() == 0 && EquipmentRules.fuel(stack).getFluid().isEmpty()), "creative entries full durability and no fuel");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void secondaryGunBlueprintPermission(GameTestHelper h) {
        var type = BlueprintType.SECONDARY_GUN;
        h.assertTrue(type.subclass() == BlueprintSubclass.DEVICE && type.id().equals(id("equipment/secondary_gun")) && BlueprintSubclass.DEVICE.types().size() == 7, "secondary gun is seventh device permission");
        h.assertTrue(BlueprintRules.allowsType(BlueprintRules.create(BlueprintSubclass.DEVICE),type), "complete device blueprint covers secondary gun");
        h.assertTrue(!BlueprintRules.allowsType(BlueprintRules.create(BlueprintType.MAIN_GUN),type), "weapon blueprint does not cover secondary gun");
        h.succeed();
    }
}
