package com.lex0v0.piranport.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// 新掉落、读档和跨维度加入世界时恢复无限寿命标记。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class RiggingItemEvents {
    private RiggingItemEvents() {}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && RiggingCoreRules.isCore(item.getItem())) item.setUnlimitedLifetime();
    }
}
