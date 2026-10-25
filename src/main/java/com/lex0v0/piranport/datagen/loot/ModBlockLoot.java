package com.lex0v0.piranport.datagen.loot;

import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModDataComponents;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

// 生成矿物与制造设施的方块掉落，保留缓存组件。
public final class ModBlockLoot extends BlockLootSubProvider {
    public ModBlockLoot(HolderLookup.Provider registries) {
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
        // 船坞按容器完整掉落，避免爆炸衰减吞掉已保存的缓存。
        add(ModBlocks.SHIPYARD.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block))).apply(
                CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                        .include(ModDataComponents.SHIPYARD_STORAGE.get())));
        add(ModBlocks.DEVELOPMENT_FACTORY.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block))).apply(
                CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY).include(ModDataComponents.DEVELOPMENT_FACTORY_STORAGE.get())));
    }
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(ModBlocks.BAUXITE_BLOCK.get(), ModBlocks.ALUMINUM_BLOCK.get(),
                ModBlocks.STEEL_BLOCK.get(), ModBlocks.FUEL_BARREL.get(), ModBlocks.SHIPYARD.get(), ModBlocks.DEVELOPMENT_FACTORY.get());
    }
}
