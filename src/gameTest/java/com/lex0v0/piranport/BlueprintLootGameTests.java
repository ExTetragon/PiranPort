package com.lex0v0.piranport;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lex0v0.piranport.item.blueprint.*;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 验证图纸奖励的数量、独立内容、候选合法性和投放范围。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class BlueprintLootGameTests {
    private static LootTable table(GameTestHelper h, String id) {
        return h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id)));
    }

    private static LootParams params(GameTestHelper h) {
        return new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,2,2))))
                .create(LootContextParamSets.CHEST);
    }

    private static final class ControlledRandom extends LegacyRandomSource {
        private final float[] chances;
        private int index;
        private ControlledRandom(float... chances) { super(0); this.chances = chances; }
        @Override public float nextFloat() { return chances[(index++) % chances.length]; }
        @Override public int nextInt(int bound) { return 0; }
    }

    @GameTest(template = "empty")
    public static void rewardCountBranchesAndSeparateStacks(GameTestHelper h) {
        var params = params(h);
        var shipwreck = table(h, "piranport:blueprints/shipwreck");
        var buried = table(h, "piranport:blueprints/buried_treasure");
        h.assertTrue(shipwreck.getRandomItems(params, new ControlledRandom(0.75F)).isEmpty(), "two misses give zero");
        h.assertTrue(shipwreck.getRandomItems(params, new ControlledRandom(0.25F,0.75F)).size() == 1, "one hit gives one");
        var pair = shipwreck.getRandomItems(params, new ControlledRandom(0.25F));
        h.assertTrue(pair.size() == 2 && pair.get(0) != pair.get(1) && pair.stream().allMatch(stack -> stack.getCount() == 1), "two independently allocated count-one outputs");
        h.assertTrue(ItemStack.isSameItemSameComponents(pair.get(0), pair.get(1)), "identical contents are still not compacted");
        h.assertTrue(buried.getRandomItems(params, new ControlledRandom(0.75F)).size() == 1, "buried guaranteed one");
        h.assertTrue(buried.getRandomItems(params, new ControlledRandom(0.25F)).size() == 2, "buried optional second");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void rewardDistributionAndIndependentContents(GameTestHelper h) {
        var params = params(h);
        var random = RandomSource.create(20261023);
        var wreck = table(h, "piranport:blueprints/shipwreck");
        var buried = table(h, "piranport:blueprints/buried_treasure");
        int[] counts = new int[3]; int[] buriedCounts = new int[3];
        boolean differentPair = false;
        for (int i = 0; i < 4096; i++) {
            var loot = wreck.getRandomItems(params, random);
            counts[loot.size()]++;
            buriedCounts[buried.getRandomItems(params, random).size()]++;
            if (loot.size() == 2 && !ItemStack.isSameItemSameComponents(loot.get(0), loot.get(1))) differentPair = true;
            for (var stack : loot) {
                h.assertTrue(BlueprintRules.isValid(stack) && stack.getCount() == 1, "legal individual reward");
                h.assertTrue(BlueprintRules.getAllowedTypes(stack).stream().map(BlueprintType::subclass).distinct().count() == 1, "one subclass only");
            }
        }
        for (int i : List.of(0,2)) h.assertTrue(Math.abs(counts[i] / 4096.0 - 0.25) < 0.04, "wreck 25% tails");
        h.assertTrue(Math.abs(counts[1] / 4096.0 - 0.5) < 0.04, "wreck 50% single");
        h.assertTrue(buriedCounts[0] == 0 && Math.abs(buriedCounts[1]/4096.0 - 0.5) < 0.04, "buried 50/50");
        h.assertTrue(differentPair, "second content is independently selected");
        h.succeed();
    }

    private static void collectCandidates(JsonElement element, List<BlueprintContent> result) {
        if (element.isJsonArray()) for (var child : element.getAsJsonArray()) collectCandidates(child, result);
        if (element.isJsonObject()) {
            var obj = element.getAsJsonObject();
            if (obj.has("piranport:blueprint_content")) result.add(BlueprintContent.CODEC.parse(JsonOps.INSTANCE, obj.get("piranport:blueprint_content")).getOrThrow());
            else for (var entry : obj.entrySet()) collectCandidates(entry.getValue(), result);
        }
    }

    @GameTest(template = "empty")
    public static void allCandidatesAndContentWeights(GameTestHelper h) throws Exception {
        var resource = h.getLevel().getServer().getResourceManager().getResource(ResourceLocation.parse("piranport:loot_table/blueprints/content.json")).orElseThrow();
        List<BlueprintContent> candidates = new ArrayList<>();
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) { collectCandidates(JsonParser.parseReader(reader), candidates); }
        h.assertTrue(candidates.size() == 114 && new HashSet<>(candidates).size() == 114, "114 unique nonempty candidates");
        int ship = 0;
        for (var candidate : candidates) {
            var first = BlueprintType.byId(candidate.permissions().getFirst());
            if (first.family() == BlueprintFamily.SHIP) ship++;
            h.assertTrue(candidate.permissions().stream().allMatch(id -> BlueprintType.byId(id).subclass() == first.subclass()), "all candidate IDs known and in same subclass");
        }
        h.assertTrue(ship == 21, "21 ship and 93 equipment candidates");
        var params = params(h); var content = table(h, "piranport:blueprints/content");
        int[] subclasses = new int[6];
        var random = RandomSource.create(128128);
        Set<BlueprintContent> seen = new HashSet<>();
        for (int i = 0; i < 4096; i++) {
            var stack = content.getRandomItems(params, random).getFirst();
            var type = BlueprintRules.getAllowedTypes(stack).iterator().next();
            subclasses[type.subclass().ordinal()]++;
            seen.add(stack.get(ModDataComponents.BLUEPRINT_CONTENT.get()));
        }
        for (int count : subclasses) h.assertTrue(Math.abs(count/4096.0 - 1.0/6) < 0.035, "equal family and subclass weighting");
        h.assertTrue(seen.size() > 100, "content explores subsets rather than a fixed permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTreasureSourcesAreModified(GameTestHelper h) {
        var params = params(h);
        for (var id : List.of("minecraft:chests/shipwreck_treasure", "minecraft:chests/buried_treasure")) {
            var loot = table(h, id).getRandomItems(params, new ControlledRandom(0.25F));
            h.assertTrue(loot.stream().filter(BlueprintRules::isValid).count() == 2, "treasure GLM contributes two count-one results");
            h.assertTrue(loot.stream().anyMatch(stack -> !BlueprintRules.isValid(stack)), "vanilla contents retained");
        }
        for (var id : List.of("minecraft:chests/shipwreck_map", "minecraft:chests/shipwreck_supply", "minecraft:chests/simple_dungeon")) {
            h.assertTrue(table(h,id).getRandomItems(params, RandomSource.create(1)).stream().noneMatch(BlueprintRules::isValid), "unrelated tables unchanged");
        }
        h.succeed();
    }
}
