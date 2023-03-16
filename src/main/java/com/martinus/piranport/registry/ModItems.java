package com.martinus.piranport.registry;

import com.google.common.base.Supplier;
import com.martinus.piranport.PiranPort;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
            ()-> new ItemNameBlockItem(ModBlocks.RICE.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> RICE = register("rice",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> LABLAB_BEAN = register("lablab_bean",
            ()-> new ItemNameBlockItem(ModBlocks.LABLAB_BEAN.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> RED_BEAN = register("red_bean",
            ()-> new ItemNameBlockItem(ModBlocks.RED_BEAN.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> PINEAPPLE_SEED = register("pineapple_seed",
            ()-> new ItemNameBlockItem(ModBlocks.PINEAPPLE.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> PINEAPPLE = register("pineapple",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> CELERY_SEED = register("celery_seed",
            ()-> new ItemNameBlockItem(ModBlocks.CELERY.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> CELERY = register("celery",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> TOMATO_SEED = register("tomato_seed",
            ()-> new ItemNameBlockItem(ModBlocks.TOMATO.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> TOMATO = register("tomato",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> LETTUCE_SEED = register("lettuce_seed",
            ()-> new ItemNameBlockItem(ModBlocks.LETTUCE.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> LETTUCE = register("lettuce",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> SOYBEAN_POD = register("soybean_pod",
            ()-> new ItemNameBlockItem(ModBlocks.SOYBEAN.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Item> SOYBEAN = register("soybean",
            ()-> new Item(new Item.Properties().tab(ModTabs.MODTAB_MAIN)));

    private static <T extends Item> RegistryObject<T> register(final String name, final Supplier<T> item){
        return ITEMS.register(name, item);
    }

}
