package com.lex0v0.piranport;

import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class FuelAndTagsGameTests {
    private static final BlockPos POS = new BlockPos(5, 2, 5);

    private static void fuel(GameTestHelper h, boolean refined, Block appliance, int duration) {
        h.setBlock(POS, appliance);
        var furnace = (AbstractFurnaceBlockEntity) h.getBlockEntity(POS);
        var input = appliance == Blocks.FURNACE ? Items.COBBLESTONE
                : appliance == Blocks.BLAST_FURNACE ? Items.IRON_ORE : Items.BEEF;
        var output = appliance == Blocks.FURNACE ? Items.STONE
                : appliance == Blocks.BLAST_FURNACE ? Items.IRON_INGOT : Items.COOKED_BEEF;
        var bucket = new ItemStack(refined ? ModItems.FUEL_OIL_BUCKET.get() : ModItems.CRUDE_OIL_BUCKET.get());
        h.assertTrue(furnace.canPlaceItem(1, bucket), "fuel slot must accept oil bucket");
        furnace.setItem(0, new ItemStack(input, 64));
        furnace.setItem(1, bucket);
        AbstractFurnaceBlockEntity.serverTick(h.getLevel(), h.absolutePos(POS), furnace.getBlockState(), furnace);
        h.assertTrue(furnace.saveWithFullMetadata(h.getLevel().registryAccess()).getInt("BurnTime") == duration,
                "exact duration including blast furnace / smoker halving");
        for (int i = 1; i <= duration; i++)
            AbstractFurnaceBlockEntity.serverTick(h.getLevel(), h.absolutePos(POS), furnace.getBlockState(), furnace);
        int count = refined ? 60 : 24;
        h.assertTrue(furnace.getItem(2).is(output) && furnace.getItem(2).getCount() == count,
                "one oil bucket must cook exactly " + count + " standard items");
        h.assertTrue(furnace.getItem(0).getCount() == 64 - count
                && furnace.getItem(1).is(Items.BUCKET) && furnace.getItem(1).getCount() == 1,
                "remaining input and exactly one returned bucket");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void crudeFurnace(GameTestHelper h) { fuel(h, false, Blocks.FURNACE, 4800); }

    @GameTest(template = "empty")
    public static void refinedFurnace(GameTestHelper h) { fuel(h, true, Blocks.FURNACE, 12000); }

    @GameTest(template = "empty")
    public static void crudeBlast(GameTestHelper h) { fuel(h, false, Blocks.BLAST_FURNACE, 2400); }

    @GameTest(template = "empty")
    public static void refinedBlast(GameTestHelper h) { fuel(h, true, Blocks.BLAST_FURNACE, 6000); }

    @GameTest(template = "empty")
    public static void crudeSmoker(GameTestHelper h) { fuel(h, false, Blocks.SMOKER, 2400); }

    @GameTest(template = "empty")
    public static void refinedSmoker(GameTestHelper h) { fuel(h, true, Blocks.SMOKER, 6000); }

    @GameTest(template = "empty")
    public static void barrelIsNotFuel(GameTestHelper h) {
        var stack = new ItemStack(ModItems.FUEL_BARREL_ITEM.get());
        h.assertTrue(stack.getBurnTime(null) == 0, "barrel is not a fuel");
        h.setBlock(POS, Blocks.FURNACE);
        h.assertTrue(!((AbstractFurnaceBlockEntity) h.getBlockEntity(POS)).canPlaceItem(1, stack), "fuel slot rejects barrel");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void commonItemAndBlockTags(GameTestHelper h) {
        String[][] items = {
            {"aluminum_ingot", "ingots/aluminum", "ingots"}, {"steel_ingot", "ingots/steel", "ingots"},
            {"aluminum_nugget", "nuggets/aluminum", "nuggets"}, {"steel_nugget", "nuggets/steel", "nuggets"},
            {"bauxite", "raw_materials/bauxite", "raw_materials"},
            {"bauxite_ore", "ores/bauxite", "ores"}, {"saltpeter_block", "ores/saltpeter", "ores"},
            {"bauxite_block", "storage_blocks/raw_bauxite", "storage_blocks"},
            {"aluminum_block", "storage_blocks/aluminum", "storage_blocks"},
            {"steel_block", "storage_blocks/steel", "storage_blocks"}
        };
        for (var row : items) for (int i = 1; i < row.length; i++) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("piranport:" + row[0]));
            h.assertTrue(item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM,
                    ResourceLocation.parse("c:" + row[i]))), "item tag: " + row[0] + " / " + row[i]);
            if (row[1].startsWith("ores/") || row[1].startsWith("storage_blocks/")) {
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("piranport:" + row[0]));
                h.assertTrue(block.builtInRegistryHolder().is(TagKey.create(Registries.BLOCK,
                        ResourceLocation.parse("c:" + row[i]))), "block tag: " + row[0] + " / " + row[i]);
            }
        }
        h.assertTrue(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ModBlocks.BAUXITE_ORE.get().defaultBlockState()), "stone pickaxe works");
        h.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(ModBlocks.BAUXITE_ORE.get().defaultBlockState()), "wooden pickaxe rejected");
        h.assertTrue(new ItemStack(Items.IRON_SHOVEL).getDestroySpeed(ModBlocks.SALTPETER_BLOCK.get().defaultBlockState())
                > new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(ModBlocks.SALTPETER_BLOCK.get().defaultBlockState()), "shovel tag speeds up saltpeter");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void beaconPaymentAndEffect(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        for (var item : java.util.List.of(ModItems.ALUMINUM_INGOT.get(), ModItems.STEEL_INGOT.get())) {
            var menu = new BeaconMenu(1, player.getInventory());
            var stack = new ItemStack(item);
            h.assertTrue(menu.getSlot(0).mayPlace(stack), "beacon payment slot accepts " + item);
            menu.setData(0, 1);
            menu.getSlot(0).set(stack);
            menu.updateEffects(java.util.Optional.of(MobEffects.MOVEMENT_SPEED), java.util.Optional.empty());
            h.assertTrue(!menu.hasPayment() && MobEffects.MOVEMENT_SPEED.equals(menu.getPrimaryEffect()), "payment consumed and effect selected");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void mixedBeaconBaseActivates(GameTestHelper h) {
        for (int x = 4; x <= 6; x++) for (int z = 4; z <= 6; z++)
            h.setBlock(new BlockPos(x, 1, z), (x + z) % 2 == 0 ? ModBlocks.ALUMINUM_BLOCK.get() : ModBlocks.STEEL_BLOCK.get());
        h.setBlock(POS, Blocks.BEACON);
        h.runAfterDelay(100, () -> {
            var beacon = (BeaconBlockEntity) h.getBlockEntity(POS);
            h.assertTrue(beacon.saveWithFullMetadata(h.getLevel().registryAccess()).getInt("Levels") == 1
                    && !beacon.getBeamSections().isEmpty(), "mixed aluminum / steel base activates a real beacon");
            h.succeed();
        });
    }
}
