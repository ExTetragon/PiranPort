package com.lex0v0.piranport.network;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.menu.AmmunitionFactoryMenu;
import com.lex0v0.piranport.network.payload.AmmunitionFactoryActionPayload;
import com.lex0v0.piranport.network.payload.AmmunitionFactorySyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

// 两端入口只依赖公共菜单，不加载客户端界面类。
@EventBusSubscriber(modid=PiranPort.MODID)
public final class AmmunitionFactoryNetworking {
    private AmmunitionFactoryNetworking(){}
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        var r=event.registrar("1");
        r.playToServer(AmmunitionFactoryActionPayload.TYPE,AmmunitionFactoryActionPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player().containerMenu instanceof AmmunitionFactoryMenu m&&m.containerId==p.containerId())m.handleAction(p.action(),p.data());
        }));
        r.playToClient(AmmunitionFactorySyncPayload.TYPE,AmmunitionFactorySyncPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player().containerMenu instanceof AmmunitionFactoryMenu m&&m.containerId==p.containerId())m.receiveState(p.state(),p.browse());
        }));
    }
}
