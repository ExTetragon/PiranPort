package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.blueprint.BlueprintContent;
import com.lex0v0.piranport.item.rigging.RiggingIdentity;
import com.lex0v0.piranport.item.rigging.RiggingProficiency;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册燃油、蓝图许可与舰装核心的不可变组件。
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PiranPort.MODID);

    // 保存燃料桶掉落物携带的流体内容。
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            DATA_COMPONENTS.register("fluid_content", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.lex0v0.piranport.item.equipment.EquipmentIdentity>> EQUIPMENT_IDENTITY =
            DATA_COMPONENTS.register("equipment_identity", () -> DataComponentType.<com.lex0v0.piranport.item.equipment.EquipmentIdentity>builder()
                    .persistent(com.lex0v0.piranport.item.equipment.EquipmentIdentity.CODEC)
                    .networkSynchronized(com.lex0v0.piranport.item.equipment.EquipmentIdentity.STREAM_CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> DEVELOPMENT_FACTORY_STORAGE =
            DATA_COMPONENTS.register("development_factory_storage", () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());

    private ModDataComponents() {
    }
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> SHIPYARD_STORAGE =
            DATA_COMPONENTS.register("shipyard_storage", () -> DataComponentType.<CustomData>builder()
                    .persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RiggingIdentity>> RIGGING_IDENTITY =
            DATA_COMPONENTS.register("rigging_identity", () -> DataComponentType.<RiggingIdentity>builder()
                    .persistent(RiggingIdentity.CODEC).networkSynchronized(RiggingIdentity.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RiggingProficiency>> RIGGING_PROFICIENCY =
            DATA_COMPONENTS.register("rigging_proficiency", () -> DataComponentType.<RiggingProficiency>builder()
                    .persistent(RiggingProficiency.CODEC).networkSynchronized(RiggingProficiency.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> RIGGING_WEAPONS = container("rigging_weapons");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> RIGGING_EQUIPMENT = container("rigging_equipment");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> RIGGING_AMMUNITION = container("rigging_ammunition");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> RIGGING_CARGO = container("rigging_cargo");

    private static DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> container(String name) {
        return DATA_COMPONENTS.register(name, () -> DataComponentType.<ItemContainerContents>builder()
                .persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());
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
