package com.lex0v0.piranport.network.payload;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 客户端只发送菜单操作意图，服务端重查库存、任务及距离。
public record DevelopmentFactoryActionPayload(int containerId, int action, CompoundTag data) implements CustomPacketPayload {
    public static final Type<DevelopmentFactoryActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "development_factory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DevelopmentFactoryActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public DevelopmentFactoryActionPayload decode(RegistryFriendlyByteBuf buffer) {
            int id = buffer.readVarInt(), action = buffer.readVarInt(); CompoundTag data = buffer.readNbt();
            return new DevelopmentFactoryActionPayload(id, action, data == null ? new CompoundTag() : data);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, DevelopmentFactoryActionPayload payload) {
            buffer.writeVarInt(payload.containerId); buffer.writeVarInt(payload.action); buffer.writeNbt(payload.data);
        }
    };
    @Override public Type<DevelopmentFactoryActionPayload> type() { return TYPE; }
}
