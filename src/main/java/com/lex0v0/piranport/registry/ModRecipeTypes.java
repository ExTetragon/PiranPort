package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.recipe.ShipyardRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册制造设施使用的配方类型。
public final class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, PiranPort.MODID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ShipyardRecipe>> SHIPYARD =
            TYPES.register("shipyard", () -> new RecipeType<>() { @Override public String toString() { return "piranport:shipyard"; } });
    private ModRecipeTypes() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
