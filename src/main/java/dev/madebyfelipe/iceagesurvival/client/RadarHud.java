package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.core.locator.RadarMath;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O radar dos localizadores — o das criaturas ({@link CreatureTracker}) e o rastreador da caverna
 * ({@link CaveTrackerHud}): no canto superior direito, de proa para cima, anel de bússola que gira com o olhar,
 * varredura, o marcador do alvo (preso na borda além de {@link RadarMath#RANGE} blocos) e, embaixo, um painel com o
 * nome, a distância e a diferença de altura. Sem sinal (outra dimensão, nada para achar), chuvisco no disco.
 */
final class RadarHud {
    static final int ACCENT = TechStyle.ACCENT;
    static final int BRIGHT = TechStyle.BRIGHT;
    static final int TARGET = TechStyle.AMBER;
    static final int TEXT = TechStyle.TEXT;
    static final int SUBTLE = TechStyle.SUBTLE;
    static final int PANEL = TechStyle.HUD_PANEL;
    static final int GRID = 0x3538C6D9;

    private static final int BEZEL = 0xE01C262D;
    private static final int TICK = 0x90B8D4E0;
    private static final int LETTER = 0xFFDDEFF5;
    private static final int NORTH = 0xFFFF6B5B;
    private static final int SCANLINE = 0x0C38C6D9;

    /** Raio do disco do radar, por dentro do anel da bússola. */
    private static final float SCOPE = 28.0F;
    /** Borda de fora do anel da bússola. */
    private static final float RING = 37.0F;
    /** O espaço que o radar ocupa em volta do centro, com o índice e a marca do alvo por fora do anel. */
    private static final int REACH = 43;
    private static final int MARGIN = 6;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_MAX_WIDTH = 140;
    /** Uma volta da varredura, em milissegundos. */
    private static final float SWEEP_MILLIS = 2400.0F;
    /** Rastro da varredura, em graus. */
    private static final float SWEEP_TRAIL = 70.0F;
    private static final float SMALL = 0.75F;

    private static final String[] CARDINALS = {"n", "e", "s", "w"};
    private static final float[] CARDINAL_YAWS = {RadarMath.NORTH, RadarMath.EAST, RadarMath.SOUTH, RadarMath.WEST};

    /**
     * Onde o alvo está em relação ao jogador agora: ângulo no radar (0 à frente, horário), distância e diferença de
     * altura. Sem sinal, só o aviso.
     */
    record Reading(boolean signal, float bearing, double distance, int height) {
        static final Reading NO_SIGNAL = new Reading(false, 0.0F, 0.0, 0);
    }

    private RadarHud() {
    }

    /** A leitura de um alvo em {@code target}, na dimensão dada, para o jogador de agora; {@code null} sem jogador. */
    @Nullable
    static Reading read(Vec3 target, ResourceKey<Level> dimension) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        if (!dimension.equals(minecraft.player.level().dimension())) {
            return Reading.NO_SIGNAL;
        }
        Vec3 from = minecraft.player.position();
        return new Reading(true, RadarMath.bearing(target.x - from.x, target.z - from.z, minecraft.player.getYRot()),
                target.distanceTo(from), RadarMath.heightDifference(target.y - from.y));
    }

    /** "248 m", ou o aviso de sem sinal. */
    static Component distanceText(Reading reading, Component noSignal) {
        return reading.signal()
                ? Component.translatable("iceagesurvival.locator.distance", Math.round(reading.distance()))
                : noSignal;
    }

    /** "▲ 14 m", "▼ 30 m" ou "mesmo nível"; vazio sem sinal. */
    static Component heightText(Reading reading) {
        if (!reading.signal()) {
            return Component.empty();
        }
        if (reading.height() == 0) {
            return Component.translatable("iceagesurvival.locator.same_level");
        }
        return Component.translatable(reading.height() > 0 ? "iceagesurvival.locator.above" : "iceagesurvival.locator.below",
                Math.abs(reading.height()));
    }

    /** Liga e desliga duas vezes por segundo, para o ponto de "rastreando" e o "sem sinal". */
    static boolean blink() {
        return TechStyle.blink();
    }

    /** Se o radar pode aparecer agora: some com a interface escondida e com o F3, que ocupa o mesmo canto. */
    static boolean visible() {
        Minecraft minecraft = Minecraft.getInstance();
        return !minecraft.options.hideGui && !minecraft.options.renderDebug && minecraft.player != null;
    }

    /**
     * O radar e o painel no canto superior direito.
     *
     * @param name     a primeira linha do painel
     * @param badge    à direita do nome (o nível da criatura); pode ser vazio
     * @param noSignal no lugar da distância quando não há sinal
     */
    static void render(GuiGraphics graphics, int screenWidth, Reading reading, Component name, Component badge,
                       Component noSignal) {
        Minecraft minecraft = Minecraft.getInstance();
        float centerX = screenWidth - MARGIN - REACH;
        float centerY = MARGIN + effectRows(minecraft) + REACH;
        drawRadar(graphics, minecraft.font, centerX, centerY, minecraft.player.getYRot(), reading);
        drawPanel(graphics, minecraft.font, screenWidth - MARGIN, Math.round(centerY) + REACH + 1, name, badge,
                reading, noSignal);
    }

    /** Os ícones de efeito do vanilla ficam no canto superior direito: o radar desce uma fileira por linha usada. */
    private static int effectRows(Minecraft minecraft) {
        boolean beneficial = false;
        boolean harmful = false;
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (effect.showIcon()) {
                if (effect.getEffect().isBeneficial()) {
                    beneficial = true;
                } else {
                    harmful = true;
                }
            }
        }
        return harmful ? 52 : beneficial ? 26 : 0;
    }

    private static void drawRadar(GuiGraphics graphics, Font font, float x, float y, float yaw, Reading reading) {
        // Anel da bússola e o disco.
        HudShapes.ring(graphics, x, y, RING, RING + 1.0F, TechStyle.METAL_LIGHT);
        HudShapes.ring(graphics, x, y, RING + 1.0F, RING + 2.0F, TechStyle.METAL_SHADOW);
        HudShapes.ring(graphics, x, y, SCOPE, RING, BEZEL);
        HudShapes.disc(graphics, x, y, SCOPE, PANEL);
        HudShapes.ring(graphics, x, y, SCOPE - 0.5F, SCOPE + 0.5F, HudShapes.fade(ACCENT, 0.6F));

        // Grade fixa: anel dos 50 blocos (meio raio, pela raiz), cruz e linhas de varredura de tela.
        HudShapes.ring(graphics, x, y, SCOPE / 2.0F - 0.5F, SCOPE / 2.0F, GRID);
        for (int angle = 0; angle < 360; angle += 90) {
            HudShapes.spoke(graphics, x, y, 2.0F, SCOPE - 1.0F, angle, 0.5F, GRID);
        }
        for (float row = -SCOPE + 2.0F; row < SCOPE - 1.0F; row += 3.0F) {
            float half = (float) Math.sqrt(SCOPE * SCOPE - row * row) - 1.0F;
            HudShapes.line(graphics, x - half, y + row, x + half, y + row, 0.5F, SCANLINE);
        }

        // Marcas a cada 15° e os pontos cardeais, que giram com o olhar.
        float middle = (SCOPE + RING) / 2.0F;
        for (int degrees = 0; degrees < 360; degrees += 15) {
            if (degrees % 90 == 0) {
                continue;
            }
            float angle = RadarMath.screenAngle(degrees, yaw);
            boolean major = degrees % 45 == 0;
            HudShapes.spoke(graphics, x, y, major ? SCOPE + 2.0F : RING - 3.0F, RING - 0.5F, angle, major ? 1.0F : 0.6F,
                    TICK);
        }
        for (int index = 0; index < CARDINALS.length; index++) {
            float angle = RadarMath.screenAngle(CARDINAL_YAWS[index], yaw);
            Component letter = Component.translatable("iceagesurvival.locator.cardinal." + CARDINALS[index]);
            drawSmallCentered(graphics, font, letter, HudShapes.pointX(x, middle, angle),
                    HudShapes.pointY(y, middle, angle), index == 0 ? NORTH : LETTER);
        }

        // Varredura com rastro.
        float sweep = (Util.getMillis() % (long) SWEEP_MILLIS) / SWEEP_MILLIS * 360.0F;
        HudShapes.arc(graphics, x, y, 0.0F, SCOPE - 1.0F, sweep - SWEEP_TRAIL, sweep, 0x0038C6D9, 0x6038C6D9);
        HudShapes.spoke(graphics, x, y, 0.0F, SCOPE - 1.0F, sweep, 1.0F, HudShapes.fade(BRIGHT, 0.8F));

        // Índice da proa, no topo e por fora do anel.
        HudShapes.triangle(graphics, x, y - RING - 0.5F, x - 3.5F, y - RING - 5.0F, x + 3.5F, y - RING - 5.0F, BRIGHT);

        if (reading.signal()) {
            drawTarget(graphics, x, y, sweep, reading);
        } else {
            drawNoSignal(graphics, font, x, y);
        }

        // O jogador, no centro, olhando para cima.
        HudShapes.arrowHead(graphics, x, y - 3.5F, 0.0F, 6.0F, 3.0F, TEXT);
    }

    private static void drawTarget(GuiGraphics graphics, float x, float y, float sweep, Reading reading) {
        float bearing = reading.bearing();
        // Marca da direção por fora do anel: alinhada com o índice, o alvo está bem à frente.
        HudShapes.arrowHead(graphics, HudShapes.pointX(x, RING + 0.5F, bearing), HudShapes.pointY(y, RING + 0.5F, bearing),
                bearing + 180.0F, 4.5F, 3.0F, TARGET);

        float glow = RadarMath.sweepGlow(sweep, bearing);
        if (RadarMath.outOfRange(reading.distance())) {
            // Longe: seta na borda do disco apontando para fora.
            float tip = SCOPE - 1.5F;
            HudShapes.arrowHead(graphics, HudShapes.pointX(x, tip, bearing), HudShapes.pointY(y, tip, bearing), bearing,
                    6.0F, 4.0F, HudShapes.fade(TARGET, 0.55F + 0.45F * glow));
            return;
        }
        float radius = RadarMath.radial(reading.distance()) * (SCOPE - 5.0F);
        float markerX = HudShapes.pointX(x, radius, bearing);
        float markerY = HudShapes.pointY(y, radius, bearing);
        HudShapes.disc(graphics, markerX, markerY, 2.5F + 3.5F * glow, HudShapes.fade(TARGET, 0.35F * glow));
        HudShapes.diamond(graphics, markerX, markerY, 3.0F, HudShapes.fade(TARGET, 0.6F + 0.4F * glow));
    }

    /** Sem sinal: chuvisco no disco e "sem sinal" piscando. */
    private static void drawNoSignal(GuiGraphics graphics, Font font, float x, float y) {
        Random noise = new Random(Util.getMillis() / 80);
        for (int dot = 0; dot < 40; dot++) {
            float angle = noise.nextFloat() * 360.0F;
            float radius = (float) Math.sqrt(noise.nextFloat()) * (SCOPE - 2.0F);
            float dotX = HudShapes.pointX(x, radius, angle);
            float dotY = HudShapes.pointY(y, radius, angle);
            HudShapes.line(graphics, dotX, dotY, dotX + 1.0F, dotY, 1.0F, HudShapes.fade(BRIGHT, 0.2F + 0.4F * noise.nextFloat()));
        }
        if (blink()) {
            drawSmallCentered(graphics, font, Component.translatable("iceagesurvival.locator.no_signal"), x, y + 9.0F,
                    TARGET);
        }
    }

    /** Embaixo do radar, alinhado à direita: "● RASTREANDO", nome e selo, distância e altura. */
    private static void drawPanel(GuiGraphics graphics, Font font, int right, int top, Component name, Component badge,
                                  Reading reading, Component noSignal) {
        Component distance = distanceText(reading, noSignal);
        Component height = heightText(reading);
        int lineWidth = Math.max(font.width(name) + 6 + font.width(badge), font.width(distance) + 8 + font.width(height));
        int width = Math.min(PANEL_MAX_WIDTH, Math.max(REACH * 2, lineWidth + PANEL_PADDING * 2 + 2));
        int left = right - width;
        int bottom = top + PANEL_PADDING + 7 + 2 + font.lineHeight * 2 + 1 + PANEL_PADDING;
        TechStyle.hudFrame(graphics, left, top, right, bottom, ACCENT);

        int x = left + 2 + PANEL_PADDING;
        int textRight = right - PANEL_PADDING;
        int y = top + PANEL_PADDING;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(SMALL, SMALL, 1.0F);
        graphics.drawString(font, blink() ? "●" : "○", 0, 0, BRIGHT, false);
        graphics.drawString(font, Component.translatable("iceagesurvival.locator.tracking"), 10, 0, ACCENT, false);
        graphics.pose().popPose();

        y += 7 + 2;
        int nameWidth = textRight - x - font.width(badge) - 6;
        graphics.drawString(font, font.substrByWidth(name, nameWidth).getString(), x, y, TEXT);
        graphics.drawString(font, badge, textRight - font.width(badge), y, TARGET);
        y += font.lineHeight + 1;
        graphics.drawString(font, distance, x, y, reading.signal() ? BRIGHT : SUBTLE);
        graphics.drawString(font, height, textRight - font.width(height), y, SUBTLE);
    }

    /** Texto a 3/4 do tamanho, centrado em (x, y). */
    static void drawSmallCentered(GuiGraphics graphics, Font font, Component text, float x, float y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(SMALL, SMALL, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2 + 1, color, false);
        graphics.pose().popPose();
    }
}
