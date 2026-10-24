package com.lex0v0.piranport.network;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 客户端只发送菜单操作意图，服务端重查库存、任务及距离。
public record ShipyardActionPayload(int containerId, int action, CompoundTag data) implements CustomPacketPayload {
    public static final Type<ShipyardActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, "shipyard_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShipyardActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ShipyardActionPayload decode(RegistryFriendlyByteBuf buffer) {
            int id = buffer.readVarInt(), action = buffer.readVarInt(); CompoundTag data = buffer.readNbt();
            return new ShipyardActionPayload(id, action, data == null ? new CompoundTag() : data);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ShipyardActionPayload payload) {
            buffer.writeVarInt(payload.containerId); buffer.writeVarInt(payload.action); buffer.writeNbt(payload.data);
        }
    };
    @Override public Type<ShipyardActionPayload> type() { return TYPE; }
}
