package com.lex0v0.piranport.client.renderer;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.blockentity.FuelBarrelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

// 仅绘制视窗背后的动态油体，外壳与双桶接缝交由烘焙模型处理。
public final class FuelBarrelRenderer implements BlockEntityRenderer<FuelBarrelBlockEntity> {
    private static final float INSET = .002F;

    public static float liquidHeight(Direction.Axis axis, FuelBarrelBlock.Part part, double fill) {
        if (fill <= 0) return 0;
        boolean verticalPair = axis == Direction.Axis.Y && part != FuelBarrelBlock.Part.SINGLE;
        double height = 6.0 / 32 + (verticalPair ? 52.0 / 32 : 20.0 / 32) * Math.min(1, fill);
        if (verticalPair && part == FuelBarrelBlock.Part.UPPER) height--;
        return Mth.clamp((float) height, 0, 1 - INSET);
    }

    @Override
    public void render(FuelBarrelBlockEntity barrel, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        var fluid = barrel.getDisplayFluid();
        if (fluid.isEmpty()) return;
        float height = liquidHeight(barrel.getBlockState().getValue(FuelBarrelBlock.AXIS),
                barrel.getBlockState().getValue(FuelBarrelBlock.PART), barrel.getDisplayFill());
        if (height <= INSET) return;
        var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = extensions.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        int color = extensions.getTintColor(fluid);
        var consumer = buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        float a = INSET, b = 1 - INSET;
        // 真实动画图集和流体色调同时用于自有油品及其他模组流体。
        quad(consumer, poses.last(), sprite, color, light, Direction.NORTH, new float[][]{{b,height,a},{b,a,a},{a,a,a},{a,height,a}});
        quad(consumer, poses.last(), sprite, color, light, Direction.SOUTH, new float[][]{{a,height,b},{a,a,b},{b,a,b},{b,height,b}});
        quad(consumer, poses.last(), sprite, color, light, Direction.WEST, new float[][]{{a,height,a},{a,a,a},{a,a,b},{a,height,b}});
        quad(consumer, poses.last(), sprite, color, light, Direction.EAST, new float[][]{{b,height,b},{b,a,b},{b,a,a},{b,height,a}});
        quad(consumer, poses.last(), sprite, color, light, Direction.UP, new float[][]{{a,height,b},{b,height,b},{b,height,a},{a,height,a}});
        quad(consumer, poses.last(), sprite, color, light, Direction.DOWN, new float[][]{{a,a,a},{b,a,a},{b,a,b},{a,a,b}});
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             int color, int light, Direction normal, float[][] vertices) {
        for (int i = 0; i < 4; i++) {
            float[] v = vertices[i];
            // 每格固定一个纹理周期，按顶点位置裁剪，液位变化不缩放已显示的纹理。
            float localU = switch (normal) {
                case NORTH -> 1 - v[0];
                case EAST -> 1 - v[2];
                case WEST -> v[2];
                default -> v[0];
            };
            float localV = switch (normal) {
                case UP -> v[2];
                case DOWN -> 1 - v[2];
                default -> 1 - v[1];
            };
            float u = sprite.getU(localU);
            float w = sprite.getV(localV);
            out.addVertex(pose, v[0], v[1], v[2]).setColor(color).setUv(u, w)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
    }
}
