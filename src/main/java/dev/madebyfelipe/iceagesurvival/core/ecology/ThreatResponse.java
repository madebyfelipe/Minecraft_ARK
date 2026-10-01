package dev.madebyfelipe.iceagesurvival.core.ecology;

import java.util.function.DoubleSupplier;

/**
 * Como um animal selvagem reage a uma ameaça (jogador ou predador) — o "lutar ou fugir" da fauna.
 *
 * <p>O modelo segue o rinoceronte: cauteloso, confia no tamanho. Percebe a ameaça de longe e
 * encara; se ela se aproxima, ora se afasta, ora faz uma investida de blefe que para antes, ora
 * investe de verdade. Pego de surpresa perto demais, ou guardando o filhote, investe sem blefar.
 * Uma chance pequena de investir do nada o torna imprevisível. Diante de algo muito maior que ele,
 * foge. Espécies sem investida ({@code chargeRadius} 0) — o dodô — fogem de tudo.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class ThreatResponse {
    /** Ameaça com área de colisão este tanto maior que a do animal: fuga, sempre. */
    public static final double OUTMATCHED_SIZE_RATIO = 1.5;
    /** Com a ameaça mais perto que esta fração do alerta, uma aproximação vira investida (ou blefe). */
    public static final double CONFRONT_FRACTION = 0.6;
    /** Notada já perto, até este múltiplo do raio de investida: pego de surpresa. */
    public static final double SURPRISE_MULTIPLIER = 1.5;

    public enum Reaction {
        /** Fora do alcance dos sentidos. */
        IGNORE,
        /** Para, encara, bufa. */
        ALERT,
        /** Afasta-se andando, sem pânico. */
        RETREAT,
        /** Foge correndo. */
        FLEE,
        /** Investida que para antes da ameaça: intimidação. */
        BLUFF,
        /** Investida de verdade. */
        CHARGE
    }

    /**
     * @param alertRadius  raio em que percebe a ameaça, em blocos
     * @param chargeRadius perto assim, investe; 0 = nunca investe, só foge
     * @param bluffChance  chance de uma investida ser blefe
     * @param retreatChance chance, a cada decisão com a ameaça se aproximando, de se afastar em vez de encarar
     * @param chargeChance chance, a cada decisão, de investir do nada (imprevisível)
     * @param sneakFactor  agachado, o jogador é percebido a esta fração do raio (o faro e o ouvido
     *                     contam mais que a vista)
     * @param calfRadius   com filhote, o raio de alerta passa a ser pelo menos este
     */
    public record Tuning(double alertRadius, double chargeRadius, double bluffChance, double retreatChance,
                         double chargeChance, double sneakFactor, double calfRadius) {
    }

    /**
     * @param distance      distância até a ameaça, em blocos
     * @param approaching   a ameaça chegou mais perto desde a última decisão
     * @param sneaking      a ameaça é um jogador agachado
     * @param firstContact  é a primeira vez que o animal nota esta ameaça
     * @param guardingCalf  há filhote da espécie por perto
     * @param sizeRatio     área de colisão da ameaça ÷ a do animal
     */
    public record Situation(double distance, boolean approaching, boolean sneaking, boolean firstContact,
                            boolean guardingCalf, double sizeRatio, boolean hunted, int predators, double stress) {
        /** Sem caçada em curso e calmo. */
        public Situation(double distance, boolean approaching, boolean sneaking, boolean firstContact,
                         boolean guardingCalf, double sizeRatio) {
            this(distance, approaching, sneaking, firstContact, guardingCalf, sizeRatio, false, 1, 0.0);
        }
    }

    /**
     * Sendo caçado por algo deste tamanho relativo para cima (ou por um bando), corre: é assim que
     * a manada de mamutes dispara quando os alossauros vêm.
     */
    public static final double HUNTED_FLEE_SIZE_RATIO = 0.6;

    private ThreatResponse() {
    }

    /** Até onde o animal percebe esta ameaça. */
    public static double detectionRadius(Tuning tuning, boolean sneaking, boolean guardingCalf) {
        return detectionRadius(tuning, sneaking, guardingCalf, 0.0);
    }

    /** Até onde o animal percebe esta ameaça; estressado, de mais longe ({@link Stress#perceptionMultiplier}). */
    public static double detectionRadius(Tuning tuning, boolean sneaking, boolean guardingCalf, double stress) {
        double radius = guardingCalf ? Math.max(tuning.alertRadius(), tuning.calfRadius()) : tuning.alertRadius();
        radius *= Stress.perceptionMultiplier(stress);
        return sneaking ? radius * tuning.sneakFactor() : radius;
    }

    /**
     * A reação nesta decisão. {@code roll} devolve números uniformes em [0, 1); é chamado só quando
     * a decisão depende de sorte.
     */
    public static Reaction react(Situation situation, Tuning tuning, DoubleSupplier roll) {
        double radius = detectionRadius(tuning, situation.sneaking(), situation.guardingCalf(), situation.stress());
        if (situation.distance() > radius) {
            return Reaction.IGNORE;
        }
        if (situation.sizeRatio() >= OUTMATCHED_SIZE_RATIO || tuning.chargeRadius() <= 0.0) {
            return Reaction.FLEE;
        }
        boolean panicked = Stress.mood(situation.stress()).atLeast(Stress.Mood.PANICKED);
        // Em pânico não há blefe: foge, ou, com o filhote ali, vai para cima.
        if (panicked) {
            return situation.guardingCalf() ? Reaction.CHARGE : Reaction.FLEE;
        }
        // Caçado por um bando ou por algo do seu porte: a manada dispara.
        if (situation.hunted() && !situation.guardingCalf()
                && (situation.predators() >= 2 || situation.sizeRatio() >= HUNTED_FLEE_SIZE_RATIO)) {
            return Reaction.FLEE;
        }
        if (situation.distance() <= tuning.chargeRadius()
                || situation.firstContact() && situation.distance() <= tuning.chargeRadius() * SURPRISE_MULTIPLIER) {
            return Reaction.CHARGE;
        }
        boolean close = situation.distance() <= radius * CONFRONT_FRACTION;
        if (situation.guardingCalf() && close) {
            return Reaction.CHARGE;
        }
        if (situation.approaching() && close) {
            return roll.getAsDouble() < tuning.bluffChance() ? Reaction.BLUFF : Reaction.CHARGE;
        }
        if (situation.approaching() && !situation.guardingCalf() && roll.getAsDouble() < tuning.retreatChance()) {
            return Reaction.RETREAT;
        }
        if (roll.getAsDouble() < tuning.chargeChance() * Stress.unpredictability(situation.stress())) {
            return Reaction.CHARGE;
        }
        return Reaction.ALERT;
    }
}
