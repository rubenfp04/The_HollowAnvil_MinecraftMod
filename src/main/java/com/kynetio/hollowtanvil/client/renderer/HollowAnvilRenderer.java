package com.kynetio.hollowtanvil.client.renderer;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.block.HollowAnvilBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class HollowAnvilRenderer implements BlockEntityRenderer<HollowAnvilBlockEntity, HollowAnvilRenderState> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "textures/block/hollow_anvil2.png");

    private static final float CUBE_SIZE = 0.125f;
    private static final float ORBIT_RADIUS = 0.3f;
    private static final float ORBIT_HEIGHT = 1.25f;
    private static final float ORBIT_SPEED = 2.0f;
    private static final float BOB_AMPLITUDE = 0.06f;
    private static final float BOB_SPEED = 0.08f;

    private static final float[] NORTH_UV = {8.75f/16f, 0.25f/16f, 9.25f/16f, 0.75f/16f};
    private static final float[] EAST_UV  = {9.75f/16f, 0.25f/16f, 10.25f/16f, 0.75f/16f};
    private static final float[] SOUTH_UV = {9.75f/16f, 0.75f/16f, 10.25f/16f, 1.25f/16f};
    private static final float[] WEST_UV  = {4.5f/16f, 9.75f/16f, 5f/16f, 10.25f/16f};
    private static final float[] UP_UV    = {5f/16f, 9.75f/16f, 5.5f/16f, 10.25f/16f};
    private static final float[] DOWN_UV  = {5.5f/16f, 9.75f/16f, 6f/16f, 10.25f/16f};
    private static final float[] FLAT_UV  = {0f, 0f, 1f/16f, 1f/16f};

    public HollowAnvilRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public HollowAnvilRenderState createRenderState() {
        return new HollowAnvilRenderState();
    }

    @Override
    public void extractRenderState(HollowAnvilBlockEntity be, HollowAnvilRenderState state,
                                    float partialTick, Vec3 cameraPos,
                                    @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.progressiveTier = be.getProgressiveTier();
        state.storedEssence = be.getStoredEssence();
        state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(HollowAnvilRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        int tier = state.progressiveTier;
        float time = state.gameTime + state.partialTick;

        int cr, cg, cb, ca;
        switch (tier) {
            case 2  -> { cr = 255; cg = 230; cb = 50;  ca = 255; }
            case 3  -> { cr = 255; cg = 140; cb = 30;  ca = 255; }
            case 4  -> { cr = 160; cg = 50;  cb = 255; ca = 230; }
            default -> { cr = 255; cg = 255; cb = 255; ca = 255; }
        }

        if (tier >= 2) {
            int overlayAlpha = switch (tier) { case 2 -> 40; case 3 -> 60; case 4 -> 80; default -> 0; };
            poseStack.pushPose();
            float inset = 0.001f;
            poseStack.translate(inset, inset, inset);
            float s = 1.0f - inset * 2;
            poseStack.scale(s, s, s);

            final int fCr = cr, fCg = cg, fCb = cb, fOa = overlayAlpha;
            collector.submitCustomGeometry(poseStack, RenderType.entityTranslucent(TEXTURE),
                    (pose, consumer) -> {
                        int ov = OverlayTexture.NO_OVERLAY;
                        float bh = 0.5f;
                        quad(consumer, pose, 15728880, ov, fCr, fCg, fCb, fOa,
                                -bh, -bh, bh, bh, -bh, bh, bh, bh, bh, -bh, bh, bh, 0, 0, 1, FLAT_UV);
                        quad(consumer, pose, 15728880, ov, fCr, fCg, fCb, fOa,
                                bh, -bh, -bh, -bh, -bh, -bh, -bh, bh, -bh, bh, bh, -bh, 0, 0, -1, FLAT_UV);
                        quad(consumer, pose, 15728880, ov, fCr, fCg, fCb, fOa,
                                bh, -bh, bh, bh, -bh, -bh, bh, bh, -bh, bh, bh, bh, 1, 0, 0, FLAT_UV);
                        quad(consumer, pose, 15728880, ov, fCr, fCg, fCb, fOa,
                                -bh, -bh, -bh, -bh, -bh, bh, -bh, bh, bh, -bh, bh, -bh, -1, 0, 0, FLAT_UV);
                        quad(consumer, pose, 15728880, ov, fCr, fCg, fCb, fOa,
                                -bh, bh, bh, bh, bh, bh, bh, bh, -bh, -bh, bh, -bh, 0, 1, 0, FLAT_UV);
                        quad(consumer, pose, 15728880, ov, fCr, fCg, fCb, fOa,
                                -bh, -bh, -bh, bh, -bh, -bh, bh, -bh, bh, -bh, -bh, bh, 0, -1, 0, FLAT_UV);
                    });

            poseStack.popPose();
        }

        float speedMult = switch (tier) { case 2 -> 1.3f; case 3 -> 1.7f; case 4 -> 2.2f; default -> 1.0f; };
        float orbitDeg = time * ORBIT_SPEED * speedMult;
        float bob = (float) Math.sin(time * BOB_SPEED) * BOB_AMPLITUDE;

        poseStack.pushPose();

        poseStack.translate(0.5, ORBIT_HEIGHT + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(orbitDeg));
        poseStack.translate(ORBIT_RADIUS, 0, 0);

        poseStack.mulPose(Axis.YP.rotationDegrees(-orbitDeg * 3));
        poseStack.mulPose(Axis.XP.rotationDegrees(orbitDeg * 1.7f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(orbitDeg * 1.3f));

        float scale = switch (tier) { case 2 -> CUBE_SIZE * 1.1f; case 3 -> CUBE_SIZE * 1.2f; case 4 -> CUBE_SIZE * 1.35f; default -> CUBE_SIZE; };
        poseStack.scale(scale, scale, scale);

        RenderType renderType = tier >= 4
                ? RenderType.entityTranslucent(TEXTURE)
                : RenderType.entitySolid(TEXTURE);

        final int fCr2 = cr, fCg2 = cg, fCb2 = cb, fCa2 = ca;
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, consumer) -> {
                    int overlay = OverlayTexture.NO_OVERLAY;
                    float h = 0.5f;

                    quad(consumer, pose, 15728880, overlay, fCr2, fCg2, fCb2, fCa2,
                            -h, -h,  h,  h, -h,  h,  h,  h,  h, -h,  h,  h,
                            0, 0, 1, SOUTH_UV);
                    quad(consumer, pose, 15728880, overlay, fCr2, fCg2, fCb2, fCa2,
                             h, -h, -h, -h, -h, -h, -h,  h, -h,  h,  h, -h,
                            0, 0, -1, NORTH_UV);
                    quad(consumer, pose, 15728880, overlay, fCr2, fCg2, fCb2, fCa2,
                             h, -h,  h,  h, -h, -h,  h,  h, -h,  h,  h,  h,
                            1, 0, 0, EAST_UV);
                    quad(consumer, pose, 15728880, overlay, fCr2, fCg2, fCb2, fCa2,
                            -h, -h, -h, -h, -h,  h, -h,  h,  h, -h,  h, -h,
                            -1, 0, 0, WEST_UV);
                    quad(consumer, pose, 15728880, overlay, fCr2, fCg2, fCb2, fCa2,
                            -h,  h,  h,  h,  h,  h,  h,  h, -h, -h,  h, -h,
                            0, 1, 0, UP_UV);
                    quad(consumer, pose, 15728880, overlay, fCr2, fCg2, fCb2, fCa2,
                            -h, -h, -h,  h, -h, -h,  h, -h,  h, -h, -h,  h,
                            0, -1, 0, DOWN_UV);
                });

        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int light, int overlay,
                             int cr, int cg, int cb, int ca,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float nx, float ny, float nz,
                             float[] uv) {
        float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];
        consumer.addVertex(pose, x0, y0, z0).setColor(cr, cg, cb, ca)
                .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x1, y1, z1).setColor(cr, cg, cb, ca)
                .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(cr, cg, cb, ca)
                .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3).setColor(cr, cg, cb, ca)
                .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
