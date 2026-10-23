package com.lex0v0.piranport.client;

import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

// 检查图纸模型和双语文本资源，不代替世界内界面验收。
final class BlueprintClientChecks {
    private BlueprintClientChecks() {}

    static boolean verify(Minecraft minecraft) {
        for (var subclass : BlueprintSubclass.values()) {
            var model = minecraft.getItemRenderer().getModel(BlueprintRules.create(subclass), null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()) return false;
        }
        Language previous = Language.getInstance();
        try {
            for (String code : List.of("en_us", "zh_cn")) {
                var language = ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of("en_us", code), false);
                Language.inject(language);
                String[] names = code.equals("zh_cn")
                        ? new String[]{"大型船图纸", "中型船图纸", "小型船图纸", "武器图纸", "设备图纸", "飞机图纸"}
                        : new String[]{"Large Ship Blueprint", "Medium Ship Blueprint", "Small Ship Blueprint", "Weapon Blueprint", "Device Blueprint", "Aircraft Blueprint"};
                for (var type : BlueprintType.values()) if (!language.has(type.translationKey())) return false;
                if (!language.getOrDefault(BlueprintType.MISSILE_LAUNCHER.translationKey())
                        .equals(code.equals("zh_cn") ? "导弹发射器" : "Missile Launcher")) return false;
                for (var subclass : BlueprintSubclass.values()) {
                    var stack = BlueprintRules.create(subclass);
                    if (!stack.getHoverName().getString().equals(names[subclass.ordinal()])) return false;
                    List<Component> tooltip = new ArrayList<>();
                    stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
                    if (tooltip.size() != 1 || !tooltip.getFirst().getString().startsWith(language.getOrDefault(subclass.translationKey())
                            + (code.equals("zh_cn") ? "：" : ": "))) return false;
                    for (var type : subclass.types()) if (!tooltip.getFirst().getString().contains(language.getOrDefault(type.translationKey()))) return false;
                }
            }
            return true;
        } finally { Language.inject(previous); }
    }
}
