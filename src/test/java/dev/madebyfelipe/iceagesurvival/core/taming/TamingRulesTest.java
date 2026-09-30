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
    void bonusPointsClampEffectiveness() {
        assertEquals(50, TamingRules.bonusPoints(100, 0.5, 3.0));
        assertEquals(0, TamingRules.bonusPoints(100, 0.5, -1.0));
    }
}
