package com.lex0v0.piranport;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;

/**
 * 皮兰港 PiranPort 主类。
 *
 * <p>注册入口集中在这里；后续内容可以按功能拆分到单独的注册类中。
 */
@Mod(PiranPort.MODID)
public class PiranPort {
    /** 必须与 {@code META-INF/neoforge.mods.toml} 中的 modId 一致。 */
    public static final String MODID = "piranport";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 注册器在构造函数中挂到 mod 事件总线上。
    // 示例：public static final DeferredItem<Item> EXAMPLE_ITEM =
    //         ITEMS.registerSimpleItem("example_item", new Item.Properties());
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // IEventBus 和 ModContainer 由 FML 自动注入。
    public PiranPort(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 只有本类含有 @SubscribeEvent 方法时才需要注册到游戏事件总线。
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("PiranPort 已加载");
    }
}
