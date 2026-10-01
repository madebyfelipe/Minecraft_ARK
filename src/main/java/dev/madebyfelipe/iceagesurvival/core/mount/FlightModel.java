package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Voo montado no estilo do Cobblemon: a montaria voa para onde quem monta olha, com embalo.
 *
 * <ul>
 *   <li>Frente acelera até a velocidade máxima; trás freia; sem tecla, ela plana e perde embalo devagar.</li>
 *   <li>Olhar para cima sobe e olhar para baixo mergulha — a direção é a da câmera, com inclinação.</li>
 *   <li>Abaixo da velocidade de sustentação ela afunda, mais rápido quanto mais devagar estiver.</li>
 *   <li>Pulo segurado bate as asas e sobe; o impulso (sprint) multiplica a velocidade máxima.</li>
 *   <li>Pousa ao tocar o chão olhando reto ou para baixo, sem estar batendo as asas.</li>
 * </ul>
 *
 * <p>Sem classes do Minecraft (D10): velocidades em blocos/tick, ângulos em graus na convenção do
 * jogo (yaw 0 = +Z, pitch positivo = olhando para baixo).
 */
public final class FlightModel {
    /** Fração da velocidade máxima com que a montaria sai do chão. */
    public static final double TAKEOFF_SPEED_FRACTION = 0.35;
    /** Impulso vertical da decolagem, em blocos/tick. */
    public static final double TAKEOFF_LIFT = 0.55;
    /** Olhando mais para cima que isto (graus), não pousa mesmo encostando no chão. */
    private static final float LANDING_MAX_UPWARD_PITCH = -10.0F;

    private FlightModel() {
    }

    /**
     * @param forward entrada para frente (−1 a 1; positivo = frente)
     * @param strafe  entrada lateral (−1 a 1; positivo = esquerda, como no Minecraft)
     * @param climb   pulo segurado: bate as asas
     * @param boost   impulso (tecla de correr)
     */
    public record Input(double forward, double strafe, boolean climb, boolean boost) {
    }

    /**
     * @param maxSpeed        velocidade de cruzeiro máxima, em blocos/tick
     * @param acceleration    ganho por tick com a frente apertada
     * @param brake           perda por tick com trás apertado
     * @param drag            perda por tick planando, sem tecla
     * @param stallFraction   abaixo desta fração da máxima, perde sustentação
     * @param sinkRate        queda por tick parada no ar (sem velocidade nenhuma)
     * @param climbRate       subida por tick batendo as asas
     * @param boostMultiplier multiplicador da máxima com impulso
     * @param strafeFraction  deslocamento lateral, em fração da velocidade atual
     */
    public record Tuning(double maxSpeed, double acceleration, double brake, double drag, double stallFraction,
                         double sinkRate, double climbRate, double boostMultiplier, double strafeFraction) {
        /** Balanceamento padrão para uma montaria com esta velocidade máxima. */
        public static Tuning forMaxSpeed(double maxSpeed) {
            return new Tuning(maxSpeed, maxSpeed / 25.0, maxSpeed / 12.0, maxSpeed / 400.0, 0.3,
                    0.12, 0.3, 1.5, 0.35);
        }
    }

    public record Velocity(double x, double y, double z) {
        public double horizontal() {
            return Math.sqrt(x * x + z * z);
        }
    }

    /** Velocidade escalar no próximo tick. */
    public static double nextSpeed(double speed, Input input, Tuning tuning) {
        double max = tuning.maxSpeed() * (input.boost() ? tuning.boostMultiplier() : 1.0);
        double next;
        if (input.forward() > 0) {
            next = speed + tuning.acceleration() * input.forward() * (input.boost() ? tuning.boostMultiplier() : 1.0);
            // Passou da máxima (o impulso acabou): volta a ela aos poucos, sem tranco.
            next = speed > max ? Math.max(max, speed - tuning.brake()) : Math.min(max, next);
        } else if (input.forward() < 0) {
            next = speed - tuning.brake() * -input.forward();
        } else {
            next = speed - (speed > max ? tuning.brake() : tuning.drag());
        }
        return Math.max(0.0, next);
    }

    /** Velocidade da montaria: na direção do olhar, mais lateral, sustentação e asas. */
    public static Velocity velocity(double speed, float yawDegrees, float pitchDegrees, Input input, Tuning tuning) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cosPitch = Math.cos(pitch);
        double x = -Math.sin(yaw) * cosPitch * speed;
        double y = -Math.sin(pitch) * speed;
        double z = Math.cos(yaw) * cosPitch * speed;

        if (input.strafe() != 0) {
            double lateral = input.strafe() * Math.max(speed, tuning.maxSpeed() * 0.2) * tuning.strafeFraction();
            x += Math.cos(yaw) * lateral;
            z += Math.sin(yaw) * lateral;
        }
        y -= sink(speed, tuning);
        if (input.climb()) {
            y += tuning.climbRate();
        }
        return new Velocity(x, y, z);
    }

    /** Quanto a montaria afunda por falta de velocidade: 0 acima da sustentação, {@code sinkRate} parada. */
    public static double sink(double speed, Tuning tuning) {
        double stallSpeed = tuning.maxSpeed() * tuning.stallFraction();
        if (stallSpeed <= 0 || speed >= stallSpeed) {
            return 0.0;
        }
        return tuning.sinkRate() * (1.0 - speed / stallSpeed);
    }

    /** Pousa encostando no chão, sem bater as asas e sem estar olhando para cima. */
    public static boolean shouldLand(boolean onGround, boolean climbing, float pitchDegrees) {
        return onGround && !climbing && pitchDegrees >= LANDING_MAX_UPWARD_PITCH;
    }
}
