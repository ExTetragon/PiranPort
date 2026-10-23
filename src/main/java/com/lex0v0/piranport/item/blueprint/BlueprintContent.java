package com.lex0v0.piranport.item.blueprint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

// 规范化不可变许可集合，保留未知 ID 并另行校验玩法合法性。
public record BlueprintContent(List<ResourceLocation> permissions) {
    public static final int MAX_PERMISSIONS = 128;
    public static final Codec<BlueprintContent> CODEC = ResourceLocation.CODEC.listOf()
            .validate(list -> list.size() <= MAX_PERMISSIONS ? DataResult.success(list)
                    : DataResult.error(() -> "Too many blueprint permissions"))
            .xmap(BlueprintContent::new, BlueprintContent::permissions)
            .fieldOf("permissions").codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintContent> STREAM_CODEC = StreamCodec.of(
            (buffer, content) -> {
                buffer.writeVarInt(content.permissions.size());
                content.permissions.forEach(buffer::writeResourceLocation);
            }, buffer -> {
                int size = buffer.readVarInt();
                if (size < 0 || size > MAX_PERMISSIONS) throw new DecoderException("Invalid blueprint permission count: " + size);
                List<ResourceLocation> ids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) ids.add(buffer.readResourceLocation());
                return new BlueprintContent(ids);
            });

    // 去重并排序，使保存、网络同步和堆叠判定保持一致。
    public BlueprintContent {
        if (permissions.size() > MAX_PERMISSIONS) throw new IllegalArgumentException("Too many blueprint permissions");
        permissions = List.copyOf(new TreeSet<>(permissions));
    }
}
