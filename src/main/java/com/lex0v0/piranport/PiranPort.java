package com.lex0v0.piranport;

import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModCreativeTabs;
import com.lex0v0.piranport.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;

/**
 * 皮兰港 PiranPort 主类。
 *
 * <p>注册入口集中在 {@code registry} 包中，这里只负责把它们挂到 mod 事件总线。
 */
@Mod(PiranPort.MODID)
public class PiranPort {
    /** 必须与 {@code META-INF/neoforge.mods.toml} 中的 modId 一致。 */
    public static final String MODID = "piranport";
    public static final Logger LOGGER = LogUtils.getLogger();

    // IEventBus 和 ModContainer 由 FML 自动注入。
    public PiranPort(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 只有本类含有 @SubscribeEvent 方法时才需要注册到游戏事件总线。
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("PiranPort 已加载");
    }
}
