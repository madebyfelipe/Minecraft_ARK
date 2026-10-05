package dev.madebyfelipe.iceagesurvival.core.taming;

/** Fórmulas de domesticação. */
public final class TamingRules {
    /**
     * Ninguém acorda sozinho em menos que isto a partir do torpor cheio. Sem o piso, as criaturas de torpor baixo
     * (dodô, lobo, ornitolestes) acordavam em uns 40 s, rápido demais para alimentar.
     */
    public static final double MIN_KNOCKOUT_SECONDS = 300.0;

    private TamingRules() {
    }

    /**
     * Torpor perdido por segundo: o da espécie, limitado para que o torpor cheio dure ao menos
     * {@link #MIN_KNOCKOUT_SECONDS}. As espécies que já duram mais não mudam.
     */
    public static double torporDecayPerSecond(double speciesDecay, double maxTorpor) {
        return Math.min(speciesDecay, Math.max(0.0, maxTorpor) / MIN_KNOCKOUT_SECONDS);
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
