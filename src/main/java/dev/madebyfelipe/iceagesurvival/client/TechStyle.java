package dev.madebyfelipe.iceagesurvival.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * O visual de aparelho de todas as telas e do HUD do mod, o mesmo do radar dos localizadores: painel quase preto
 * com linhas de varredura, borda verde fina e cantoneiras claras, título com ponto piscando e um brilho que corre
 * pela linha, espaços de item escuros de borda verde, barras segmentadas e botões chapados ({@link TechButton}).
 * Tudo desenhado em código, sem textura de GUI.
 */
public final class TechStyle {
    public static final int ACCENT = 0xFF5FB36B;
    public static final int BRIGHT = 0xFF8CF0A8;
    public static final int AMBER = 0xFFFFC857;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int SUBTLE = 0xFFA0A8B0;
    public static final int WARNING = 0xFFFFB347;
    /** Fundo das telas (opaco o bastante para ler por cima do mundo). */
    public static final int PANEL = 0xEA060C0A;
    /** Fundo dos painéis do HUD, mais transparente. */
    public static final int HUD_PANEL = 0xD0071510;
    public static final int SLOT = 0xFF0B1511;
    public static final int SLOT_EDGE = 0xFF284434;
    /** Trilho das barras. */
    public static final int TRACK = 0xFF0B1511;
    /** Realce de linha (passar o mouse, linha escolhida). */
    public static final int HOVER = 0x10FFFFFF;

    private static final int SCANLINE = 0x068CF0A8;
    private static final int CORNER = 8;
    /** Uma divisão a cada tanto da barra, para parecer um medidor. */
    private static final int SEGMENTS = 10;

    private TechStyle() {
    }

    /** O painel de uma tela: fundo, linhas de varredura, borda fina e cantoneiras. */
    public static void frame(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, PANEL);
        for (int y = top + 2; y < bottom - 1; y += 3) {
            graphics.fill(left + 1, y, right - 1, y + 1, SCANLINE);
        }
        border(graphics, left, top, right, bottom, HudShapes.fade(ACCENT, 0.45F));
        corners(graphics, left, top, right, bottom, Math.min(CORNER, Math.min(right - left, bottom - top) / 3));
    }

    /** Painel do HUD: fundo mais leve, linha no topo e barra de cor à esquerda (a cor diz de quem é). */
    public static void hudFrame(GuiGraphics graphics, int left, int top, int right, int bottom, int accent) {
        graphics.fill(left, top, right, bottom, HUD_PANEL);
        graphics.fill(left, top, right, top + 1, HudShapes.fade(accent, 0.6F));
        graphics.fill(left, top, left + 2, bottom, accent);
    }

    public static void border(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    /** Cantoneiras claras de 2 px, um pouco para fora da borda. */
    public static void corners(GuiGraphics graphics, int left, int top, int right, int bottom, int size) {
        int color = BRIGHT;
        graphics.fill(left - 1, top - 1, left + size, top + 1, color);
        graphics.fill(left - 1, top - 1, left + 1, top + size, color);
        graphics.fill(right - size, top - 1, right + 1, top + 1, color);
        graphics.fill(right - 1, top - 1, right + 1, top + size, color);
        graphics.fill(left - 1, bottom - 1, left + size, bottom + 1, color);
        graphics.fill(left - 1, bottom - size, left + 1, bottom + 1, color);
        graphics.fill(right - size, bottom - 1, right + 1, bottom + 1, color);
        graphics.fill(right - 1, bottom - size, right + 1, bottom + 1, color);
    }

    /** Liga e desliga duas vezes por segundo. */
    public static boolean blink() {
        return Util.getMillis() / 500 % 2 == 0;
    }

    /**
     * Cabeçalho das telas grandes: ponto piscando e título em (x, y), e a linha com brilho correndo
     * {@code ruleGap} px abaixo, de {@code x} a {@code right}.
     */
    public static void header(GuiGraphics graphics, Font font, Component title, int x, int y, int right, int ruleGap) {
        graphics.drawString(font, blink() ? "●" : "○", x, y, BRIGHT);
        graphics.drawString(font, title, x + 10, y, TEXT);
        rule(graphics, x, right, y + ruleGap);
    }

    /**
     * Título das telas de inventário, que não têm folga embaixo dele: ponto, título e a linha seguindo na mesma
     * altura até {@code right}. Coordenadas como as do {@code renderLabels} (relativas ao painel, sem sombra).
     */
    public static void titleLine(GuiGraphics graphics, Font font, Component title, int x, int y, int right) {
        graphics.drawString(font, blink() ? "●" : "○", x, y, BRIGHT, false);
        graphics.drawString(font, title, x + 10, y, TEXT, false);
        int from = x + 10 + font.width(title) + 4;
        if (right - from > 8) {
            rule(graphics, from, right, y + font.lineHeight / 2 - 1);
        }
    }

    /** Linha de 1 px com um brilho que corre da esquerda para a direita. */
    public static void rule(GuiGraphics graphics, int left, int right, int y) {
        graphics.fill(left, y, right, y + 1, HudShapes.fade(ACCENT, 0.5F));
        int span = right - left;
        if (span <= 0) {
            return;
        }
        int glowWidth = Math.min(28, span / 3);
        int glowX = left + (int) (Util.getMillis() / 6 % (span + glowWidth)) - glowWidth;
        graphics.fill(Math.max(left, glowX), y, Math.min(right, glowX + glowWidth), y + 1, BRIGHT);
    }

    /** Espaço de item de 16 px em (x, y), com a borda de 1 px por fora. */
    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + 16, y + 16, SLOT);
    }

    /**
     * Barra de medidor; {@code fraction} de 0 a 1. A partir de 4 px de altura ganha divisões a cada décimo e um
     * brilho na borda de cima da parte cheia.
     */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        graphics.fill(x, y, x + width, y + height, TRACK);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, color);
            if (height >= 4) {
                graphics.fill(x, y, x + filled, y + 1, HudShapes.lerpColor(color, 0xFFFFFFFF, 0.35F));
            }
        }
        if (height >= 4) {
            for (int segment = 1; segment < SEGMENTS; segment++) {
                int at = x + width * segment / SEGMENTS;
                graphics.fill(at, y, at + 1, y + height, 0x80000000);
            }
        }
        if (height >= 6) {
            border(graphics, x - 1, y - 1, x + width + 1, y + height + 1, HudShapes.fade(color, 0.45F));
        }
    }
}
