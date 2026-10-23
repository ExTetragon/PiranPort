package com.lex0v0.piranport.item.blueprint;

import com.lex0v0.piranport.item.BlueprintItem;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModItems;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// 集中校验和查询许可，创建输出时不修改输入或玩家状态。
public final class BlueprintRules {
    private BlueprintRules() {}

    public static boolean isValid(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlueprintItem item)) return false;
        BlueprintContent content = stack.get(ModDataComponents.BLUEPRINT_CONTENT.get());
        return content != null && !content.permissions().isEmpty() && content.permissions().stream().allMatch(id -> {
            BlueprintType type = BlueprintType.byId(id);
            return type != null && type.family() == item.family();
        });
    }

    public static Set<BlueprintType> getAllowedTypes(ItemStack stack) {
        if (!isValid(stack)) return Set.of();
        EnumSet<BlueprintType> types = EnumSet.noneOf(BlueprintType.class);
        stack.get(ModDataComponents.BLUEPRINT_CONTENT.get()).permissions().forEach(id -> types.add(BlueprintType.byId(id)));
        return Collections.unmodifiableSet(types);
    }

    public static Set<BlueprintType> getAllowedTypes(ItemStack stack, BlueprintSubclass subclass) {
        EnumSet<BlueprintType> types = EnumSet.noneOf(BlueprintType.class);
        getAllowedTypes(stack).stream().filter(type -> type.subclass() == subclass).forEach(types::add);
        return Collections.unmodifiableSet(types);
    }

    public static boolean allowsType(ItemStack stack, BlueprintType type) {
        return isValid(stack) && stack.get(ModDataComponents.BLUEPRINT_CONTENT.get()).permissions().contains(type.id());
    }

    // 子类有所覆盖仅供筛选，制造时仍须检查具体许可。
    public static boolean allowsSubclass(ItemStack stack, BlueprintSubclass subclass) {
        return !getAllowedTypes(stack, subclass).isEmpty();
    }

    public static boolean coversSubclass(ItemStack stack, BlueprintSubclass subclass) {
        return getAllowedTypes(stack, subclass).containsAll(subclass.types());
    }

    public static ItemStack create(BlueprintFamily family, Collection<BlueprintType> types) {
        ItemStack stack = new ItemStack(family == BlueprintFamily.SHIP ? ModItems.SHIP_BLUEPRINT.get() : ModItems.EQUIPMENT_BLUEPRINT.get());
        stack.set(ModDataComponents.BLUEPRINT_CONTENT.get(), new BlueprintContent(types.stream().map(BlueprintType::id).toList()));
        if (!isValid(stack)) throw new IllegalArgumentException("Empty or mixed-family blueprint");
        return stack;
    }

    public static ItemStack create(BlueprintType... types) {
        if (types.length == 0) throw new IllegalArgumentException("Empty blueprint");
        return create(types[0].family(), List.of(types));
    }

    public static ItemStack create(BlueprintSubclass subclass) { return create(subclass.family(), subclass.types()); }

    // 许可与名称单独结算，其余组件必须一致才能合并。
    private static DataComponentPatch mergeComponents(ItemStack stack) {
        return stack.getComponentsPatch().forget(type -> type == ModDataComponents.BLUEPRINT_CONTENT.get() || type == DataComponents.CUSTOM_NAME);
    }

    // 每个参与槽只取一张，许可取并集并固定产出一张新图纸。
    public static ItemStack merge(List<ItemStack> inputs) {
        if (inputs.size() < 2 || inputs.size() > 4 || inputs.stream().anyMatch(stack -> !isValid(stack))) return ItemStack.EMPTY;
        ItemStack first = inputs.getFirst();
        DataComponentPatch extras = mergeComponents(first);
        if (inputs.stream().anyMatch(stack -> !stack.is(first.getItem()) || !mergeComponents(stack).equals(extras))) return ItemStack.EMPTY;
        Set<ResourceLocation> union = new TreeSet<>();
        inputs.forEach(stack -> union.addAll(stack.get(ModDataComponents.BLUEPRINT_CONTENT.get()).permissions()));
        ItemStack result = first.copyWithCount(1);
        result.set(ModDataComponents.BLUEPRINT_CONTENT.get(), new BlueprintContent(List.copyOf(union)));
        if (!inputs.stream().allMatch(stack -> Objects.equals(stack.get(DataComponents.CUSTOM_NAME), first.get(DataComponents.CUSTOM_NAME)))) {
            result.remove(DataComponents.CUSTOM_NAME);
        }
        return result;
    }
}
