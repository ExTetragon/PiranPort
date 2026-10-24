package com.lex0v0.piranport.item.equipment;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

// 只保存稳定型号 ID，合法但未知的 ID 原样往返。
public record EquipmentIdentity(ResourceLocation model) {
    public static final Codec<EquipmentIdentity> CODEC = ResourceLocation.CODEC.xmap(EquipmentIdentity::new, EquipmentIdentity::model);
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentIdentity> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EquipmentIdentity::model, EquipmentIdentity::new);
    public EquipmentIdentity { Objects.requireNonNull(model); }
}
