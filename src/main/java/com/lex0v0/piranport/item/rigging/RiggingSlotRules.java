package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.registry.ModItemTags;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

// 插入时统一检查类别和受支持的容器，保守拒绝无法完成的超深扫描。
public final class RiggingSlotRules {
    private static final List<Function<ItemStack, Iterable<ItemStack>>> CONTAINERS = new CopyOnWriteArrayList<>(List.of(
            stack -> stack.has(DataComponents.CONTAINER) ? stack.get(DataComponents.CONTAINER).nonEmptyItemsCopy() : List.of(),
            stack -> stack.has(DataComponents.BUNDLE_CONTENTS) ? stack.get(DataComponents.BUNDLE_CONTENTS).itemsCopy() : List.of()));
    private RiggingSlotRules() {}
    public static void registerContainerAdapter(Function<ItemStack, Iterable<ItemStack>> adapter) {
        CONTAINERS.add(java.util.Objects.requireNonNull(adapter));
    }
    public static boolean accepts(RiggingRegion region, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (containsCore(stack)) return false;
        return switch (region) {
            case WEAPONS -> stack.is(ModItemTags.WEAPONS) || stack.is(ModItemTags.AIRCRAFT);
            case EQUIPMENT -> stack.is(ModItemTags.DEVICES);
            case AMMUNITION -> stack.is(ModItemTags.AMMUNITION);
            case CARGO -> true;
        };
    }
    public static boolean containsCore(ItemStack stack) {
        record Entry(ItemStack stack, int depth) {}
        var pending = new ArrayDeque<Entry>();
        pending.push(new Entry(stack, 0));
        int visited = 0;
        while (!pending.isEmpty()) {
            var entry = pending.pop();
            if (++visited > 16384 || entry.depth() > 64 || RiggingCoreRules.isCore(entry.stack())) return true;
            for (var adapter : CONTAINERS) {
                for (var child : adapter.apply(entry.stack())) {
                    if (!child.isEmpty()) pending.push(new Entry(child, entry.depth() + 1));
                    if (pending.size() > 16384) return true;
                }
            }
        }
        return false;
    }
}
