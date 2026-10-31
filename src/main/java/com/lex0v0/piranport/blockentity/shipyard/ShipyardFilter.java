package com.lex0v0.piranport.blockentity.shipyard;

import com.lex0v0.piranport.domain.ship.ShipDefinition;
import com.lex0v0.piranport.domain.ship.ShipNationality;
import com.lex0v0.piranport.domain.ship.ShipSize;
import com.lex0v0.piranport.domain.ship.ShipType;
import com.lex0v0.piranport.text.SearchText;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

// 客户端与服务端共用筛选规则，空集合表示不限，同组取并集、组间取交集。
public record ShipyardFilter(String text, Set<ShipSize> sizes, Set<ShipNationality> nationalities, Set<ShipType> types) {
    public static final int MAX_TEXT_LENGTH = 128;
    public static final ShipyardFilter NONE = new ShipyardFilter("", Set.of(), Set.of(), Set.of());

    public ShipyardFilter {
        text = text.strip();
        if (text.length() > MAX_TEXT_LENGTH) text = text.substring(0, MAX_TEXT_LENGTH);
        sizes = sizes.size() == ShipSize.values().length ? Set.of() : Set.copyOf(sizes);
        nationalities = nationalities.size() == ShipNationality.values().length ? Set.of() : Set.copyOf(nationalities);
        types = types.size() == ShipType.values().length ? Set.of() : Set.copyOf(types);
    }

    public boolean active() { return !text.isEmpty() || !sizes.isEmpty() || !nationalities.isEmpty() || !types.isEmpty(); }
    public boolean matches(ShipDefinition ship) {
        return (sizes.isEmpty() || sizes.contains(ship.size()))
                && (nationalities.isEmpty() || nationalities.contains(ship.nationality()))
                && (types.isEmpty() || types.contains(ship.type()))
                && SearchText.matches(text, ship.searchNames());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Text", text);
        ListTag sizeTags = new ListTag(), nationalityTags = new ListTag(), typeTags = new ListTag();
        Arrays.stream(ShipSize.values()).filter(sizes::contains).forEach(value -> sizeTags.add(StringTag.valueOf(value.path())));
        Arrays.stream(ShipNationality.values()).filter(nationalities::contains).forEach(value -> nationalityTags.add(StringTag.valueOf(value.path())));
        Arrays.stream(ShipType.values()).filter(types::contains).forEach(value -> typeTags.add(StringTag.valueOf(value.path())));
        tag.put("Sizes", sizeTags); tag.put("Nationalities", nationalityTags); tag.put("Types", typeTags);
        return tag;
    }

    public static ShipyardFilter load(CompoundTag tag) {
        var sizes = java.util.EnumSet.noneOf(ShipSize.class);
        var nationalities = java.util.EnumSet.noneOf(ShipNationality.class);
        var types = java.util.EnumSet.noneOf(ShipType.class);
        for (Tag entry : tag.getList("Sizes", Tag.TAG_STRING)) {
            Arrays.stream(ShipSize.values()).filter(value -> value.path().equals(entry.getAsString())).findFirst().ifPresent(sizes::add);
        }
        for (Tag entry : tag.getList("Nationalities", Tag.TAG_STRING)) {
            var nationality = ShipNationality.byPath(entry.getAsString());
            if (nationality != null) nationalities.add(nationality);
        }
        for (Tag entry : tag.getList("Types", Tag.TAG_STRING)) {
            Arrays.stream(ShipType.values()).filter(value -> value.path().equals(entry.getAsString())).findFirst().ifPresent(types::add);
        }
        return new ShipyardFilter(tag.getString("Text"), sizes, nationalities, types);
    }
}
