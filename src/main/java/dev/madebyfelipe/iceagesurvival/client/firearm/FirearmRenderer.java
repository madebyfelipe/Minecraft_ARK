package dev.madebyfelipe.iceagesurvival.client.firearm;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Uma arma de fogo em 3D (mão, chão, moldura), com o clarão do cano e as partes que brilham acesas no escuro (camada
 * {@code _glowmask}), também na GUI (inclinado, de lado). Arte de {@code tools/gen_dc2_weapons.py}.
 */
public class FirearmRenderer extends GeoItemRenderer<FirearmItem> {
    public FirearmRenderer(Firearm gun) {
        super(new Model(gun));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        // Na GUI, a luz das entidades no inventário: o modelo 3D com sombra, não chapado.
        useAlternateGuiLighting();
    }

    /** Caminhos fixos por arma. */
    private static final class Model extends GeoModel<FirearmItem> {
        private final ResourceLocation geo;
        private final ResourceLocation texture;
        private final ResourceLocation animations;

        Model(Firearm gun) {
            geo = IceAgeSurvival.id("geo/item/" + gun.id() + ".geo.json");
            texture = IceAgeSurvival.id("textures/item/" + gun.id() + "_3d.png");
            animations = IceAgeSurvival.id("animations/item/" + gun.id() + ".animation.json");
        }

        @Override
        public ResourceLocation getModelResource(FirearmItem animatable) {
            return geo;
        }

        @Override
        public ResourceLocation getTextureResource(FirearmItem animatable) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(FirearmItem animatable) {
            return animations;
        }
    }
}
