package com.lex0v0.piranport.advancement.predicate;

import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.mojang.serialization.Codec;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

// 先校验图纸，再按具体许可做集合包含判断。
public record BlueprintPermissionPredicate(ResourceLocation permission) implements ItemSubPredicate {
    public static final Codec<BlueprintPermissionPredicate> CODEC = ResourceLocation.CODEC
            .fieldOf("permission").xmap(BlueprintPermissionPredicate::new, BlueprintPermissionPredicate::permission).codec();

    @Override
    public boolean matches(ItemStack stack) {
        BlueprintType type = BlueprintType.byId(permission);
        return type != null && BlueprintRules.allowsType(stack, type);
    }
}
