package com.lex0v0.piranport;

import com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;
import com.lex0v0.piranport.blockentity.manufacturing.ManufacturingMaterials;
import com.lex0v0.piranport.domain.ammunition.AmmunitionCategory;
import com.lex0v0.piranport.domain.ammunition.AmmunitionType;
import com.lex0v0.piranport.item.AmmunitionItem;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.item.rigging.RiggingRegion;
import com.lex0v0.piranport.recipe.ShipyardRecipe;
import com.lex0v0.piranport.registry.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 通过实际核心和两制造设施验证八弹种身份、扣料顺序及原物退款。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class AmmunitionGameTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path); }

    @GameTest(template = "empty")
    public static void identitiesTagsAndDefaultState(GameTestHelper h) {
        int[] points = {2, 3, 3, 1, 3, 4, 8, 6};
        for (var type : AmmunitionType.values()) {
            var item = ModItems.ammunition(type);
            var stack = new ItemStack(item, 64);
            h.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(type.id()) && item.type() == type
                    && AmmunitionItem.points(stack) == points[type.ordinal()], "identity and per-type points");
            h.assertTrue(stack.is(ModItemTags.AMMUNITION) && stack.is(ModItemTags.ammunition(type.category())), "parent and category tags");
            for (var category : AmmunitionCategory.values())
                h.assertTrue(stack.is(ModItemTags.ammunition(category)) == (category == type.category()), "exactly one category");
            h.assertTrue(stack.getMaxStackSize() == 64 && !stack.isDamageableItem() && !stack.has(DataComponents.MAX_DAMAGE)
                    && !stack.has(ModDataComponents.EQUIPMENT_IDENTITY) && !stack.has(ModDataComponents.FLUID_CONTENT), "plain consumable state");
            h.assertTrue(!stack.is(ModItemTags.WEAPONS) && !stack.is(ModItemTags.DEVICES) && !stack.is(ModItemTags.AIRCRAFT), "not equipment");
            var lines = new java.util.ArrayList<Component>();
            item.appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
            h.assertTrue(lines.isEmpty(), "no custom item tooltip");
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed ammunition"));
            h.assertTrue(item.type() == type && AmmunitionItem.points(stack) == points[type.ordinal()], "rename does not alter identity");
        }
        h.assertTrue(AmmunitionItem.points(ItemStack.EMPTY) == 0, "empty stack has no value");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void coreAdmissionSimulationAndRoundtrip(GameTestHelper h) {
        var core = RiggingCoreRules.create(id("lexington_cv2"));
        var inventory = RiggingCoreRules.inventory(core, RiggingRegion.AMMUNITION);
        for (var type : AmmunitionType.values()) {
            var stack = new ItemStack(ModItems.ammunition(type), 64);
            h.assertTrue(inventory.insertItem(type.ordinal(), stack, true).isEmpty()
                    && inventory.getStackInSlot(type.ordinal()).isEmpty(), "simulation does not write core");
            h.assertTrue(inventory.insertItem(type.ordinal(), stack, false).isEmpty(), "full ammunition stack admitted");
            var copy = inventory.getStackInSlot(type.ordinal()); copy.shrink(10);
            h.assertTrue(inventory.getStackInSlot(type.ordinal()).getCount() == 64, "query returns a copy");
            h.assertTrue(!RiggingCoreRules.inventory(core, RiggingRegion.WEAPONS).isItemValid(0, stack)
                    && !RiggingCoreRules.inventory(core, RiggingRegion.EQUIPMENT).isItemValid(0, stack), "ammo cannot be equipped");
        }
        var restored = ItemStack.parse(h.getLevel().registryAccess(), core.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(ItemStack.matches(core, restored), "core preserves all independent ammunition stacks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mixedPointCeilingAndSlotOrder(GameTestHelper h) {
        var sources = List.of(new ItemStack(ModItems.DEPTH_CHARGE.get()), new ItemStack(ModItems.ANTI_SHIP_MISSILE.get()),
                new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(), 64));
        var plan = ManufacturingMaterials.plan(sources, new ShipyardRecipe(id("glowworm"), 0, 4, 0, 0, 1));
        h.assertTrue(plan != null && plan.ammunition().size() == 2 && plan.ammunition().get(1).is(ModItems.ANTI_SHIP_MISSILE.get())
                && plan.remaining().get(2).getCount() == 64, "nine points in slot order without cheaper substitution");
        h.assertTrue(sources.getFirst().getCount() == 1 && sources.get(1).getCount() == 1, "planning leaves sources intact");
        for (var type : AmmunitionType.values()) {
            int needed = (4 + type.materialPoints() - 1) / type.materialPoints();
            var single = ManufacturingMaterials.plan(List.of(new ItemStack(ModItems.ammunition(type), 64)),
                    new ShipyardRecipe(id("glowworm"), 0, 4, 0, 0, 1));
            h.assertTrue(single != null && single.ammunition().getFirst().getCount() == needed, "per-type ceiling");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allTypesShipyardConsumptionAndRefund(GameTestHelper h) {
        for (var type : AmmunitionType.values()) {
            var pos = new BlockPos(2 + type.ordinal() * 2, 2, 2);
            h.setBlock(pos, ModBlocks.SHIPYARD.get());
            var yard = (ShipyardBlockEntity) h.getBlockEntity(pos);
            var recipe = yard.recipe(id("glowworm"));
            int count = (recipe.ammunition() + type.materialPoints() - 1) / type.materialPoints();
            var ammo = new ItemStack(ModItems.ammunition(type), count);
            ammo.set(DataComponents.CUSTOM_NAME, Component.literal("Shipyard " + type.id()));
            yard.insert(0, BlueprintRules.create(BlueprintType.DESTROYER), false);
            yard.insert(2, new ItemStack(ModItems.STEEL_INGOT.get(), recipe.steel()), false);
            yard.insert(3, new ItemStack(ModItems.ALUMINUM_INGOT.get(), recipe.aluminum()), false);
            yard.insert(4, ammo, false);
            yard.fluid(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(), recipe.fuel()), FluidAction.EXECUTE);
            h.assertTrue(yard.start(id("glowworm"), null) == ShipyardBlockEntity.StartResult.STARTED, "all types start shipyard");
            h.assertTrue(yard.stack(4).isEmpty(), "exact whole items consumed");
            yard.cancelCurrent(null);
            h.assertTrue(yard.materials().stream().anyMatch(s -> ItemStack.matches(s, ammo)), "original type, count and name refunded");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allTypesFactoryConsumptionAndRefund(GameTestHelper h) {
        for (var type : AmmunitionType.values()) {
            var pos = new BlockPos(2 + type.ordinal() * 2, 2, 2);
            h.setBlock(pos, ModBlocks.DEVELOPMENT_FACTORY.get());
            var factory = (DevelopmentFactoryBlockEntity) h.getBlockEntity(pos);
            var recipe = factory.recipe(id("quick_repair"));
            int count = (recipe.ammunition() + type.materialPoints() - 1) / type.materialPoints();
            var ammo = new ItemStack(ModItems.ammunition(type), count);
            ammo.set(DataComponents.CUSTOM_NAME, Component.literal("Factory " + type.id()));
            factory.insert(0, BlueprintRules.create(BlueprintType.QUICK_REPAIR), false);
            factory.insert(2, new ItemStack(ModItems.STEEL_INGOT.get(), recipe.steel()), false);
            factory.insert(3, new ItemStack(ModItems.ALUMINUM_INGOT.get(), recipe.aluminum()), false);
            factory.insert(4, ammo, false);
            factory.fluid(null).fill(new FluidStack(ModFluids.FUEL_OIL.get(), recipe.fuel()), FluidAction.EXECUTE);
            h.assertTrue(factory.start(id("quick_repair"), null) == DevelopmentFactoryBlockEntity.StartResult.STARTED, "all types start factory");
            h.assertTrue(factory.stack(4).isEmpty(), "exact whole items consumed");
            factory.cancelCurrent(null);
            h.assertTrue(factory.materials().stream().anyMatch(s -> ItemStack.matches(s, ammo)), "original type, count and name refunded");
        }
        h.succeed();
    }
}
