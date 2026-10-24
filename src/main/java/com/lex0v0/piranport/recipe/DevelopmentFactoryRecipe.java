package com.lex0v0.piranport.recipe;

import com.lex0v0.piranport.blockentity.developmentfactory.FactoryCatalog;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import com.lex0v0.piranport.registry.ModRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

// 费用与产量属于单次任务，型号输出和普通消耗品输出明确区分。
public record DevelopmentFactoryRecipe(ResourceLocation target, boolean model, int count, int fuel, int ammunition, int steel, int aluminum, int seconds)
        implements Recipe<SingleRecipeInput>, ManufacturingCost {
    private static final Codec<Boolean> KIND=Codec.STRING.comapFlatMap(value -> switch(value){case "model" -> DataResult.success(true);case "item" -> DataResult.success(false);default -> DataResult.error(()->"Unknown development output kind");},value->value?"model":"item");
    private static final MapCodec<DevelopmentFactoryRecipe> RAW=RecordCodecBuilder.mapCodec(i->i.group(
            ResourceLocation.CODEC.fieldOf("target").forGetter(DevelopmentFactoryRecipe::target),
            KIND.fieldOf("kind").forGetter(DevelopmentFactoryRecipe::model),
            Codec.intRange(1,64).fieldOf("count").forGetter(DevelopmentFactoryRecipe::count),
            Codec.intRange(0,16000).fieldOf("fuel").forGetter(DevelopmentFactoryRecipe::fuel),
            Codec.intRange(0,Integer.MAX_VALUE).validate(v->v%4==0?DataResult.success(v):DataResult.error(()->"Ammunition must be a multiple of four")).fieldOf("ammunition").forGetter(DevelopmentFactoryRecipe::ammunition),
            Codec.intRange(0,Integer.MAX_VALUE/9).fieldOf("steel").forGetter(DevelopmentFactoryRecipe::steel),
            Codec.intRange(0,Integer.MAX_VALUE/9).fieldOf("aluminum").forGetter(DevelopmentFactoryRecipe::aluminum),
            Codec.intRange(1,Integer.MAX_VALUE/20).fieldOf("seconds").forGetter(DevelopmentFactoryRecipe::seconds)
    ).apply(i,DevelopmentFactoryRecipe::new));
    public static final MapCodec<DevelopmentFactoryRecipe> CODEC=RAW.flatXmap(r->r.knownTarget()?DataResult.success(r):DataResult.error(()->"Invalid development output"),DataResult::success);
    public DevelopmentFactoryRecipe {
        if(target==null||count<1||count>64||fuel<0||fuel>16000||ammunition<0||ammunition%4!=0||steel<0||steel>Integer.MAX_VALUE/9||aluminum<0||aluminum>Integer.MAX_VALUE/9||seconds<1||seconds>Integer.MAX_VALUE/20)
            throw new IllegalArgumentException("Invalid development recipe");
    }
    public boolean knownTarget(){var entry=FactoryCatalog.byId(target);return entry!=null&&entry.model()==model&&count<=entry.maxStack();}
    public int ticks(){return seconds*20;}
    public ItemStack result(){return knownTarget()?FactoryCatalog.byId(target).create(count):ItemStack.EMPTY;}
    @Override public boolean matches(SingleRecipeInput input,Level level){return false;}
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries){return result();}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return result();}
    @Override public boolean canCraftInDimensions(int width,int height){return false;}
    @Override public boolean isSpecial(){return true;}
    @Override public boolean isIncomplete(){return !knownTarget();}
    @Override public ItemStack getToastSymbol(){return new ItemStack(ModItems.DEVELOPMENT_FACTORY.get());}
    @Override public RecipeSerializer<?> getSerializer(){return ModRecipeSerializers.DEVELOPMENT_FACTORY.get();}
    @Override public RecipeType<?> getType(){return ModRecipeTypes.DEVELOPMENT_FACTORY.get();}
    public static final class Serializer implements RecipeSerializer<DevelopmentFactoryRecipe>{
        private static final StreamCodec<RegistryFriendlyByteBuf,DevelopmentFactoryRecipe> STREAM=new StreamCodec<>(){
            @Override public DevelopmentFactoryRecipe decode(RegistryFriendlyByteBuf b){var r=new DevelopmentFactoryRecipe(b.readResourceLocation(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt());if(!r.knownTarget())throw new IllegalArgumentException("Invalid output");return r;}
            @Override public void encode(RegistryFriendlyByteBuf b,DevelopmentFactoryRecipe r){b.writeResourceLocation(r.target);b.writeBoolean(r.model);b.writeVarInt(r.count);b.writeVarInt(r.fuel);b.writeVarInt(r.ammunition);b.writeVarInt(r.steel);b.writeVarInt(r.aluminum);b.writeVarInt(r.seconds);}
        };
        @Override public MapCodec<DevelopmentFactoryRecipe> codec(){return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,DevelopmentFactoryRecipe> streamCodec(){return STREAM;}
    }
}
