package com.lex0v0.piranport.datagen.tags;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModItemTags;
import com.lex0v0.piranport.registry.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

// 生成矿物、装备和弹药的物品标签。
public final class ModItemTagsProvider extends TagsProvider<Item> {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
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
        for (var category : com.lex0v0.piranport.domain.equipment.EquipmentCategory.values()) {
            var target = switch (category.group()) { case WEAPON -> ModItemTags.WEAPONS; case DEVICE -> ModItemTags.DEVICES; case AIRCRAFT -> ModItemTags.AIRCRAFT; };
            tag(target).add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getResourceKey(ModItems.equipment(category)).orElseThrow());
        }
        tag(ModItemTags.EQUIPMENT_CONSUMABLES).add(ModItems.QUICK_REPAIR.getKey(),ModItems.DAMAGE_CONTROL.getKey());
        tag(ModItemTags.AMMUNITION).add(ModItems.MAIN_GUN_AMMUNITION.getKey(), ModItems.TORPEDO.getKey(),
                ModItems.DEPTH_CHARGE.getKey(), ModItems.MISSILE.getKey());
        material("ores", "bauxite", "bauxite_ore");
        material("ores", "saltpeter", "saltpeter_block");
        material("storage_blocks", "raw_bauxite", "bauxite_block");
        material("storage_blocks", "aluminum", "aluminum_block");
        material("storage_blocks", "steel", "steel_block");
    }
}
