package com.martinus.piranport.setup;

import com.martinus.piranport.PiranPort;
import com.martinus.piranport.registry.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = PiranPort.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    public static  void init(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.RICE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LABLAB_BEAN.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.RED_BEAN.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.PINEAPPLE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.CELERY.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TOMATO.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LETTUCE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SOYBEAN.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.WILD_VEGE.get(), RenderType.cutout());
    }
}
