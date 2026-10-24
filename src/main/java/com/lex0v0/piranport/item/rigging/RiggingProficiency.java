package com.lex0v0.piranport.item.rigging;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// 每枚核心独立保存的熟练度，本阶段不保存训练计时或发放收益。
public record RiggingProficiency(int value) {
    public static final int MAX = 200;
    public static final RiggingProficiency ZERO = new RiggingProficiency(0);
    public static final Codec<RiggingProficiency> CODEC = Codec.intRange(0, MAX).xmap(RiggingProficiency::new, RiggingProficiency::value);
    public static final StreamCodec<RegistryFriendlyByteBuf, RiggingProficiency> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(RiggingProficiency::new, RiggingProficiency::value).cast();

    public RiggingProficiency {
        if (value < 0 || value > MAX) throw new IllegalArgumentException("Proficiency out of range: " + value);
    }
}
