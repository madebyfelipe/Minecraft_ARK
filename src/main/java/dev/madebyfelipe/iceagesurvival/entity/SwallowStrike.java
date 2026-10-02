package dev.madebyfelipe.iceagesurvival.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Engole inteira ({@code hunt_special: swallow}), o golpe do Quetzalcoatlus: caçador a pé como a cegonha e o
 * calau-de-chão, apanha a presa pequena com o bico e a engole de uma vez (Witton &amp; Naish 2008, 2015).
 */
public final class SwallowStrike {
    private SwallowStrike() {
    }

    /** O bote da caçada ({@link PrehistoricCreature#onPounce}). */
    public static void onPounce(PrehistoricCreature hunter, LivingEntity target) {
    }

    /** O golpe que acertou ({@code doHurtTarget}). */
    public static void onStrike(PrehistoricCreature hunter, LivingEntity target) {
    }
}
