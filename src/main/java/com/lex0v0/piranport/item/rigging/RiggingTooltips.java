package com.lex0v0.piranport.item.rigging;

import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.text.ClassificationText;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// 纯查询构建详情，客户端只负责决定是否按下 Shift。
public final class RiggingTooltips {
    private RiggingTooltips() {}
    private static Component line(String field, Object... values) {
        return Component.translatable("tooltip.piranport.rigging." + field, values).withStyle(ChatFormatting.GRAY);
    }
    private static String number(double value) { return new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value); }
    public static List<Component> details(ItemStack core) {
        var lines = new ArrayList<Component>();
        var definition = RiggingCoreRules.definition(core);
        if (definition.isEmpty()) {
            lines.add(line("invalid." + RiggingCoreRules.status(core).name().toLowerCase(Locale.ROOT)));
            var identity = core.get(ModDataComponents.RIGGING_IDENTITY);
            if (identity != null) lines.add(line("raw_identity", identity.shipId().toString(), identity.typeId().toString()));
            lines.add(line("proficiency", RiggingCoreRules.getProficiency(core), RiggingProficiency.MAX));
            return lines;
        }
        var ship = definition.orElseThrow();
        lines.add(Component.translatable(ship.translationKey()).withStyle(ChatFormatting.GRAY));
        lines.add(ClassificationText.ship(ship).withStyle(ChatFormatting.GRAY));
        lines.add(line("proficiency", RiggingCoreRules.getProficiency(core), RiggingProficiency.MAX));
        var stats = RiggingStatsCalculator.calculate(core).orElseThrow();
        lines.add(line("durability", number(stats.durability())));
        lines.add(line("armor", number(stats.armor())));
        lines.add(line("speed", String.format(Locale.ROOT, "%.2f", stats.speed())));
        lines.add(line("fuel", RiggingCoreRules.fuel(core).getFluid().getAmount(), ship.size().fuel()));
        for (var region : RiggingRegion.values())
            lines.add(line(region.name().toLowerCase(Locale.ROOT), RiggingCoreRules.inventory(core, region).occupiedSlots(), region.capacity(ship.size())));
        return lines;
    }
}
