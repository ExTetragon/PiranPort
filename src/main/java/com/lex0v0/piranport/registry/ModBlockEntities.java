package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册燃料桶等方块实体类型。
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PiranPort.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelBarrelBlockEntity>> FUEL_BARREL =
            BLOCK_ENTITIES.register("fuel_barrel", () -> BlockEntityType.Builder
                    .of(FuelBarrelBlockEntity::new, ModBlocks.FUEL_BARREL.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipyardBlockEntity>> SHIPYARD =
            BLOCK_ENTITIES.register("shipyard", () -> BlockEntityType.Builder.of(ShipyardBlockEntity::new, ModBlocks.SHIPYARD.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity>> DEVELOPMENT_FACTORY =
            BLOCK_ENTITIES.register("development_factory", () -> BlockEntityType.Builder.of(com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity::new, ModBlocks.DEVELOPMENT_FACTORY.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
