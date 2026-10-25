package com.lex0v0.piranport.network.payload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 机器任务与当前窗口的服务端草稿分别同步，避免共享玩家浏览状态。
public record AmmunitionFactorySyncPayload(int containerId,CompoundTag state,CompoundTag browse) implements CustomPacketPayload {
    public static final Type<AmmunitionFactorySyncPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("piranport","ammunition_factory_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AmmunitionFactorySyncPayload> STREAM_CODEC=new StreamCodec<>(){
        @Override public AmmunitionFactorySyncPayload decode(RegistryFriendlyByteBuf b){int id=b.readVarInt();var s=b.readNbt();var v=b.readNbt();return new AmmunitionFactorySyncPayload(id,s==null?new CompoundTag():s,v==null?new CompoundTag():v);}
        @Override public void encode(RegistryFriendlyByteBuf b,AmmunitionFactorySyncPayload p){b.writeVarInt(p.containerId);b.writeNbt(p.state);b.writeNbt(p.browse);}
    };
    @Override public Type<AmmunitionFactorySyncPayload> type(){return TYPE;}
}
