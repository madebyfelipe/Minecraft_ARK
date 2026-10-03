package dev.madebyfelipe.iceagesurvival.core.locator;

/**
 * As contas do radar do localizador: em que ângulo da tela fica a criatura, a que distância do centro do
 * disco e quanto o marcador brilha quando a varredura passa por ele.
 *
 * <p>O radar é de proa para cima: o jogador fica no centro olhando para o topo e o mundo gira em volta.
 * Ângulos de tela em graus, 0 no topo e crescendo no sentido horário (90 é a direita). O yaw é o do
 * Minecraft: 0 olha para o sul (+Z), 90 para o oeste (−X).
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class RadarMath {
    /** Até esta distância (blocos) a criatura anda dentro do disco; além, fica presa na borda. */
    public static final double RANGE = 200.0;
    /** Diferença de altura (blocos) abaixo da qual a criatura está no mesmo nível. */
    public static final int SAME_LEVEL = 2;

    /** Yaw do Minecraft para cada ponto cardeal. */
    public static final float NORTH = 180.0F;
    public static final float EAST = -90.0F;
    public static final float SOUTH = 0.0F;
    public static final float WEST = 90.0F;

    private RadarMath() {
    }

    /** Ângulo em (−180, 180]. */
    public static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped <= -180.0F) {
            wrapped += 360.0F;
        } else if (wrapped > 180.0F) {
            wrapped -= 360.0F;
        }
        return wrapped;
    }

    /** O yaw que olha da origem para o deslocamento (dx, dz). */
    public static float yawTowards(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    }

    /** Onde, na tela do radar, fica o que está no yaw {@code worldYaw} para quem olha para {@code playerYaw}. */
    public static float screenAngle(float worldYaw, float playerYaw) {
        return wrap(worldYaw - playerYaw);
    }

    /** O ângulo de tela de um alvo a (dx, dz) do jogador. */
    public static float bearing(double dx, double dz, float playerYaw) {
        return screenAngle(yawTowards(dx, dz), playerYaw);
    }

    /**
     * Distância do centro, de 0 (no jogador) a 1 (borda do disco). Raiz quadrada para o perto ter mais
     * espaço: a 50 blocos o marcador já está na metade do raio.
     */
    public static float radial(double distance) {
        if (distance <= 0.0) {
            return 0.0F;
        }
        return (float) Math.sqrt(Math.min(distance, RANGE) / RANGE);
    }

    public static boolean outOfRange(double distance) {
        return distance > RANGE;
    }

    /**
     * Brilho do marcador, de 0 a 1: acende quando a varredura passa por cima e apaga até a volta seguinte.
     *
     * @param sweep  ângulo de tela da varredura agora
     * @param target ângulo de tela do marcador
     */
    public static float sweepGlow(float sweep, float target) {
        float behind = ((sweep - target) % 360.0F + 360.0F) % 360.0F;
        float fade = 1.0F - behind / 360.0F;
        return fade * fade;
    }

    /** A diferença de altura arredondada; dentro de {@link #SAME_LEVEL}, 0. */
    public static int heightDifference(double dy) {
        long rounded = Math.round(dy);
        return Math.abs(rounded) < SAME_LEVEL ? 0 : (int) rounded;
    }
}
