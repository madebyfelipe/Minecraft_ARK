package dev.madebyfelipe.iceagesurvival.client.item;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * As recompensas do Titanovenator em 3D (mão, chão, moldura): o soro, com o sangue aceso, e o projetor de êxtase, com
 * a lente e o anel do campo (camada {@code _glowmask}). Na GUI o modelo do item troca para o ícone plano. Arte de
 * {@code tools/gen_titan_rewards.py}.
 */
public class TitanRewardRenderer<T extends Item & GeoItem> extends GeoItemRenderer<T> {
    public TitanRewardRenderer(String name) {
        super(new Model<>(name));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** Ligado ao item pelo {@code initializeClient} dele; o renderer nasce no primeiro desenho. */
    public static <T extends Item & GeoItem> IClientItemExtensions extensions(String name) {
        return new IClientItemExtensions() {
            private TitanRewardRenderer<T> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new TitanRewardRenderer<>(name);
                }
                return renderer;
            }
        };
    }

    /** Caminhos fixos: a textura do modelo não é a do ícone ({@code textures/item/<nome>.png}). */
    private static final class Model<T extends Item & GeoItem> extends GeoModel<T> {
        private final ResourceLocation geo;
        private final ResourceLocation texture;
        private final ResourceLocation animations;

        Model(String name) {
            geo = IceAgeSurvival.id("geo/item/" + name + ".geo.json");
            texture = IceAgeSurvival.id("textures/item/" + name + "_3d.png");
            animations = IceAgeSurvival.id("animations/item/" + name + ".animation.json");
        }

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return geo;
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return animations;
        }
    }
}
