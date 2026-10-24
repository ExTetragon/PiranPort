package com.lex0v0.piranport.client;

import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.item.rigging.RiggingTooltips;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModCreativeTabs;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

// 检查原版有无归属行时的顺序，保留数量、样式及第三方内容。
final class ModTooltipClientChecks {
    private ModTooltipClientChecks() {}

    static boolean verify(Minecraft minecraft) {
        var previous = Language.getInstance();
        try {
            for (String code : List.of("en_us", "zh_cn")) {
                var language = ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of("en_us", code), false);
                Language.inject(language);
                if (!language.getOrDefault("itemGroup.piranport.main").equals(code.equals("zh_cn") ? "皮兰港" : "PiranPort")) return false;
                for (var entry : ModItems.ITEMS.getEntries()) {
                    var stack = new ItemStack(entry.get());
                    if (!verifyStack(minecraft, stack)) return false;
                }
                for (var subclass : BlueprintSubclass.values())
                    if (!verifyStack(minecraft, BlueprintRules.create(subclass))) return false;
                if (!verifyStack(minecraft, BlueprintRules.create(BlueprintType.BATTLESHIP, BlueprintType.LIGHT_CRUISER, BlueprintType.DESTROYER))) return false;
                for (var ship : ShipDefinitions.ALL) {
                    var core = RiggingCoreRules.create(ship.id());
                    if (!verifyStack(minecraft, core) || !verifyStack(minecraft, core, true)) return false;
                }
                var renamed = new ItemStack(ModItems.SHIP_BLUEPRINT.get());
                renamed.set(DataComponents.CUSTOM_NAME, Component.translatable("itemGroup.piranport.main"));
                renamed.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Additional item description"))));
                if (!verifyStack(minecraft, renamed) || !verifyOtherComponents(renamed)) return false;
                if (!verifyForeignItem()) return false;
            }
            System.out.println("PIRANPORT_TOOLTIP_ITEMS_CHECKED=" + ModItems.ITEMS.getEntries().size());
            return true;
        } finally { Language.inject(previous); }
    }

    private static boolean verifyStack(Minecraft minecraft, ItemStack stack) {
        return verifyStack(minecraft, stack, false);
    }

    private static boolean verifyStack(Minecraft minecraft, ItemStack stack, boolean shift) {
        var before = stack.copy();
        for (var flag : List.of(TooltipFlag.NORMAL, TooltipFlag.ADVANCED)) {
            var original = stack.getTooltipLines(Item.TooltipContext.EMPTY, null, flag);
            var descriptions = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, descriptions, flag);
            if (shift) {
                RiggingClientEvents.applyTooltip(stack, original, true);
                descriptions = new ArrayList<>(RiggingTooltips.details(stack));
            }
            for (boolean hasNativeCategory : List.of(false, true)) {
                var creativeLines = new ArrayList<>(original);
                var expected = new ArrayList<>(original);
                var category = category();
                if (hasNativeCategory) {
                    creativeLines.add(1, category);
                    expected.add(1 + descriptions.size(), category);
                }
                List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
                for (var line : creativeLines) elements.add(Either.left(line));
                var event = new RenderTooltipEvent.GatherComponents(stack, 8192, 8192, elements, -1);
                NeoForge.EVENT_BUS.post(event);
                if (!event.getTooltipElements().stream().map(e -> e.left().orElseThrow()).toList().equals(expected)) return false;
                NeoForge.EVENT_BUS.post(event);
                if (!event.getTooltipElements().stream().map(e -> e.left().orElseThrow()).toList().equals(expected)) return false;
                if (ClientHooks.gatherTooltipComponents(stack, creativeLines, 0, 8192, 8192, minecraft.font).size() != expected.size()) return false;
            }
        }
        return ItemStack.matches(stack, before);
    }

    private static Component category() {
        return ModCreativeTabs.MAIN.get().getDisplayName().copy().withStyle(ChatFormatting.BLUE);
    }

    private static boolean verifyOtherComponents(ItemStack stack) {
        var marker = new TooltipComponent() {};
        var sameName = Component.literal(Language.getInstance().getOrDefault("itemGroup.piranport.main"));
        var otherModName = Component.translatable("tooltip.othermod.mod_name");
        var description = Component.translatable("tooltip.piranport.blueprint.group", Component.literal("Group"), Component.literal("Types"));
        var category = category();
        var secondCategory = category.copy().withStyle(ChatFormatting.GREEN);
        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        elements.add(Either.left(Component.literal("Title")));
        elements.add(Either.left(category));
        elements.add(Either.left(otherModName));
        elements.add(Either.left(description));
        elements.add(Either.right(marker));
        elements.add(Either.left(sameName));
        elements.add(Either.left(secondCategory));
        var event = new RenderTooltipEvent.GatherComponents(stack, 8192, 8192, elements, -1);
        NeoForge.EVENT_BUS.post(event);
        var expected = List.of(Either.<FormattedText, TooltipComponent>left(Component.literal("Title")),
                Either.<FormattedText, TooltipComponent>left(otherModName), Either.<FormattedText, TooltipComponent>left(description),
                Either.<FormattedText, TooltipComponent>left(category), Either.<FormattedText, TooltipComponent>right(marker),
                Either.<FormattedText, TooltipComponent>left(sameName), Either.<FormattedText, TooltipComponent>left(secondCategory));
        if (!event.getTooltipElements().equals(expected) || event.getTooltipElements().get(3).left().orElseThrow() != category) return false;
        var withoutNative = new ArrayList<>(expected);
        withoutNative.remove(6);
        withoutNative.remove(3);
        var expectedWithoutNative = List.copyOf(withoutNative);
        var noCategory = new RenderTooltipEvent.GatherComponents(stack, 8192, 8192, withoutNative, -1);
        NeoForge.EVENT_BUS.post(noCategory);
        return noCategory.getTooltipElements().equals(expectedWithoutNative);
    }

    private static boolean verifyForeignItem() {
        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        elements.add(Either.left(Component.literal("Stone")));
        elements.add(Either.left(Component.translatable("itemGroup.piranport.main")));
        var expected = List.copyOf(elements);
        var event = new RenderTooltipEvent.GatherComponents(new ItemStack(Items.STONE), 8192, 8192, elements, -1);
        NeoForge.EVENT_BUS.post(event);
        return event.getTooltipElements().equals(expected);
    }
}
