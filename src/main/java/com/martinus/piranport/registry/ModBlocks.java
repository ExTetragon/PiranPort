package com.martinus.piranport.registry;

import com.google.common.base.Supplier;
import com.martinus.piranport.PiranPort;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PiranPort.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = ModItems.ITEMS;

    //矿物
    public static RegistryObject<Block> BAUXITE_ORE = register("bauxite_ore",
            ()-> new Block(AbstractBlock.Properties.of(Material.STONE).strength(3.0F,3.0F).harvestTool(ToolType.PICKAXE).requiresCorrectToolForDrops().harvestLevel(1).sound(SoundType.STONE)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Block> SALT_BLOCK = register("salt_block",
            ()-> new Block(AbstractBlock.Properties.of(Material.SAND).strength(0.5F,0.5F).harvestTool(ToolType.SHOVEL).sound(SoundType.SAND)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));
    public static RegistryObject<Block> ALUMINUM_BLOCK = register("aluminum_block",
            ()-> new Block(AbstractBlock.Properties.of(Material.METAL).strength(3.0F, 6.0F).harvestTool(ToolType.PICKAXE).requiresCorrectToolForDrops().harvestLevel(1).sound(SoundType.METAL)),
            object -> () -> new BlockItem(object.get(), new Item.Properties().tab(ModTabs.MODTAB_MAIN)));

    //作物


    private static <T extends Block> RegistryObject<T> registerBlock(final String name, final Supplier<T> block){
        return BLOCKS.register(name, block);
    }

    private static <T extends Block> RegistryObject<T> register(final String name, final Supplier<T> block, Function<RegistryObject<T>, Supplier<? extends Item>> item) {
        RegistryObject<T> obj = registerBlock(name, block);
        ITEMS.register(name, item.apply(obj));
        return obj;
    }
}
