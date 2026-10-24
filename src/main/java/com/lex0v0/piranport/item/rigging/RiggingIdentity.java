package com.lex0v0.piranport.item.rigging;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

// 原样保存合法的未知 ID，定义缺失不会使内容解码失败。
public record RiggingIdentity(ResourceLocation shipId, ResourceLocation typeId) {
    public static final Codec<RiggingIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("ship").forGetter(RiggingIdentity::shipId),
            ResourceLocation.CODEC.fieldOf("type").forGetter(RiggingIdentity::typeId)).apply(instance, RiggingIdentity::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RiggingIdentity> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RiggingIdentity::shipId,
            ResourceLocation.STREAM_CODEC, RiggingIdentity::typeId, RiggingIdentity::new);

    public RiggingIdentity { Objects.requireNonNull(shipId); Objects.requireNonNull(typeId); }
}
