package com.lex0v0.piranport.domain.equipment;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.domain.ship.ShipStats;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

// 首批型号与临时参数；作战数值留空，舰娘三项属性修正统一为零。
public final class EquipmentDefinitions {
    private EquipmentDefinitions() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path); }
    public static final List<EquipmentDefinition> ALL = List.of(
            new EquipmentDefinition(id("experimental_coilgun"), EquipmentCategory.MAIN_GUN,
                    List.of("神秘武器", "Experimental Coilgun", "电磁炮", "神秘主炮"), 250, ShipStats.ZERO, 0, Optional.of(GunClass.SMALL), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("british_twin_mkn5_6in"), EquipmentCategory.MAIN_GUN,
                    List.of("英国双联MK.N5型6英寸高平速射炮", "British Twin Mk.N5 6-inch Guns", "MK.N5", "MkN5", "英国六英寸炮"), 1561, ShipStats.ZERO, 0, Optional.of(GunClass.MEDIUM_LIGHT), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("us_triple_mk16_8in"), EquipmentCategory.MAIN_GUN,
                    List.of("MK.16三联8英寸自动炮", "Triple Mk.16 8-inch Automatic Guns", "MK16", "德梅因炮", "三联八英寸炮"), 1561, ShipStats.ZERO, 0, Optional.of(GunClass.MEDIUM_HEAVY), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("us_triple_mk6_16in"), EquipmentCategory.MAIN_GUN,
                    List.of("美国三联16英寸炮(MK6)", "US Triple Mk.6 16-inch Guns", "MK6", "MK.6", "三联16英寸炮"), 2031, ShipStats.ZERO, 0, Optional.of(GunClass.LARGE), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("italian_triple_381mm"), EquipmentCategory.MAIN_GUN,
                    List.of("意大利三联381毫米炮", "Italian Triple 381 mm Guns", "意大利381", "维内托炮", "VV炮"), 2031, ShipStats.ZERO, 0, Optional.of(GunClass.LARGE), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("japanese_sextuple_610mm"), EquipmentCategory.TORPEDO_LAUNCHER,
                    List.of("日本六联61厘米鱼雷", "Japanese Sextuple 61 cm Torpedoes", "六联610", "六联鱼雷", "六联61厘米鱼雷发射器"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.of(TorpedoUse.GENERAL), Optional.empty()),
            new EquipmentDefinition(id("g7e_t10_submarine"), EquipmentCategory.TORPEDO_LAUNCHER,
                    List.of("G7eT10线导鱼雷(潜艇)", "G7e T10 Wire-guided Torpedoes", "G7eT10", "G7e T10", "线导鱼雷", "潜艇鱼雷"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.of(TorpedoUse.SUBMARINE), Optional.empty()),
            new EquipmentDefinition(id("hedgehog_projector"), EquipmentCategory.DEPTH_CHARGE_PROJECTOR,
                    List.of("刺猬弹深弹投射器", "Hedgehog Projector", "刺猬弹", "刺猬", "Hedgehog"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("sy1_launcher"), EquipmentCategory.MISSILE_LAUNCHER,
                    List.of("上游-1发射器", "SY-1 Launcher", "上游1", "SY1", "SY-1"), 2031, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("mk10_launcher"), EquipmentCategory.MISSILE_LAUNCHER,
                    List.of("MK-10导弹发射器", "Mk.10 Missile Launcher", "MK10", "MK-10", "MK.10"), 2031, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("phantom_fg1"), EquipmentCategory.FIGHTER,
                    List.of("“鬼怪”FG.1战斗机", "Phantom FG.1 Fighter", "鬼怪", "FG1", "FG.1", "F-4K", "Phantom"), 1561, ShipStats.ZERO, 2000, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("b25_doolittle"), EquipmentCategory.BOMBER,
                    List.of("B-25(杜立特队)", "B-25 (Doolittle Raiders)", "B25", "B-25", "杜立特", "米切尔"), 1561, ShipStats.ZERO, 2000, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("tu91"), EquipmentCategory.TORPEDO_BOMBER,
                    List.of("图-91", "Tu-91", "图91", "Tu91"), 1561, ShipStats.ZERO, 2000, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("c6n_saiun"), EquipmentCategory.RECONNAISSANCE_AIRCRAFT,
                    List.of("彩云", "C6N Saiun", "C6N", "Saiun", "彩雲"), 1561, ShipStats.ZERO, 2000, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("additional_armor_small"), EquipmentCategory.ARMOR,
                    List.of("附加装甲（小型）", "Additional Armor (Small)", "小型装甲", "附加装甲(小型)"), 250, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.of(ArmorClass.SMALL)),
            new EquipmentDefinition(id("additional_armor_medium"), EquipmentCategory.ARMOR,
                    List.of("附加装甲（中型）", "Additional Armor (Medium)", "中型装甲", "附加装甲(中型)"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.of(ArmorClass.MEDIUM)),
            new EquipmentDefinition(id("additional_armor_large"), EquipmentCategory.ARMOR,
                    List.of("附加装甲（大型）", "Additional Armor (Large)", "大型装甲", "附加装甲(大型)"), 2031, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.of(ArmorClass.LARGE)),
            new EquipmentDefinition(id("advanced_propulsion"), EquipmentCategory.PROPULSION_SYSTEM,
                    List.of("先进型动力系统", "Advanced Propulsion System", "先进动力", "彩马达", "马达"), 2031, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("advanced_radar"), EquipmentCategory.RADAR,
                    List.of("先进型雷达", "Advanced Radar", "先进型对海雷达", "先进型对空雷达", "对海雷达", "对空雷达"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("advanced_sonar"), EquipmentCategory.SONAR,
                    List.of("先进型声纳", "Advanced Sonar", "先进型声呐", "声纳", "声呐", "Sonar"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()),
            new EquipmentDefinition(id("us_twin_3in_secondary"), EquipmentCategory.SECONDARY_GUN,
                    List.of("美国双联3英寸副炮", "US Twin 3-inch Secondary Guns", "美国双联3英寸防空炮", "双联3英寸", "3in AA"), 1561, ShipStats.ZERO, 0, Optional.empty(), Optional.empty(), Optional.empty()));
    private static final Map<ResourceLocation, EquipmentDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(EquipmentDefinition::id, Function.identity()));
    public static Optional<EquipmentDefinition> find(ResourceLocation id) { return Optional.ofNullable(BY_ID.get(id)); }
}
