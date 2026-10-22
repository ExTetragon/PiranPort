package com.lex0v0.piranport.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** 生成重复且容易漏项的储存方块战利品及对应物品标签。 */
@EventBusSubscriber(modid = PiranPort.MODID)
public final class ModDataGenerators {
    private ModDataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        generator.addProvider(event.includeServer(), new LootTableProvider(generator.getPackOutput(), Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), event.getLookupProvider()));
        generator.addProvider(event.includeServer(), new MineralItemTags(generator.getPackOutput(),
                event.getLookupProvider(), event.getExistingFileHelper()));
    }

    private static final class BlockLoot extends BlockLootSubProvider {
        private BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }
        @Override
        protected void generate() {
            dropSelf(ModBlocks.BAUXITE_BLOCK.get());
            dropSelf(ModBlocks.ALUMINUM_BLOCK.get());
            dropSelf(ModBlocks.STEEL_BLOCK.get());
            add(ModBlocks.FUEL_BARREL.get(), block -> createSingleItemTable(block).apply(
                    CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                            .include(ModDataComponents.FLUID_CONTENT.get())));
        }
        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModBlocks.BAUXITE_BLOCK.get(), ModBlocks.ALUMINUM_BLOCK.get(),
                    ModBlocks.STEEL_BLOCK.get(), ModBlocks.FUEL_BARREL.get());
        }
    }

    private static final class MineralItemTags extends TagsProvider<Item> {
        private MineralItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, Registries.ITEM, lookup, PiranPort.MODID, helper);
        }
        private static TagKey<Item> common(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        }
        private void material(String parent, String leaf, String item) {
            TagKey<Item> child = common(parent + "/" + leaf);
            tag(child).add(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, item)));
            tag(common(parent)).addTag(child);
        }
        @Override
        protected void addTags(HolderLookup.Provider provider) {
            material("ores", "bauxite", "bauxite_ore");
            material("ores", "saltpeter", "saltpeter_block");
            material("storage_blocks", "raw_bauxite", "bauxite_block");
            material("storage_blocks", "aluminum", "aluminum_block");
            material("storage_blocks", "steel", "steel_block");
        }
    }
}
