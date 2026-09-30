package dev.madebyfelipe.iceagesurvival.core.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.spawn.WildSpawnRules.Candidate;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class WildSpawnRulesTest {
    @Test
    void speciesWithoutRoomIsNeverPicked() {
        Random random = new Random(7);
        List<Candidate> candidates = List.of(
                new Candidate(100, 4, 4),   // lotada
                new Candidate(1, 0, 4));
        for (int i = 0; i < 500; i++) {
            assertEquals(1, WildSpawnRules.pick(candidates, random));
        }
    }

    @Test
    void weightZeroTakesTheSpeciesOutOfTheDraw() {
        Random random = new Random(7);
        List<Candidate> candidates = List.of(new Candidate(0, 0, 4), new Candidate(5, 0, 4));
        for (int i = 0; i < 200; i++) {
            assertEquals(1, WildSpawnRules.pick(candidates, random));
        }
    }

    @Test
    void nothingIsPickedWhenEveryoneIsFull() {
        assertEquals(-1, WildSpawnRules.pick(
                List.of(new Candidate(10, 4, 4), new Candidate(10, 9, 8)), new Random(1)));
        assertEquals(-1, WildSpawnRules.pick(List.of(), new Random(1)));
    }

    @Test
    void weightsDecideTheProportion() {
        Random random = new Random(2024);
        List<Candidate> candidates = List.of(new Candidate(1, 0, 100), new Candidate(9, 0, 100));
        int second = 0;
        int rolls = 20_000;
        for (int i = 0; i < rolls; i++) {
            if (WildSpawnRules.pick(candidates, random) == 1) {
                second++;
            }
        }
        double share = (double) second / rolls;
        assertTrue(share > 0.87 && share < 0.93, "proporção do peso 9: " + share);
    }

    @Test
    void groupSizeStaysBetweenMinAndMax() {
        Random random = new Random(3);
        Set<Integer> seen = new TreeSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(WildSpawnRules.groupSize(2, 4, 16, random));
        }
        assertEquals(Set.of(2, 3, 4), seen);
    }

    @Test
    void groupSizeNeverExceedsTheRoomLeft() {
        Random random = new Random(3);
        for (int i = 0; i < 500; i++) {
            assertEquals(1, WildSpawnRules.groupSize(2, 4, 1, random));
        }
        assertEquals(0, WildSpawnRules.groupSize(2, 4, 0, random));
    }

    @Test
    void offsetStaysInsideTheRing() {
        Random random = new Random(11);
        for (int i = 0; i < 5000; i++) {
            WildSpawnRules.Offset offset = WildSpawnRules.ringOffset(40, 96, random);
            double distance = Math.hypot(offset.x(), offset.z());
            // Uma folga de um bloco absorve o arredondamento para inteiro.
            assertTrue(distance >= 39.0 && distance <= 97.0, "distância " + distance);
        }
    }

    @Test
    void offsetSpreadsOverEveryDirection() {
        Random random = new Random(12);
        boolean north = false;
        boolean south = false;
        boolean east = false;
        boolean west = false;
        for (int i = 0; i < 2000; i++) {
            WildSpawnRules.Offset offset = WildSpawnRules.ringOffset(40, 96, random);
            north |= offset.z() < -20;
            south |= offset.z() > 20;
            east |= offset.x() > 20;
            west |= offset.x() < -20;
        }
        assertTrue(north && south && east && west, "a reposição não cobriu as quatro direções");
    }
}
