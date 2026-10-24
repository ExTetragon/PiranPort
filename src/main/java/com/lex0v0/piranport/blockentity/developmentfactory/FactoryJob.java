package com.lex0v0.piranport.blockentity.developmentfactory;

import com.lex0v0.piranport.recipe.DevelopmentFactoryRecipe;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

// 保存开工快照和原始弹药，不重新读取配方估算退款。
public final class FactoryJob {
    public final UUID id;
    public final DevelopmentFactoryRecipe recipe;
    public final ItemStack blueprint;
    public final List<ItemStack> ammunition;
    public final FluidStack fuel;
    private int elapsed;

    public FactoryJob(DevelopmentFactoryRecipe recipe, ItemStack blueprint, List<ItemStack> ammunition, FluidStack fuel) {
        this(UUID.randomUUID(), recipe, blueprint, ammunition, fuel, 0);
    }

    private FactoryJob(UUID id, DevelopmentFactoryRecipe recipe, ItemStack blueprint, List<ItemStack> ammunition, FluidStack fuel, int elapsed) {
        this.id = id; this.recipe = recipe; this.blueprint = blueprint.copyWithCount(1);
        this.ammunition = ammunition.stream().map(ItemStack::copy).toList(); this.fuel = fuel.copy();
        this.elapsed = Math.clamp(elapsed, 0, recipe.ticks());
    }

    public int elapsed() { return elapsed; }
    public void tick() { if (elapsed < recipe.ticks()) elapsed++; }
    public boolean finished() { return elapsed >= recipe.ticks(); }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Job", id); tag.putString("Target", recipe.target().toString());
        tag.putBoolean("Model",recipe.model()); tag.putInt("Count",recipe.count());
        tag.putInt("FuelCost", recipe.fuel()); tag.putInt("AmmoCost", recipe.ammunition());
        tag.putInt("Steel", recipe.steel()); tag.putInt("Aluminum", recipe.aluminum()); tag.putInt("Seconds", recipe.seconds());
        tag.putInt("Elapsed", elapsed); tag.put("Blueprint", blueprint.saveOptional(registries));
        tag.put("Fuel", fuel.saveOptional(registries));
        ListTag spent = new ListTag();
        for (ItemStack stack : ammunition) spent.add(stack.saveOptional(registries));
        tag.put("SpentAmmo", spent); return tag;
    }

    public static FactoryJob load(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation target = ResourceLocation.tryParse(tag.getString("Target"));
        if (target == null || !tag.hasUUID("Job")) return null;
        DevelopmentFactoryRecipe recipe = new DevelopmentFactoryRecipe(target, tag.getBoolean("Model"), tag.getInt("Count"), tag.getInt("FuelCost"), tag.getInt("AmmoCost"),
                tag.getInt("Steel"), tag.getInt("Aluminum"), tag.getInt("Seconds"));
        List<ItemStack> spent = tag.getList("SpentAmmo", Tag.TAG_COMPOUND).stream()
                .map(entry -> ItemStack.parseOptional(registries, (CompoundTag) entry)).filter(stack -> !stack.isEmpty()).toList();
        return new FactoryJob(tag.getUUID("Job"), recipe, ItemStack.parseOptional(registries, tag.getCompound("Blueprint")),
                spent, FluidStack.parseOptional(registries, tag.getCompound("Fuel")), tag.getInt("Elapsed"));
    }
}
