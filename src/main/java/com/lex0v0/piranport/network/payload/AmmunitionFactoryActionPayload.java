package com.lex0v0.piranport.network.payload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 客户端发送意图，材料与任务始终由服务端复核。
public record AmmunitionFactoryActionPayload(int containerId,int action,CompoundTag data) implements CustomPacketPayload {
    public static final Type<AmmunitionFactoryActionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("piranport","ammunition_factory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AmmunitionFactoryActionPayload> STREAM_CODEC=new StreamCodec<>(){
        @Override public AmmunitionFactoryActionPayload decode(RegistryFriendlyByteBuf b){int id=b.readVarInt(),a=b.readVarInt();var t=b.readNbt();return new AmmunitionFactoryActionPayload(id,a,t==null?new CompoundTag():t);}
        @Override public void encode(RegistryFriendlyByteBuf b,AmmunitionFactoryActionPayload p){b.writeVarInt(p.containerId);b.writeVarInt(p.action);b.writeNbt(p.data);}
    };
    @Override public Type<AmmunitionFactoryActionPayload> type(){return TYPE;}
}
