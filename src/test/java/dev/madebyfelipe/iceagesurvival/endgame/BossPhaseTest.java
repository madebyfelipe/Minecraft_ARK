package dev.madebyfelipe.iceagesurvival.endgame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BossPhaseTest {
    @Test
    void phasesFollowTheHealthThatIsLeft() {
        assertEquals(BossPhase.ONE, BossPhase.of(1.0));
        assertEquals(BossPhase.ONE, BossPhase.of(0.5));
        assertEquals(BossPhase.TWO, BossPhase.of(0.49));
        assertEquals(BossPhase.TWO, BossPhase.of(0.25));
        assertEquals(BossPhase.THREE, BossPhase.of(0.24));
        assertEquals(BossPhase.THREE, BossPhase.of(0.0));
    }

    @Test
    void aHundredDamageBecomesAHundredSeventyAndFifty() {
        assertEquals(100.0F, BossPhase.ONE.reduce(100.0F), 1.0E-4);
        assertEquals(70.0F, BossPhase.TWO.reduce(100.0F), 1.0E-4);
        assertEquals(50.0F, BossPhase.THREE.reduce(100.0F), 1.0E-4);
    }

    @Test
    void onlyTheLaterPhasesCharge() {
        assertFalse(BossPhase.ONE.charges());
        assertTrue(BossPhase.TWO.charges());
        assertTrue(BossPhase.THREE.charges());
        assertTrue(BossPhase.THREE.chargeCooldownTicks() < BossPhase.TWO.chargeCooldownTicks(),
                "frenético, investe mais seguido");
    }

    @Test
    void theFrenzyIsFasterAndCutsDeeper() {
        assertTrue(BossPhase.THREE.speedBonus() > BossPhase.TWO.speedBonus());
        assertTrue(BossPhase.TWO.speedBonus() > BossPhase.ONE.speedBonus());
        assertTrue(BossPhase.THREE.bleedLevelsPerBite() > BossPhase.ONE.bleedLevelsPerBite());
        assertTrue(BossPhase.THREE.bleedLevelCap() > BossPhase.ONE.bleedLevelCap());
    }

    @Test
    void bitesStackBleedingUpToTheCap() {
        // Amplificador -1 = sem sangramento; 0 = nível 1.
        int first = BossPhase.stackedAmplifier(-1, 1, 3);
        int second = BossPhase.stackedAmplifier(first, 1, 3);
        int third = BossPhase.stackedAmplifier(second, 1, 3);
        int fourth = BossPhase.stackedAmplifier(third, 1, 3);
        assertEquals(0, first);
        assertEquals(1, second);
        assertEquals(2, third);
        assertEquals(2, fourth, "três níveis no máximo na fase 1");
        assertEquals(1, BossPhase.stackedAmplifier(-1, 2, 5), "a fase 3 corta dois níveis de uma vez");
        assertEquals(4, BossPhase.stackedAmplifier(3, 2, 5));
    }

    @Test
    void aWeakerCutDoesNotLowerADeeperWound() {
        assertEquals(4, BossPhase.stackedAmplifier(4, 1, 3));
        assertEquals(4, BossPhase.stackedAmplifier(4, 1, 1), "a espada não alivia a ferida do boss");
    }
}
