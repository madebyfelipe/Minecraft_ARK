package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderer de qualquer {@link LandCreature}. Os assets vêm do id do tipo de entidade e o
 * tamanho vem de {@link CreatureModelSettings}.
 */
public class CreatureRenderer extends GeoEntityRenderer<LandCreature> {
    private final ResourceLocation typeId;

    public CreatureRenderer(EntityRendererProvider.Context context, ResourceLocation typeId) {
        super(context, new Model(typeId));
        this.typeId = typeId;
    }

    @Override
    public void render(LandCreature entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float scale = CreatureModelSettings.INSTANCE.scale(typeId);
        scaleWidth = scale;
        scaleHeight = scale;
        shadowRadius = entity.getBbWidth() * 0.5F;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static class Model extends DefaultedEntityGeoModel<LandCreature> {
        Model(ResourceLocation typeId) {
            super(typeId, true);
        }

        @Override
        public void setCustomAnimations(LandCreature animatable, long instanceId, AnimationState<LandCreature> state) {
            // A cabeça acompanha o olhar só enquanto a criatura está consciente.
            if (!animatable.isUnconscious()) {
                super.setCustomAnimations(animatable, instanceId, state);
            }
        }
    }
}
