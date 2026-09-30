package dev.madebyfelipe.iceagesurvival.client;

import net.minecraft.client.gui.Gui;
import net.neoforged.neoforge.event.entity.player.PlayerHeartTypeEvent;

/**
 * Corações azuis enquanto o jogador está congelado.
 *
 * <p>O vanilla só os desenha quando o contador de congelamento chega ao máximo, e o nosso frio para
 * um tick antes disso de propósito (ver {@code ColdExposure}). Aqui olhamos a fração, que o
 * congelamento do vanilla já sincroniza, e devolvemos o sinal que o teto tirava.
 */
final class FrozenHearts {
    /** 139 de 140 ticks. */
    private static final float FROZEN_FRACTION = 0.99F;

    private FrozenHearts() {
    }

    static void onHeartType(PlayerHeartTypeEvent event) {
        if (event.getType() == Gui.HeartType.NORMAL && event.getEntity().getPercentFrozen() >= FROZEN_FRACTION) {
            event.setType(Gui.HeartType.FROZEN);
        }
    }
}
