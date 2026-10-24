package com.lex0v0.piranport.recipe;

import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

// 保留原版有序配方与配方查询，仅额外拒绝含油的燃料桶原料。
public final class EmptyFuelBarrelShapedRecipe extends ShapedRecipe {
    public EmptyFuelBarrelShapedRecipe(ShapedRecipe source){super(source.getGroup(),source.category(),source.pattern,source.getResultItem(null).copy(),source.showNotification());}
    @Override public boolean matches(CraftingInput input,Level level){
        if(!super.matches(input,level))return false;
        for(int i=0;i<input.size();i++)if(input.getItem(i).is(ModItems.FUEL_BARREL_ITEM.get())&&!input.getItem(i).getOrDefault(ModDataComponents.FLUID_CONTENT,SimpleFluidContent.EMPTY).isEmpty())return false;
        return true;
    }
    @Override public RecipeSerializer<?> getSerializer(){return ModRecipeSerializers.EMPTY_FUEL_BARREL_SHAPED.get();}
    public static final class Serializer implements RecipeSerializer<EmptyFuelBarrelShapedRecipe>{
        @Override public MapCodec<EmptyFuelBarrelShapedRecipe> codec(){return ShapedRecipe.Serializer.CODEC.xmap(EmptyFuelBarrelShapedRecipe::new,r->r);}
        @Override public StreamCodec<RegistryFriendlyByteBuf,EmptyFuelBarrelShapedRecipe> streamCodec(){return ShapedRecipe.Serializer.STREAM_CODEC.map(EmptyFuelBarrelShapedRecipe::new,r->r);}
    }
}
