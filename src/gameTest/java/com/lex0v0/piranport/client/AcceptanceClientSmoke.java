package com.lex0v0.piranport.client;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.PiranPort;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// 自动启动检查结束后退出，普通验收客户端保留交互。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class AcceptanceClientSmoke {
    private static boolean finished;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (finished || !Boolean.getBoolean("piranport.acceptance.clientSmoke")) return;
        var minecraft = Minecraft.getInstance();
        if (!AcceptanceClientStartup.titleScreenReady(minecraft)) return;
        finished = true;
        boolean jei = ModList.get().isLoaded("jei"), spark = ModList.get().isLoaded("spark");
        boolean blueprints = false;
        boolean riggingCore = false;
        boolean modTooltips = false;
        boolean equipment = false;
        boolean ammunition = false;
        boolean shipyardModel = false;
        try { blueprints = BlueprintClientChecks.verify(minecraft); }
        catch (RuntimeException error) { ModLog.failure(ModLog.Failure.BLUEPRINT_RESOURCES, error); }
        try { riggingCore = RiggingClientChecks.verify(minecraft); }
        catch (RuntimeException error) { ModLog.failure(ModLog.Failure.CORE_RESOURCES, error); }
        try { modTooltips = ModTooltipClientChecks.verify(minecraft); }
        catch (RuntimeException error) { ModLog.failure(ModLog.Failure.TOOLTIPS, error); }
        try { equipment = EquipmentClientChecks.verify(minecraft); }
        catch (RuntimeException error) { ModLog.failure(ModLog.Failure.EQUIPMENT_RESOURCES, error); }
        try { ammunition = AmmunitionClientChecks.verify(minecraft); }
        catch (RuntimeException error) { ModLog.failure(ModLog.Failure.TOOLTIPS, error); }
        try { shipyardModel = ShipyardClientChecks.verify(minecraft); }
        catch (RuntimeException error) { ModLog.failure(ModLog.Failure.SHIPYARD_RESOURCES, error); }
        ModLog.evidence(ModLog.Evidence.HELPERS, new com.google.gson.Gson().toJson(
                java.util.Map.of("jei", jei, "spark", spark, "titleScreen", true, "blueprints", blueprints, "riggingCore", riggingCore, "modTooltips", modTooltips, "equipment", equipment, "ammunition", ammunition, "shipyardModel", shipyardModel)));
        minecraft.stop();
    }
}
