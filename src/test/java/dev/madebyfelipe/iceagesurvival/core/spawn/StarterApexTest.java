package dev.madebyfelipe.iceagesurvival.core.spawn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class StarterApexTest {
    @Test
    void territoryStaysInsideTheStarterRadius() {
        assertTrue(StarterApex.MAX_DISTANCE + StarterApex.TERRITORY_RADIUS <= StarterApex.OUTER_RADIUS);
        assertTrue(StarterApex.OUTER_RADIUS <= 300, "o apex garantido fica a até 300 blocos do spawn");
    }

    @Test
    void ringOffsetsKeepTheTerritoryInside() {
        RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
        for (int i = 0; i < 1000; i++) {
            WildSpawnRules.Offset offset = WildSpawnRules.ringOffset(
                    StarterApex.MIN_DISTANCE, StarterApex.MAX_DISTANCE, random);
            double distance = Math.sqrt(offset.x() * offset.x() + offset.z() * offset.z());
            // O arredondamento para bloco mexe menos de um bloco.
            assertTrue(distance >= StarterApex.MIN_DISTANCE - 1
                    && distance + StarterApex.TERRITORY_RADIUS <= StarterApex.OUTER_RADIUS,
                    "centro fora do anel a " + distance);
        }
    }

    @Test
    void centersOutsideTheRingAreRejected() {
        assertFalse(StarterApex.validCenter(100));
        assertFalse(StarterApex.validCenter(290));
    }

    @Test
    void dueOnlyWithoutApexAndAfterTheWait() {
        assertTrue(StarterApex.due(false, 0, 0));
        assertFalse(StarterApex.due(true, 1000, 0));
        assertFalse(StarterApex.due(false, 10, 100));
        assertTrue(StarterApex.due(false, 100, 100));
    }
}
