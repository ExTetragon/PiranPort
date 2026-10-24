package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import com.lex0v0.piranport.item.BlueprintItem;
import com.lex0v0.piranport.item.RiggingCoreItem;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册主创造栏，图纸和核心展示有效身份示例。
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PiranPort.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.main"))
                    .icon(() -> RiggingCoreRules.create(ShipDefinitions.ALL.getFirst().id()))
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().stream().filter(item -> !(item.get() instanceof BlueprintItem) && !(item.get() instanceof RiggingCoreItem) && !(item.get() instanceof com.lex0v0.piranport.item.EquipmentItem))
                                .forEach(item -> output.accept(item.get()));
                        for (var subclass : BlueprintSubclass.values()) output.accept(BlueprintRules.create(subclass));
                        for (var ship : ShipDefinitions.ALL) output.accept(RiggingCoreRules.create(ship.id()));
                        for (var model : com.lex0v0.piranport.domain.equipment.EquipmentDefinitions.ALL) output.accept(com.lex0v0.piranport.item.equipment.EquipmentRules.create(model.id()));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
