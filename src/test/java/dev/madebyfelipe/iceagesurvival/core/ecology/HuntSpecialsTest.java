package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HuntSpecialsTest {
    @Test
    void theLeapOnlyHappensAtMediumRange() {
        assertFalse(HuntSpecials.inLeapRange(2.0), "perto demais: morde");
        assertTrue(HuntSpecials.inLeapRange(6.0));
        assertFalse(HuntSpecials.inLeapRange(14.0), "longe demais: corre");
    }

    @Test
    void theLeapGoesTowardThePreyAndUp() {
        double[] v = HuntSpecials.leapVelocity(3.0, 4.0);
        assertEquals(HuntSpecials.LEAP_SPEED * 0.6, v[0], 1e-9);
        assertEquals(HuntSpecials.LEAP_LIFT, v[1], 1e-9);
        assertEquals(HuntSpecials.LEAP_SPEED * 0.8, v[2], 1e-9);
    }

    @Test
    void theBeakPiercesPartOfTheDamage() {
        assertEquals(6.0, HuntSpecials.beakPierce(20.0), 1e-9);
    }

    @Test
    void bitesStackBleedingUpToTheCap() {
        // Amplificador -1 = sem sangramento; 0 = nível 1.
        int cap = HuntSpecials.BLEED_LEVEL_CAP;
        int first = HuntSpecials.stackedBleedAmplifier(-1, HuntSpecials.BLEED_LEVELS_PER_BITE, cap);
        int second = HuntSpecials.stackedBleedAmplifier(first, HuntSpecials.BLEED_LEVELS_PER_BITE, cap);
        int third = HuntSpecials.stackedBleedAmplifier(second, HuntSpecials.BLEED_LEVELS_PER_BITE, cap);
        int fourth = HuntSpecials.stackedBleedAmplifier(third, HuntSpecials.BLEED_LEVELS_PER_BITE, cap);
        assertEquals(0, first);
        assertEquals(1, second);
        assertEquals(2, third);
        assertEquals(2, fourth, "três níveis no máximo");
    }

    @Test
    void aWeakerCutDoesNotLowerADeeperWound() {
        assertEquals(4, HuntSpecials.stackedBleedAmplifier(4, 1, 3));
        assertEquals(4, HuntSpecials.stackedBleedAmplifier(4, 1, 1), "a espada não alivia uma ferida mais funda");
    }
}
