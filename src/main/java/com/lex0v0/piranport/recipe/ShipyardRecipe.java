package com.lex0v0.piranport.recipe;

import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import com.lex0v0.piranport.registry.ModRecipeTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import com.mojang.serialization.Codec;

// 船坞按具体身份查询配方，开工后保存独立费用快照。
public record ShipyardRecipe(ResourceLocation ship, int fuel, int ammunition, int steel, int aluminum, int seconds)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<ShipyardRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.validate(id -> ShipDefinitions.byId(id) == null ? DataResult.error(() -> "Unknown ship: " + id) : DataResult.success(id))
                    .fieldOf("ship").forGetter(ShipyardRecipe::ship),
            Codec.intRange(0, 32000).fieldOf("fuel").forGetter(ShipyardRecipe::fuel),
            Codec.intRange(0, Integer.MAX_VALUE).validate(value -> value % 4 == 0 ? DataResult.success(value)
                    : DataResult.error(() -> "Ammunition cost must be a multiple of 4")).fieldOf("ammunition").forGetter(ShipyardRecipe::ammunition),
            Codec.intRange(0, Integer.MAX_VALUE / 9).fieldOf("steel").forGetter(ShipyardRecipe::steel),
            Codec.intRange(0, Integer.MAX_VALUE / 9).fieldOf("aluminum").forGetter(ShipyardRecipe::aluminum),
            Codec.intRange(1, Integer.MAX_VALUE / 20).fieldOf("seconds").forGetter(ShipyardRecipe::seconds)
    ).apply(instance, ShipyardRecipe::new));

    public ShipyardRecipe {
        // 仅约束合法取值和粒、tick 换算，初期平衡倾向不构成配方上限。
        if (fuel < 0 || fuel > 32000 || ammunition < 0 || ammunition % 4 != 0 || steel < 0 || steel > Integer.MAX_VALUE / 9
                || aluminum < 0 || aluminum > Integer.MAX_VALUE / 9 || seconds < 1 || seconds > Integer.MAX_VALUE / 20)
            throw new IllegalArgumentException("Invalid shipyard recipe values");
    }

    public int ticks() { return seconds * 20; }
    public boolean knownShip() { return ShipDefinitions.byId(ship) != null; }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return false; }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) { return RiggingCoreRules.create(ship); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return knownShip() ? RiggingCoreRules.create(ship) : ItemStack.EMPTY; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(ModItems.SHIPYARD.get()); }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean isIncomplete() { return !knownShip(); }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.SHIPYARD.get(); }
    @Override public RecipeType<?> getType() { return ModRecipeTypes.SHIPYARD.get(); }

    public static final class Serializer implements RecipeSerializer<ShipyardRecipe> {
        private static final StreamCodec<RegistryFriendlyByteBuf, ShipyardRecipe> STREAM = new StreamCodec<>() {
            @Override public ShipyardRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new ShipyardRecipe(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, ShipyardRecipe recipe) {
                buffer.writeResourceLocation(recipe.ship); buffer.writeVarInt(recipe.fuel); buffer.writeVarInt(recipe.ammunition);
                buffer.writeVarInt(recipe.steel); buffer.writeVarInt(recipe.aluminum); buffer.writeVarInt(recipe.seconds);
            }
        };
        @Override public MapCodec<ShipyardRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShipyardRecipe> streamCodec() { return STREAM; }
    }
}
