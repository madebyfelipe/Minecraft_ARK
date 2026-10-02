package dev.madebyfelipe.iceagesurvival.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Peçonha ({@code hunt_special: venom}), a mordida da Megalania: morde, solta e segue o rastro da presa envenenada
 * (Fry et al. 2009; o dragão-de-komodo como análogo).
 */
public final class VenomBite {
    private VenomBite() {
    }

    /** O bote da caçada ({@link PrehistoricCreature#onPounce}). */
    public static void onPounce(PrehistoricCreature hunter, LivingEntity target) {
    }

    /** A mordida que acertou ({@code doHurtTarget}): envenena. */
    public static void onStrike(PrehistoricCreature hunter, LivingEntity target) {
    }
}
