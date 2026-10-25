package com.lex0v0.piranport.client;

import com.lex0v0.piranport.client.event.EquipmentClientEvents;

import com.lex0v0.piranport.domain.equipment.EquipmentDefinitions;
import com.lex0v0.piranport.item.EquipmentItem;
import com.lex0v0.piranport.item.equipment.EquipmentIdentity;
import com.lex0v0.piranport.item.equipment.EquipmentRules;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModItems;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

// 主菜单检查真实烘焙模型及双语提示，不读取玩家存档。
final class EquipmentClientChecks {
    private EquipmentClientChecks() {}
    static boolean verify(Minecraft minecraft) {
        var models = new HashSet<net.minecraft.client.resources.model.BakedModel>();
        for (var definition : EquipmentDefinitions.ALL) {
            var stack = EquipmentRules.create(definition.id());var model = minecraft.getItemRenderer().getModel(stack,null,null,0);
            if (model == minecraft.getModelManager().getMissingModel() || !model.getParticleIcon().contents().name().equals(definition.texture())) return false;
            models.add(model);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Renamed"));EquipmentRules.damage(stack,1);
            if (minecraft.getItemRenderer().getModel(stack,null,null,0) != model) return false;
        }
        if (models.size() != 21) return false;
        for (var item : List.of(ModItems.QUICK_REPAIR.get(),ModItems.DAMAGE_CONTROL.get())) {
            var stack=new ItemStack(item);var model=minecraft.getItemRenderer().getModel(stack,null,null,0);
            if(model==minecraft.getModelManager().getMissingModel()||stack.isDamageableItem()||stack.has(ModDataComponents.EQUIPMENT_IDENTITY))return false;
            if(!model.getParticleIcon().contents().name().getPath().startsWith("item/equipment/"))return false;
            models.add(model);
        }
        if(models.size()!=23)return false;
        for (var category : com.lex0v0.piranport.domain.equipment.EquipmentCategory.values()) {
            var stack = new ItemStack(ModItems.equipment(category));var fallback = minecraft.getItemRenderer().getModel(stack,null,null,0);
            if (fallback == minecraft.getModelManager().getMissingModel()) return false;
            stack.set(ModDataComponents.EQUIPMENT_IDENTITY,new EquipmentIdentity(net.minecraft.resources.ResourceLocation.parse("piranport:unknown")));
            if (minecraft.getItemRenderer().getModel(stack,null,null,0) != fallback) return false;
        }
        var previous = Language.getInstance();
        try {
            for (String code : List.of("en_us","zh_cn")) {
                var language = ClientLanguage.loadFrom(minecraft.getResourceManager(),List.of("en_us",code),false);Language.inject(language);
                for (var definition : EquipmentDefinitions.ALL) {
                    var stack = EquipmentRules.create(definition.id());
                    if (!language.has(definition.translationKey()) || !language.has(definition.category().translationKey())) return false;
                    var lines = stack.getTooltipLines(Item.TooltipContext.EMPTY,null,TooltipFlag.NORMAL);
                    EquipmentClientEvents.applyTooltip(stack,lines,true);
                    String expected = language.getOrDefault(definition.category().translationKey());
                    var subtype = definition.gunClass().map(type -> type.translationKey())
                            .or(() -> definition.torpedoUse().map(type -> type.translationKey()))
                            .or(() -> definition.armorClass().map(type -> type.translationKey()));
                    if (subtype.isPresent()) expected += (code.equals("zh_cn") ? "·" : " · ") + language.getOrDefault(subtype.get());
                    if (!lines.get(1).getString().equals(expected)
                            || !lines.get(2).getString().startsWith(code.equals("zh_cn") ? "装备耐久：" : "Equipment durability: ")
                            || lines.stream().anyMatch(line -> line.getString().startsWith(code.equals("zh_cn") ? "细分：" : "Subtype: "))) return false;
                    if (lines.stream().noneMatch(line -> line.getString().contains(code.equals("zh_cn") ? "尚未实现" : "not implemented"))) return false;
                    if (lines.stream().anyMatch(line -> line.getString().contains("equipment.piranport.")
                            || line.getString().contains(code.equals("zh_cn") ? "国籍" : "Nationality"))) return false;
                    if (!stack.getHoverName().getString().equals(language.getOrDefault(definition.translationKey()))) return false;
                    stack.set(DataComponents.CUSTOM_NAME,Component.literal("My equipment"));
                    if (!stack.getHoverName().getString().equals("My equipment")) return false;
                }
                if (!language.has("piranport.configuration.retainBrokenEquipment") || !language.has("piranport.configuration.retainBrokenEquipment.tooltip")) return false;
                for(String key:List.of("allowQuickRepairFromPlayerInventory","allowDamageControlFromPlayerInventory"))
                    if(!language.has("piranport.configuration."+key)||!language.has("piranport.configuration."+key+".tooltip"))return false;
                for(String key:List.of("tab_build","tab_repair","requirements","search","durability","fuel_capacity"))
                    if(!language.has("gui.piranport.development_factory."+key))return false;
            }
            return true;
        } finally { Language.inject(previous); }
    }
}
