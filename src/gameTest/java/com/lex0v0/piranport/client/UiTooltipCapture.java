package com.lex0v0.piranport.client;

import com.lex0v0.piranport.PiranPort;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

// 在真实界面渲染阶段截取悬浮，沿用当前正交投影和深度状态。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class UiTooltipCapture {
    private record Request(Screen screen, Path output, String name, int x, int y) {}
    private static final ArrayDeque<Request> PENDING = new ArrayDeque<>();
    static final Map<String, List<String>> TOOLTIP_TEXT = new HashMap<>();
    private static boolean capturing;
    private static List<String> observed = List.of();
    private UiTooltipCapture() {}
    static void request(Screen screen, Path output, String name, int x, int y) {
        PENDING.add(new Request(screen, output, name, x, y));
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        while (!PENDING.isEmpty() && PENDING.peek().screen == event.getScreen()) {
            var request = PENDING.remove();
            var graphics = event.getGuiGraphics();
            com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xff15212c);
            graphics.flush();
            observed = List.of();
            capturing = true;
            try { request.screen.renderWithTooltip(graphics, request.x, request.y, event.getPartialTick()); }
            finally { capturing = false; }
            TOOLTIP_TEXT.put(request.name, observed);
            graphics.flush();
            Screenshot.grab(request.output.toFile(), request.name, Minecraft.getInstance().getMainRenderTarget(), message -> System.out.println("TOOLTIP_SCREENSHOT=" + message.getString()));
        }
    }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.GatherComponents event) {
        if (capturing) observed = event.getTooltipElements().stream().flatMap(element -> element.left().stream())
                .map(net.minecraft.network.chat.FormattedText::getString).toList();
    }
}
