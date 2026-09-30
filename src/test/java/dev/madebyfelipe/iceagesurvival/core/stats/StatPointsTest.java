package dev.madebyfelipe.iceagesurvival.core.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class StatPointsTest {
    @Test
    void wildRollSpendsExactlyLevelMinusOnePoints() {
        for (int level : new int[] {1, 10, 50, 100}) {
            StatPoints points = StatPoints.rollWild(level, new Random(level));
            assertEquals(level - 1, points.total());
            assertEquals(level, points.level());
        }
    }

    @Test
    void wildRollNeverPutsPointsInSpeed() {
        for (long seed = 0; seed < 200; seed++) {
            assertEquals(0, StatPoints.rollWild(100, new Random(seed)).get(Stat.SPEED));
        }
    }

    @Test
    void wildRollIsDeterministicForASeedAndVariesAcrossSeeds() {
        assertEquals(StatPoints.rollWild(60, new Random(7)), StatPoints.rollWild(60, new Random(7)));
        assertNotEquals(StatPoints.rollWild(60, new Random(7)), StatPoints.rollWild(60, new Random(8)));
    }

    @Test
    void wildRollSpreadsPointsAcrossEveryScalableStat() {
        StatPoints points = StatPoints.rollWild(100, new Random(1));
        for (Stat stat : Stat.wildScalableStats()) {
            assertTrue(points.get(stat) > 0, stat.id());
        }
    }

    @Test
    void withReplacesOneStatAndLeavesTheOriginalUntouched() {
        StatPoints changed = StatPoints.NONE.with(Stat.ATTACK, 5);
        assertEquals(5, changed.get(Stat.ATTACK));
        assertEquals(6, changed.level());
        assertEquals(0, StatPoints.NONE.get(Stat.ATTACK));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> StatPoints.rollWild(0, new Random()));
        assertThrows(IllegalArgumentException.class, () -> StatPoints.NONE.with(Stat.HEALTH, -1));
    }
}
