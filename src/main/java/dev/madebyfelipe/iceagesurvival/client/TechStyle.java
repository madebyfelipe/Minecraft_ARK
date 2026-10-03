package dev.madebyfelipe.iceagesurvival.client;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * O visual de todas as telas e do HUD do mod, inspirado no equipamento de campo de Dino Crisis 2 (só a ideia; nenhum
 * asset da Capcom): painel azul-petróleo com grade fina, moldura de metal chanfrada com bisel, títulos brancos em
 * itálico e caixa alta atrás de um losango âmbar, dados em ciano, cursor âmbar piscando e vermelho de perigo. Espaços
 * de item escuros de borda ciano, barras segmentadas e botões de placa de metal ({@link TechButton}). Tudo desenhado
 * em código, sem textura de GUI.
 */
public final class TechStyle {
    /** Linhas e dados: ciano. */
    public static final int ACCENT = 0xFF38C6D9;
    public static final int BRIGHT = 0xFFA8F4FF;
    /** Cursor, seleção e alvo: âmbar. */
    public static final int AMBER = 0xFFFFB21E;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int SUBTLE = 0xFF9DB2BF;
    public static final int WARNING = 0xFFFFB347;
    public static final int DANGER = 0xFFFF3B3B;
    /** Fundo das telas: azul-petróleo quase opaco. */
    public static final int PANEL = 0xF20A1A22;
    /** Fundo dos painéis do HUD, mais transparente. */
    public static final int HUD_PANEL = 0xC00A1A22;
    public static final int SLOT = 0xFF061218;
    public static final int SLOT_EDGE = 0xFF1F5866;
    /** Trilho das barras. */
    public static final int TRACK = 0xFF061218;
    /** Realce de linha (passar o mouse). */
    public static final int HOVER = 0x14A8F4FF;
    /** Metal da moldura, do brilho à sombra. */
    public static final int METAL_LIGHT = 0xFFB4C6D1;
    public static final int METAL = 0xFF5C6F7B;
    public static final int METAL_DARK = 0xFF2A3740;
    public static final int METAL_SHADOW = 0xFF080D11;

    private static final int GRID = 0x1438C6D9;
    private static final int GRID_STEP = 8;
    /** Corte dos cantos da moldura das telas. */
    private static final int CHAMFER = 5;
    /** Uma divisão a cada tanto da barra, para parecer um medidor. */
    private static final int SEGMENTS = 10;

    private TechStyle() {
    }

    /** O painel de uma tela: moldura de metal chanfrada com bisel, fundo azul-petróleo e grade. */
    public static void frame(GuiGraphics graphics, int left, int top, int right, int bottom) {
        int cut = Math.min(CHAMFER, Math.min(right - left, bottom - top) / 4);
        chamfer(graphics, left - 3, top - 3, right + 3, bottom + 3, cut + 3, METAL_SHADOW);
        chamfer(graphics, left - 2, top - 2, right + 2, bottom + 2, cut + 2, METAL);
        // Bisel: luz em cima e à esquerda, sombra embaixo e à direita.
        graphics.fill(left - 2 + cut + 2, top - 2, right + 2 - cut - 2, top - 1, METAL_LIGHT);
        graphics.fill(left - 2, top - 2 + cut + 2, left - 1, bottom + 2 - cut - 2, METAL_LIGHT);
        HudShapes.line(graphics, left - 2, top - 2 + cut + 2, left - 2 + cut + 2, top - 2, 1.0F, METAL_LIGHT);
        graphics.fill(left - 2 + cut + 2, bottom + 1, right + 2 - cut - 2, bottom + 2, METAL_DARK);
        graphics.fill(right + 1, top - 2 + cut + 2, right + 2, bottom + 2 - cut - 2, METAL_DARK);
        HudShapes.line(graphics, right + 2 - cut - 2, bottom + 2, right + 2, bottom + 2 - cut - 2, 1.0F, METAL_DARK);
        chamfer(graphics, left, top, right, bottom, cut, PANEL);
        grid(graphics, left, top, right, bottom, cut);
        // Parafusos nos cantos de cima, como nas placas de equipamento.
        graphics.fill(left + cut + 2, top + 2, left + cut + 4, top + 4, METAL);
        graphics.fill(right - cut - 4, top + 2, right - cut - 2, top + 4, METAL);
    }

    /** Painel do HUD: fundo mais leve, borda de metal fina chanfrada e uma aba de cor à esquerda (de quem é). */
    public static void hudFrame(GuiGraphics graphics, int left, int top, int right, int bottom, int accent) {
        chamfer(graphics, left - 1, top - 1, right + 1, bottom + 1, 4, HudShapes.fade(METAL, 0.9F));
        chamfer(graphics, left, top, right, bottom, 3, HUD_PANEL);
        graphics.fill(left + 3, top, right - 3, top + 1, HudShapes.fade(METAL_LIGHT, 0.7F));
        graphics.fill(left, top + 3, left + 2, bottom - 3, accent);
    }

    /** Retângulo com os quatro cantos cortados em 45°. */
    public static void chamfer(GuiGraphics graphics, int left, int top, int right, int bottom, int cut, int color) {
        cut = Math.max(0, Math.min(cut, Math.min(right - left, bottom - top) / 2));
        if (cut == 0) {
            graphics.fill(left, top, right, bottom, color);
            return;
        }
        graphics.fill(left + cut, top, right - cut, bottom, color);
        graphics.fill(left, top + cut, left + cut, bottom - cut, color);
        graphics.fill(right - cut, top + cut, right, bottom - cut, color);
        HudShapes.triangle(graphics, left + cut, top, left + cut, top + cut, left, top + cut, color);
        HudShapes.triangle(graphics, right - cut, top, right, top + cut, right - cut, top + cut, color);
        HudShapes.triangle(graphics, left, bottom - cut, left + cut, bottom - cut, left + cut, bottom, color);
        HudShapes.triangle(graphics, right - cut, bottom - cut, right, bottom - cut, right - cut, bottom, color);
    }

    /** Grade fina dentro do painel chanfrado. */
    private static void grid(GuiGraphics graphics, int left, int top, int right, int bottom, int cut) {
        for (int y = top + GRID_STEP; y < bottom - 1; y += GRID_STEP) {
            int inset = Math.max(0, Math.max(top + cut - y, y - (bottom - cut)));
            graphics.fill(left + 1 + inset, y, right - 1 - inset, y + 1, GRID);
        }
        for (int x = left + GRID_STEP; x < right - 1; x += GRID_STEP) {
            int inset = Math.max(0, Math.max(left + cut - x, x - (right - cut)));
            graphics.fill(x, top + 1 + inset, x + 1, bottom - 1 - inset, GRID);
        }
    }

    public static void border(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    /** Cantoneiras de 1 px (molduras internas, como a da prévia do modelo). */
    public static void corners(GuiGraphics graphics, int left, int top, int right, int bottom, int size) {
        int color = BRIGHT;
        graphics.fill(left, top, left + size, top + 1, color);
        graphics.fill(left, top, left + 1, top + size, color);
        graphics.fill(right - size, top, right, top + 1, color);
        graphics.fill(right - 1, top, right, top + size, color);
        graphics.fill(left, bottom - 1, left + size, bottom, color);
        graphics.fill(left, bottom - size, left + 1, bottom, color);
        graphics.fill(right - size, bottom - 1, right, bottom, color);
        graphics.fill(right - 1, bottom - size, right, bottom, color);
    }

    /** Liga e desliga duas vezes por segundo. */
    public static boolean blink() {
        return Util.getMillis() / 500 % 2 == 0;
    }

    /** O título no jeito de DC2: caixa alta, negrito e itálico. */
    public static Component titleText(Component title) {
        return Component.literal(title.getString().toUpperCase(Locale.ROOT))
                .withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC);
    }

    /** O losango âmbar que abre os títulos. */
    public static void marker(GuiGraphics graphics, int x, int y) {
        HudShapes.diamond(graphics, x + 3.0F, y + 3.5F, 3.0F, AMBER);
        HudShapes.diamond(graphics, x + 3.0F, y + 3.5F, 1.2F, HudShapes.fade(0xFFFFFFFF, 0.8F));
    }

    /**
     * Cabeçalho das telas grandes: losango e título em (x, y), e a linha com brilho correndo {@code ruleGap} px
     * abaixo, de {@code x} a {@code right}.
     */
    public static void header(GuiGraphics graphics, Font font, Component title, int x, int y, int right, int ruleGap) {
        marker(graphics, x, y);
        graphics.drawString(font, titleText(title), x + 10, y, TEXT);
        rule(graphics, x, right, y + ruleGap);
    }

    /**
     * Título das telas de inventário, que não têm folga embaixo dele: losango, título e a linha seguindo na mesma
     * altura até {@code right}. Coordenadas como as do {@code renderLabels} (relativas ao painel).
     */
    public static void titleLine(GuiGraphics graphics, Font font, Component title, int x, int y, int right) {
        marker(graphics, x, y);
        Component text = titleText(title);
        graphics.drawString(font, text, x + 10, y, TEXT, true);
        int from = x + 10 + font.width(text) + 4;
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

    /** Espaço de item de 16 px em (x, y), com a borda de 1 px por fora e sombra interna em cima. */
    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + 16, y + 16, SLOT);
        graphics.fill(x, y, x + 16, y + 1, METAL_SHADOW);
    }

    /**
     * Barra de medidor; {@code fraction} de 0 a 1. A partir de 4 px de altura ganha divisões a cada décimo e um
     * brilho na borda de cima da parte cheia; a partir de 6 px, uma borda.
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
            border(graphics, x - 1, y - 1, x + width + 1, y + height + 1, HudShapes.fade(METAL, 0.9F));
        }
    }
}
