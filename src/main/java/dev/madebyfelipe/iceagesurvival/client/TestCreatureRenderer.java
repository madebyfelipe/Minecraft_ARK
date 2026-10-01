package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Reaproveita o modelo e a textura do porco vanilla; a criatura de teste não tem assets próprios. */
public class TestCreatureRenderer extends MobRenderer<TestCreature, PigModel<TestCreature>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

    public TestCreatureRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(ModelLayers.PIG)), 0.7F);
    }

    @Override
    protected void setupRotations(TestCreature entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick);
        if (entity.isUnconscious()) {
            // Tomba de lado, no chão.
            poseStack.translate(0.0F, 0.5F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TestCreature entity) {
        return TEXTURE;
    }
}
