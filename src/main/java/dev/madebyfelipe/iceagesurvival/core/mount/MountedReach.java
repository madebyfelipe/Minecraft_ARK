package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Alcance do golpe de quem monta, proporcional ao corpo da montaria: o T-Rex morde bem mais longe
 * que o Smilodon, e os dois acertam um dodô no chão à frente deles.
 *
 * <p>Sem classes do Minecraft (D10); distâncias em blocos, medidas a partir da borda do corpo.
 */
public final class MountedReach {
    /** Alcance de uma montaria de largura zero. */
    public static final double BASE_REACH = 3.5;
    /** Quanto o alcance cresce por bloco de largura do corpo. */
    public static final double REACH_PER_WIDTH = 1.25;
    /** Meia-abertura do cone da mordida, em graus: o golpe pega o que está à frente e dos lados. */
    public static final double BITE_HALF_ANGLE = 70.0;
    /** Quanto a quebra de blocos passa da largura do corpo, de cada lado. */
    public static final double BREAK_SIDE_MARGIN = 1.0;

    private MountedReach() {
    }

    /** Alcance da mordida, a partir da borda do corpo. */
    public static double reach(double bodyWidth) {
        return BASE_REACH + REACH_PER_WIDTH * bodyWidth;
    }

    /** Profundidade da quebra de blocos à frente do corpo. */
    public static double breakDepth(double bodyWidth) {
        return 2.0 + bodyWidth;
    }

    /**
     * Se um alvo nesta direção está dentro do cone da mordida.
     *
     * @param facingX rumo da montaria (vetor horizontal, não precisa ser unitário)
     * @param toX     da montaria até o alvo (horizontal)
     */
    public static boolean inBiteCone(double facingX, double facingZ, double toX, double toZ) {
        double facingLength = Math.sqrt(facingX * facingX + facingZ * facingZ);
        double toLength = Math.sqrt(toX * toX + toZ * toZ);
        if (facingLength < 1.0E-6 || toLength < 1.0E-6) {
            // Em cima da montaria, ou sem rumo: pega.
            return true;
        }
        double cos = (facingX * toX + facingZ * toZ) / (facingLength * toLength);
        return cos >= Math.cos(Math.toRadians(BITE_HALF_ANGLE));
    }
}
