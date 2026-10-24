package com.lex0v0.piranport.domain.equipment;

// 通用与潜用鱼雷保留装备属性，发射行为另行设计。
public enum TorpedoUse {
    GENERAL, SUBMARINE;
    public String translationKey() { return "equipment_torpedo_use.piranport." + name().toLowerCase(java.util.Locale.ROOT); }
}
