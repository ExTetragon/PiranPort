package com.lex0v0.piranport.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lex0v0.piranport.item.blueprint.BlueprintFamily;
import com.lex0v0.piranport.item.blueprint.BlueprintSubclass;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

// 生成蓝图服务端资源，按家族、子类和许可数量分层保持等权。
public final class BlueprintData implements DataProvider {
    private final Path data;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public BlueprintData(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.data = output.getOutputFolder(PackOutput.Target.DATA_PACK);
        this.registries = registries;
    }

    private static JsonObject object(Object... values) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < values.length; i += 2) {
            String key = (String) values[i];
            Object value = values[i + 1];
            if (value instanceof JsonElement json) result.add(key, json);
            else if (value instanceof Number n) result.addProperty(key, n);
            else if (value instanceof Boolean b) result.addProperty(key, b);
            else result.addProperty(key, (String) value);
        }
        return result;
    }

    private static JsonArray array(JsonElement... values) {
        JsonArray result = new JsonArray();
        for (JsonElement value : values) result.add(value);
        return result;
    }

    private static JsonObject table(JsonArray pools) { return object("type", "minecraft:chest", "pools", pools); }
    private static JsonObject pool(JsonArray entries) { return object("rolls", 1, "entries", entries); }
    private static JsonObject inline(JsonObject table) { return object("type", "minecraft:loot_table", "value", table); }
    private static JsonObject reference() { return object("type", "minecraft:loot_table", "value", "piranport:blueprints/content"); }

    private static JsonObject content(List<BlueprintType> types) {
        JsonArray ids = new JsonArray();
        types.stream().map(BlueprintType::id).sorted().forEach(id -> ids.add(id.toString()));
        return object("permissions", ids);
    }

    // 分层选择家族、子类、数量与不重复组合，避免组合数影响分类权重。
    private static JsonObject contentTable() {
        JsonArray families = new JsonArray();
        for (var family : BlueprintFamily.values()) {
            JsonArray subclasses = new JsonArray();
            for (var subclass : BlueprintSubclass.values()) {
                if (subclass.family() != family) continue;
                var types = subclass.types();
                JsonArray sizes = new JsonArray();
                for (int count = 1; count <= types.size(); count++) {
                    JsonArray subsets = new JsonArray();
                    for (int mask = 1; mask < (1 << types.size()); mask++) {
                        if (Integer.bitCount(mask) != count) continue;
                        List<BlueprintType> selected = new ArrayList<>();
                        for (int bit = 0; bit < types.size(); bit++) if ((mask & (1 << bit)) != 0) selected.add(types.get(bit));
                        subsets.add(object("type", "minecraft:item", "name", "piranport:" + family.path() + "_blueprint",
                                "functions", array(object("function", "minecraft:set_components", "components",
                                        object("piranport:blueprint_content", content(selected))))));
                    }
                    sizes.add(inline(table(array(pool(subsets)))));
                }
                subclasses.add(inline(table(array(pool(sizes)))));
            }
            families.add(inline(table(array(pool(subclasses)))));
        }
        return table(array(pool(families)));
    }

    // 两次独立抽取分别生成物品，保留同内容的两个数量 1 结果。
    private static JsonObject reward(boolean buried) {
        JsonArray pools = new JsonArray();
        for (int i = 0; i < 2; i++) {
            JsonObject pool = pool(array(reference()));
            if (!buried || i != 0) pool.add("conditions", array(object("condition", "minecraft:random_chance", "chance", 0.5)));
            pools.add(pool);
        }
        return table(pools);
    }

    private static JsonObject modifier(String source, String table) {
        return object("type", "neoforge:add_table", "conditions", array(object("condition", "neoforge:loot_table_id", "loot_table_id", source)),
                "table", "piranport:blueprints/" + table);
    }

    // 每项许可独立累计，完成全部条件后取得家族进度。
    private static JsonObject advancement(BlueprintFamily family) {
        String name = family.path();
        var types = Arrays.stream(BlueprintType.values()).filter(type -> type.family() == family).toList();
        JsonObject criteria = new JsonObject();
        JsonArray requirements = new JsonArray();
        for (var type : types) {
            String criterion = type.id().getPath().substring(name.length() + 1);
            criteria.add(criterion, object("trigger", "minecraft:inventory_changed", "conditions", object("items", array(object(
                    "predicates", object("piranport:blueprint_permission", object("permission", type.id().toString())))))));
            JsonArray requirement = new JsonArray();
            requirement.add(criterion);
            requirements.add(requirement);
        }
        return object("display", object("icon", object("id", "piranport:" + name + "_blueprint", "components",
                        object("piranport:blueprint_content", content(types))),
                "title", object("translate", "advancements.piranport.all_" + name + "_types.title"),
                "description", object("translate", "advancements.piranport.all_" + name + "_types.description"),
                "background", "minecraft:textures/block/prismarine.png", "frame", "goal", "show_toast", true, "announce_to_chat", true, "hidden", false),
                "criteria", criteria, "requirements", requirements);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return registries.thenCompose(provider -> {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            save(output, writes, "c/tags/item/papers", object("replace", false, "values", stringArray("minecraft:paper")));
            save(output, writes, "piranport/recipe/blueprint_merge", object("type", "piranport:blueprint_merge", "category", "misc"));
            save(output, writes, "piranport/recipe/blueprint_expand", object("type", "piranport:blueprint_expand", "category", "misc"));
            save(output, writes, "piranport/loot_table/blueprints/content", contentTable());
            save(output, writes, "piranport/loot_table/blueprints/shipwreck", reward(false));
            save(output, writes, "piranport/loot_table/blueprints/buried_treasure", reward(true));
            save(output, writes, "piranport/loot_modifiers/blueprints_shipwreck", modifier("minecraft:chests/shipwreck_treasure", "shipwreck"));
            save(output, writes, "piranport/loot_modifiers/blueprints_buried_treasure", modifier("minecraft:chests/buried_treasure", "buried_treasure"));
            save(output, writes, "neoforge/loot_modifiers/global_loot_modifiers", object("replace", false, "entries", stringArray(
                    "piranport:blueprints_shipwreck", "piranport:blueprints_buried_treasure")));
            for (var family : BlueprintFamily.values()) save(output, writes, "piranport/advancement/blueprints/all_" + family.path() + "_types", advancement(family));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    private static JsonArray stringArray(String... values) {
        JsonArray result = new JsonArray();
        for (String value : values) result.add(value);
        return result;
    }

    private void save(CachedOutput output, List<CompletableFuture<?>> writes, String path, JsonObject json) {
        writes.add(DataProvider.saveStable(output, json, data.resolve(path + ".json")));
    }

    @Override
    public String getName() { return "PiranPort blueprints"; }
}
