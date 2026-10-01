package dev.madebyfelipe.iceagesurvival.core.spawn;

/**
 * O mundo fica mais perigoso conforme o jogador se afasta do spawn: cada espécie tem uma
 * distância mínima para nascer (o T-Rex não aparece perto do spawn) e o nível das criaturas
 * selvagens sobe com a distância.
 */
public final class DangerZones {
    /** Fração do nível máximo permitida no próprio spawn. */
    public static final double LEVEL_FRACTION_AT_SPAWN = 0.3;

    private DangerZones() {
    }

    /** Distância horizontal entre dois pontos, em blocos. */
    public static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Se uma espécie com esta distância mínima pode nascer a esta distância do spawn. */
    public static boolean allowed(double distanceFromSpawn, int minDistance) {
        return distanceFromSpawn >= minDistance;
    }

    /**
     * Nível máximo de uma criatura selvagem a esta distância do spawn: {@link #LEVEL_FRACTION_AT_SPAWN}
     * do máximo no spawn, subindo em linha reta até o máximo em {@code fullDangerDistance}.
     * Arredondado para baixo a um múltiplo de {@code step}, nunca menor que {@code step}.
     */
    public static int levelCap(int maxLevel, int step, double distanceFromSpawn, int fullDangerDistance) {
        double progress = fullDangerDistance <= 0 ? 1.0 : Math.clamp(distanceFromSpawn / fullDangerDistance, 0.0, 1.0);
        double fraction = LEVEL_FRACTION_AT_SPAWN + (1.0 - LEVEL_FRACTION_AT_SPAWN) * progress;
        int cap = (int) (maxLevel * fraction) / step * step;
        return Math.clamp(cap, step, maxLevel / step * step);
    }
}
