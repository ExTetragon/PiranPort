package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.blueprint.BlueprintContent;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册燃料内容与蓝图许可的持久化和网络组件。
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PiranPort.MODID);

    // 保存燃料桶掉落物携带的流体内容。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            DATA_COMPONENTS.register("fluid_content", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintContent>> BLUEPRINT_CONTENT =
            DATA_COMPONENTS.register("blueprint_content", () -> DataComponentType.<BlueprintContent>builder()
                    .persistent(BlueprintContent.CODEC)
                    .networkSynchronized(BlueprintContent.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
