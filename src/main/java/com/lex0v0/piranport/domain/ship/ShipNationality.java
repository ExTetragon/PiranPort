package com.lex0v0.piranport.domain.ship;

import java.util.Arrays;

// 舰船国籍目录仅包含舰R九组，供身份和船坞筛选使用。
public enum ShipNationality {
    CHINA("c"), UNITED_KINGDOM("e"), FRANCE("f"), GERMANY("g"), ITALY("i"),
    JAPAN("j"), SOVIET_UNION("s"), UNITED_STATES("u"), OTHER("other");

    private final String path;

    ShipNationality(String path) { this.path = path; }
    public String path() { return path; }
    public String translationKey() { return "ship_nationality.piranport." + path; }
    public String countryTranslationKey() { return translationKey() + ".country"; }
    public static ShipNationality byPath(String path) {
        return Arrays.stream(values()).filter(value -> value.path.equals(path)).findFirst().orElse(null);
    }
}
