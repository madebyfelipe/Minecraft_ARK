package dev.madebyfelipe.iceagesurvival.core.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class WildLevelsTest {
    @Test
    void defaultsProduceExactlyTheTenTiers() {
        Random random = new Random(42);
        Set<Integer> seen = new TreeSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(WildLevels.roll(100, 10, random));
        }
        assertEquals(Set.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100), seen);
    }

    @Test
    void stepOfOneAllowsIntermediateLevels() {
        Random random = new Random(42);
        Set<Integer> seen = new TreeSet<>();
        for (int i = 0; i < 2000; i++) {
            seen.add(WildLevels.roll(5, 1, random));
        }
        assertEquals(Set.of(1, 2, 3, 4, 5), seen);
    }

    @Test
    void maxNotDivisibleByStepRoundsDown() {
        Random random = new Random(1);
        for (int i = 0; i < 500; i++) {
            int level = WildLevels.roll(35, 10, random);
            assertEquals(0, level % 10);
            assertEquals(true, level <= 30);
        }
    }

    @Test
    void rejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> WildLevels.roll(5, 10, new Random()));
        assertThrows(IllegalArgumentException.class, () -> WildLevels.roll(10, 0, new Random()));
    }
}
