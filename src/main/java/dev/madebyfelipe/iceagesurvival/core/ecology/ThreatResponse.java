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

    /** Com esta desvantagem, ou mais, o predador cede ao confronto de um animal que não caça. */
    public static final double STARE_DOWN_RATIO = 2.0;

    /**
     * Se o confronto de um animal que não é caçador (o mamute, o Estegossauro) afasta o predador.
     * Compara a força dos dois lados como {@link #reactToHunter}. Quem tem metade da força do
     * outro lado, ou menos, cede só de ser encarado — mesmo com fome ou no meio da caçada. Em forças
     * parecidas, uma investida (ou blefe) afasta o predador saciado e sem alvo; o faminto, ou o
     * bando que já escolheu a presa, segura a posição.
     *
     * @param relativePower porte do animal ÷ o do predador × manada que confronta ÷ bando do predador
     * @param charging      o animal está investindo ou blefando agora
     * @param committed     o predador tem fome ou já tem alvo
     */
    public static boolean deters(double relativePower, boolean charging, boolean committed) {
        return relativePower >= STARE_DOWN_RATIO
                || charging && !committed && relativePower >= 1.0 / OUTMATCHED_SIZE_RATIO;
    }

    /**
     * Raio mínimo em que os da mesma espécie contam como um só grupo num confronto. O raio da manada
     * do Velociraptor (10) partia o bando em dois quando ele se espalhava um pouco, e a conta da
     * força virava de lado de um tick para o outro.
     */
    public static final double ALLY_RADIUS = 16.0;

    /**
     * Porte relativo: razão dos volumes de colisão elevada a 2/3, a escala de uma área. O mesmo para
     * a presa avaliando o caçador, o caçador avaliando a presa e o predador avaliando quem o encara.
     *
     * @return porte do outro ÷ o próprio
     */
    public static double sizeRatio(double ownWidth, double ownHeight, double otherWidth, double otherHeight) {
        double own = ownWidth * ownWidth * ownHeight;
        double other = otherWidth * otherWidth * otherHeight;
        return own <= 0 ? 1.0 : Math.pow(other / own, 2.0 / 3.0);
    }

    /** {@code relativePower} de {@link #deters}. */
    public static double confrontationPower(double sizeRatio, int confronters, int defenders) {
        return sizeRatio * Math.max(1, confronters) / Math.max(1, defenders);
    }

    /**
     * Como um predador reage a um animal que não é caçador e que o está confrontando: se o confronto
     * o afasta ({@link #deters}), corre (e larga a caça) diante de um lado claramente mais forte, ou
     * sai andando em forças parecidas; senão, encara de volta.
     *
     * @param defenders   quantos do bando do predador estão ali
     * @param confronters quantos da manada do animal o confrontam
     * @param charging    o animal está investindo ou blefando agora
     * @param committed   o predador tem fome ou já tem alvo
     */
    public static Reaction reactToIntimidation(Situation situation, int defenders, int confronters,
                                               boolean charging, boolean committed) {
        double power = confrontationPower(situation.sizeRatio(), confronters, defenders);
        if (deters(power, charging, committed)) {
            return power >= OUTMATCHED_SIZE_RATIO ? Reaction.FLEE : Reaction.RETREAT;
        }
        // Não se impressiona: encara de volta. O porte individual não decide aqui — o bando decide.
        return Reaction.ALERT;
    }

    /**
     * Como uma presa reage a um caçador, comparando a força provável dos grupos. A razão combina
     * porte do caçador, número de atacantes e defensores; vantagem clara permite intimidar, e
     * desvantagem clara manda fugir. Em confrontos equilibrados valem as probabilidades da espécie.
     */
    public static Reaction reactToHunter(Situation situation, Tuning tuning, int defenders,
                                         DoubleSupplier roll) {
        double radius = detectionRadius(tuning, situation.sneaking(), situation.guardingCalf(), situation.stress());
        if (situation.distance() > radius) {
            return Reaction.IGNORE;
        }

        double relativePower = situation.sizeRatio() * Math.max(1, situation.predators())
                / Math.max(1, defenders);
        if (relativePower >= OUTMATCHED_SIZE_RATIO || tuning.chargeRadius() <= 0.0) {
            return Reaction.FLEE;
        }
        boolean panicked = Stress.mood(situation.stress()).atLeast(Stress.Mood.PANICKED);
        if (panicked) {
            return situation.guardingCalf() ? Reaction.CHARGE : Reaction.FLEE;
        }
        if (situation.hunted() && !situation.guardingCalf() && relativePower >= HUNTED_FLEE_SIZE_RATIO) {
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
        if (close && relativePower <= 1.0 / OUTMATCHED_SIZE_RATIO) {
            return roll.getAsDouble() < tuning.bluffChance() ? Reaction.BLUFF : Reaction.CHARGE;
        }
        if (situation.approaching() && close) {
            return roll.getAsDouble() < tuning.bluffChance() ? Reaction.BLUFF : Reaction.CHARGE;
        }
        if (situation.approaching() && roll.getAsDouble() < tuning.retreatChance()) {
            return Reaction.RETREAT;
        }
        if (roll.getAsDouble() < tuning.chargeChance() * Stress.unpredictability(situation.stress())) {
            return Reaction.CHARGE;
        }
        return Reaction.ALERT;
    }
}
