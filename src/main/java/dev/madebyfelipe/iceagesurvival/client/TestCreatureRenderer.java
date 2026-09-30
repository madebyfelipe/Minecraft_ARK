package dev.madebyfelipe.iceagesurvival.client;

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
    public ResourceLocation getTextureLocation(TestCreature entity) {
        return TEXTURE;
    }
}
