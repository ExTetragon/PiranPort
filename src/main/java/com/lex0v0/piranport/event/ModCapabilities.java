package com.lex0v0.piranport.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModBlockEntities;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.item.FuelBarrelItemFluidHandler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// 为燃料桶与舰装核心暴露存储能力。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class ModCapabilities {
    private ModCapabilities() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FUEL_BARREL.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.SHIPYARD.get(), (entity, side) -> entity.fluid(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SHIPYARD.get(), (entity, side) -> entity.items(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.DEVELOPMENT_FACTORY.get(), (entity, side) -> entity.fluid(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.DEVELOPMENT_FACTORY.get(), (entity, side) -> entity.items(side));
        event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> RiggingCoreRules.inventory(stack), ModItems.RIGGING_CORE.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> RiggingCoreRules.fuel(stack), ModItems.RIGGING_CORE.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FuelBarrelItemFluidHandler(stack), ModItems.FUEL_BARREL_ITEM.get());
        for (var category : com.lex0v0.piranport.domain.equipment.EquipmentCategory.values()) {
            if (category.group() == com.lex0v0.piranport.domain.equipment.EquipmentCategory.Group.AIRCRAFT)
                event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> com.lex0v0.piranport.item.equipment.EquipmentRules.fuel(stack), ModItems.equipment(category));
        }
    }
}
