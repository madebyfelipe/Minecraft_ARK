package dev.madebyfelipe.iceagesurvival.client.item;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.AnalyzerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * O Analisador em 3D (mão, chão, moldura), com a tela, o LED e a ponta da antena acesos no escuro (camada
 * {@code _glowmask}). Na GUI o modelo do item troca para o ícone plano ({@code models/item/analyzer.json}), então
 * este renderer nem é chamado lá. Arte de {@code tools/gen_analyzer.py}.
 */
public class AnalyzerRenderer extends GeoItemRenderer<AnalyzerItem> {
    /** Ligado ao item por {@link AnalyzerItem#initializeClient}; o renderer nasce no primeiro desenho. */
    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        private AnalyzerRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                renderer = new AnalyzerRenderer();
            }
            return renderer;
        }
    };

    public AnalyzerRenderer() {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /**
     * Se algum jogador do mundo está escaneando com este aparelho: o uso em andamento é este mesmo estoque, ou um com
     * o mesmo id do GeckoLib (o cliente pode ter trocado a instância quando o servidor reenviou o item).
     */
    public static boolean isScanning(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || stack.isEmpty()) {
            return false;
        }
        for (Player player : minecraft.level.players()) {
            if (!player.isUsingItem()) {
                continue;
            }
            ItemStack used = player.getUseItem();
            if (used == stack || (used.getItem() instanceof AnalyzerItem && hasId(used) && hasId(stack)
                    && GeoItem.getId(used) == GeoItem.getId(stack))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasId(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(GeoItem.ID_NBT_KEY);
    }

    /** Caminhos fixos: a textura do modelo não é a do ícone ({@code textures/item/analyzer.png}). */
    private static final class Model extends GeoModel<AnalyzerItem> {
        private static final ResourceLocation GEO = rl("geo/item/analyzer.geo.json");
        private static final ResourceLocation TEXTURE = rl("textures/item/analyzer_3d.png");
        private static final ResourceLocation ANIMATIONS = rl("animations/item/analyzer.animation.json");

        private static ResourceLocation rl(String path) {
            return new ResourceLocation(IceAgeSurvival.MODID, path);
        }

        @Override
        public ResourceLocation getModelResource(AnalyzerItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(AnalyzerItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(AnalyzerItem animatable) {
            return ANIMATIONS;
        }
    }
}
