package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.madebyfelipe.iceagesurvival.entity.CreatureAppearance;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.data.EntityModelData;
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
            // Sem o "turnsHead" do GeckoLib: ele SUBSTITUI a rotação do osso pelo olhar e apaga a
            // inclinação de repouso que o modelo tem (a cabeça do T-Rex fica 32° para cima, a dos
            // raptores e do espinossauro, mais de 80°).
            super(typeId, false);
        }

        @Override
        public ResourceLocation getModelResource(LandCreature animatable) {
            return CreatureAppearance.forEntity(typeId(animatable))
                    .map(CreatureAppearance::modelResource)
                    .orElseGet(() -> super.getModelResource(animatable));
        }

        @Override
        public ResourceLocation getTextureResource(LandCreature animatable) {
            return CreatureAppearance.forEntity(typeId(animatable))
                    .map(CreatureAppearance::textureResource)
                    .orElseGet(() -> super.getTextureResource(animatable));
        }

        @Override
        public ResourceLocation getAnimationResource(LandCreature animatable) {
            return CreatureAppearance.forEntity(typeId(animatable))
                    .map(CreatureAppearance::animationResource)
                    .orElseGet(() -> super.getAnimationResource(animatable));
        }

        private ResourceLocation typeId(LandCreature animatable) {
            return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType());
        }

        @Override
        public void setCustomAnimations(LandCreature animatable, long instanceId, AnimationState<LandCreature> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            // A cabeça acompanha o olhar só enquanto a criatura está consciente.
            CoreGeoBone head = getAnimationProcessor().getBone("head");
            if (head == null || animatable.isUnconscious()) {
                return;
            }
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            BoneSnapshot rest = head.getInitialSnapshot();
            head.setRotX(rest.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(rest.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
