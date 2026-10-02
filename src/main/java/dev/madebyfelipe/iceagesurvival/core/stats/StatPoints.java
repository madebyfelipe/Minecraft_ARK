package dev.madebyfelipe.iceagesurvival.core.stats;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * Pontos investidos em cada atributo de uma criatura. É isto que diferencia dois
 * indivíduos do mesmo nível e o que será herdado na reprodução.
 *
 * <p>O nível é derivado: {@code 1 + total de pontos}.
 */
public final class StatPoints {
    public static final StatPoints NONE = new StatPoints(new int[Stat.values().length]);

    private final int[] points;

    private StatPoints(int[] points) {
        this.points = points;
    }

    /** Distribui {@code level - 1} pontos ao acaso entre os atributos escaláveis. */
    public static StatPoints rollWild(int level, RandomGenerator random) {
        return rollWild(level, random, Stat.wildScalableStats());
    }

    /** Como {@link #rollWild(int, RandomGenerator)}, entre os atributos dados. */
    public static StatPoints rollWild(int level, RandomGenerator random, java.util.List<Stat> scalable) {
        if (level < 1) {
            throw new IllegalArgumentException("Nível inválido: " + level);
        }
        return NONE.addRandom(level - 1, random, scalable);
    }

    public int get(Stat stat) {
        return points[stat.ordinal()];
    }

    /** Acrescenta {@code count} pontos ao acaso entre os atributos escaláveis. */
    public StatPoints addRandom(int count, RandomGenerator random) {
        return addRandom(count, random, Stat.wildScalableStats());
    }

    /** Acrescenta {@code count} pontos ao acaso entre os atributos dados. */
    public StatPoints addRandom(int count, RandomGenerator random, java.util.List<Stat> scalable) {
        if (count < 0) {
            throw new IllegalArgumentException("Quantidade negativa: " + count);
        }
        int[] copy = points.clone();
        for (int i = 0; i < count; i++) {
            copy[scalable.get(random.nextInt(scalable.size())).ordinal()]++;
        }
        return new StatPoints(copy);
    }

    public StatPoints with(Stat stat, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Pontos negativos: " + value);
        }
        int[] copy = points.clone();
        copy[stat.ordinal()] = value;
        return new StatPoints(copy);
    }

    public int total() {
        return Arrays.stream(points).sum();
    }

    public int level() {
        return 1 + total();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StatPoints that && Arrays.equals(points, that.points);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("StatPoints{");
        for (Stat stat : Stat.values()) {
            if (stat.ordinal() > 0) {
                out.append(", ");
            }
            out.append(stat.id()).append('=').append(get(stat));
        }
        return out.append('}').toString();
    }
}
