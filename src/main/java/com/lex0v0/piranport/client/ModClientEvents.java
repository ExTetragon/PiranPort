package com.lex0v0.piranport.client;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import net.minecraft.client.renderer.item.ItemProperties;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.lex0v0.piranport.registry.ModMenus;

// 仅在客户端注册流体外观与核心身份模型属性。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    private static final IClientFluidTypeExtensions CRUDE_OIL = new IClientFluidTypeExtensions() {
        @Override
        public ResourceLocation getStillTexture() {
            return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "block/crude_oil_still");
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "block/crude_oil_flow");
        }

        @Override
        public int getTintColor() {
            return 0xFF1B1B1B;
        }
    };

    private static final IClientFluidTypeExtensions FUEL_OIL = new IClientFluidTypeExtensions() {
        @Override
        public ResourceLocation getStillTexture() {
            return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "block/fuel_oil_still");
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "block/fuel_oil_flow");
        }

        @Override
        public int getTintColor() {
            return 0xFF3A2A12;
        }
    };

    private ModClientEvents() {
    }
    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event) { event.register(ModMenus.SHIPYARD.get(), ShipyardScreen::new); }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var ship : ShipDefinitions.ALL) {
                ItemProperties.register(ModItems.RIGGING_CORE.get(),
                        ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "ship/" + ship.id().getPath()),
                        (stack, level, entity, seed) -> RiggingCoreRules.definition(stack).filter(value -> value.id().equals(ship.id())).isPresent() ? 1.0F : 0.0F);
            }
            ItemBlockRenderTypes.setRenderLayer(ModFluids.CRUDE_OIL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_CRUDE_OIL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FUEL_OIL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_FUEL_OIL.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(CRUDE_OIL, ModFluids.CRUDE_OIL_TYPE.get());
        event.registerFluidType(FUEL_OIL, ModFluids.FUEL_OIL_TYPE.get());
    }
}
