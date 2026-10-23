package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// 注册油的类型及静止和流动态，方块与桶物品分开注册。
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, PiranPort.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, PiranPort.MODID);

    public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = FLUID_TYPES.register("crude_oil",
            () -> new FluidType(FluidType.Properties.create()
                    .density(900)
                    .viscosity(3000)
                    .canDrown(false)
                    .canSwim(false)
                    .canHydrate(false)
                    .descriptionId("fluid_type.piranport.crude_oil")));

    public static final DeferredHolder<FluidType, FluidType> FUEL_OIL_TYPE = FLUID_TYPES.register("fuel_oil",
            () -> new FluidType(FluidType.Properties.create()
                    .density(850)
                    .viscosity(1500)
                    .canDrown(false)
                    .canSwim(false)
                    .canHydrate(false)
                    .descriptionId("fluid_type.piranport.fuel_oil")));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CRUDE_OIL =
            FLUIDS.register("crude_oil", () -> new BaseFlowingFluid.Source(crudeOilProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_CRUDE_OIL =
            FLUIDS.register("flowing_crude_oil", () -> new BaseFlowingFluid.Flowing(crudeOilProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_OIL =
            FLUIDS.register("fuel_oil", () -> new BaseFlowingFluid.Source(fuelOilProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FUEL_OIL =
            FLUIDS.register("flowing_fuel_oil", () -> new BaseFlowingFluid.Flowing(fuelOilProperties()));

    private ModFluids() {
    }

    private static BaseFlowingFluid.Properties crudeOilProperties() {
        return new BaseFlowingFluid.Properties(CRUDE_OIL_TYPE, CRUDE_OIL, FLOWING_CRUDE_OIL)
                .bucket(ModItems.CRUDE_OIL_BUCKET)
                .block(ModBlocks.CRUDE_OIL_BLOCK)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(20);
    }

    private static BaseFlowingFluid.Properties fuelOilProperties() {
        return new BaseFlowingFluid.Properties(FUEL_OIL_TYPE, FUEL_OIL, FLOWING_FUEL_OIL)
                .bucket(ModItems.FUEL_OIL_BUCKET)
                .block(ModBlocks.FUEL_OIL_BLOCK)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(20);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
