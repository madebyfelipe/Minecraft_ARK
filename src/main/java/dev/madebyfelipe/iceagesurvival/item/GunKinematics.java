package dev.madebyfelipe.iceagesurvival.item;

/**
 * Matemática pura das armas tranquilizantes, sem classes do Minecraft (testada no JUnit): onde fica a boca do cano,
 * o coice e a arma abaixada durante a recarga (primeira e terceira pessoa) e o desvanecimento do traçante do rifle.
 *
 * <p>Tempo em ticks (com fração do {@code partialTick}); ângulos como no Minecraft: guinada 0 olha para +Z (sul),
 * 90 para −X (oeste); arfagem positiva olha para baixo.
 */
public final class GunKinematics {
    /** Boca do cano à frente do olho, em blocos. */
    public static final double MUZZLE_FORWARD = 0.6;
    /** Para o lado da mão que segura a arma. */
    public static final double MUZZLE_SIDE = 0.28;
    /** Abaixo do olho: a arma fica na altura do ombro. */
    public static final double MUZZLE_DROP = 0.18;

    /** O coice sobe em 1 tick e volta em 4: rápido o bastante para não atrapalhar a mira. */
    public static final double KICK_RISE_TICKS = 1.0;
    public static final double KICK_RETURN_TICKS = 4.0;

    /** Vida do traçante: some em 4 ticks (0,2 s). */
    public static final int TRACER_LIFETIME_TICKS = 4;

    private GunKinematics() {
    }

    /** Deslocamento em coordenadas do mundo, em blocos. */
    public record Offset(double x, double y, double z) {
        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }
    }

    /**
     * Onde fica a boca do cano em relação ao olho: um pouco à frente, para o lado da mão da arma e abaixo.
     *
     * @param yawDegrees   guinada do olhar
     * @param pitchDegrees arfagem do olhar (positiva = para baixo)
     * @param side         +1 se a arma está na mão direita, −1 na esquerda
     */
    public static Offset muzzleOffset(float yawDegrees, float pitchDegrees, int side) {
        return muzzleOffset(yawDegrees, pitchDegrees, side, MUZZLE_FORWARD, MUZZLE_SIDE, MUZZLE_DROP);
    }

    /** Como {@link #muzzleOffset(float, float, int)}, com as distâncias dadas (a primeira pessoa usa outras). */
    public static Offset muzzleOffset(float yawDegrees, float pitchDegrees, int side, double forward, double sideways,
                                      double drop) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        // Frente (o vetor de visão do Minecraft), direita (horizontal) e cima da câmera = direita × frente.
        double fx = -Math.sin(yaw) * Math.cos(pitch);
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;
        double s = sideways * Math.signum(side);
        return new Offset(fx * forward + rx * s - ux * drop,
                fy * forward - uy * drop,
                fz * forward + rz * s - uz * drop);
    }

    /**
     * Coice do disparo, de 0 a 1: sobe em {@link #KICK_RISE_TICKS} e volta suave em {@link #KICK_RETURN_TICKS}.
     *
     * @param ticksSinceShot ticks desde o disparo; negativo ou muito antigo dá 0
     */
    public static double kick(double ticksSinceShot) {
        if (ticksSinceShot < 0.0) {
            return 0.0;
        }
        if (ticksSinceShot < KICK_RISE_TICKS) {
            return Math.sin(ticksSinceShot / KICK_RISE_TICKS * Math.PI / 2.0);
        }
        double back = (ticksSinceShot - KICK_RISE_TICKS) / KICK_RETURN_TICKS;
        return back >= 1.0 ? 0.0 : (1.0 - back) * (1.0 - back);
    }

    /**
     * Quanto a arma está abaixada para recarregar, de 0 (pronta) a 1: depois do coice ela desce, fica baixa durante a
     * recarga e volta a tempo de estar erguida quando a espera acaba — é o sinal de que dá para atirar de novo.
     *
     * @param ticksSinceShot ticks desde o disparo
     * @param reloadTicks    espera da arma
     */
    public static double lowered(double ticksSinceShot, int reloadTicks) {
        if (reloadTicks <= 0 || ticksSinceShot <= 0.0 || ticksSinceShot >= reloadTicks) {
            return 0.0;
        }
        double start = Math.min(KICK_RISE_TICKS + KICK_RETURN_TICKS / 2.0, reloadTicks * 0.15);
        double down = Math.min(6.0, reloadTicks * 0.2);
        double up = Math.min(8.0, reloadTicks * 0.25);
        if (ticksSinceShot < start) {
            return 0.0;
        }
        if (ticksSinceShot < start + down) {
            return smooth((ticksSinceShot - start) / down);
        }
        if (ticksSinceShot > reloadTicks - up) {
            return smooth((reloadTicks - ticksSinceShot) / up);
        }
        return 1.0;
    }

    /** Opacidade do traçante pela idade: forte no começo e some de vez ao fim da vida. */
    public static float tracerAlpha(double age, int lifetime) {
        if (age < 0.0 || lifetime <= 0 || age >= lifetime) {
            return age < 0.0 && lifetime > 0 ? 1.0F : 0.0F;
        }
        double x = age / lifetime;
        return (float) (1.0 - x * x);
    }

    private static double smooth(double x) {
        double t = Math.max(0.0, Math.min(1.0, x));
        return t * t * (3.0 - 2.0 * t);
    }
}
