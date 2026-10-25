package com.lex0v0.piranport.blockentity.manufacturing;

import com.lex0v0.piranport.recipe.AmmunitionFactoryRecipe;
import com.lex0v0.piranport.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// 生产线专用四原料规划，不扩展船坞和开发工厂的准入。
public final class AmmunitionMaterials {
    public static final List<String> MATERIALS=List.of("copper","steel","aluminum","gunpowder");
    private AmmunitionMaterials(){}
    public static String material(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.is(Items.GUNPOWDER))return "gunpowder";
        String found=null;
        for(String name:MATERIALS.subList(0,3))if(ManufacturingMaterials.value(stack,name)>0){if(found!=null)return null;found=name;}
        return found;
    }
    public static int value(ItemStack stack,String material){return material.equals(material(stack))?(material.equals("gunpowder")?1:ManufacturingMaterials.value(stack,material)):0;}
    public static boolean accepts(ItemStack stack){return material(stack)!=null;}
    public static int maximum(List<ItemStack> source,AmmunitionFactoryRecipe recipe){
        if(recipe==null||!recipe.valid())return 0;
        long runs=Integer.MAX_VALUE/recipe.count();
        for(String name:MATERIALS)if(recipe.cost(name)>0){long available=0;for(ItemStack stack:source)available+=(long)value(stack,name)*stack.getCount();runs=Math.min(runs,available/recipe.cost(name));}
        return (int)(runs*recipe.count());
    }
    public static int normalize(long requested,int count,int maximum){
        if(requested<=0||count<=0||maximum<count||requested>Integer.MAX_VALUE)return 0;
        return (int)Math.min(((requested+count-1)/count)*count,(long)maximum/count*count);
    }
    public record Plan(List<ItemStack> remaining,List<ItemStack> change){}
    public static Plan plan(List<ItemStack> source,AmmunitionFactoryRecipe recipe){
        if(recipe==null||!recipe.valid())return null;
        List<ItemStack> remaining=source.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<ItemStack> change=new ArrayList<>();
        for(String name:MATERIALS){
            long needed=recipe.cost(name);
            for(int unit:new int[]{1,9,81})for(ItemStack stack:remaining){
                if(needed<=0)break;
                if(value(stack,name)!=unit)continue;
                int taken=(int)Math.min(stack.getCount(),(needed+unit-1)/unit);stack.shrink(taken);needed-=(long)taken*unit;
            }
            if(needed>0)return null;
            if(needed<0)change.addAll(normalized(name,(int)-needed));
        }
        return new Plan(List.copyOf(remaining),List.copyOf(change));
    }
    public static List<ItemStack> normalized(String material,int nuggets){
        if(!material.equals("copper"))return ManufacturingMaterials.normalized(material,nuggets);
        List<ItemStack> result=new ArrayList<>();
        if(nuggets/9>0)result.add(new ItemStack(Items.COPPER_INGOT,nuggets/9));
        if(nuggets%9>0)result.add(new ItemStack(ModItems.COPPER_NUGGET.get(),nuggets%9));
        return result;
    }
}
