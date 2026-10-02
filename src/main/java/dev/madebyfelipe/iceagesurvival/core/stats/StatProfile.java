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

    /** Sem fôlego de voo: a espécie não voa. */
    public static final Entry NO_FLIGHT = new Entry(0.0, 0.0);

    public StatProfile(Map<Stat, Entry> entries) {
        EnumMap<Stat, Entry> all = new EnumMap<>(Stat.class);
        all.putAll(entries);
        // Só o fôlego de voo é opcional: a espécie que não o declara não voa.
        all.putIfAbsent(Stat.FLIGHT_STAMINA, NO_FLIGHT);
        for (Stat stat : Stat.values()) {
            if (!all.containsKey(stat)) {
                throw new IllegalArgumentException("Atributo ausente no perfil: " + stat.id());
            }
        }
        this.entries = all;
    }

    /** Se a espécie tem fôlego de voo (voa). */
    public boolean flies() {
        return entries.get(Stat.FLIGHT_STAMINA).base() > 0.0;
    }

    /** Atributos que recebem pontos de nível nesta espécie. */
    public java.util.List<Stat> scalableStats() {
        return Stat.wildScalableStats(flies());
    }

    public Entry entry(Stat stat) {
        return entries.get(stat);
    }

    public Map<Stat, Entry> entries() {
        EnumMap<Stat, Entry> out = new EnumMap<>(entries);
        // Ao gravar, a espécie que não voa continua sem o campo.
        if (out.get(Stat.FLIGHT_STAMINA).equals(NO_FLIGHT)) {
            out.remove(Stat.FLIGHT_STAMINA);
        }
        return out;
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
