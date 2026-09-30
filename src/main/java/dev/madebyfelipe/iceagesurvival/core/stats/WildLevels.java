package dev.madebyfelipe.iceagesurvival.core.stats;

import java.util.random.RandomGenerator;

/** Sorteio do nível de criaturas selvagens. */
public final class WildLevels {
    private WildLevels() {
    }

    /**
     * Sorteia uniformemente um múltiplo de {@code step} entre {@code step} e {@code max}.
     * Com os padrões (100, 10) o resultado é 10, 20, ..., 100.
     */
    public static int roll(int max, int step, RandomGenerator random) {
        if (step < 1 || max < step) {
            throw new IllegalArgumentException("Faixa de nível inválida: max=" + max + ", step=" + step);
        }
        return step * (1 + random.nextInt(max / step));
    }
}
