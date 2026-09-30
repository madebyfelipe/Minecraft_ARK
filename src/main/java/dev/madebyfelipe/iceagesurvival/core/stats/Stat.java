package dev.madebyfelipe.iceagesurvival.core.stats;

import java.util.Arrays;
import java.util.List;

/** Atributos de uma criatura. */
public enum Stat {
    HEALTH("health", true),
    ATTACK("attack", true),
    /** Velocidade não recebe pontos selvagens; só muda por mutação. */
    SPEED("speed", false),
    TORPOR("torpor", true),
    ARMOR("armor", true);

    private static final List<Stat> WILD_SCALABLE =
            Arrays.stream(values()).filter(Stat::wildScalable).toList();

    private final String id;
    private final boolean wildScalable;

    Stat(String id, boolean wildScalable) {
        this.id = id;
        this.wildScalable = wildScalable;
    }

    /** Nome usado em JSON e NBT. */
    public String id() {
        return id;
    }

    /** Se pontos de nível de uma criatura selvagem podem cair neste atributo. */
    public boolean wildScalable() {
        return wildScalable;
    }

    public static List<Stat> wildScalableStats() {
        return WILD_SCALABLE;
    }

    public static Stat byId(String id) {
        for (Stat stat : values()) {
            if (stat.id.equals(id)) {
                return stat;
            }
        }
        throw new IllegalArgumentException("Atributo desconhecido: " + id);
    }
}
