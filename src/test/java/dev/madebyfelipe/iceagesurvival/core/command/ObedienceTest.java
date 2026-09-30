package dev.madebyfelipe.iceagesurvival.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ObedienceTest {
    @Test
    void chanceGoesFromMinimumToCertainty() {
        assertEquals(0.6, Obedience.chance(0, 100, 0.6), 1e-9);
        assertEquals(0.8, Obedience.chance(50, 100, 0.6), 1e-9);
        assertEquals(1.0, Obedience.chance(100, 100, 0.6), 1e-9);
    }

    @Test
    void chanceClampsOutOfRangeInput() {
        assertEquals(1.0, Obedience.chance(500, 100, 0.6), 1e-9);
        assertEquals(0.6, Obedience.chance(-5, 100, 0.6), 1e-9);
        assertEquals(1.0, Obedience.chance(0, 100, 7), 1e-9);
    }

    @Test
    void maxAffinityAlwaysObeys() {
        Random random = new Random(3);
        for (int i = 0; i < 1000; i++) {
            assertTrue(Obedience.obeys(100, 100, 0.0, random));
        }
    }

    @Test
    void zeroAffinityObeysAboutAsOftenAsTheMinimum() {
        Random random = new Random(3);
        int obeyed = 0;
        for (int i = 0; i < 10000; i++) {
            if (Obedience.obeys(0, 100, 0.6, random)) {
                obeyed++;
            }
        }
        assertEquals(0.6, obeyed / 10000.0, 0.02);
    }
}
