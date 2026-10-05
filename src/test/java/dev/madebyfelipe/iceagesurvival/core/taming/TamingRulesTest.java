package dev.madebyfelipe.iceagesurvival.core.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TamingRulesTest {
    @Test
    void requiredFoodGrowsLinearlyWithLevel() {
        assertEquals(40, TamingRules.requiredFood(40, 0.02, 1), 1e-9);
        assertEquals(40 * (1 + 0.02 * 99), TamingRules.requiredFood(40, 0.02, 100), 1e-9);
    }

    @Test
    void bonusPointsScaleWithLevelAndEffectiveness() {
        assertEquals(50, TamingRules.bonusPoints(100, 0.5, 1.0));
        assertEquals(25, TamingRules.bonusPoints(100, 0.5, 0.5));
        assertEquals(0, TamingRules.bonusPoints(100, 0.5, 0.0));
        assertEquals(2, TamingRules.bonusPoints(10, 0.5, 0.5));
    }

    @Test
    void torporDecayKeepsFullTorporForAtLeastFiveMinutes() {
        // Dodô: 20 de torpor a 0,5/s acordava em 40 s; agora leva 300 s.
        assertEquals(20.0 / 300.0, TamingRules.torporDecayPerSecond(0.5, 20.0), 1e-9);
        // Brontossauro nível alto: 3000 a 4/s já dura 750 s e não muda.
        assertEquals(4.0, TamingRules.torporDecayPerSecond(4.0, 3000.0), 1e-9);
        assertEquals(0.0, TamingRules.torporDecayPerSecond(1.0, 0.0), 1e-9);
    }

    @Test
    void bonusPointsClampEffectiveness() {
        assertEquals(50, TamingRules.bonusPoints(100, 0.5, 3.0));
        assertEquals(0, TamingRules.bonusPoints(100, 0.5, -1.0));
    }
}
