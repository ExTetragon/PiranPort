package com.lex0v0.piranport;

import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.item.rigging.RiggingRegion;
import com.lex0v0.piranport.item.equipment.EquipmentRules;
import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

// 捕获真实日志，验证分级、模拟无输出与恢复限频，不启动客户端。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class LoggingGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(PiranPort.MODID, path); }
    private static FluidStack oil(int amount) { return new FluidStack(ModFluids.FUEL_OIL.get(), amount); }
    private static final class Capture extends AbstractAppender implements AutoCloseable {
        final List<LogEvent> events = new ArrayList<>();
        final Thread thread = Thread.currentThread();
        final boolean previous = ModLog.debugEnabled();
        final Logger logger = (Logger) LogManager.getLogger(PiranPort.MODID);
        Capture() { super("PiranPortLoggingTest", null, null, true, Property.EMPTY_ARRAY); start(); logger.addAppender(this); }
        @Override public void append(LogEvent event) { if (Thread.currentThread() == thread) events.add(event.toImmutable()); }
        long count(String text) { return events.stream().filter(e -> e.getMessage().getFormattedMessage().contains(text)).count(); }
        @Override public void close() { logger.removeAppender(this); stop(); ModLog.configureDebug(previous); }
    }

    @GameTest(template = "empty", batch = "logging")
    public static void debugGateDoesNotChangeRootOrHideWarnings(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.SHIPYARD.get());
        var yard = h.getBlockEntity(POS);
        try (var capture = new Capture()) {
            Level root = LogManager.getRootLogger().getLevel();
            ModLog.configureDebug(false);
            ModLog.debug(ModLog.Event.JOB_START, "job", "hidden");
            ModLog.aggregate(ModLog.Event.OIL_LAKE, 0, false);
            ModLog.flush();
            h.assertTrue(capture.events.isEmpty(), "disabled debug and aggregate emit nothing");
            ModLog.invalidRestore(yard, "logging-test");
            h.assertTrue(capture.events.size() == 1 && capture.events.getFirst().getLevel() == Level.WARN, "warnings remain visible");
            ModLog.configureDebug(true);
            ModLog.debug(ModLog.Event.JOB_START, "job", "visible");
            h.assertTrue(capture.events.size() == 2 && capture.events.getLast().getLevel() == Level.DEBUG, "debug emits under normal root logging");
            h.assertTrue(LogManager.getRootLogger().getLevel() == root, "root logger unchanged");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "logging")
    public static void simulationIsSilentAndTransfersAreAggregated(GameTestHelper h) {
        try (var capture = new Capture()) {
            ModLog.configureDebug(false); ModLog.configureDebug(true);
            var core = RiggingCoreRules.create(id("lexington_cv2"));
            var fuel = RiggingCoreRules.fuel(core);
            var slots = RiggingCoreRules.inventory(core, RiggingRegion.CARGO);
            fuel.fill(oil(1000), FluidAction.SIMULATE);
            slots.insertItem(0, new ItemStack(Items.STONE), true);
            RiggingCoreRules.definition(core); slots.getStackInSlot(0);
            ModLog.flush();
            h.assertTrue(capture.events.isEmpty() && fuel.getFluid().isEmpty(), "query and simulation stay silent and pure");
            fuel.fill(oil(1000), FluidAction.EXECUTE);
            fuel.drain(oil(200), FluidAction.SIMULATE);
            fuel.drain(oil(200), FluidAction.EXECUTE);
            fuel.drain(100, FluidAction.EXECUTE);
            h.assertTrue(capture.events.isEmpty(), "successful fluid transfers wait for aggregation");
            ModLog.flush();
            h.assertTrue(capture.events.size() == 2 && capture.count("operations=2, successes=2, amount=300") == 1
                    && fuel.getFluid().getAmount() == 700, "drain overloads counted once, simulated drain excluded");
            slots.insertItem(0, new ItemStack(Items.STONE), false);
            h.assertTrue(capture.events.size() == 3, "actual inventory write recorded once");
            ModLog.flush();
            h.assertTrue(capture.events.size() == 3, "empty summary emits nothing");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "logging")
    public static void invalidStoredJobWarnsOnlyOnServerLoad(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.SHIPYARD.get());
        var yard = (ShipyardBlockEntity) h.getBlockEntity(POS);
        try (var capture = new Capture()) {
            ModLog.configureDebug(false);
            CompoundTag storage = yard.saveWithoutMetadata(h.getLevel().registryAccess());
            CompoundTag invalid = new CompoundTag(); invalid.putString("Ship", "invalid id"); storage.put("Construction", invalid);
            yard.loadWithComponents(storage, h.getLevel().registryAccess());
            h.assertTrue(capture.events.isEmpty(), "network-style state decode does not log");
            yard.onLoad(); yard.onLoad();
            h.assertTrue(capture.events.size() == 1 && capture.events.getFirst().getLevel() == Level.WARN, "invalid nonempty task warns once");
            storage.remove("Construction"); yard.loadWithComponents(storage, h.getLevel().registryAccess()); yard.onLoad();
            h.assertTrue(capture.events.size() == 1, "ordinary idle storage does not warn");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "logging")
    public static void jobTransitionsDoNotLogEachTick(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.SHIPYARD.get());
        var yard = (ShipyardBlockEntity) h.getBlockEntity(POS);
        var ship = ShipDefinitions.byId(id("lexington_cv2"));
        var recipe = yard.recipe(ship.id());
        var blueprint = BlueprintRules.create(BlueprintType.byId(ship.type().id()));
        yard.insert(0, blueprint, false);
        yard.insert(2, new ItemStack(ModItems.STEEL_INGOT.get(), recipe.steel()), false);
        yard.insert(3, new ItemStack(ModItems.ALUMINUM_INGOT.get(), recipe.aluminum()), false);
        yard.insert(4, new ItemStack(ModItems.HIGH_EXPLOSIVE_SHELL.get(), recipe.ammunition()/2), false);
        yard.fluid(null).fill(oil(recipe.fuel()), FluidAction.EXECUTE);
        try (var capture = new Capture()) {
            ModLog.configureDebug(false); ModLog.configureDebug(true);
            h.assertTrue(yard.start(ship.id(), null) == ShipyardBlockEntity.StartResult.STARTED, "task started");
            for (int i = 0; i < 20; i++) ShipyardBlockEntity.tick(h.getLevel(), yard.getBlockPos(), yard.getBlockState(), yard);
            h.assertTrue(capture.count("制造开工") == 1 && capture.count("制造状态改变") == 1, "start once and running state once");
            h.assertTrue(yard.cancel(yard.job().id, null), "cancelled");
            h.assertTrue(capture.count("取消制造") == 1 && yard.fuel().getAmount() == recipe.fuel(), "cancel log does not alter refund");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "logging")
    public static void durabilityLogsOnlyCommittedWear(GameTestHelper h) {
        boolean retain = Config.RETAIN_BROKEN_EQUIPMENT.get();
        try (var capture = new Capture()) {
            ModLog.configureDebug(true);
            Config.RETAIN_BROKEN_EQUIPMENT.set(false);
            var weapon = EquipmentRules.create(id("experimental_coilgun"));
            int maximum = weapon.getMaxDamage();
            weapon.getItem().damageItem(weapon, 1, null, item -> {});
            h.assertTrue(capture.events.isEmpty() && EquipmentRules.remainingDurability(weapon) == maximum,
                    "vanilla preparation hook alone does not report wear");
            weapon.hurtAndBreak(1, h.getLevel(), (net.minecraft.server.level.ServerPlayer) null, item -> {});
            h.assertTrue(capture.count("装备耗损") == 1 && EquipmentRules.remainingDurability(weapon) == maximum-1,
                    "vanilla committed wear logged once");
            weapon.hurtAndBreak(maximum, h.getLevel(), (net.minecraft.server.level.ServerPlayer) null, item -> {});
            h.assertTrue(weapon.isEmpty() && capture.count("装备耗损") == 2 && capture.count("destroyed=true") == 1,
                    "vanilla destruction logged after wear");
            Config.RETAIN_BROKEN_EQUIPMENT.set(true);
            var retained = EquipmentRules.create(id("experimental_coilgun"));
            retained.hurtAndBreak(maximum, h.getLevel(), (net.minecraft.server.level.ServerPlayer) null, item -> {});
            h.assertTrue(!retained.isEmpty() && EquipmentRules.broken(retained) && capture.count("装备耗损") == 3,
                    "retained broken equipment logged once");
        } finally { Config.RETAIN_BROKEN_EQUIPMENT.set(retain); }
        h.succeed();
    }
}
