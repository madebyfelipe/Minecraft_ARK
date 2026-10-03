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

    @Test
    void aQuarterOfTheCapIsLeftForCarnivores() {
        // Teto de 28: no máximo 21 herbívoros, 7 vagas que só carnívoro ocupa.
        assertEquals(21, WildSpawnRules.herbivoreCap(28));
        assertEquals(5, WildSpawnRules.herbivoreRoom(28, 16));
        assertEquals(0, WildSpawnRules.herbivoreRoom(28, 21));
        assertEquals(0, WildSpawnRules.herbivoreRoom(28, 25), "herbívoro a mais não abre vaga negativa");
    }

    @Test
    void absentSpeciesGetsTheBoostAndAFullHerdWaitsWhenContested() {
        assertEquals(6 * WildSpawnRules.ABSENT_BOOST, WildSpawnRules.fairWeight(6, 0, 4, true));
        assertEquals(6, WildSpawnRules.fairWeight(6, 2, 4, true), "bando incompleto segue no sorteio");
        assertEquals(0, WildSpawnRules.fairWeight(6, 4, 4, true), "bando inteiro, vaga disputada: espera");
        assertEquals(6, WildSpawnRules.fairWeight(6, 4, 4, false), "vaga folgada: pode o segundo bando");
        assertEquals(0, WildSpawnRules.fairWeight(0, 0, 4, false), "peso zero continua fora");
        assertTrue(WildSpawnRules.contested(14, 27));
        assertTrue(!WildSpawnRules.contested(13, 27));
    }

    /**
     * Os herbívoros do spawn enchendo as 27 vagas, um grupo por vez, a partir do zero: com a regra de diversidade as
     * sete espécies aparecem quase sempre; só pelo peso, Estegossauro, Tricerátopo ou Brontossauro ficavam de fora.
     */
    @Test
    void fillingTheHerbivoreCapKeepsEverySpecies() {
        // dodô, Elasmotério, Galimimo, mamute, Brontossauro, Estegossauro, Tricerátopo: peso, bando mín./máx., teto
        int[][] species = {{14, 2, 5, 10}, {12, 1, 1, 1}, {7, 3, 5, 5}, {10, 3, 5, 5}, {6, 2, 4, 4}, {12, 2, 4, 4},
                {8, 2, 4, 4}};
        int cap = WildSpawnRules.herbivoreCap(36);
        Random random = new Random(11);
        int complete = 0;
        int runs = 400;
        for (int run = 0; run < runs; run++) {
            int[] nearby = new int[species.length];
            int total = 0;
            for (int draw = 0; draw < 60 && total < cap; draw++) {
                boolean contested = WildSpawnRules.contested(total, cap);
                List<Candidate> candidates = new java.util.ArrayList<>();
                for (int i = 0; i < species.length; i++) {
                    int[] sp = species[i];
                    candidates.add(new Candidate(WildSpawnRules.fairWeight(sp[0], nearby[i], sp[2], contested),
                            nearby[i], sp[3]));
                }
                int chosen = WildSpawnRules.pick(candidates, random);
                if (chosen < 0) {
                    break;
                }
                int room = Math.min(species[chosen][3] - nearby[chosen], cap - total);
                int group = WildSpawnRules.groupSize(species[chosen][1], species[chosen][2], room, random);
                nearby[chosen] += group;
                total += group;
            }
            if (java.util.Arrays.stream(nearby).allMatch(count -> count > 0)) {
                complete++;
            }
        }
        assertTrue(complete >= runs * 0.9, "todas as espécies em só " + complete + " de " + runs);
    }
}
