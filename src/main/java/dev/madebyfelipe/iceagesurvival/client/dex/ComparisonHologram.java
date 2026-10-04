package dev.madebyfelipe.iceagesurvival.client.dex;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.client.HudShapes;
import dev.madebyfelipe.iceagesurvival.client.TechStyle;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * O holograma comparativo do dossiê ({@code tools/wiki_lore/dossie/holograma.md}): os modelos das espécies lado a lado
 * na <b>mesma escala</b> (o mais alto preenche o quadro e os outros ficam em proporção), girando juntos e devagar com o
 * mesmo ângulo, cada um numa cor só e translúcido (a pele desligada: a textura vira branco e a cor vem do vértice),
 * sobre a grade com marcas de altura de um em um bloco, sem número. O primeiro sai em ciano e o segundo em âmbar.
 */
final class ComparisonHologram {
    /** Altura do quadro, em pixels da tela. */
    static final int HEIGHT = 136;

    private static final long TURN_MILLIS = 16000L;
    private static final int RULER = 14;
    private static final int LABEL = 14;
    private static final int TOP = 8;
    /** Folga sobre a caixa: cabeça e cauda passam um pouco da altura da caixa. */
    private static final float HEIGHT_SLACK = 1.2F;
    /** Diâmetro que o corpo varre girando, em múltiplos do maior lado da caixa (o comprimento do corpo). */
    private static final float SWEEP = 2.2F;
    private static final float GAP = 0.6F;
    private static final int[] COLORS = {TechStyle.ACCENT, TechStyle.AMBER};
    private static final int BACK = 0xFF040D12;
    private static final int GRID = 0x1838C6D9;
    private static final int GRID_MAJOR = 0x3838C6D9;
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final RenderType HOLOGRAM = RenderType.entityTranslucentCull(WHITE);

    private ComparisonHologram() {
    }

    /** Um modelo no holograma: o exemplar, onde fica o centro e a cor. */
    private record Slot(@Nullable LivingEntity model, String label, float sweep, float height, int color) {
    }

    static void draw(GuiGraphics graphics, Font font, List<String> species, int x1, int y1, int x2, int y2) {
        List<Slot> slots = new ArrayList<>();
        for (int index = 0; index < species.size(); index++) {
            String id = species.get(index);
            LivingEntity model = AnalyzerScreen.model(id);
            String label = WikiManual.get().sheet(id).map(Manual.Sheet::name)
                    .orElse(id.substring(id.indexOf(':') + 1)).toUpperCase(Locale.ROOT);
            float height = model == null ? 1.0F : model.getBbHeight();
            float length = model == null ? 1.0F : Math.max(model.getBbWidth(), model.getBbHeight() * 0.5F) * SWEEP;
            slots.add(new Slot(model, label, length, height, COLORS[index % COLORS.length]));
        }
        float worldWidth = slots.stream().map(Slot::sweep).reduce(0.0F, Float::sum) + GAP * (slots.size() - 1);
        float worldHeight = slots.stream().map(Slot::height).reduce(0.0F, Math::max) * HEIGHT_SLACK;
        int areaLeft = x1 + RULER + 4;
        int areaRight = x2 - 6;
        int feetY = y2 - LABEL - 4;
        float scale = Math.max(1.0F, Math.min((feetY - y1 - TOP) / worldHeight, (areaRight - areaLeft) / worldWidth));

        graphics.fill(x1, y1, x2, y2, BACK);
        graphics.enableScissor(x1 + 1, y1 + 1, x2 - 1, y2 - 1);
        drawGrid(graphics, x1, y1, x2, feetY, scale);

        float angle = (Util.getMillis() % TURN_MILLIS) / (float) TURN_MILLIS * 360.0F;
        float flicker = 0.66F + 0.06F * Mth.sin(Util.getMillis() / 180.0F);
        float cursor = (areaLeft + areaRight) / 2.0F - worldWidth * scale / 2.0F;
        float[] centers = new float[slots.size()];
        for (int index = 0; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            centers[index] = cursor + slot.sweep() * scale / 2.0F;
            cursor += (slot.sweep() + GAP) * scale;
            drawPlatform(graphics, centers[index], feetY, slot.sweep() * scale / 2.0F, slot.color());
        }
        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).model() != null) {
                drawModel(graphics, slots.get(index), centers[index], feetY, scale, angle, flicker);
            }
        }
        drawScan(graphics, x1, y1, x2, y2);
        graphics.disableScissor();

        for (int index = 0; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            graphics.drawCenteredString(font, slot.label(), Math.round(centers[index]), y2 - LABEL + 2, slot.color());
        }
        TechStyle.border(graphics, x1, y1, x2, y2, TechStyle.SLOT_EDGE);
        TechStyle.corners(graphics, x1 + 1, y1 + 1, x2 - 1, y2 - 1, 6);
    }

    /** A grade: uma linha a cada bloco de altura a partir do chão, mais forte de cinco em cinco, e a régua à esquerda. */
    private static void drawGrid(GuiGraphics graphics, int x1, int y1, int x2, int feetY, float scale) {
        for (int gx = x1 + RULER + 8; gx < x2; gx += 8) {
            graphics.fill(gx, y1 + 1, gx + 1, feetY, GRID);
        }
        int rulerX = x1 + RULER;
        graphics.fill(rulerX, y1 + 4, rulerX + 1, feetY + 1, HudShapes.fade(TechStyle.ACCENT, 0.7F));
        for (int step = 0; feetY - step * scale > y1 + 2; step++) {
            int y = Math.round(feetY - step * scale);
            boolean major = step % 5 == 0;
            graphics.fill(rulerX + 1, y, x2 - 1, y + 1, major ? GRID_MAJOR : GRID);
            graphics.fill(rulerX - (major ? 7 : 4), y, rulerX, y + 1,
                    major ? TechStyle.ACCENT : HudShapes.fade(TechStyle.ACCENT, 0.6F));
        }
        graphics.fill(rulerX + 1, feetY, x2 - 1, feetY + 1, HudShapes.fade(TechStyle.ACCENT, 0.8F));
    }

    /** A plataforma da DINO FILE, do tamanho do giro de cada um, na cor dele. */
    private static void drawPlatform(GuiGraphics graphics, float centerX, int feetY, float radius, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, feetY, 0.0F);
        graphics.pose().scale(1.0F, 0.3F, 1.0F);
        HudShapes.disc(graphics, 0.0F, 0.0F, radius, HudShapes.fade(color, 0.12F));
        HudShapes.ring(graphics, 0.0F, 0.0F, radius - 1.0F, radius, color);
        HudShapes.ring(graphics, 0.0F, 0.0F, radius / 2.0F - 1.0F, radius / 2.0F, HudShapes.fade(color, 0.6F));
        graphics.pose().popPose();
    }

    /**
     * O modelo da entidade como em {@code InventoryScreen.renderEntityInInventory}, mas por um buffer que troca
     * qualquer tipo de render pelo holograma: textura branca, cor do vértice fixa e translúcida.
     */
    private static void drawModel(GuiGraphics graphics, Slot slot, float centerX, int feetY, float scale, float angle,
                                  float alpha) {
        LivingEntity model = slot.model();
        model.yBodyRot = angle;
        model.yBodyRotO = angle;
        model.setYRot(angle);
        model.yRotO = angle;
        model.yHeadRot = angle;
        model.yHeadRotO = angle;
        model.setXRot(0.0F);
        model.xRotO = 0.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, feetY, 50.0F);
        pose.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
        pose.mulPose(new Quaternionf().rotateZ((float) Math.PI).rotateX((float) Math.toRadians(-12.0)));
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        int color = slot.color();
        MultiBufferSource tinted = type -> new Tint(buffers.getBuffer(HOLOGRAM),
                (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
        try {
            RenderSystem.runAsFancy(() -> dispatcher.render(model, 0.0, 0.0, 0.0, 0.0F, 1.0F, pose, tinted,
                    LightTexture.FULL_BRIGHT));
            graphics.flush();
        } finally {
            dispatcher.setRenderShadow(true);
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }

    /** Linhas de varredura e a faixa clara descendo, por cima dos modelos. */
    private static void drawScan(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        for (int y = y1 + 2; y < y2 - 1; y += 3) {
            graphics.fill(x1 + 1, y, x2 - 1, y + 1, 0x14040D12);
        }
        int band = y1 + (int) ((Util.getMillis() / 25L) % Math.max(1, y2 - y1));
        graphics.fill(x1 + 1, band, x2 - 1, Math.min(y2 - 1, band + 2), 0x1CA8F4FF);
        graphics.pose().popPose();
    }

    /** Repassa os vértices trocando a cor pela do holograma (a textura é branca, então a pele some). */
    private record Tint(VertexConsumer delegate, int red, int green, int blue, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(red, green, blue, Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay,
                           int light, float normalX, float normalY, float normalZ) {
            delegate.vertex(x, y, z, red / 255.0F, green / 255.0F, blue / 255.0F, a * alpha, u, v, overlay, light,
                    normalX, normalY, normalZ);
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(red, green, blue, Math.round(a * alpha));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }
}
