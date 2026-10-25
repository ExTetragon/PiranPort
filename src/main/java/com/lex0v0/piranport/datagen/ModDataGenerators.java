package com.lex0v0.piranport.datagen;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.datagen.loot.ModBlockLoot;
import com.lex0v0.piranport.datagen.tags.ModItemTagsProvider;
import java.util.List;
import java.util.Set;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

// 仅挂接数据生成器，具体资源规则由对应 provider 负责。
@EventBusSubscriber(modid = PiranPort.MODID)
public final class ModDataGenerators {
    private ModDataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new BlueprintData(generator.getPackOutput(), event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new LootTableProvider(generator.getPackOutput(), Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)), event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new ModItemTagsProvider(generator.getPackOutput(),
                event.getLookupProvider(), event.getExistingFileHelper()));
    }
}
