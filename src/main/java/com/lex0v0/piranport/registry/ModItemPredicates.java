package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.advancement.predicate.BlueprintPermissionPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册按具体许可判断图纸的物品子判定。
public final class ModItemPredicates {
    private static final DeferredRegister<ItemSubPredicate.Type<?>> PREDICATES = DeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE, PiranPort.MODID);
    public static final DeferredHolder<ItemSubPredicate.Type<?>, ItemSubPredicate.Type<BlueprintPermissionPredicate>> BLUEPRINT_PERMISSION =
            PREDICATES.register("blueprint_permission", () -> new ItemSubPredicate.Type<>(BlueprintPermissionPredicate.CODEC));

    private ModItemPredicates() {}
    public static void register(IEventBus bus) { PREDICATES.register(bus); }
}
