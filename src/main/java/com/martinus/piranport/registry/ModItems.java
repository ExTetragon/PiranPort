package com.martinus.piranport.registry;

import com.google.common.base.Supplier;
import com.martinus.piranport.PiranPort;
import net.minecraft.item.BlockNamedItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PiranPort.MOD_ID);

    public static RegistryObject<Item> MAIN_ICON = register("main_icon",
            ()-> new Item(new Item.Properties()));

    //minerals
    public static RegistryObject<Item> ALUMINUM_INGOT = register("aluminum_ingot",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> BAUXITE = register("bauxite",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> ALUMINUM_NUGGET = register("aluminum_nugget",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static  RegistryObject<Item> SALT = register("salt",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));

    //crops
    public static RegistryObject<Item> RICE_SPIKE = register("rice_spike",
            ()-> new BlockNamedItem(ModBlocks.RICE.get(), new Item.Properties().tab(ModTabs.MODTAB_FOOD)));
    public static RegistryObject<Item> RICE = register("rice",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_FOOD)));

    private static <T extends Item>RegistryObject<T> register(final String name, final Supplier<T> item){
        return ITEMS.register(name, item);
    }
}
