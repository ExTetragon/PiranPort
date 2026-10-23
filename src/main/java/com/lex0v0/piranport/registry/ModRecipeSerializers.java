package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.recipe.BlueprintExpandRecipe;
import com.lex0v0.piranport.recipe.BlueprintMergeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册图纸合并与扩展配方的序列化器。
public final class ModRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, PiranPort.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<BlueprintMergeRecipe>> BLUEPRINT_MERGE =
            SERIALIZERS.register("blueprint_merge", () -> new SimpleCraftingRecipeSerializer<>(BlueprintMergeRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<BlueprintExpandRecipe>> BLUEPRINT_EXPAND =
            SERIALIZERS.register("blueprint_expand", () -> new SimpleCraftingRecipeSerializer<>(BlueprintExpandRecipe::new));

    private ModRecipeSerializers() {}
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
}
