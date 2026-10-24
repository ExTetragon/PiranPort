package com.lex0v0.piranport.domain.ship;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

// 舰船目录保存身份、国籍、中英文检索名及基础属性，不保存单枚核心的成长或内容。
public record ShipDefinition(ResourceLocation id, ShipType type, ShipNationality nationality, List<String> searchNames, ShipStats baseStats) {
    public ShipDefinition { searchNames = List.copyOf(searchNames); }
    public String translationKey() { return "ship.piranport." + id.getPath(); }
    public ShipSize size() { return type.size(); }
}
