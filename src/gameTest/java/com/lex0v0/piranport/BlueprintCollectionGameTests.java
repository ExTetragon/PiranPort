package com.lex0v0.piranport;

import com.lex0v0.piranport.advancement.predicate.BlueprintPermissionPredicate;
import com.lex0v0.piranport.item.blueprint.*;
import com.lex0v0.piranport.event.BlueprintCollectionEvents;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModItems;
import java.util.Arrays;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static com.lex0v0.piranport.item.blueprint.BlueprintType.*;

// 验证许可累计、家族隔离、登录补判和进度保存。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class BlueprintCollectionGameTests {
    private static AdvancementHolder advancement(GameTestHelper h, BlueprintFamily family) {
        return h.getLevel().getServer().getAdvancements().get(ResourceLocation.parse("piranport:blueprints/all_" + family.path() + "_types"));
    }

    private static AdvancementProgress progress(GameTestHelper h, ServerPlayer player, BlueprintFamily family) {
        return player.getAdvancements().getOrStartProgress(advancement(h,family));
    }

    private static void acquire(ServerPlayer player, ItemStack stack) {
        player.getInventory().setItem(0, stack);
        player.inventoryMenu.broadcastChanges();
    }

    @GameTest(template = "empty")
    public static void cumulativeCollectionAndFamilyIsolation(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        h.assertTrue(advancement(h,BlueprintFamily.SHIP).value().criteria().size() == 9
                && advancement(h,BlueprintFamily.EQUIPMENT).value().criteria().size() == 14, "two independent permission goals");
        acquire(player, BlueprintRules.create(BATTLESHIP, DESTROYER));
        var ship = progress(h,player,BlueprintFamily.SHIP);
        h.assertTrue(ship.getCriterion("battleship").isDone() && ship.getCriterion("destroyer").isDone() && !ship.isDone(), "composite blueprint satisfies contained criteria only");
        h.assertTrue(!progress(h,player,BlueprintFamily.EQUIPMENT).hasProgress(), "families do not cross");
        acquire(player, ItemStack.EMPTY);
        h.assertTrue(ship.getCriterion("battleship").isDone(), "consumption or gifting never retracts criteria");
        for (var type : BlueprintType.values()) acquire(player,BlueprintRules.create(type));
        h.assertTrue(ship.isDone() && progress(h,player,BlueprintFamily.EQUIPMENT).isDone(), "history completes without holding all at once");
        player.getAdvancements().save();
        var json = AdvancementProgress.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, ship).getOrThrow();
        var saved = AdvancementProgress.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        saved.update(advancement(h,BlueprintFamily.SHIP).value().requirements());
        h.assertTrue(saved.isDone(), "native advancement progress persistence roundtrip");
        player.getAdvancements().reload(h.getLevel().getServer().getAdvancements());
        h.assertTrue(progress(h,player,BlueprintFamily.SHIP).isDone() && progress(h,player,BlueprintFamily.EQUIPMENT).isDone(),
                "saved progress survives native reload and listener reconstruction");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void loginBackfillAndOffhand(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.getInventory().items.set(0, BlueprintRules.create(BlueprintSubclass.LARGE));
        player.getInventory().offhand.set(0, BlueprintRules.create(RADAR, SONAR));
        BlueprintCollectionEvents.checkExistingInventory(player);
        var ship = progress(h,player,BlueprintFamily.SHIP);
        h.assertTrue(ship.getCriterion("aircraft_carrier").isDone() && ship.getCriterion("battlecruiser").isDone(), "login backfills existing main inventory");
        var equipment = progress(h,player,BlueprintFamily.EQUIPMENT);
        h.assertTrue(equipment.getCriterion("radar").isDone() && equipment.getCriterion("sonar").isDone(), "offhand contributes");
        h.assertTrue(!equipment.getCriterion("armor").isDone(), "unowned permissions remain absent");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidAndUnacquiredBlueprintsGiveNoProgress(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var preview = BlueprintRules.create(BlueprintSubclass.DEVICE);
        h.assertTrue(!progress(h,player,BlueprintFamily.EQUIPMENT).hasProgress(), "creating or viewing an item is not acquiring it");
        var invalid = new ItemStack(ModItems.SHIP_BLUEPRINT.get());
        var retired = ResourceLocation.parse("piranport:ship/replenishment_ship");
        for (var ids : List.of(List.of(retired), List.of(BATTLESHIP.id(), retired), List.of(BATTLESHIP.id(), RADAR.id()))) {
            invalid.set(ModDataComponents.BLUEPRINT_CONTENT.get(), new BlueprintContent(ids));
            acquire(player,invalid);
            h.assertTrue(!progress(h,player,BlueprintFamily.SHIP).hasProgress()
                    && !progress(h,player,BlueprintFamily.EQUIPMENT).hasProgress(), "retired and mixed-family permissions cannot satisfy criteria");
        }
        var predicate = new BlueprintPermissionPredicate(RADAR.id());
        h.assertTrue(predicate.matches(preview) && !predicate.matches(invalid), "predicate tests containment plus legality");
        player.getInventory().armor.set(0, BlueprintRules.create(BATTLESHIP));
        player.getInventory().items.clear();
        BlueprintCollectionEvents.checkExistingInventory(player);
        h.assertTrue(!progress(h,player,BlueprintFamily.SHIP).hasProgress(), "login scan does not search armor or nested storage");
        h.succeed();
    }
}
