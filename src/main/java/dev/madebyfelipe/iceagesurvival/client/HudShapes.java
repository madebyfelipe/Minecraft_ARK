package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Formas que o {@link GuiGraphics} não desenha — disco, anel, setor em degradê, triângulo e traço em
 * qualquer ângulo — para o radar do localizador. Tudo em quadriláteros de cor ({@code RenderType.gui()},
 * o mesmo do {@code fill}) e descarregado na hora, para respeitar a ordem de pintura com o texto.
 *
 * <p>Ângulos em graus, 0 no topo e crescendo no sentido horário, como em {@code core.locator.RadarMath}.
 */
final class HudShapes {
    /** Segmentos de uma volta inteira; o radar tem ~38 px de raio, então não se vê o polígono. */
    private static final int SEGMENTS = 64;

    private HudShapes() {
    }

    static float pointX(float centerX, float radius, float degrees) {
        return centerX + radius * Mth.sin(degrees * Mth.DEG_TO_RAD);
    }

    static float pointY(float centerY, float radius, float degrees) {
        return centerY - radius * Mth.cos(degrees * Mth.DEG_TO_RAD);
    }

    static void disc(GuiGraphics graphics, float x, float y, float radius, int color) {
        ring(graphics, x, y, 0.0F, radius, color);
    }

    static void ring(GuiGraphics graphics, float x, float y, float inner, float outer, int color) {
        arc(graphics, x, y, inner, outer, 0.0F, 360.0F, color, color);
    }

    /**
     * Pedaço de anel de {@code from} a {@code to} (sentido horário), com a cor indo de {@code fromColor}
     * a {@code toColor} ao longo do ângulo. Com {@code inner} 0 vira um setor de pizza.
     */
    static void arc(GuiGraphics graphics, float x, float y, float inner, float outer, float from, float to,
                    int fromColor, int toColor) {
        float sweep = to - from;
        int steps = Math.max(1, Mth.ceil(SEGMENTS * Math.abs(sweep) / 360.0F));
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (int step = 0; step < steps; step++) {
            float a0 = from + sweep * step / steps;
            float a1 = from + sweep * (step + 1) / steps;
            int c0 = lerpColor(fromColor, toColor, (float) step / steps);
            int c1 = lerpColor(fromColor, toColor, (float) (step + 1) / steps);
            quad(buffer, matrix,
                    pointX(x, inner, a0), pointY(y, inner, a0), c0,
                    pointX(x, outer, a0), pointY(y, outer, a0), c0,
                    pointX(x, outer, a1), pointY(y, outer, a1), c1,
                    pointX(x, inner, a1), pointY(y, inner, a1), c1);
        }
        graphics.flush();
    }

    static void triangle(GuiGraphics graphics, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        quad(buffer, matrix, x1, y1, color, x2, y2, color, x3, y3, color, x3, y3, color);
        graphics.flush();
    }

    /** Traço reto de largura {@code width}, de qualquer inclinação. */
    static void line(GuiGraphics graphics, float x1, float y1, float x2, float y2, float width, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float nx = -dy / length * width / 2.0F;
        float ny = dx / length * width / 2.0F;
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        quad(buffer, matrix, x1 + nx, y1 + ny, color, x2 + nx, y2 + ny, color, x2 - nx, y2 - ny, color,
                x1 - nx, y1 - ny, color);
        graphics.flush();
    }

    /** Traço radial do raio {@code inner} ao {@code outer}, no ângulo dado. */
    static void spoke(GuiGraphics graphics, float x, float y, float inner, float outer, float degrees, float width,
                      int color) {
        line(graphics, pointX(x, inner, degrees), pointY(y, inner, degrees),
                pointX(x, outer, degrees), pointY(y, outer, degrees), width, color);
    }

    /** Losango (marcador) centrado em (x, y). */
    static void diamond(GuiGraphics graphics, float x, float y, float half, int color) {
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        quad(buffer, matrix, x, y - half, color, x - half, y, color, x, y + half, color, x + half, y, color);
        graphics.flush();
    }

    /**
     * Seta de ponta em (x, y) apontando para {@code degrees}: o triângulo do marcador na borda do radar e
     * da agulha das bússolas pequenas.
     */
    static void arrowHead(GuiGraphics graphics, float x, float y, float degrees, float length, float halfWidth,
                          int color) {
        float backX = pointX(x, -length, degrees);
        float backY = pointY(y, -length, degrees);
        triangle(graphics, x, y,
                pointX(backX, halfWidth, degrees + 90.0F), pointY(backY, halfWidth, degrees + 90.0F),
                pointX(backX, halfWidth, degrees - 90.0F), pointY(backY, halfWidth, degrees - 90.0F), color);
    }

    /** Cor com a opacidade multiplicada por {@code alpha} (0 a 1). */
    static int fade(int color, float alpha) {
        int a = Mth.clamp(Math.round((color >>> 24) * alpha), 0, 255);
        return (a << 24) | (color & 0xFFFFFF);
    }

    static int lerpColor(int from, int to, float t) {
        int a = Math.round(Mth.lerp(t, from >>> 24, to >>> 24));
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return a << 24 | r << 16 | g << 8 | b;
    }

    /**
     * Um quadrilátero. O {@code RenderType.gui()} descarta a face de trás, então a ordem dos vértices é
     * acertada aqui para a de {@code GuiGraphics.fill} (anti-horária na tela).
     */
    private static void quad(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, int c1, float x2, float y2,
                             int c2, float x3, float y3, int c3, float x4, float y4, int c4) {
        float area = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1)
                + (x3 - x1) * (y4 - y1) - (y3 - y1) * (x4 - x1);
        if (area > 0.0F) {
            vertex(buffer, matrix, x1, y1, c1);
            vertex(buffer, matrix, x4, y4, c4);
            vertex(buffer, matrix, x3, y3, c3);
            vertex(buffer, matrix, x2, y2, c2);
        } else {
            vertex(buffer, matrix, x1, y1, c1);
            vertex(buffer, matrix, x2, y2, c2);
            vertex(buffer, matrix, x3, y3, c3);
            vertex(buffer, matrix, x4, y4, c4);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, int color) {
        buffer.vertex(matrix, x, y, 0.0F)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24)
                .endVertex();
    }
}
