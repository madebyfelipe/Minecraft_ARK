package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Em quem mirar primeiro, sem Minecraft (D10): a ameaça maior ou mais imediata vem antes. O jogador
 * não tem prioridade especial — com um predador maior investindo, um bando rival ou um golem batendo,
 * ele é ignorado, mesmo que tenha provocado.
 */
public final class TargetPriority {
    /** Distância, em blocos, em que o perigo de uma ameaça cai pela metade. */
    public static final double HALF_DANGER_DISTANCE = 8.0;
    /** Quem já está atacando (esta criatura ou o bando) conta este tanto a mais. */
    public static final double ATTACKING_FACTOR = 2.0;

    private TargetPriority() {
    }

    /**
     * Perigo de uma ameaça.
     *
     * @param sizeRatio porte dela ÷ o desta criatura ({@link ThreatResponse#sizeRatio})
     * @param group     quantos vêm juntos com ela (o bando dela)
     * @param attacking se está atacando esta criatura ou o bando agora
     * @param distance  distância até ela, em blocos
     */
    public static double danger(double sizeRatio, int group, boolean attacking, double distance) {
        double base = sizeRatio * Math.max(1, group) * (attacking ? ATTACKING_FACTOR : 1.0);
        return base / (1.0 + Math.max(0.0, distance) / HALF_DANGER_DISTANCE);
    }

    /** Se a ameaça {@code other} merece o alvo no lugar do atual. */
    public static boolean outranks(double otherDanger, double currentDanger) {
        return otherDanger > currentDanger;
    }
}
