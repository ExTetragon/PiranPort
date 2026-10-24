package com.lex0v0.piranport.domain.ship;

import com.lex0v0.piranport.PiranPort;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

// 首批十艘舰船的不可变目录，参数变更直接作用于已有核心。
public final class ShipDefinitions {
    public static final List<ShipDefinition> ALL = List.of(
            ship("lexington_cv2", ShipType.AIRCRAFT_CARRIER, ShipNationality.UNITED_STATES, "列克星敦（CV-2）", "Lexington (CV-2)", 32, 4, 33.2),
            ship("saratoga", ShipType.AIRCRAFT_CARRIER, ShipNationality.UNITED_STATES, "萨拉托加", "Saratoga", 32, 4, 33.2),
            ship("vittorio_veneto", ShipType.BATTLESHIP, ShipNationality.ITALY, "维托里奥·维内托", "Vittorio Veneto", 38, 8, 31),
            ship("hood", ShipType.BATTLECRUISER, ShipNationality.UNITED_KINGDOM, "胡德", "Hood", 38, 6, 31),
            ship("quincy", ShipType.HEAVY_CRUISER, ShipNationality.UNITED_STATES, "昆西", "Quincy", 22, 4, 32.7),
            ship("prinz_eugen", ShipType.HEAVY_CRUISER, ShipNationality.GERMANY, "欧根亲王", "Prinz Eugen", 28, 5, 32),
            ship("yat_sen", ShipType.LIGHT_CRUISER, ShipNationality.CHINA, "逸仙", "Yat Sen", 10, 2, 20),
            ship("glowworm", ShipType.DESTROYER, ShipNationality.UNITED_KINGDOM, "萤火虫", "Glowworm", 10, 1, 36),
            ship("changchun", ShipType.DESTROYER, ShipNationality.CHINA, "长春", "Changchun", 16, 2, 39),
            ship("u47", ShipType.SUBMARINE, ShipNationality.GERMANY, "U-47", "U-47", 10, 1, 18));
    private static final Map<ResourceLocation, ShipDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(ShipDefinition::id, Function.identity()));

    private ShipDefinitions() {}
    private static ShipDefinition ship(String path, ShipType type, ShipNationality nationality, String chinese, String english,
                                       double health, double armor, double referenceSpeed) {
        return new ShipDefinition(ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path), type, nationality, java.util.stream.Stream.of(chinese, english).distinct().toList(),
                new ShipStats(health, armor, referenceSpeed * 0.2 + 0.4));
    }
    public static ShipDefinition byId(ResourceLocation id) { return BY_ID.get(id); }
}
