package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Nado montado ({@code mount.swims}) e a linha d'água de quem é anfíbio ({@code body.amphibious}). Como o resto do
 * movimento do veículo (D18), roda no cliente de quem monta: a montaria fica na superfície com o corpo meio
 * submerso — o dorso, e quem vai nele, fora d'água —, a velocidade horizontal é a de nado da espécie (o atributo
 * {@code SWIM_SPEED} do Forge, que o {@code travel} do vanilla já aplica na água) e o Espaço sobe e pula para a
 * margem. O servidor não precisa de pacote: um veículo na água tem bloco em volta, e o vanilla só derruba quem
 * "flutua" no ar.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class SwimModel {
    /** Fração da altura do corpo que fica debaixo d'água nadando: o anfíbio nada baixo, como um crocodilo. */
    public static final double FLOAT_FRACTION = 0.6;
    /** O assento fica pelo menos isto (blocos) acima da linha d'água. */
    public static final double SEAT_CLEARANCE = 0.5;
    /** Subida por bloco submerso além da linha d'água (blocos/tick). */
    public static final double RISE_GAIN = 0.25;
    /** Subida máxima voltando à linha d'água (blocos/tick). */
    public static final double MAX_RISE = 0.12;
    /** Subida com o Espaço segurado na água aberta (blocos/tick). */
    public static final double JUMP_RISE = 0.2;
    /** Com o Espaço contra a margem: o pulo que põe a montaria em terra (blocos/tick). */
    public static final double BANK_HOP = 0.55;
    /** Com o Espaço, sobe até ficar só isto (blocos) submerso; mais que isso seria sair voando da água. */
    public static final double MIN_SUBMERGED = 0.3;

    private SwimModel() {
    }

    /** Quanto do corpo fica submerso nadando, para uma montaria desta altura e com o assento a esta altura. */
    public static double floatDepth(double bodyHeight, double seatHeight) {
        double depth = bodyHeight * FLOAT_FRACTION;
        if (seatHeight > 0.0) {
            depth = Math.min(depth, seatHeight - SEAT_CLEARANCE);
        }
        return Math.max(0.2, depth);
    }

    /**
     * A velocidade vertical da montaria nadando neste tick, antes do {@code travel} do vanilla.
     *
     * @param submerged  quanto do corpo está debaixo d'água agora (altura do fluido na caixa), em blocos
     * @param floatDepth a linha d'água desejada ({@link #floatDepth})
     * @param jump       Espaço segurado
     * @param againstBank encostada num bloco à frente (a margem)
     * @param currentVy  a velocidade vertical atual
     */
    public static double verticalSpeed(double submerged, double floatDepth, boolean jump, boolean againstBank,
                                       double currentVy) {
        if (jump && againstBank) {
            return Math.max(currentVy, BANK_HOP);
        }
        if (jump && submerged > MIN_SUBMERGED) {
            return Math.max(currentVy, JUMP_RISE);
        }
        double excess = submerged - floatDepth;
        if (excess > 0.0) {
            return Math.max(currentVy, Math.min(MAX_RISE, excess * RISE_GAIN));
        }
        return currentVy;
    }
}
