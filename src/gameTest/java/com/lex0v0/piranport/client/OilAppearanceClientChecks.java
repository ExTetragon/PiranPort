package com.lex0v0.piranport.client;

import com.google.gson.GsonBuilder;
import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.lwjgl.opengl.GL11;

// 独立主菜单检查真实图集动画与桶模型，不读取玩家存档。
@EventBusSubscriber(modid = PiranPort.MODID, value = Dist.CLIENT)
public final class OilAppearanceClientChecks {
    private static final Path OUTPUT = Path.of(System.getProperty("piranport.acceptance.oilOutput", "build/oil-appearance-client"));
    private static final Map<String, Set<Integer>> FRAMES = new LinkedHashMap<>();
    private static final Map<String, Object> REPORT = new LinkedHashMap<>();
    private static Preview screen;
    private static int ticks;
    private static boolean finished;

    private OilAppearanceClientChecks() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (finished || !Boolean.getBoolean("piranport.acceptance.oilAppearance")) return;
        var mc = Minecraft.getInstance();
        try {
            if (screen == null) {
                if (!AcceptanceClientStartup.titleScreenReady(mc)) return;
                Files.createDirectories(OUTPUT);
                verifyResources(mc);
                screen = new Preview();
                mc.setScreen(screen);
            }
            var atlas = (TextureAtlas) mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            // 主菜单没有世界 tick，主动推进同一套原版图集动画。
            atlas.tick();
            atlas.bind();
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            try (var image = new NativeImage(w, h, false)) {
                image.downloadTexture(0, false);
                for (var entry : FRAMES.entrySet()) {
                    var sprite = sprite(mc, entry.getKey());
                    int hash = 1;
                    for (int y = 0; y < sprite.contents().height(); y++) {
                        for (int x = 0; x < sprite.contents().width(); x++) {
                            hash = 31 * hash + image.getPixelRGBA(sprite.getX() + x, sprite.getY() + y);
                        }
                    }
                    entry.getValue().add(hash);
                }
            }
            ticks++;
            if (ticks == 8 || ticks == 39) UiTooltipCapture.requestScreen(screen, OUTPUT, "oil-animation-" + ticks + ".png");
            if (ticks < 72 || UiTooltipCapture.pending()) return;
            for (var entry : FRAMES.entrySet()) require(entry.getValue().size() > 1, entry.getKey() + " did not animate on GPU");
            require(Files.isRegularFile(OUTPUT.resolve("screenshots/oil-animation-8.png")), "first screenshot missing");
            require(Files.isRegularFile(OUTPUT.resolve("screenshots/oil-animation-39.png")), "second screenshot missing");
            REPORT.put("gpuDistinctFrames", FRAMES.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
            REPORT.put("atlasTicks", ticks);
            REPORT.put("passed", true);
            finish(mc);
        } catch (Exception error) {
            REPORT.put("passed", false);
            REPORT.put("error", error.toString());
            finish(mc);
        }
    }

    private static void verifyResources(Minecraft mc) throws Exception {
        for (var fluid : List.of(ModFluids.CRUDE_OIL.get(), ModFluids.FLOWING_CRUDE_OIL.get(), ModFluids.FUEL_OIL.get(), ModFluids.FLOWING_FUEL_OIL.get())) {
            var extension = IClientFluidTypeExtensions.of(fluid);
            require(extension.getTintColor() == 0xffffffff, "oil was tinted twice");
            String oil = fluid.isSame(ModFluids.CRUDE_OIL.get()) ? "crude_oil" : "fuel_oil";
            require(extension.getStillTexture().equals(id(oil + "_still")), "wrong still texture");
            require(extension.getFlowingTexture().equals(id(oil + "_flow")), "wrong flow texture");
        }
        for (String oil : List.of("crude_oil", "fuel_oil")) {
            int[] base = oil.equals("crude_oil") ? new int[]{27, 27, 27} : new int[]{193, 158, 87};
            for (String kind : List.of("still", "flow")) {
                String name = oil + "_" + kind;
                var content = sprite(mc, name).contents();
                int size = kind.equals("still") ? 16 : 32;
                require(content.name().equals(id(name)), "missing atlas sprite " + name);
                require(content.width() == size && content.height() == size, "wrong frame size " + name);
                require(content.getUniqueFrames().count() == 32, "wrong animation frame count " + name);
                var location = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/water_" + kind + ".png");
                try (var stream = mc.getResourceManager().getResource(location).orElseThrow().open(); var water = NativeImage.read(stream)) {
                    var oilImage = content.byMipLevel[0];
                    require(oilImage.getWidth() == water.getWidth() && oilImage.getHeight() == water.getHeight(), "strip dimensions changed");
                    for (int y = 0; y < water.getHeight(); y++) {
                        for (int x = 0; x < water.getWidth(); x++) {
                            int source = water.getPixelRGBA(x, y), target = oilImage.getPixelRGBA(x, y);
                            int expected = (oil.equals("crude_oil") ? 242 : source >>> 24) << 24;
                            for (int channel = 0; channel < 3; channel++) expected |= Math.min(255, Math.round(base[channel] * (source & 255) / 165.0f)) << (8 * channel);
                            require(target == expected, "water pattern or alpha changed: " + name);
                        }
                    }
                }
                FRAMES.put(name, new HashSet<>());
            }
        }
        for (var item : List.of(ModItems.CRUDE_OIL_BUCKET.get(), ModItems.FUEL_OIL_BUCKET.get())) {
            var model = mc.getItemRenderer().getModel(new ItemStack(item), null, null, 0);
            require(model != mc.getModelManager().getMissingModel(), "missing bucket model");
            require(model.getParticleIcon().contents().width() == 16 && model.getParticleIcon().contents().height() == 16, "wrong bucket size");
        }
        REPORT.put("fourFluidStates", true);
        REPORT.put("fourWaterPatternsAndOilAlpha", true);
        REPORT.put("alpha", Map.of("crudeOil", 242, "fuelOil", 180));
        REPORT.put("twoBucketModels", true);
        REPORT.put("worldLoaded", false);
    }

    private static TextureAtlasSprite sprite(Minecraft mc, String name) {
        return mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id(name));
    }

    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("piranport", "block/" + name); }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }

    private static void finish(Minecraft mc) {
        finished = true;
        try { Files.writeString(OUTPUT.resolve("client-validation.json"), new GsonBuilder().setPrettyPrinting().create().toJson(REPORT)); }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
        mc.stop();
    }

    private static final class Preview extends Screen {
        private Preview() { super(Component.literal("Oil appearance verification")); }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xff27313a);
            graphics.drawCenteredString(font, "Vanilla water animation / oil palette", width / 2, 10, 0xffffff);
            String[] oils = {"crude_oil", "fuel_oil", "water"};
            for (int row = 0; row < 2; row++) {
                String kind = row == 0 ? "still" : "flow";
                for (int col = 0; col < 3; col++) {
                    int x = (width - 300) / 2 + col * 100 + 18, y = 42 + row * 94;
                    var texture = col == 2 ? minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.withDefaultNamespace("block/water_" + kind)) : sprite(minecraft, oils[col] + "_" + kind);
                    graphics.drawCenteredString(font, oils[col].replace('_', ' ') + " " + kind, x + 32, y - 12, 0xeeeeee);
                    graphics.blit(x, y, 0, 64, 64, texture);
                }
            }
            var items = List.of(new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()), new ItemStack(ModItems.FUEL_OIL_BUCKET.get()), new ItemStack(Items.WATER_BUCKET));
            for (int col = 0; col < 3; col++) {
                graphics.pose().pushPose();
                graphics.pose().translate((width - 300) / 2 + col * 100 + 34, 235, 0);
                graphics.pose().scale(2, 2, 1);
                graphics.renderItem(items.get(col), 0, 0);
                graphics.pose().popPose();
            }
        }
    }
}
