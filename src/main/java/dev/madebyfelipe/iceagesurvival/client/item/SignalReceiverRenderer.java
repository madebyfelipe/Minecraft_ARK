package dev.madebyfelipe.iceagesurvival.client.item;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.SignalReceiverItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * O Receptor de Sinal em 3D (mão, chão, moldura), com a tela, o ponto do sinal e o LED acesos no escuro (camada
 * {@code _glowmask}). Na GUI o modelo do item troca para o ícone plano. Arte de {@code tools/gen_signal_receiver.py}.
 */
public class SignalReceiverRenderer extends GeoItemRenderer<SignalReceiverItem> {
    /** Ligado ao item por {@link SignalReceiverItem#initializeClient}; o renderer nasce no primeiro desenho. */
    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        private SignalReceiverRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                renderer = new SignalReceiverRenderer();
            }
            return renderer;
        }
    };

    public SignalReceiverRenderer() {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** Caminhos fixos: a textura do modelo não é a do ícone ({@code textures/item/signal_receiver.png}). */
    private static final class Model extends GeoModel<SignalReceiverItem> {
        private static final ResourceLocation GEO = rl("geo/item/signal_receiver.geo.json");
        private static final ResourceLocation TEXTURE = rl("textures/item/signal_receiver_3d.png");
        private static final ResourceLocation ANIMATIONS = rl("animations/item/signal_receiver.animation.json");

        private static ResourceLocation rl(String path) {
            return IceAgeSurvival.id(path);
        }

        @Override
        public ResourceLocation getModelResource(SignalReceiverItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(SignalReceiverItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(SignalReceiverItem animatable) {
            return ANIMATIONS;
        }
    }
}
