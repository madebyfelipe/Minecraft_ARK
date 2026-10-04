package dev.madebyfelipe.iceagesurvival.client.tabula;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.client.CreatureModelSettings;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer das criaturas com modelo Tabula do Jurassic Reborn ({@link JurassicRebornAppearance}). Por ser um
 * {@link MobRenderer}, já faz o que o {@code GeoEntityRenderer} dá às outras: rumo do corpo, vermelho de dano,
 * luz, sombra, nome, guia (leash), transparência para espectador e o contorno do efeito Brilho (o "Localizar").
 * Aqui entram a escala da espécie ({@link CreatureModelSettings}), o tamanho de filhote, a textura por sexo e as
 * pálpebras. Sem o Jurassic Reborn instalado, não desenha nada (o aviso sai uma vez, no carregamento).
 */
public class TabulaCreatureRenderer extends MobRenderer<LandCreature, TabulaCreatureModel> {
    private final ResourceLocation typeId;
    private final JurassicRebornAppearance appearance;
    private boolean warned;

    public TabulaCreatureRenderer(EntityRendererProvider.Context context, ResourceLocation typeId) {
        super(context, new TabulaCreatureModel(), 0.5F);
        this.typeId = typeId;
        this.appearance = JurassicRebornAppearance.forEntity(typeId)
                .orElseThrow(() -> new IllegalArgumentException(typeId + " não tem aparência do Jurassic Reborn"));
        addLayer(new EyelidLayer(this));
    }

    @Override
    public void render(LandCreature entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        BakedTabulaModel baked = TabulaModels.get(typeId).orElse(null);
        if (baked == null) {
            if (!warned) {
                warned = true;
                IceAgeSurvival.LOGGER.warn("Sem modelo do Jurassic Reborn para {}: a criatura fica invisível.", typeId);
            }
            return;
        }
        shadowRadius = entity.getBbWidth() * 0.5F;
        // O relógio da animação usa a fração do quadro do jogo, não a recebida: a prévia do painel de status
        // desenha com 1,0 e, assim, não adianta a animação da mesma criatura no mundo.
        float now = entity.tickCount + Minecraft.getInstance().getPartialTick();
        model.bind(baked, now, renderScale(entity));
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    /** Escala do modelo: a da espécie vezes a de filhote (de 40% a 100%, a mesma da caixa de colisão). */
    private float renderScale(LandCreature entity) {
        return CreatureModelSettings.INSTANCE.scale(typeId) * entity.getAgeScale();
    }

    @Override
    protected void scale(LandCreature entity, PoseStack poseStack, float partialTick) {
        float scale = renderScale(entity);
        poseStack.scale(scale, scale, scale);
    }

    /** Com a animação de morte do modelo, ela tomba sozinha: sem o giro de lado do vanilla. */
    @Override
    protected float getFlipDegrees(LandCreature entity) {
        return TabulaModels.get(typeId).map(baked -> baked.has(CreaturePoses.DYING)).orElse(false) ? 0.0F : 90.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(LandCreature entity) {
        return appearance.textureResource(entity.isFemale());
    }

    /** As pálpebras fechadas, por cima da pele, com a mesma geometria já posada. */
    private final class EyelidLayer extends RenderLayer<LandCreature, TabulaCreatureModel> {
        EyelidLayer(RenderLayerParent<LandCreature, TabulaCreatureModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, LandCreature entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            boolean available = TabulaModels.get(typeId).map(BakedTabulaModel::hasEyelids).orElse(false);
            if (!available || entity.isInvisible() || !getParentModel().eyelidsClosed()) {
                return;
            }
            VertexConsumer buffer = buffers.getBuffer(
                    RenderType.entityCutoutNoCull(appearance.eyelidResource(entity.isFemale())));
            getParentModel().renderToBuffer(poseStack, buffer, packedLight,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
