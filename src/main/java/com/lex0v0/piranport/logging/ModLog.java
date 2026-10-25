package com.lex0v0.piranport.logging;

import com.lex0v0.piranport.Config;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModRecipeTypes;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

// 集中管理控制台文案、级别与汇总；业务代码只报告实际事件。
public final class ModLog {
    private static final Logger LOGGER = LogManager.getLogger(PiranPort.MODID);
    private static volatile boolean debug;
    private static volatile boolean reloadPending;
    private static String lastConfig;
    private static String version;
    private static final Map<Event, Totals> TOTALS = new EnumMap<>(Event.class);
    private ModLog() {}

    public enum Event {
        AMMUNITION_BATCH("弹药批次产出汇总"),
        JOB_START("制造开工"), JOB_FINISH("制造完成"), JOB_CANCEL("取消制造"),
        JOB_STATE("制造状态改变"), JOB_RESTORE("恢复制造任务"), REFUND("返还物品"),
        ACTION("制造操作结果"), CONNECT("燃料桶连接"), DISCONNECT("燃料桶断连"),
        AXIS("燃料桶切轴"), TRANSFER("容器流体转移"),
        BARREL_FILL("燃料桶灌入汇总"), BARREL_DRAIN("燃料桶抽出汇总"),
        MACHINE_FILL("制造设施灌入汇总"), MACHINE_DRAIN("制造设施抽出汇总"),
        CORE_FILL("核心灌油汇总"), CORE_DRAIN("核心取油汇总"),
        AIRCRAFT_FILL("飞机灌油汇总"), AIRCRAFT_DRAIN("飞机取油汇总"),
        BARREL_ITEM_FILL("燃料桶物品灌入汇总"), BARREL_ITEM_DRAIN("燃料桶物品抽出汇总"),
        CORE_SLOT("核心栏位改变"), PROFICIENCY("核心熟练度改变"), WEAR("装备耗损"),
        BLUEPRINT_CRAFT("图纸合成"), BLUEPRINT_LOGIN("图纸登录补判"),
        OIL_LAKE("油湖生成汇总"), SALTPETER("硝土生成汇总");
        private final String text;
        Event(String text) { this.text = text; }
    }

    // 验收标记保留原始行格式，测试类和夹具仍不进入发行包。
    public enum Evidence {
        HELPERS("PIRANPORT_CLIENT_HELPERS_LOADED="), TOOLTIP_ITEMS("PIRANPORT_TOOLTIP_ITEMS_CHECKED="),
        FACTORY_UI("PIRANPORT_FACTORY_UI="), SHIPYARD_UI("PIRANPORT_SHIPYARD_UI="), AMMUNITION_UI("PIRANPORT_AMMUNITION_UI="),
        TOOLTIP_SCREENSHOT("TOOLTIP_SCREENSHOT="), UI_SCREENSHOT("UI_SCREENSHOT="),
        NATURAL_REPORT("PIRANPORT_NATURAL_WORLDGEN_REPORT="), NATURAL_REGION("PIRANPORT_NATURAL_REGION="),
        WORLDGEN_SHAPES("PIRANPORT_WORLDGEN_SHAPES="), SALTPETER_SAMPLES("PIRANPORT_SALTPETER_SAMPLES=");
        private final String prefix;
        Evidence(String prefix) { this.prefix = prefix; }
    }

    public enum Failure {
        BLUEPRINT_RESOURCES("图纸客户端资源检查失败"), CORE_RESOURCES("核心客户端资源检查失败"),
        TOOLTIPS("物品悬浮检查失败"), EQUIPMENT_RESOURCES("装备客户端资源检查失败"),
        SHIPYARD_UI("船坞界面验收失败"), FACTORY_UI("工厂界面验收失败"), AMMUNITION_UI("弹药生产线界面验收失败"),
        WORLDGEN("自然生成验收失败"), REPORT("验收报告写入失败");
        private final String text;
        Failure(String text) { this.text = text; }
    }

    public static void register(IEventBus modBus, ModContainer container) {
        version = container.getModInfo().getVersion().toString();
        modBus.addListener(ModLog::configChanged);
        NeoForge.EVENT_BUS.addListener(ModLog::serverStarting);
        NeoForge.EVENT_BUS.addListener(ModLog::tagsUpdated);
        NeoForge.EVENT_BUS.addListener(ModLog::serverTick);
        NeoForge.EVENT_BUS.addListener(ModLog::serverStopped);
    }

    private static synchronized void configChanged(ModConfigEvent event) {
        if (!PiranPort.MODID.equals(event.getConfig().getModId()) || event instanceof ModConfigEvent.Unloading) return;
        configureDebug(Config.DEBUG_LOGGING.get());
        String current = configuration();
        if (!Objects.equals(lastConfig, current)) {
            LOGGER.info("公共配置{}: {}", lastConfig == null ? "加载" : "更新", current);
            lastConfig = current;
        }
    }

    // 仅调整本模组命名空间，不改变 Minecraft、NeoForge 或其他模组的日志级别。
    public static synchronized void configureDebug(boolean enabled) {
        debug = enabled;
        Configurator.setLevel(PiranPort.MODID, enabled ? Level.DEBUG : Level.INFO);
        if (!enabled) TOTALS.clear();
    }

    public static boolean debugEnabled() { return debug; }

    private static String configuration() {
        return "debugLogging=" + Config.DEBUG_LOGGING.get() + ", fuelBarrelCapacity=" + Config.FUEL_BARREL_CAPACITY.get()
                + ", fuelBarrelAxialConnection=" + Config.FUEL_BARREL_AXIAL_CONNECTION.get()
                + ", retainBrokenEquipment=" + Config.RETAIN_BROKEN_EQUIPMENT.get()
                + ", allowQuickRepairFromPlayerInventory=" + Config.ALLOW_QUICK_REPAIR_FROM_PLAYER_INVENTORY.get()
                + ", allowDamageControlFromPlayerInventory=" + Config.ALLOW_DAMAGE_CONTROL_FROM_PLAYER_INVENTORY.get();
    }

    private static void serverStarting(ServerStartingEvent event) {
        LOGGER.info("PiranPort {} 服务端启动: {}", version, configuration());
        recipes(event.getServer());
    }

    private static void tagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) reloadPending = true;
    }

    private static void recipes(MinecraftServer server) {
        reloadPending = false;
        LOGGER.info("配方与标签已加载: shipyardRecipes={}, developmentFactoryRecipes={}, ammunitionFactoryRecipes={}",
                server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHIPYARD.get()).size(),
                server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.DEVELOPMENT_FACTORY.get()).size(),
                server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AMMUNITION_FACTORY.get()).size());
        var ammunition=server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AMMUNITION_FACTORY.get());
        for(var type:com.lex0v0.piranport.domain.ammunition.AmmunitionType.values()){
            var matching=ammunition.stream().filter(h->h.value().target().equals(type.id())).toList();
            if(matching.size()>1)LOGGER.warn("弹药生产配方目标重复，已拒绝开工: target={}, recipes={}",type.id(),matching.stream().map(h->h.id().toString()).toList());
        }
    }

    private static void serverTick(ServerTickEvent.Post event) {
        if (reloadPending) recipes(event.getServer());
        if (event.getServer().getTickCount() % 100 == 0) flush();
    }

    private static void serverStopped(ServerStoppedEvent event) {
        flush();
        reloadPending = false;
    }

    private static String fields(Object... fields) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i + 1 < fields.length; i += 2) {
            if (!result.isEmpty()) result.append(", ");
            result.append(fields[i]).append('=').append(fields[i + 1]);
        }
        return result.toString();
    }

    public static void debug(Event event, Object... fields) {
        if (debug) LOGGER.debug("{}: {}", event.text, fields(fields));
    }

    public static void machine(Event event, BlockEntity entity, Object... fields) {
        if (debug && entity.getLevel() != null && !entity.getLevel().isClientSide)
            LOGGER.debug("{}: dimension={}, pos={}, {}", event.text, entity.getLevel().dimension().location(),
                    entity.getBlockPos().toShortString(), fields(fields));
    }

    public static void invalidRestore(BlockEntity entity, String field) {
        if (entity.getLevel() != null && !entity.getLevel().isClientSide)
            LOGGER.warn("非空存档字段无法恢复: dimension={}, pos={}, field={}", entity.getLevel().dimension().location(),
                    entity.getBlockPos().toShortString(), field);
    }

    // 固定事件桶限制内存占用，多线程世界生成与流体高频调用只累计数值。
    public static void aggregate(Event event, long amount, boolean success) {
        if (!debug) return;
        synchronized (ModLog.class) {
            if (!debug) return;
            Totals totals = TOTALS.computeIfAbsent(event, ignored -> new Totals());
            totals.attempts++; totals.successes += success ? 1 : 0; totals.amount += amount;
        }
    }

    public static synchronized void flush() {
        if (debug) TOTALS.forEach((event, totals) -> LOGGER.debug("{}: operations={}, successes={}, amount={}",
                event.text, totals.attempts, totals.successes, totals.amount));
        TOTALS.clear();
    }

    private static final class Totals { long attempts, successes, amount; }

    public static void evidence(Evidence evidence, Object value) { System.out.println(evidence.prefix + value); }
    public static void onboardingDismissed() { LOGGER.info("PIRANPORT_ACCEPTANCE_ONBOARDING_DISMISSED"); }
    public static void failure(Failure failure, Throwable error) { LOGGER.error(failure.text, error); }
}
