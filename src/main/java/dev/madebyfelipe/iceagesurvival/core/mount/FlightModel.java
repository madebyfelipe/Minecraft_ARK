package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Voo montado no estilo do Cobblemon, com o mergulho do grifo do ARK.
 *
 * <ul>
 *   <li>A montaria segue o olhar de quem monta, mas vira e inclina com velocidade angular
 *       limitada: a câmera gira na hora, o bicho faz a curva. Quanto mais rápido, mais aberta a curva.</li>
 *   <li>Frente acelera até o cruzeiro; trás freia; sem tecla, ela plana e perde embalo devagar.</li>
 *   <li>Mergulhar troca altura por velocidade (até {@code diveMultiplier} × o cruzeiro); subir faz o
 *       contrário. Descer pouco (até {@link #GLIDE_PITCH}) é só planeio e não embala. Ao sair do mergulho o
 *       excesso de velocidade se perde aos poucos: é o rasante.</li>
 *   <li>Abaixo da velocidade de sustentação ela afunda, mais rápido quanto mais devagar estiver.</li>
 *   <li>Pulo segurado bate as asas e sobe; o impulso (sprint) multiplica a velocidade máxima.</li>
 *   <li>Pousa ao tocar o chão devagar, sem bater as asas e sem olhar para cima. Rápida, raspa o
 *       chão e segue voando.</li>
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
    /** Inclinação máxima da trajetória, para cima ou para baixo, em graus. */
    public static final float MAX_PITCH = 75.0F;
    /** Olhando mais para cima que isto (graus), não pousa mesmo encostando no chão. */
    private static final float LANDING_MAX_UPWARD_PITCH = -10.0F;
    /**
     * Inclinação para baixo a partir da qual a montaria está mergulhando (pose e animação de mergulho). Era 25°; o
     * Felipe viu a pose de rasante em qualquer descidinha e ela passou para 40°, bem além do planeio
     * ({@link #GLIDE_PITCH}).
     */
    public static final float DIVE_PITCH = 40.0F;
    /**
     * Zona morta do mergulho, em graus para baixo: até aqui a descida é planeio e não ganha velocidade pela inclinação
     * (só o arrasto do planeio vale). Acima dela, o ganho cresce com o ângulo além da zona ({@link #diveGain}).
     */
    public static final float GLIDE_PITCH = 15.0F;
    /** Ticks do zero ao cruzeiro com a frente apertada, no padrão (o Pteranodonte): 1,25 s. */
    public static final double DEFAULT_ACCELERATION_TICKS = 25.0;

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
     * @param gravity         ganho por tick num mergulho vertical (perda, subindo na vertical); escala com o seno da inclinação
     *                        ({@link FlightModel#diveGain})
     * @param diveMultiplier  teto do mergulho, em múltiplos do cruzeiro
     * @param overspeedBleed  fração do excesso sobre o cruzeiro perdida por tick (duração do rasante)
     * @param turnRate        curva máxima por tick na velocidade de cruzeiro, em graus
     * @param pitchRate       mudança máxima de inclinação por tick, em graus
     * @param landingFraction pousa só abaixo desta fração do cruzeiro; acima, raspa o chão
     */
    public record Tuning(double maxSpeed, double acceleration, double brake, double drag, double stallFraction,
                         double sinkRate, double climbRate, double boostMultiplier, double strafeFraction,
                         double gravity, double diveMultiplier, double overspeedBleed, double turnRate,
                         double pitchRate, double landingFraction) {
        /** Balanceamento padrão para uma montaria com esta velocidade máxima e curva (graus/segundo). */
        public static Tuning forMaxSpeed(double maxSpeed, double turnDegreesPerSecond) {
            return forMaxSpeed(maxSpeed, turnDegreesPerSecond, DEFAULT_ACCELERATION_TICKS);
        }

        /**
         * Balanceamento padrão com a aceleração da espécie: {@code accelerationTicks} do zero ao cruzeiro com a frente
         * apertada. O Quetzalcoatlus, de 200–250 kg e asas de planador, embala devagar.
         */
        public static Tuning forMaxSpeed(double maxSpeed, double turnDegreesPerSecond, double accelerationTicks) {
            return new Tuning(maxSpeed, maxSpeed / Math.max(1.0, accelerationTicks), maxSpeed / 12.0, maxSpeed / 400.0,
                    0.3, 0.12, 0.3, 1.5, 0.35, maxSpeed / 20.0, 2.2, 0.025, turnDegreesPerSecond / 20.0,
                    turnDegreesPerSecond / 20.0 * 0.75, 0.6);
        }

        public double diveSpeed() {
            return maxSpeed * diveMultiplier;
        }
    }

    public record Velocity(double x, double y, double z) {
        public double horizontal() {
            return Math.sqrt(x * x + z * z);
        }
    }

    /**
     * Velocidade escalar no próximo tick.
     *
     * @param pitchDegrees inclinação atual da trajetória (positivo = descendo)
     */
    public static double nextSpeed(double speed, float pitchDegrees, Input input, Tuning tuning) {
        double cruise = tuning.maxSpeed() * (input.boost() ? tuning.boostMultiplier() : 1.0);
        double next = speed;
        if (input.forward() > 0 && speed < cruise) {
            double gain = tuning.acceleration() * input.forward() * (input.boost() ? tuning.boostMultiplier() : 1.0);
            next = Math.min(cruise, speed + gain);
        } else if (input.forward() < 0) {
            next = speed - tuning.brake() * -input.forward();
        }
        // Mergulhar (além do planeio) ganha velocidade; subir perde.
        next += diveGain(pitchDegrees, tuning);
        if (next > cruise) {
            // Acima do cruzeiro (mergulho ou impulso solto) o excesso se perde aos poucos: o rasante.
            next -= (next - cruise) * tuning.overspeedBleed();
        } else if (input.forward() == 0) {
            next -= tuning.drag();
        }
        return Math.max(0.0, Math.min(tuning.diveSpeed(), next));
    }

    /**
     * Quanto a inclinação soma à velocidade por tick: {@code gravity × sen(ângulo efetivo)}.
     *
     * <ul>
     *   <li>Subindo (pitch negativo), o ângulo efetivo é a própria inclinação: perde velocidade como sempre.</li>
     *   <li>Descendo até {@link #GLIDE_PITCH} (15°), é planeio: ganho zero.</li>
     *   <li>Além disso, os 15°–90° reais viram 0°–90° efetivos, {@code (pitch − 15) × 90 / 75}: o ganho nasce do zero em
     *   15° (contínuo, sem degrau) e chega ao mesmo de antes no mergulho vertical. No limite de {@link #MAX_PITCH}
     *   (75°, efetivo 72°) dá 95% do mergulho vertical; a 40° ({@link #DIVE_PITCH}, efetivo 30°), metade.</li>
     * </ul>
     */
    public static double diveGain(float pitchDegrees, Tuning tuning) {
        if (pitchDegrees <= 0.0F) {
            return tuning.gravity() * Math.sin(Math.toRadians(pitchDegrees));
        }
        if (pitchDegrees <= GLIDE_PITCH) {
            return 0.0;
        }
        double effective = Math.min(90.0, (pitchDegrees - GLIDE_PITCH) * 90.0 / (90.0 - GLIDE_PITCH));
        return tuning.gravity() * Math.sin(Math.toRadians(effective));
    }

    /** Curva máxima por tick nesta velocidade: plena até o cruzeiro, mais aberta acima dele. */
    public static double turnRate(double speed, Tuning tuning) {
        double ratio = tuning.maxSpeed() <= 0 ? 1.0 : speed / tuning.maxSpeed();
        return tuning.turnRate() / Math.max(1.0, ratio);
    }

    /** Leva {@code current} até {@code target} girando no máximo {@code maxStep} graus, pelo lado mais curto. */
    public static float approachAngle(float current, float target, double maxStep) {
        float delta = wrapDegrees(target - current);
        float step = (float) Math.max(-maxStep, Math.min(maxStep, delta));
        return wrapDegrees(current + step);
    }

    /** Próximo rumo da montaria, seguindo o olhar de quem monta com a curva limitada. */
    public static float nextYaw(float yaw, float riderYaw, double speed, Tuning tuning) {
        return approachAngle(yaw, riderYaw, turnRate(speed, tuning));
    }

    /** Próxima inclinação da trajetória, seguindo o olhar com a inclinação limitada. */
    public static float nextPitch(float pitch, float riderPitch, Tuning tuning) {
        float target = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, riderPitch));
        double step = tuning.pitchRate();
        float delta = target - pitch;
        return pitch + (float) Math.max(-step, Math.min(step, delta));
    }

    /** Velocidade da montaria: no rumo e na inclinação dela, mais lateral, sustentação e asas. */
    public static Velocity velocity(double speed, float yawDegrees, float pitchDegrees, Input input, Tuning tuning) {
        return velocity(speed, yawDegrees, pitchDegrees, input, tuning, 0.0);
    }

    /**
     * Velocidade da montaria numa térmica ({@link Thermals}): o ar quente soma {@code thermalLift} blocos/tick de subida,
     * sem bater as asas — o planador sobe devagar só planando.
     */
    public static Velocity velocity(double speed, float yawDegrees, float pitchDegrees, Input input, Tuning tuning,
                                    double thermalLift) {
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
        y += Math.max(0.0, thermalLift);
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

    /**
     * Pousa encostando no chão devagar, sem bater as asas e sem estar olhando para cima. Rápida
     * demais, raspa o chão e segue voando — é o fim do rasante.
     */
    public static boolean shouldLand(boolean onGround, boolean climbing, float pitchDegrees, double speed,
                                     Tuning tuning) {
        return onGround && !climbing && pitchDegrees >= LANDING_MAX_UPWARD_PITCH
                && speed <= tuning.maxSpeed() * tuning.landingFraction();
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }
}
