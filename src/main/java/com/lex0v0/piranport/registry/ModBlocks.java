package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册入口。
 *
 * <p>在此处声明 {@code DeferredBlock}，例如：
 * <pre>{@code
 * public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock(
 *         "example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
 * }</pre>
 *
 * <p>如果需要对应的方块物品，可以通过 {@code ModItems.ITEMS.registerSimpleBlockItem(...)} 注册。
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PiranPort.MODID);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
