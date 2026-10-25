package com.lex0v0.piranport.domain.ammunition;

// 类别只负责弹药分组，不决定弹种身份或制造点数。
public enum AmmunitionCategory {
    SHELLS("shells"), DEPTH_CHARGES("depth_charges"), TORPEDOES("torpedoes"), MISSILES("missiles");

    private final String path;

    AmmunitionCategory(String path) { this.path = path; }

    public String path() { return path; }
    public String translationKey() { return "ammunition_category.piranport." + path; }
}
