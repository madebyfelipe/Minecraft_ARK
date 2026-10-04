package dev.madebyfelipe.iceagesurvival.core.titan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TitanPhaseTest {
    @Test
    void phasesFollowTheHealthThatIsLeft() {
        assertEquals(TitanPhase.ONE, TitanPhase.of(1.0));
        assertEquals(TitanPhase.ONE, TitanPhase.of(0.66));
        assertEquals(TitanPhase.TWO, TitanPhase.of(0.65));
        assertEquals(TitanPhase.TWO, TitanPhase.of(0.33));
        assertEquals(TitanPhase.THREE, TitanPhase.of(0.32));
        assertEquals(TitanPhase.THREE, TitanPhase.of(0.0));
    }

    @Test
    void resistanceGrowsWithThePhases() {
        assertEquals(100.0F, TitanPhase.ONE.reduce(100.0F), 1.0E-4);
        assertEquals(70.0F, TitanPhase.TWO.reduce(100.0F), 1.0E-4);
        assertEquals(50.0F, TitanPhase.THREE.reduce(100.0F), 1.0E-4);
    }

    @Test
    void onlyTheLaterPhasesChargeAndTheFrenzyChargesMoreOften() {
        assertFalse(TitanPhase.ONE.charges());
        assertTrue(TitanPhase.TWO.charges());
        assertTrue(TitanPhase.THREE.charges());
        assertTrue(TitanPhase.THREE.chargeCooldownTicks() < TitanPhase.TWO.chargeCooldownTicks());
    }

    @Test
    void theFrenzyIsFasterAndCutsDeeper() {
        assertTrue(TitanPhase.THREE.speedBonus() > TitanPhase.TWO.speedBonus());
        assertTrue(TitanPhase.TWO.speedBonus() > TitanPhase.ONE.speedBonus());
        assertTrue(TitanPhase.THREE.bleedLevelsPerBite() > TitanPhase.ONE.bleedLevelsPerBite());
        assertTrue(TitanPhase.THREE.bleedLevelCap() > TitanPhase.ONE.bleedLevelCap());
    }

    @Test
    void eachPhaseHasItsOwnAnimationVariant() {
        assertEquals("", TitanPhase.ONE.animationSuffix());
        assertEquals("_f2", TitanPhase.TWO.animationSuffix());
        assertEquals("_f3", TitanPhase.THREE.animationSuffix());
        assertEquals(1, TitanPhase.ONE.number());
        assertEquals(3, TitanPhase.THREE.number());
    }
}
