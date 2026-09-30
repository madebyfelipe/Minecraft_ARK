package dev.madebyfelipe.iceagesurvival.core.stats;

import java.util.EnumMap;
import java.util.Map;

/**
 * Curva de atributos de uma espécie: valor base no nível 1 e quanto cada ponto
 * investido acrescenta, como fração do valor base.
 */
public final class StatProfile {
    /** Valor base e ganho por ponto de um atributo. */
    public record Entry(double base, double perPoint) {
        public Entry {
            if (base < 0 || perPoint < 0) {
                throw new IllegalArgumentException("base e perPoint não podem ser negativos");
            }
        }
    }

    private final Map<Stat, Entry> entries;

    public StatProfile(Map<Stat, Entry> entries) {
        for (Stat stat : Stat.values()) {
            if (!entries.containsKey(stat)) {
                throw new IllegalArgumentException("Atributo ausente no perfil: " + stat.id());
            }
        }
        this.entries = new EnumMap<>(entries);
    }

    public Entry entry(Stat stat) {
        return entries.get(stat);
    }

    public Map<Stat, Entry> entries() {
        return new EnumMap<>(entries);
    }

    /** Valor final do atributo para a quantidade de pontos investida. */
    public double value(Stat stat, int points) {
        Entry entry = entries.get(stat);
        return entry.base() * (1.0 + entry.perPoint() * points);
    }

    public double value(Stat stat, StatPoints points) {
        return value(stat, points.get(stat));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StatProfile that && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }
}
