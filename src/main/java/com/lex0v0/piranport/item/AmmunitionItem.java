package com.lex0v0.piranport.item;

import com.lex0v0.piranport.domain.ammunition.AmmunitionType;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// 注册物品决定固定弹种；制造点数只由制造界面显示。
public final class AmmunitionItem extends Item {
    private final AmmunitionType type;

    public AmmunitionItem(AmmunitionType type, Properties properties) {
        super(properties.stacksTo(64));
        this.type = Objects.requireNonNull(type);
    }

    public AmmunitionType type() { return type; }
    public int materialPoints() { return type.materialPoints(); }

    public static int points(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AmmunitionItem item ? item.materialPoints() : 0;
    }
}
