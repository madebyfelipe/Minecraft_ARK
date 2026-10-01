package dev.madebyfelipe.iceagesurvival.core.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DangerZonesTest {
    @Test
    void nearSpawnLevelsAreCapped() {
        assertEquals(30, DangerZones.levelCap(100, 10, 0, 3000));
        assertEquals(60, DangerZones.levelCap(100, 10, 1500, 3000), "metade do caminho: 30% + 35%, arredondado a 10");
        assertEquals(100, DangerZones.levelCap(100, 10, 3000, 3000));
        assertEquals(100, DangerZones.levelCap(100, 10, 50_000, 3000));
    }

    @Test
    void capNeverDropsBelowOneStep() {
        assertEquals(10, DangerZones.levelCap(20, 10, 0, 3000));
    }

    @Test
    void capGrowsWithDistance() {
        int previous = 0;
        for (int distance = 0; distance <= 4000; distance += 250) {
            int cap = DangerZones.levelCap(100, 10, distance, 3000);
            assertTrue(cap >= previous, "nível caiu em " + distance);
            previous = cap;
        }
    }

    @Test
    void minDistanceKeepsSpeciesAwayFromSpawn() {
        assertFalse(DangerZones.allowed(1999, 2000));
        assertTrue(DangerZones.allowed(2000, 2000));
        assertTrue(DangerZones.allowed(0, 0));
    }

    @Test
    void distanceIsHorizontal() {
        assertEquals(5.0, DangerZones.horizontalDistance(3, 4, 0, 0), 1e-9);
    }
}
