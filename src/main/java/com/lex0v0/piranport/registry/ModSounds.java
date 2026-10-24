package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册真实建造完成时播放的短音效。
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, PiranPort.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIPYARD_COMPLETE = SOUNDS.register("shipyard_complete",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "shipyard_complete")));
    private ModSounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
