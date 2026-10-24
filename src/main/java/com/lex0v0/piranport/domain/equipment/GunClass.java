package com.lex0v0.piranport.domain.equipment;

// 主炮口径分级供后续适配和超重计算，不在收纳时限制舰种。
public enum GunClass {
    SMALL, MEDIUM_LIGHT, MEDIUM_HEAVY, LARGE;
    public String translationKey() { return "equipment_gun_class.piranport." + name().toLowerCase(java.util.Locale.ROOT); }
}
