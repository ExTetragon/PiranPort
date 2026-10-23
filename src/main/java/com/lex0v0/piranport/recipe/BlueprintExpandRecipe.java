package com.lex0v0.piranport.recipe;

import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// 按固定布局复制图纸并保留组件，实际扣料交给原版合成流程。
public final class BlueprintExpandRecipe extends CustomRecipe {
    public static final TagKey<Item> PAPERS = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:papers"));
    public static final TagKey<Item> BLUE_DYES = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:dyes/blue"));

    public BlueprintExpandRecipe(CraftingBookCategory category) { super(category); }

    private boolean validLayout(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (i == 4 ? !BlueprintRules.isValid(stack) : stack.isEmpty() || !stack.is(i == 1 ? BLUE_DYES : PAPERS)) return false;
        }
        return true;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) { return validLayout(input); }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return validLayout(input) ? input.getItem(4).copyWithCount(8) : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) { return NonNullList.withSize(input.size(), ItemStack.EMPTY); }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width >= 3 && height >= 3; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.BLUEPRINT_EXPAND.get(); }
}
