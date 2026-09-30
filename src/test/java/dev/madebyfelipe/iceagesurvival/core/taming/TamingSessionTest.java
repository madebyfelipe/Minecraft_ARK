package dev.madebyfelipe.iceagesurvival.core.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TamingSessionTest {
    @Test
    void preferredFoodWithoutDamageGivesFullEffectiveness() {
        TamingSession session = new TamingSession();
        session.feed(20, 1.0);
        session.feed(20, 1.0);
        assertEquals(1.0, session.effectiveness(), 1e-9);
    }

    @Test
    void effectivenessIsTheValueWeightedAverageOfFoodQuality() {
        TamingSession session = new TamingSession();
        session.feed(30, 1.0);
        session.feed(10, 0.2);
        assertEquals((30 + 2) / 40.0, session.effectiveness(), 1e-9);
    }

    @Test
    void damageWhileUnconsciousLowersEffectiveness() {
        TamingSession session = new TamingSession();
        session.feed(20, 1.0);
        session.recordDamage(0.25);
        assertEquals(0.75, session.effectiveness(), 1e-9);
        session.recordDamage(5);
        assertEquals(0.0, session.effectiveness(), 1e-9);
    }

    @Test
    void nothingEatenMeansZeroEffectiveness() {
        assertEquals(0.0, new TamingSession().effectiveness());
    }

    @Test
    void progressTracksFoodAgainstRequirementAndCapsAtOne() {
        TamingSession session = new TamingSession();
        session.feed(25, 1.0);
        assertEquals(0.25, session.progress(100), 1e-9);
        assertFalse(session.isComplete(100));
        session.feed(100, 1.0);
        assertEquals(1.0, session.progress(100), 1e-9);
        assertTrue(session.isComplete(100));
    }

    @Test
    void resetClearsEverything() {
        TamingSession session = new TamingSession(50, 40, 0.3);
        session.reset();
        assertEquals(0.0, session.foodValue());
        assertEquals(0.0, session.qualityWeighted());
        assertEquals(0.0, session.damageFraction());
    }

    @Test
    void restoredSessionKeepsItsNumbers() {
        TamingSession session = new TamingSession(50, 40, 0.5);
        assertEquals(0.5, session.progress(100), 1e-9);
        assertEquals(0.4, session.effectiveness(), 1e-9);
    }

    @Test
    void rejectsNonPositiveFood() {
        assertThrows(IllegalArgumentException.class, () -> new TamingSession().feed(0, 1.0));
    }
}
