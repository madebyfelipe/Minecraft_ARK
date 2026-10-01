package dev.madebyfelipe.iceagesurvival.core.command;

import java.util.random.RandomGenerator;

/** Quanto mais afinidade, mais a criatura obedece. */
public final class Obedience {
    private Obedience() {
    }

    /**
     * Chance de obedecer a um comando, de {@code minimum} (afinidade zero) a 1 (afinidade máxima).
     */
    public static double chance(double affinity, double maxAffinity, double minimum) {
        double floor = Math.max(0.0, Math.min(1.0, minimum));
        double fraction = maxAffinity <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, affinity / maxAffinity));
        return floor + (1.0 - floor) * fraction;
    }

    public static boolean obeys(double affinity, double maxAffinity, double minimum, RandomGenerator random) {
        return random.nextDouble() < chance(affinity, maxAffinity, minimum);
    }
}
