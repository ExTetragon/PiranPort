package com.lex0v0.piranport.client;

import com.lex0v0.piranport.PiranPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
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
        if (!(minecraft.screen instanceof TitleScreen) || minecraft.getOverlay() != null) return;
        finished = true;
        boolean jei = ModList.get().isLoaded("jei"), spark = ModList.get().isLoaded("spark");
        boolean blueprints = false;
        try { blueprints = BlueprintClientChecks.verify(minecraft); }
        catch (RuntimeException error) { PiranPort.LOGGER.error("Blueprint client resource checks failed", error); }
        System.out.println("PIRANPORT_CLIENT_HELPERS_LOADED=" + new com.google.gson.Gson().toJson(
                java.util.Map.of("jei", jei, "spark", spark, "titleScreen", true, "blueprints", blueprints)));
        minecraft.stop();
    }
}
