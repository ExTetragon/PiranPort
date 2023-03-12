package com.martinus.piranport.registry;

import com.google.common.base.Supplier;
import com.martinus.piranport.PiranPort;
import com.martinus.piranport.block.crop.RiceBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PiranPort.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = ModItems.ITEMS;

    //minerals
    public static RegistryObject<Block> BAUXITE_ORE = register("bauxite_ore",
            ()-> new Block(BlockBehaviour.Properties.of(Material.STONE).strength(3.0F,3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Block> DEEPSLATE_BAUXITE_ORE = register("deepslate_bauxite_ore",
            ()-> new Block(BlockBehaviour.Properties.of(Material.STONE).strength(4.5F,3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Block> SALT_BLOCK = register("salt_block",
            ()-> new Block(BlockBehaviour.Properties.of(Material.SAND).strength(0.5F,0.5F).sound(SoundType.SAND)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Block> ALUMINUM_BLOCK = register("aluminum_block",
            ()-> new Block(BlockBehaviour.Properties.of(Material.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));

    //crops
    public static final RegistryObject<Block> RICE = registerBlock("rice",
            ()->new RiceBlock(BlockBehaviour.Properties.of(Material.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.CROP)));


    private static <T extends Block> RegistryObject<T> registerBlock(final String name, final Supplier<T> block){
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistryObject<T> register(final String name, final Supplier<T> block, Function<RegistryObject<T>, Supplier<? extends Item>> item){
        RegistryObject<T> obj = registerBlock(name, block);
        ITEMS.register(name, item.apply(obj));
        return obj;
    }
}
