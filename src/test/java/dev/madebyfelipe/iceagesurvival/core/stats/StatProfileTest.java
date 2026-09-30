package dev.madebyfelipe.iceagesurvival.core.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatProfileTest {
    private static StatProfile profile() {
        Map<Stat, StatProfile.Entry> entries = new EnumMap<>(Stat.class);
        entries.put(Stat.HEALTH, new StatProfile.Entry(250, 0.2));
        entries.put(Stat.ATTACK, new StatProfile.Entry(35, 0.05));
        entries.put(Stat.SPEED, new StatProfile.Entry(0.32, 0.0));
        entries.put(Stat.TORPOR, new StatProfile.Entry(100, 0.06));
        entries.put(Stat.ARMOR, new StatProfile.Entry(2, 0.1));
        return new StatProfile(entries);
    }

    @Test
    void zeroPointsGivesBaseValue() {
        assertEquals(250, profile().value(Stat.HEALTH, 0));
        assertEquals(0.32, profile().value(Stat.SPEED, 0));
    }

    @Test
    void eachPointAddsAFractionOfBase() {
        assertEquals(750, profile().value(Stat.HEALTH, 10), 1e-9);
        assertEquals(52.5, profile().value(Stat.ATTACK, 10), 1e-9);
    }

    @Test
    void readsPointsFromAStatPoints() {
        StatPoints points = StatPoints.NONE.with(Stat.TORPOR, 50);
        assertEquals(400, profile().value(Stat.TORPOR, points), 1e-9);
        assertEquals(250, profile().value(Stat.HEALTH, points), 1e-9);
    }

    @Test
    void rejectsProfileMissingAStat() {
        Map<Stat, StatProfile.Entry> entries = new EnumMap<>(Stat.class);
        entries.put(Stat.HEALTH, new StatProfile.Entry(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new StatProfile(entries));
    }

    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new StatProfile.Entry(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new StatProfile.Entry(1, -0.1));
    }
}
