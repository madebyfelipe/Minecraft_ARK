package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.core.temperature.Coldness;
import dev.madebyfelipe.iceagesurvival.network.ColdStatusPayload;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Termômetro à esquerda da hotbar: a temperatura do corpo em °C (37 aquecido, 30 congelado) e
 * uma seta dizendo se está caindo, subindo ou estável — a pergunta do jogador é "dá para
 * continuar a viagem?". Os dados chegam do servidor uma vez por segundo.
 */
public final class Thermometer {
    private static final int TUBE_WIDTH = 4;
    private static final int TUBE_HEIGHT = 20;
    private static final int BULB = 6;
    /** Distância da borda esquerda da hotbar, depois do slot da mão secundária. */
    private static final int LEFT_OF_HOTBAR = 91 + 29 + 12;

    private static final int GLASS = 0xFF26292E;
    private static final int GLASS_EDGE = 0xFFB8C4CC;
    private static final int COLD = 0xFF4FA8E8;
    private static final int WARM = 0xFFE8743B;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int FALLING = 0xFF7FC8FF;
    private static final int RISING = 0xFFFFB066;
    private static final int STABLE = 0xFFA0A8B0;

    @Nullable
    private static ColdStatusPayload status;
    /** Temperatura mostrada, suavizada entre as leituras de segundo em segundo. */
    private static float shown = (float) Coldness.NORMAL_BODY_TEMPERATURE;

    private Thermometer() {
    }

    public static void receive(ColdStatusPayload payload) {
        status = payload;
    }

    /** Ao sair do mundo: a leitura era daquele mundo, e num mundo normal nenhuma outra chega. */
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        status = null;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (status == null || minecraft.options.hideGui || minecraft.player == null
                || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }
        float target = (float) Coldness.bodyTemperature(status.exposure());
        shown = Mth.lerp(0.05F, shown, target);
        float fraction = (float) ((shown - Coldness.FROZEN_BODY_TEMPERATURE)
                / (Coldness.NORMAL_BODY_TEMPERATURE - Coldness.FROZEN_BODY_TEMPERATURE));

        int x = screenWidth / 2 - LEFT_OF_HOTBAR;
        int bottom = screenHeight - 4;
        int bulbTop = bottom - BULB;
        int tubeTop = bulbTop - TUBE_HEIGHT;
        int color = lerpColor(fraction, COLD, WARM);

        // Vidro, com borda clara, e a coluna de mercúrio.
        graphics.fill(x - 1, tubeTop - 1, x + TUBE_WIDTH + 1, bulbTop, GLASS_EDGE);
        graphics.fill(x - 2, bulbTop - 1, x + TUBE_WIDTH + 2, bottom + 1, GLASS_EDGE);
        graphics.fill(x, tubeTop, x + TUBE_WIDTH, bulbTop, GLASS);
        graphics.fill(x - 1, bulbTop, x + TUBE_WIDTH + 1, bottom, color);
        int filled = Math.round(TUBE_HEIGHT * Mth.clamp(fraction, 0.0F, 1.0F));
        graphics.fill(x + 1, bulbTop - filled, x + TUBE_WIDTH - 1, bulbTop, color);

        Font font = minecraft.font;
        String degrees = String.format(Locale.ROOT, "%.1f°", shown);
        int textX = x - 4 - font.width(degrees);
        graphics.drawString(font, degrees, textX, bottom - font.lineHeight - 8, TEXT);
        Coldness.Trend trend = Coldness.trend(status.exposure(), status.severity());
        String arrow = switch (trend) {
            case FALLING_FAST -> "▼▼";
            case FALLING -> "▼";
            case RISING -> "▲";
            case RISING_FAST -> "▲▲";
            case STABLE -> "=";
        };
        int arrowColor = switch (trend) {
            case FALLING_FAST, FALLING -> FALLING;
            case RISING, RISING_FAST -> RISING;
            case STABLE -> STABLE;
        };
        graphics.drawString(font, arrow, x - 4 - font.width(arrow), bottom - font.lineHeight + 1, arrowColor);
        if (status.wet()) {
            Component wet = Component.translatable("iceagesurvival.thermometer.wet");
            graphics.drawString(font, wet, x - 4 - font.width(wet), tubeTop - font.lineHeight - 2, FALLING);
        }
    }

    private static int lerpColor(float t, int from, int to) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int r = Math.round(Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
