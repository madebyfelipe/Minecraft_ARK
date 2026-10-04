package dev.madebyfelipe.iceagesurvival.client.firearm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.madebyfelipe.iceagesurvival.client.TracerRenderType;
import dev.madebyfelipe.iceagesurvival.firearm.SolidCannonShot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * A esfera do canhão sólido (D58): camadas de luz ciano voltadas para a câmera (mistura aditiva, sem textura), um miolo
 * branco que pulsa e dois anéis que giram — a vibração que destrói as células.
 */
public class SolidCannonShotRenderer extends EntityRenderer<SolidCannonShot> {
    private static final int SEGMENTS = 16;

    public SolidCannonShotRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SolidCannonShot shot, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        float time = shot.tickCount + partialTick;
        float pulse = 0.85F + 0.15F * Mth.sin(time * 1.7F);
        VertexConsumer consumer = buffers.getBuffer(TracerRenderType.TRACER);
        pose.pushPose();
        pose.translate(0.0F, shot.getBbHeight() / 2.0F, 0.0F);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Matrix4f matrix = pose.last().pose();
        disk(consumer, matrix, 0.62F * pulse, 0.15F, 0.75F, 0.8F, 0.18F);
        disk(consumer, matrix, 0.42F * pulse, 0.3F, 0.95F, 0.95F, 0.35F);
        disk(consumer, matrix, 0.24F, 0.75F, 1.0F, 1.0F, 0.7F);
        disk(consumer, matrix, 0.12F * pulse, 1.0F, 1.0F, 1.0F, 0.9F);
        for (int ring = 0; ring < 2; ring++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(time * (ring == 0 ? 24.0F : -31.0F)));
            pose.scale(1.0F, ring == 0 ? 0.35F : 0.55F, 1.0F);
            ring(consumer, pose.last().pose(), 0.5F + ring * 0.08F, 0.035F, 0.5F, 1.0F, 0.95F, 0.6F);
            pose.popPose();
        }
        pose.popPose();
        super.render(shot, yaw, partialTick, pose, buffers, light);
    }

    /** Disco cheio, em gomos (cada gomo é um quadrado degenerado: o modo do tipo de render é QUADS). */
    private static void disk(VertexConsumer consumer, Matrix4f matrix, float radius, float red, float green,
                             float blue, float alpha) {
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            vertex(consumer, matrix, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, 0.0F, 0.0F, red, green, blue, alpha);
            vertex(consumer, matrix, Mth.cos(a1) * radius, Mth.sin(a1) * radius, red, green, blue, 0.0F);
            vertex(consumer, matrix, Mth.cos(a0) * radius, Mth.sin(a0) * radius, red, green, blue, 0.0F);
        }
    }

    private static void ring(VertexConsumer consumer, Matrix4f matrix, float radius, float width, float red,
                             float green, float blue, float alpha) {
        for (int i = 0; i < SEGMENTS * 2; i++) {
            float a0 = i * Mth.PI / SEGMENTS;
            float a1 = (i + 1) * Mth.PI / SEGMENTS;
            float inner = radius - width;
            vertex(consumer, matrix, Mth.cos(a0) * inner, Mth.sin(a0) * inner, red, green, blue, alpha);
            vertex(consumer, matrix, Mth.cos(a1) * inner, Mth.sin(a1) * inner, red, green, blue, alpha);
            vertex(consumer, matrix, Mth.cos(a1) * radius, Mth.sin(a1) * radius, red, green, blue, alpha);
            vertex(consumer, matrix, Mth.cos(a0) * radius, Mth.sin(a0) * radius, red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float red, float green,
                               float blue, float alpha) {
        consumer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SolidCannonShot shot) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
