package com.lex0v0.piranport.blockentity.shipyard;

import com.lex0v0.piranport.item.AmmunitionItem;
import com.lex0v0.piranport.recipe.ShipyardRecipe;
import com.lex0v0.piranport.registry.ModItemTags;
import com.lex0v0.piranport.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// 在库存副本中规划扣料，找零与退款只按净费用结算。
public final class ShipyardMaterials {
    public static final int SLOTS = 12;
    private ShipyardMaterials() {}

    public static int value(ItemStack stack, String material) {
        for (int i = 0; i < 3; i++) {
            String category = switch (i) { case 0 -> "nuggets"; case 1 -> "ingots"; default -> "storage_blocks"; };
            if (stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", category + "/" + material))))
                return switch (i) { case 0 -> 1; case 1 -> 9; default -> 81; };
        }
        return 0;
    }

    public static boolean accepts(ItemStack stack) {
        return (stack.is(ModItemTags.AMMUNITION) && AmmunitionItem.points(stack) > 0)
                || value(stack, "steel") > 0 || value(stack, "aluminum") > 0;
    }

    public static long available(List<ItemStack> stacks, String material) {
        return stacks.stream().mapToLong(stack -> (long) value(stack, material) * stack.getCount()).sum();
    }

    public static long ammunition(List<ItemStack> stacks) {
        return stacks.stream().filter(stack -> stack.is(ModItemTags.AMMUNITION))
                .mapToLong(stack -> (long) AmmunitionItem.points(stack) * stack.getCount()).sum();
    }

    public record Plan(List<ItemStack> remaining, List<ItemStack> ammunition, List<ItemStack> change) {}

    public static Plan plan(List<ItemStack> source, ShipyardRecipe recipe) {
        List<ItemStack> remaining = source.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<ItemStack> ammunition = new ArrayList<>();
        List<ItemStack> change = new ArrayList<>();
        int needed = recipe.ammunition();
        for (ItemStack stack : remaining) {
            if (needed <= 0) break;
            int points = stack.is(ModItemTags.AMMUNITION) ? AmmunitionItem.points(stack) : 0;
            if (points == 0) continue;
            int taken = (int) Math.min(stack.getCount(), ((long) needed + points - 1) / points);
            ammunition.add(stack.copyWithCount(taken));
            stack.shrink(taken); needed -= taken * points;
        }
        if (needed > 0) return null;
        if (!spend(remaining, "steel", recipe.steel() * 9, change)
                || !spend(remaining, "aluminum", recipe.aluminum() * 9, change)) return null;
        return new Plan(List.copyOf(remaining), List.copyOf(ammunition), List.copyOf(change));
    }

    private static boolean spend(List<ItemStack> stacks, String material, int required, List<ItemStack> change) {
        int remaining = required;
        for (int unit : new int[]{1, 9, 81}) {
            for (ItemStack stack : stacks) {
                if (remaining <= 0) break;
                if (value(stack, material) != unit) continue;
                int count = (int) Math.min(stack.getCount(), ((long) remaining + unit - 1) / unit);
                stack.shrink(count); remaining -= count * unit;
            }
        }
        if (remaining > 0) return false;
        change.addAll(normalized(material, -remaining));
        return true;
    }

    public static List<ItemStack> normalized(String material, int nuggets) {
        Item ingot = material.equals("steel") ? ModItems.STEEL_INGOT.get() : ModItems.ALUMINUM_INGOT.get();
        Item nugget = material.equals("steel") ? ModItems.STEEL_NUGGET.get() : ModItems.ALUMINUM_NUGGET.get();
        List<ItemStack> result = new ArrayList<>();
        int count = nuggets / 9;
        while (count > 0) { int batch = Math.min(count, 64); result.add(new ItemStack(ingot, batch)); count -= batch; }
        if (nuggets % 9 != 0) result.add(new ItemStack(nugget, nuggets % 9));
        return result;
    }
}
