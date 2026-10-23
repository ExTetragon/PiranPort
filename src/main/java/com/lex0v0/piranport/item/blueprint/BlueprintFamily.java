package com.lex0v0.piranport.item.blueprint;

// 区分舰船和装备两个图纸家族。
public enum BlueprintFamily {
    SHIP("ship"), EQUIPMENT("equipment");

    private final String path;

    BlueprintFamily(String path) { this.path = path; }

    public String path() { return path; }
}
