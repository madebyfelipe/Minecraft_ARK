package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Usa a textura da flecha com ponta do vanilla; a flecha tranquilizante não tem assets próprios. */
public class TranqArrowRenderer extends ArrowRenderer<TranqArrow> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/tipped_arrow.png");

    public TranqArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TranqArrow entity) {
        return TEXTURE;
    }
}
