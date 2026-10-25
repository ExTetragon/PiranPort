package com.lex0v0.piranport.network;

import com.lex0v0.piranport.network.payload.DevelopmentFactoryActionPayload;
import com.lex0v0.piranport.network.payload.DevelopmentFactorySyncPayload;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.menu.DevelopmentFactoryMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// 两端处理器仅依赖通用菜单，不在专服加载渲染类。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class DevelopmentFactoryNetworking {
    private DevelopmentFactoryNetworking() {}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(DevelopmentFactoryActionPayload.TYPE, DevelopmentFactoryActionPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof DevelopmentFactoryMenu menu && menu.containerId == payload.containerId()) menu.handleAction(payload.action(), payload.data());
        }));
        registrar.playToClient(DevelopmentFactorySyncPayload.TYPE, DevelopmentFactorySyncPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof DevelopmentFactoryMenu menu && menu.containerId == payload.containerId()) menu.receiveState(payload.data());
        }));
    }
}
