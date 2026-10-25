package com.lex0v0.piranport.network.payload;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 单独同步任务快照，避免16位菜单整数截断或共享浏览状态。
public record DevelopmentFactorySyncPayload(int containerId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<DevelopmentFactorySyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "development_factory_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DevelopmentFactorySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public DevelopmentFactorySyncPayload decode(RegistryFriendlyByteBuf buffer) {
            int id = buffer.readVarInt(); CompoundTag data = buffer.readNbt();
            return new DevelopmentFactorySyncPayload(id, data == null ? new CompoundTag() : data);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, DevelopmentFactorySyncPayload payload) {
            buffer.writeVarInt(payload.containerId); buffer.writeNbt(payload.data);
        }
    };
    @Override public Type<DevelopmentFactorySyncPayload> type() { return TYPE; }
}
