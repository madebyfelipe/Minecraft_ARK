package dev.madebyfelipe.iceagesurvival.core.taming;

/** Fórmulas de domesticação. */
public final class TamingRules {
    private TamingRules() {
    }

    /** Alimento total exigido; cresce linearmente com o nível. */
    public static double requiredFood(double base, double perLevel, int level) {
        return base * (1.0 + perLevel * (level - 1));
    }

    /** Pontos de atributo extras que a criatura ganha ao ser domesticada. */
    public static int bonusPoints(int level, double maxFractionOfLevel, double effectiveness) {
        return (int) Math.floor(level * maxFractionOfLevel * Math.max(0.0, Math.min(1.0, effectiveness)));
    }
}
