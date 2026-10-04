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
    void theHideTakesAPlateOffEachHitAndLetsAQuarterOfTheRestThrough() {
        assertEquals(0.0F, TitanPhase.ONE.absorb(TitanPhase.HIDE_PLATE), 1.0E-4);
        assertEquals(0.0F, TitanPhase.THREE.absorb(10.0F), 1.0E-4, "golpe fraco não passa, nem negativo");
        assertEquals(12.5F, TitanPhase.ONE.absorb(100.0F), 1.0E-4);
        assertEquals(8.75F, TitanPhase.TWO.absorb(100.0F), 1.0E-4);
        assertEquals(6.25F, TitanPhase.THREE.absorb(100.0F), 1.0E-4);
    }

    /**
     * O que a mutação compra: um Rex de nível 100 típico (~37 pontos de ataque, 69 de dano) contra um com 15 mutações
     * de ataque (+30 pontos, 103 de dano). Sem couro a mutada morderia 1,5 vez mais forte; com ele, quase o triplo.
     */
    @Test
    void attackMutationsAreWhatGetsThroughTheHide() {
        float plain = 28.0F * (1 + 0.04F * 37);
        float mutated = 28.0F * (1 + 0.04F * 67);
        assertTrue(TitanPhase.ONE.absorb(mutated) > 2.5F * TitanPhase.ONE.absorb(plain));
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
