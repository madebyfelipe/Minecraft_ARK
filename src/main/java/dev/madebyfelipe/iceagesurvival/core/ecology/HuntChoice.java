package dev.madebyfelipe.iceagesurvival.core.ecology;

import java.util.List;

/**
 * Qual presa o predador escolhe, como na natureza: a mais fácil, não a mais próxima. Filhote,
 * ferida e desgarrada valem mais; presa muito maior que o caçador só entra se ele caça em bando
 * (e o bando é grande o bastante) ou se é filhote.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class HuntChoice {
    /** Presa com área de colisão até este tanto da do caçador sozinho: dá conta. */
    public static final double SOLO_MAX_SIZE_RATIO = 1.3;
    /** Cada caçador do bando soma este tanto ao tamanho de presa que o bando encara. */
    public static final double PACK_SIZE_PER_HUNTER = 1.25;
    /** Desgarrada, até uma presa bem maior fica vulnerável: o T-Rex pega o Brontossauro sozinho. */
    public static final double ISOLATED_SIZE_BONUS = 1.0;
    /** Oportunista só ataca presa fácil e a até esta fração do raio de caça. */
    public static final double OPPORTUNISTIC_RANGE_FRACTION = 0.35;
    /** Oportunista só ataca presa com pelo menos esta nota. */
    public static final double OPPORTUNISTIC_MIN_SCORE = 1.5;

    private HuntChoice() {
    }

    /**
     * Uma presa possível.
     *
     * @param distance       distância até ela, em blocos
     * @param sizeRatio      área de colisão dela ÷ a do caçador
     * @param baby           filhote
     * @param healthFraction vida atual ÷ máxima
     * @param isolated       sem outro da manada por perto (espécie solitária conta como isolada)
     */
    public record Prey(double distance, double sizeRatio, boolean baby, double healthFraction, boolean isolated) {
    }

    /**
     * Nota da presa: maior é melhor; negativa = não ataca.
     *
     * @param pack quantos caçadores da espécie estão juntos (1 = sozinho)
     */
    public static double score(Prey prey, double huntRadius, int pack) {
        if (prey.distance() > huntRadius) {
            return -1.0;
        }
        double capacity = SOLO_MAX_SIZE_RATIO + PACK_SIZE_PER_HUNTER * Math.max(0, pack - 1)
                + (prey.isolated() ? ISOLATED_SIZE_BONUS : 0.0);
        if (!prey.baby() && prey.sizeRatio() > capacity) {
            return -1.0;
        }
        // Presa de manada no meio do grupo: o caçador solitário não arrisca; o bando, sim.
        if (!prey.isolated() && !prey.baby() && pack <= 1) {
            return -1.0;
        }
        double score = 1.0;
        if (prey.baby()) {
            score += 1.5;
        }
        if (prey.isolated()) {
            score += 1.0;
        }
        score += (1.0 - Math.max(0.0, Math.min(1.0, prey.healthFraction()))) * 2.0;
        // Mais perto, melhor: perde até 1 ponto na borda do raio.
        score -= prey.distance() / Math.max(1.0, huntRadius);
        return score;
    }

    /**
     * O índice da presa escolhida, ou −1.
     *
     * @param drive com {@link Hunger.Drive#OPPORTUNISTIC} só presa fácil e perto; saciado, nenhuma
     */
    public static int choose(List<Prey> candidates, double huntRadius, int pack, Hunger.Drive drive) {
        if (drive == Hunger.Drive.SATED) {
            return -1;
        }
        double radius = drive == Hunger.Drive.OPPORTUNISTIC ? huntRadius * OPPORTUNISTIC_RANGE_FRACTION : huntRadius;
        double minimum = drive == Hunger.Drive.OPPORTUNISTIC ? OPPORTUNISTIC_MIN_SCORE : 0.0;
        int best = -1;
        double bestScore = minimum;
        for (int index = 0; index < candidates.size(); index++) {
            double score = score(candidates.get(index), radius, pack);
            if (score >= bestScore && (best < 0 || score > bestScore)) {
                best = index;
                bestScore = score;
            }
        }
        return best;
    }
}
