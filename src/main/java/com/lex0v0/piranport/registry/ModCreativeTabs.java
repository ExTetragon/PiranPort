package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import com.lex0v0.piranport.item.BlueprintItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册主创造栏，普通物品自动收录、图纸展示有效许可示例。
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PiranPort.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport.main"))
                    // 主创造栏暂用铁锭图标。
                    .icon(() -> new ItemStack(Items.IRON_INGOT))
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().stream().filter(item -> !(item.get() instanceof BlueprintItem))
                                .forEach(item -> output.accept(item.get()));
                        for (var subclass : BlueprintSubclass.values()) output.accept(BlueprintRules.create(subclass));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
