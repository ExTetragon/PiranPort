package com.lex0v0.piranport.domain.ship;

import java.util.Arrays;

// 沿用舰R的八国与其他分类，国籍由具体舰船身份决定。
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
