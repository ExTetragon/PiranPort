package com.lex0v0.piranport.recipe;

import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// 合并 2–4 个占用槽的许可，内容相同也固定产出一张。
public final class BlueprintMergeRecipe extends CustomRecipe {
    public BlueprintMergeRecipe(CraftingBookCategory category) { super(category); }

    @Override
    public boolean matches(CraftingInput input, Level level) { return !assemble(input, level.registryAccess()).isEmpty(); }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return BlueprintRules.merge(input.items().stream().filter(stack -> !stack.isEmpty()).toList());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return NonNullList.withSize(input.size(), ItemStack.EMPTY); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.BLUEPRINT_MERGE.get(); }
}
