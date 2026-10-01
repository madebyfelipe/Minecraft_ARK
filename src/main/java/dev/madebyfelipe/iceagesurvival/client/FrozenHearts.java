package dev.madebyfelipe.iceagesurvival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Indicador de congelamento ao lado da barra de vida.
 *
 * <p>O vanilla só desenha corações congelados quando o contador de congelamento chega ao máximo,
 * e o nosso frio para um tick antes disso de propósito (ver {@code ColdExposure}).
 */
final class FrozenHearts {
    /** 139 de 140 ticks. */
    private static final float FROZEN_FRACTION = 0.99F;

    private FrozenHearts() {
    }

    static void onGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getPercentFrozen() >= FROZEN_FRACTION) {
            int x = minecraft.getWindow().getGuiScaledWidth() / 2 - 99;
            int y = minecraft.getWindow().getGuiScaledHeight() - 39;
            event.getGuiGraphics().drawString(minecraft.font, Component.literal("❄"), x, y, 0xFF4FA8E8);
        }
    }
}
