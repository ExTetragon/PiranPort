package com.lex0v0.piranport.domain.equipment;

// 类别决定注册物品与装配区域，不保存具体型号或作战数值。
public enum EquipmentCategory {
    MAIN_GUN(Group.WEAPON), TORPEDO_LAUNCHER(Group.WEAPON), MISSILE_LAUNCHER(Group.WEAPON), DEPTH_CHARGE_PROJECTOR(Group.WEAPON),
    ARMOR(Group.DEVICE), PROPULSION_SYSTEM(Group.DEVICE), RADAR(Group.DEVICE), SONAR(Group.DEVICE),
    QUICK_REPAIR(Group.DEVICE), DAMAGE_CONTROL(Group.DEVICE), SECONDARY_GUN(Group.DEVICE),
    FIGHTER(Group.AIRCRAFT), BOMBER(Group.AIRCRAFT), TORPEDO_BOMBER(Group.AIRCRAFT), RECONNAISSANCE_AIRCRAFT(Group.AIRCRAFT);

    public enum Group { WEAPON, DEVICE, AIRCRAFT }
    private final Group group;
    EquipmentCategory(Group group) { this.group = group; }
    public Group group() { return group; }
    public String path() { return name().toLowerCase(java.util.Locale.ROOT); }
    public String translationKey() { return (this == QUICK_REPAIR || this == DAMAGE_CONTROL ? "equipment_category.piranport." : "item.piranport.") + path(); }
}
