package com.martinus.piranport.setup;

import com.martinus.piranport.PiranPort;
import com.martinus.piranport.registry.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = PiranPort.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    public static void init (final FMLClientSetupEvent event){
        RenderTypeLookup.setRenderLayer(ModBlocks.RICE.get(), RenderType.cutout());
    }
}
