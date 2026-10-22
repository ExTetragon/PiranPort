package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.block.FuelBarrelBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册入口。
 *
 * <p>对应的方块物品在 {@link ModItems} 中注册，避免方块与物品注册顺序互相依赖。
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PiranPort.MODID);

    // 3.1 矿物资源
    public static final DeferredBlock<Block> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)));

    public static final DeferredBlock<Block> BAUXITE_BLOCK = BLOCKS.register("bauxite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIRT)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)));

    public static final DeferredBlock<Block> ALUMINUM_BLOCK = BLOCKS.register("aluminum_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .strength(5.0F, 6.0F)));

    public static final DeferredBlock<Block> SALTPETER_BLOCK = BLOCKS.register("saltpeter_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.SNARE)
                    .sound(SoundType.SAND)
                    .strength(0.5F, 0.5F)));

    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .strength(6.0F, 10.0F)));

    public static final DeferredBlock<FuelBarrelBlock> FUEL_BARREL = BLOCKS.register("fuel_barrel",
            () -> new FuelBarrelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .strength(3.0F, 6.0F)
                    .pushReaction(PushReaction.BLOCK)));

    // 流体方块
    public static final DeferredBlock<LiquidBlock> CRUDE_OIL_BLOCK = BLOCKS.register("crude_oil",
            () -> new LiquidBlock(ModFluids.CRUDE_OIL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()
                    .liquid()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<LiquidBlock> FUEL_OIL_BLOCK = BLOCKS.register("fuel_oil",
            () -> new LiquidBlock(ModFluids.FUEL_OIL.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()
                    .liquid()
                    .pushReaction(PushReaction.DESTROY)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
