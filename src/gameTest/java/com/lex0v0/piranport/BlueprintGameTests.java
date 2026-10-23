package com.lex0v0.piranport;

import com.lex0v0.piranport.item.blueprint.*;
import com.lex0v0.piranport.recipe.BlueprintExpandRecipe;
import com.lex0v0.piranport.recipe.BlueprintMergeRecipe;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModCreativeTabs;
import com.lex0v0.piranport.item.BlueprintItem;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static com.lex0v0.piranport.item.blueprint.BlueprintType.*;

// 验证图纸组件、许可查询、显示及原版合成结算。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class BlueprintGameTests {
    private static final BlueprintMergeRecipe MERGE = new BlueprintMergeRecipe(CraftingBookCategory.MISC);
    private static final BlueprintExpandRecipe EXPAND = new BlueprintExpandRecipe(CraftingBookCategory.MISC);

    private static CraftingInput expansion(ItemStack blueprint, int count) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) items.add(i == 4 ? blueprint.copyWithCount(count)
                : new ItemStack(i == 1 ? Items.BLUE_DYE : Items.PAPER, count));
        return CraftingInput.of(3, 3, items);
    }

    private static String nameKey(ItemStack stack) {
        return ((TranslatableContents) stack.getItem().getName(stack).getContents()).getKey();
    }

    @GameTest(template = "empty")
    public static void componentPersistenceAndNetwork(GameTestHelper h) {
        List<ResourceLocation> ids = new ArrayList<>(List.of(DESTROYER.id(), BATTLESHIP.id(), DESTROYER.id()));
        var content = new BlueprintContent(ids);
        ids.clear();
        h.assertTrue(content.permissions().size() == 2, "defensive copy and deduplication");
        h.assertTrue(content.equals(new BlueprintContent(List.of(BATTLESHIP.id(), DESTROYER.id()))), "order independent");
        boolean immutable = false;
        try { content.permissions().clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        h.assertTrue(immutable, "component collection must be immutable");
        var json = BlueprintContent.CODEC.encodeStart(JsonOps.INSTANCE, content).getOrThrow();
        h.assertTrue(BlueprintContent.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(content), "codec roundtrip");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            BlueprintContent.STREAM_CODEC.encode(buffer, content);
            h.assertTrue(BlueprintContent.STREAM_CODEC.decode(buffer).equals(content) && !buffer.isReadable(), "network roundtrip");
        } finally { buffer.release(); }
        var stack = BlueprintRules.create(BATTLESHIP, DESTROYER);
        stack.setCount(37);
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag)stack.save(h.getLevel().registryAccess()));
        h.assertTrue(ItemStack.matches(stack, restored), "item persistence preserves count and permissions");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidAndUnknownPermissions(GameTestHelper h) {
        var blank = new ItemStack(ModItems.SHIP_BLUEPRINT.get());
        h.assertTrue(!BlueprintRules.isValid(blank), "missing component rejected");
        for (var ids : List.of(List.<ResourceLocation>of(), List.of(BATTLESHIP.id(), RADAR.id()),
                List.of(ResourceLocation.parse("piranport:ship/replenishment_ship")),
                List.of(BATTLESHIP.id(), ResourceLocation.parse("piranport:ship/replenishment_ship")),
                List.of(ResourceLocation.parse("othermod:ship/future_type")))) {
            blank.set(ModDataComponents.BLUEPRINT_CONTENT.get(), new BlueprintContent(ids));
            h.assertTrue(!BlueprintRules.isValid(blank) && !BlueprintRules.allowsType(blank, BATTLESHIP), "invalid blueprint grants no permission");
            h.assertTrue(BlueprintRules.merge(List.of(blank, BlueprintRules.create(BATTLESHIP))).isEmpty(), "invalid merge rejected");
            h.assertTrue(!EXPAND.matches(expansion(blank, 1), h.getLevel()), "invalid expansion rejected");
        }
        var json = BlueprintContent.CODEC.encodeStart(JsonOps.INSTANCE, blank.get(ModDataComponents.BLUEPRINT_CONTENT.get())).getOrThrow();
        h.assertTrue(BlueprintContent.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().permissions()
                .equals(List.of(ResourceLocation.parse("othermod:ship/future_type"))), "unknown ID retained through save");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            buffer.writeVarInt(BlueprintContent.MAX_PERMISSIONS + 1);
            boolean rejected = false;
            try { BlueprintContent.STREAM_CODEC.decode(buffer); } catch (io.netty.handler.codec.DecoderException expected) { rejected = true; }
            h.assertTrue(rejected, "network count bounded before allocating");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void specificAndSubclassQueries(GameTestHelper h) {
        var stack = BlueprintRules.create(BATTLESHIP, DESTROYER);
        var snapshot = stack.copy();
        h.assertTrue(BlueprintRules.allowsType(stack, BATTLESHIP) && !BlueprintRules.allowsType(stack, AIRCRAFT_CARRIER), "exact permissions");
        h.assertTrue(BlueprintRules.allowsSubclass(stack, BlueprintSubclass.LARGE) && !BlueprintRules.coversSubclass(stack, BlueprintSubclass.LARGE), "any versus all");
        h.assertTrue(!BlueprintRules.allowsSubclass(stack, BlueprintSubclass.DEVICE), "family isolation");
        h.assertTrue(BlueprintRules.getAllowedTypes(stack, BlueprintSubclass.SMALL).equals(java.util.Set.of(DESTROYER)), "filtered type set");
        for (var subclass : BlueprintSubclass.values()) h.assertTrue(BlueprintRules.coversSubclass(BlueprintRules.create(subclass), subclass), "complete subclass");
        h.assertTrue(ItemStack.matches(stack, snapshot), "queries have no side effects");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void namesAndTooltipGroups(GameTestHelper h) {
        for (var subclass : BlueprintSubclass.values()) h.assertTrue(nameKey(BlueprintRules.create(subclass)).equals(subclass.itemNameKey()), "single subclass name");
        h.assertTrue(nameKey(BlueprintRules.create(BATTLESHIP, DESTROYER)).equals("item.piranport.ship_blueprint"), "cross subclass family name");
        h.assertTrue(nameKey(BlueprintRules.create(MAIN_GUN, RADAR)).equals("item.piranport.equipment_blueprint"), "equipment family name");
        h.assertTrue(nameKey(BlueprintRules.create(RADAR, SONAR)).endsWith(".device"), "device subclass name");
        var stack = BlueprintRules.create(BATTLESHIP, BATTLECRUISER, DESTROYER);
        List<Component> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
        h.assertTrue(lines.size() == 2, "only covered subclasses, no instructional lines");
        var first = (TranslatableContents) lines.getFirst().getContents();
        h.assertTrue(first.getKey().equals("tooltip.piranport.blueprint.group")
                && ((TranslatableContents)((Component)first.getArgs()[0]).getContents()).getKey().equals(BlueprintSubclass.LARGE.translationKey()), "short subclass group");
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("named"));
        h.assertTrue(stack.getHoverName().getString().equals("named"), "anvil custom name takes precedence");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mergeSlotBoundaries(GameTestHelper h) {
        for (var type : List.of(BATTLESHIP, RADAR)) for (int n = 1; n <= 5; n++) {
            List<ItemStack> inputs = new ArrayList<>(Collections.nCopies(n, BlueprintRules.create(type).copyWithCount(64)));
            var result = MERGE.assemble(CraftingInput.of(3, 3, padded(inputs, 9)), h.getLevel().registryAccess());
            h.assertTrue(result.isEmpty() == (n == 1 || n == 5), "2-4 nonempty slots only");
            if (!result.isEmpty()) h.assertTrue(result.getCount() == 1, "identical permissions always produce exactly one blueprint");
        }
        h.assertTrue(BlueprintRules.merge(List.of(BlueprintRules.create(BATTLESHIP), BlueprintRules.create(RADAR))).isEmpty(), "mixed families rejected");
        h.assertTrue(BlueprintRules.merge(List.of(BlueprintRules.create(BATTLESHIP), new ItemStack(Items.PAPER))).isEmpty(), "extra material rejected");
        h.succeed();
    }

    private static List<ItemStack> padded(List<ItemStack> list, int count) {
        List<ItemStack> result = new ArrayList<>(list);
        while (result.size() < count) result.add(ItemStack.EMPTY);
        return result;
    }

    @GameTest(template = "empty")
    public static void mergeUnionAndPreviewPurity(GameTestHelper h) {
        var a = BlueprintRules.create(BATTLESHIP, BATTLECRUISER); a.setCount(64);
        var b = BlueprintRules.create(BATTLECRUISER, AIRCRAFT_CARRIER); b.setCount(64);
        var result = BlueprintRules.merge(List.of(a, b));
        h.assertTrue(result.getCount() == 1 && BlueprintRules.coversSubclass(result, BlueprintSubclass.LARGE), "union deduplicates; output is one regardless of input amounts");
        h.assertTrue(ItemStack.matches(result, BlueprintRules.merge(List.of(b, a))), "input order independent");
        result.setCount(7);
        h.assertTrue(a.getCount() == 64 && b.getCount() == 64, "preview/output mutation never changes inputs");
        h.assertTrue(BlueprintRules.merge(List.of(a, a)).getCount() == 1, "identical permissions still merge into one");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void namesAndExtraComponents(GameTestHelper h) {
        var a = BlueprintRules.create(BATTLESHIP); var b = BlueprintRules.create(DESTROYER);
        a.set(DataComponents.CUSTOM_NAME, Component.literal("same"));
        b.set(DataComponents.CUSTOM_NAME, Component.literal("same"));
        h.assertTrue(BlueprintRules.merge(List.of(a, b)).getHoverName().getString().equals("same"), "equal names retained");
        b.set(DataComponents.CUSTOM_NAME, Component.literal("other"));
        h.assertTrue(!BlueprintRules.merge(List.of(a, b)).has(DataComponents.CUSTOM_NAME), "different names reset");
        b.remove(DataComponents.CUSTOM_NAME);
        h.assertTrue(!BlueprintRules.merge(List.of(a, b)).has(DataComponents.CUSTOM_NAME), "named and unnamed reset");
        a.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(17));
        h.assertTrue(BlueprintRules.merge(List.of(a, b)).isEmpty(), "mismatched extra components rejected");
        b.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(17));
        h.assertTrue(BlueprintRules.merge(List.of(a, b)).get(DataComponents.CUSTOM_MODEL_DATA).value() == 17, "identical extras retained");
        var expanded = EXPAND.assemble(expansion(a, 1), h.getLevel().registryAccess());
        h.assertTrue(expanded.getCount() == 8 && ItemStack.isSameItemSameComponents(expanded, a), "expansion preserves complete component patch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void orderedExpansionLayout(GameTestHelper h) {
        for (var family : BlueprintFamily.values()) {
            var input = expansion(BlueprintRules.create(family == BlueprintFamily.SHIP ? BATTLESHIP : RADAR), 64);
            h.assertTrue(EXPAND.matches(input, h.getLevel()), "correct tag layout");
            h.assertTrue(input.getItem(0).is(BlueprintExpandRecipe.PAPERS) && input.getItem(1).is(BlueprintExpandRecipe.BLUE_DYES), "vanilla items use common tags");
            for (int wrong = 0; wrong < 9; wrong++) {
                var moved = new ArrayList<>(input.items());
                moved.set(wrong, ItemStack.EMPTY);
                h.assertTrue(!EXPAND.matches(CraftingInput.of(3, 3, moved), h.getLevel()), "every occupied slot required");
            }
            for (int slot : List.of(0, 2, 3, 5, 6, 7, 8)) {
                var rotated = new ArrayList<>(input.items());
                Collections.swap(rotated, 1, slot);
                h.assertTrue(!EXPAND.matches(CraftingInput.of(3, 3, rotated), h.getLevel()), "dye must be top middle");
            }
            h.assertTrue(EXPAND.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "no original returned");
            var result = EXPAND.assemble(input, h.getLevel().registryAccess());
            h.assertTrue(result.getCount() == 8 && input.items().stream().allMatch(stack -> stack.getCount() == 64), "preview count and purity");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void backpackAndWorkbenchSettlement(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var backpack = player.inventoryMenu;
        backpack.getSlot(1).set(BlueprintRules.create(BATTLESHIP).copyWithCount(64));
        backpack.getSlot(4).set(BlueprintRules.create(DESTROYER).copyWithCount(64));
        h.assertTrue(backpack.getSlot(0).getItem().getCount() == 1, "backpack arbitrary positions produce one blueprint");
        var taken = backpack.getSlot(0).remove(1);
        backpack.getSlot(0).onTake(player, taken);
        h.assertTrue(backpack.getSlot(1).getItem().getCount() == 63 && backpack.getSlot(4).getItem().getCount() == 63, "exactly one per slot debited");
        for (int count = 2; count <= 4; count++) {
            for (int i = 1; i <= 4; i++) backpack.getSlot(i).set(i <= count ? BlueprintRules.create(BATTLESHIP).copyWithCount(2) : ItemStack.EMPTY);
            var identical = backpack.getSlot(0).remove(1);
            h.assertTrue(identical.getCount() == 1 && BlueprintRules.allowsType(identical, BATTLESHIP), "identical inputs produce one valid result");
            backpack.getSlot(0).onTake(player, identical);
            for (int i = 1; i <= count; i++) h.assertTrue(backpack.getSlot(i).getItem().getCount() == 1, "identical inputs consume one per participating slot without remainder");
        }
        var menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2))));
        var input = expansion(BlueprintRules.create(RADAR), 2);
        for (int i = 0; i < 9; i++) menu.getSlot(i + 1).set(input.getItem(i));
        h.assertTrue(menu.getSlot(0).getItem().getCount() == 8, "workbench preview");
        var output = menu.getSlot(0).remove(8);
        menu.getSlot(0).onTake(player, output);
        for (int i = 1; i <= 9; i++) h.assertTrue(menu.getSlot(i).getItem().getCount() == 1, "one of each ingredient consumed");
        h.assertTrue(BlueprintRules.allowsType(output, RADAR), "output retains original permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftCraftingAndBlockedOutput(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var menu = player.inventoryMenu;
        menu.getSlot(1).set(BlueprintRules.create(BATTLESHIP).copyWithCount(3));
        menu.getSlot(2).set(BlueprintRules.create(DESTROYER).copyWithCount(3));
        for (int i = 0; i < 3; i++) h.assertTrue(menu.quickMoveStack(player, 0).getCount() == 1, "one shift iteration outputs one");
        h.assertTrue(menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty(), "three iterations consumed three per slot");
        h.assertTrue(player.getInventory().items.stream().filter(BlueprintRules::isValid).mapToInt(ItemStack::getCount).sum() == 3, "three merges produce exactly three blueprints");
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        menu.getSlot(1).set(BlueprintRules.create(BATTLESHIP).copyWithCount(64));
        menu.getSlot(2).set(BlueprintRules.create(DESTROYER).copyWithCount(64));
        h.assertTrue(menu.quickMoveStack(player, 0).isEmpty(), "blocked inventory prevents shift transfer");
        h.assertTrue(menu.getSlot(1).getItem().getCount() == 64 && menu.getSlot(2).getItem().getCount() == 64, "blocked output consumes nothing");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void crafterExpansionAndFeedback(GameTestHelper h) {
        BlockPos pos = new BlockPos(6, 2, 6);
        h.setBlock(pos, Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP));
        h.setBlock(pos.east(), Blocks.CHEST);
        var crafter = (CrafterBlockEntity)h.getBlockEntity(pos);
        var chest = (ChestBlockEntity)h.getBlockEntity(pos.east());
        var input = expansion(BlueprintRules.create(RADAR, SONAR), 1);
        for (int i = 0; i < 9; i++) crafter.setItem(i, i == 4 ? input.getItem(i) : input.getItem(i).copyWithCount(2));
        h.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(8, () -> {
            h.assertTrue(chest.countItem(ModItems.EQUIPMENT_BLUEPRINT.get()) == 8 && crafter.getItem(4).isEmpty(), "first pulse outputs eight and consumes original");
            var output = chest.removeItem(0, 1);
            h.assertTrue(BlueprintRules.allowsType(output, SONAR), "automated output preserves permissions");
            crafter.setItem(4, output);
            h.setBlock(pos.west(), Blocks.AIR);
            h.runAfterDelay(2, () -> {
                h.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
                h.runAfterDelay(8, () -> {
                    h.assertTrue(chest.countItem(ModItems.EQUIPMENT_BLUEPRINT.get()) == 15 && crafter.isEmpty(), "feedback consumes one, nets seven more without remainder");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 50)
    public static void crafterMergeSettlement(GameTestHelper h) {
        BlockPos pos = new BlockPos(6, 2, 6);
        h.setBlock(pos, Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, FrontAndTop.EAST_UP));
        h.setBlock(pos.east(), Blocks.CHEST);
        var crafter = (CrafterBlockEntity)h.getBlockEntity(pos);
        crafter.setItem(0, BlueprintRules.create(BATTLESHIP).copyWithCount(64));
        crafter.setItem(8, BlueprintRules.create(DESTROYER).copyWithCount(64));
        h.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(8, () -> {
            var chest = (ChestBlockEntity)h.getBlockEntity(pos.east());
            h.assertTrue(chest.countItem(ModItems.SHIP_BLUEPRINT.get()) == 1, "automated merge outputs exactly one");
            h.assertTrue(crafter.getItem(0).getCount() == 63 && crafter.getItem(8).getCount() == 63, "one per slot debited");
            h.assertTrue(BlueprintRules.getAllowedTypes(chest.getItem(0)).size() == 2, "automation preserves union");
            h.setBlock(pos.west(), Blocks.AIR);
            for (int i = 0; i < 9; i++) crafter.setItem(i, List.of(0, 2, 6, 8).contains(i) ? BlueprintRules.create(BATTLESHIP).copyWithCount(2) : ItemStack.EMPTY);
            h.runAfterDelay(2, () -> {
                h.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
                h.runAfterDelay(8, () -> {
                    h.assertTrue(chest.countItem(ModItems.SHIP_BLUEPRINT.get()) == 2, "four identical inputs add only one more output");
                    for (int i : List.of(0, 2, 6, 8)) h.assertTrue(crafter.getItem(i).getCount() == 1, "identical crafter inputs consume one per slot");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void recipesLoadedAndStackEquality(GameTestHelper h) {
        var manager = h.getLevel().getRecipeManager();
        for (String id : List.of("blueprint_merge", "blueprint_expand")) h.assertTrue(manager.byKey(ResourceLocation.parse("piranport:" + id)).isPresent(), "generated recipe loaded");
        var input = expansion(BlueprintRules.create(BATTLESHIP), 1);
        h.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow().value() instanceof BlueprintExpandRecipe, "recipe manager finds dynamic expansion");
        var a = BlueprintRules.create(BATTLESHIP, DESTROYER);
        var b = BlueprintRules.create(DESTROYER, BATTLESHIP);
        h.assertTrue(ItemStack.isSameItemSameComponents(a, b) && a.getMaxStackSize() == 64, "equal normalized content stacks");
        h.assertTrue(!ItemStack.isSameItemSameComponents(a, BlueprintRules.create(BATTLESHIP)), "different content never stacks directly");
        var tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess()));
        var examples = tab.getDisplayItems().stream().filter(stack -> stack.getItem() instanceof BlueprintItem).toList();
        h.assertTrue(examples.size() == 6 && examples.stream().allMatch(BlueprintRules::isValid), "six valid creative examples, no empty blueprint");
        for (var subclass : BlueprintSubclass.values()) h.assertTrue(examples.stream().anyMatch(stack -> BlueprintRules.coversSubclass(stack, subclass)), "all subclasses represented");
        h.succeed();
    }
}
