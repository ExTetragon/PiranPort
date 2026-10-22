package com.lex0v0.piranport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercise the real recipe manager, furnace ticks, consumption and XP creation. */
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class CookingGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);

    private static void cook(GameTestHelper h, String id, String input, String output,
                             boolean blast, int ticks, float xp) {
        var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("piranport:" + id)).orElseThrow().value();
        h.assertTrue(recipe instanceof AbstractCookingRecipe, "cooking recipe must be registered: " + id);
        var cooking = (AbstractCookingRecipe) recipe;
        h.assertTrue(cooking.getCookingTime() == ticks && Math.abs(cooking.getExperience() - xp) < 0.00001,
                "recipe time and XP: " + id);
        h.setBlock(POS, blast ? Blocks.BLAST_FURNACE : Blocks.FURNACE);
        var furnace = (AbstractFurnaceBlockEntity) h.getBlockEntity(POS);
        furnace.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        var ingredient = BuiltInRegistries.ITEM.get(ResourceLocation.parse(input));
        var result = BuiltInRegistries.ITEM.get(ResourceLocation.parse(output));
        for (int batch = 0; batch < 10; batch++) {
            furnace.setItem(0, new ItemStack(ingredient));
            for (int tick = 1; tick <= ticks; tick++) {
                AbstractFurnaceBlockEntity.serverTick(h.getLevel(), h.absolutePos(POS), furnace.getBlockState(), furnace);
                h.assertTrue(tick == ticks || furnace.getItem(2).isEmpty(), "must not finish early: " + id);
            }
            h.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(2).is(result)
                    && furnace.getItem(2).getCount() == 1, "exact output and input consumption: " + id);
            furnace.removeItem(2, 1);
        }
        h.assertTrue(furnace.getItem(1).is(Items.BUCKET) && furnace.getItem(1).getCount() == 1,
                "only fuel container remains; input oil bucket must not return an extra bucket");
        var pop = Vec3.atCenterOf(h.absolutePos(POS));
        var awarded = furnace.getRecipesToAwardAndPopExperience(h.getLevel(), pop);
        int experience = h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(h.absolutePos(POS)).inflate(2))
                .stream().mapToInt(orb -> orb.getValue() * orb.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getInt("Count")).sum();
        h.assertTrue(awarded.size() == 1 && experience >= (int) Math.floor(10 * xp)
                && experience <= (int) Math.ceil(10 * xp), "ten crafts must award expected rounded XP: " + experience);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bauxite_smelting(GameTestHelper h) {
        cook(h, "bauxite_smelting", "piranport:bauxite", "piranport:aluminum_ingot", false, 200, 0.35F);
    }

    @GameTest(template = "empty")
    public static void bauxite_ore_smelting(GameTestHelper h) {
        cook(h, "bauxite_ore_smelting", "piranport:bauxite_ore", "piranport:aluminum_ingot", false, 200, 0.7F);
    }

    @GameTest(template = "empty")
    public static void bauxite_blasting(GameTestHelper h) {
        cook(h, "bauxite_blasting", "piranport:bauxite", "piranport:aluminum_ingot", true, 100, 0.35F);
    }

    @GameTest(template = "empty")
    public static void bauxite_ore_blasting(GameTestHelper h) {
        cook(h, "bauxite_ore_blasting", "piranport:bauxite_ore", "piranport:aluminum_ingot", true, 100, 0.7F);
    }

    @GameTest(template = "empty")
    public static void steel_ingot_blasting(GameTestHelper h) {
        cook(h, "steel_ingot_blasting", "minecraft:iron_ingot", "piranport:steel_ingot", true, 100, 0.3F);
    }

    @GameTest(template = "empty")
    public static void steel_nugget_blasting(GameTestHelper h) {
        cook(h, "steel_nugget_blasting", "minecraft:iron_nugget", "piranport:steel_nugget", true, 12, 0.3F);
    }

    @GameTest(template = "empty")
    public static void steel_block_blasting(GameTestHelper h) {
        cook(h, "steel_block_blasting", "minecraft:iron_block", "piranport:steel_block", true, 900, 0.3F);
    }

    @GameTest(template = "empty")
    public static void crude_oil_bucket_smelting(GameTestHelper h) {
        cook(h, "crude_oil_bucket_smelting", "piranport:crude_oil_bucket", "piranport:fuel_oil_bucket", false, 200, 0.3F);
    }

    @GameTest(template = "empty")
    public static void crude_oil_bucket_blasting(GameTestHelper h) {
        cook(h, "crude_oil_bucket_blasting", "piranport:crude_oil_bucket", "piranport:fuel_oil_bucket", true, 100, 0.3F);
    }

    @GameTest(template = "empty")
    public static void steelRequiresBlastFurnace(GameTestHelper h) {
        for (var item : java.util.List.of(Items.IRON_INGOT, Items.IRON_NUGGET, Items.IRON_BLOCK)) {
            h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                    new SingleRecipeInput(new ItemStack(item)), h.getLevel()).isEmpty(), "steel input must not smelt: " + item);
        }
        h.succeed();
    }
}
