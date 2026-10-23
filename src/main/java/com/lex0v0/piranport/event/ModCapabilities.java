package com.lex0v0.piranport.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModBlockEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// 为燃料桶暴露共享流体能力。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class ModCapabilities {
    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FUEL_BARREL.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler(side));
    }
}
