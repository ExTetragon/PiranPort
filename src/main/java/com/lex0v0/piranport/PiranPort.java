package com.lex0v0.piranport;

import com.lex0v0.piranport.registry.ModBlockEntities;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModCreativeTabs;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModFeatures;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModRecipeSerializers;
import com.lex0v0.piranport.registry.ModItemPredicates;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import org.slf4j.Logger;

// 挂接模组注册、公共配置和生命周期事件。
@Mod(PiranPort.MODID)
public class PiranPort {
    // 模组命名空间，与元数据保持一致。
    public static final String MODID = "piranport";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PiranPort(IEventBus modEventBus, ModContainer modContainer) {
        // 按依赖顺序挂接注册器，流体先于方块和桶物品。
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModItemPredicates.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 挂接服务端生命周期监听。
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("PiranPort 已加载");
    }
}
