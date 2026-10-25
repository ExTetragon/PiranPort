package com.lex0v0.piranport.recipe;

import com.lex0v0.piranport.domain.ammunition.AmmunitionType;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import com.lex0v0.piranport.registry.ModRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

// 每份配方描述一次加工，费用以粒和火药件数记录。
public record AmmunitionFactoryRecipe(ResourceLocation target, int count, int copper, int steel, int aluminum, int gunpowder, int seconds)
        implements Recipe<SingleRecipeInput> {
    private static final MapCodec<AmmunitionFactoryRecipe> RAW = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("target").forGetter(AmmunitionFactoryRecipe::target),
            Codec.intRange(1,512).fieldOf("count").forGetter(AmmunitionFactoryRecipe::count),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("copper").forGetter(AmmunitionFactoryRecipe::copper),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("steel").forGetter(AmmunitionFactoryRecipe::steel),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("aluminum").forGetter(AmmunitionFactoryRecipe::aluminum),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("gunpowder").forGetter(AmmunitionFactoryRecipe::gunpowder),
            Codec.intRange(1,Integer.MAX_VALUE/20).fieldOf("seconds").forGetter(AmmunitionFactoryRecipe::seconds)
    ).apply(i,AmmunitionFactoryRecipe::new));
    public static final MapCodec<AmmunitionFactoryRecipe> CODEC=RAW.validate(r -> r.valid() ? DataResult.success(r) : DataResult.error(()->"Invalid ammunition recipe"));
    public AmmunitionFactoryRecipe {
        if (target==null || count<1 || count>512 || copper<0 || steel<0 || aluminum<0 || gunpowder<0
                || (long)copper+steel+aluminum+gunpowder==0 || seconds<1 || seconds>Integer.MAX_VALUE/20)
            throw new IllegalArgumentException("Invalid ammunition recipe");
    }
    public AmmunitionType ammunition() {
        for(var type:AmmunitionType.values()) if(type.id().equals(target)) return type;
        return null;
    }
    public boolean valid() {return ammunition()!=null;}
    public int cost(String material) {return switch(material){case "copper"->copper;case "steel"->steel;case "aluminum"->aluminum;case "gunpowder"->gunpowder;default->throw new IllegalArgumentException(material);};}
    public int ticks() {return seconds*20;}
    public ItemStack result() {return valid()?new ItemStack(ModItems.ammunition(ammunition()),count):ItemStack.EMPTY;}
    public CompoundTag save() {
        CompoundTag t=new CompoundTag();t.putString("Target",target.toString());t.putInt("Count",count);t.putInt("Copper",copper);t.putInt("Steel",steel);
        t.putInt("Aluminum",aluminum);t.putInt("Gunpowder",gunpowder);t.putInt("Seconds",seconds);return t;
    }
    public static AmmunitionFactoryRecipe load(CompoundTag t) {
        try {var r=new AmmunitionFactoryRecipe(ResourceLocation.tryParse(t.getString("Target")),t.getInt("Count"),t.getInt("Copper"),t.getInt("Steel"),t.getInt("Aluminum"),t.getInt("Gunpowder"),t.getInt("Seconds"));return r.valid()?r:null;}
        catch(IllegalArgumentException e){return null;}
    }
    @Override public boolean matches(SingleRecipeInput input,Level level){return false;}
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return result();}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return result();}
    @Override public boolean canCraftInDimensions(int width,int height){return false;}
    @Override public boolean isSpecial(){return true;}
    @Override public boolean isIncomplete(){return !valid();}
    @Override public ItemStack getToastSymbol(){return new ItemStack(ModItems.AMMUNITION_FACTORY.get());}
    @Override public RecipeSerializer<?> getSerializer(){return ModRecipeSerializers.AMMUNITION_FACTORY.get();}
    @Override public RecipeType<?> getType(){return ModRecipeTypes.AMMUNITION_FACTORY.get();}
    public static final class Serializer implements RecipeSerializer<AmmunitionFactoryRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf,AmmunitionFactoryRecipe> STREAM=new StreamCodec<>(){
            @Override public AmmunitionFactoryRecipe decode(RegistryFriendlyByteBuf b){var r=new AmmunitionFactoryRecipe(b.readResourceLocation(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt());if(!r.valid())throw new IllegalArgumentException("Unknown ammunition");return r;}
            @Override public void encode(RegistryFriendlyByteBuf b,AmmunitionFactoryRecipe r){b.writeResourceLocation(r.target);b.writeVarInt(r.count);b.writeVarInt(r.copper);b.writeVarInt(r.steel);b.writeVarInt(r.aluminum);b.writeVarInt(r.gunpowder);b.writeVarInt(r.seconds);}
        };
        @Override public MapCodec<AmmunitionFactoryRecipe> codec(){return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,AmmunitionFactoryRecipe> streamCodec(){return STREAM;}
    }
}
