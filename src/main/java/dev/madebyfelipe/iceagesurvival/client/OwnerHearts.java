package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Coração sobre as criaturas domesticadas (MC-14), para separá-las das selvagens. Só o dono vê, e só as que estão à
 * vista: o ícone respeita a profundidade (parede esconde), some com a criatura invisível e não aparece na que ele
 * está montando. Desenhado num passo próprio do mundo, e não no renderizador, para valer igual nos modelos GeckoLib e
 * Tabula, filhotes e adultos.
 */
final class OwnerHearts {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    /** Coração cheio vermelho no atlas de ícones do vanilla (9×9 em 256×256). */
    private static final float U0 = 52 / 256.0F;
    private static final float V0 = 0.0F;
    private static final float U1 = 61 / 256.0F;
    private static final float V1 = 9 / 256.0F;
    private static final float SCALE = 0.04F;
    private static final double RANGE = 48.0;
    /** Acima da cabeça; com nome visível, sobe para não cobrir a placa. */
    private static final double LIFT = 0.6;
    private static final double NAME_LIFT = 0.35;

    private OwnerHearts() {
    }

    static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || player == null
                || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        float partialTick = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType type = RenderType.text(ICONS);
        VertexConsumer consumer = null;
        for (PrehistoricCreature creature : minecraft.level.getEntitiesOfClass(PrehistoricCreature.class,
                player.getBoundingBox().inflate(RANGE),
                creature -> creature.isTame() && creature.isOwnedBy(player) && creature.isAlive()
                        && !creature.isInvisibleTo(player) && !creature.hasPassenger(player))) {
            if (!event.getFrustum().isVisible(creature.getBoundingBoxForCulling())) {
                continue;
            }
            Vec3 at = creature.getPosition(partialTick);
            double lift = creature.getBbHeight() + LIFT + (creature.shouldShowName() ? NAME_LIFT : 0.0);
            if (consumer == null) {
                consumer = buffers.getBuffer(type);
            }
            pose.pushPose();
            pose.translate(at.x - eye.x, at.y + lift - eye.y, at.z - eye.z);
            pose.mulPose(camera.rotation());
            pose.scale(-SCALE, -SCALE, SCALE);
            heart(consumer, pose.last().pose());
            pose.popPose();
        }
        if (consumer != null) {
            buffers.endBatch(type);
        }
    }

    private static void heart(VertexConsumer consumer, Matrix4f matrix) {
        int light = LightTexture.FULL_BRIGHT;
        consumer.vertex(matrix, -4.5F, -4.5F, 0.0F).color(255, 255, 255, 255).uv(U0, V0).uv2(light).endVertex();
        consumer.vertex(matrix, -4.5F, 4.5F, 0.0F).color(255, 255, 255, 255).uv(U0, V1).uv2(light).endVertex();
        consumer.vertex(matrix, 4.5F, 4.5F, 0.0F).color(255, 255, 255, 255).uv(U1, V1).uv2(light).endVertex();
        consumer.vertex(matrix, 4.5F, -4.5F, 0.0F).color(255, 255, 255, 255).uv(U1, V0).uv2(light).endVertex();
    }
}
