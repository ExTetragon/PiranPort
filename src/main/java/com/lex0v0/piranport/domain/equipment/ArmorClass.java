package com.lex0v0.piranport.domain.equipment;

// 附加装甲型号保留三种尺寸，不影响核心设备栏准入。
public enum ArmorClass {
    SMALL, MEDIUM, LARGE;
    public String translationKey() { return "equipment_armor_class.piranport." + name().toLowerCase(java.util.Locale.ROOT); }
}
