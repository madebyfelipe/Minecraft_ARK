package dev.madebyfelipe.iceagesurvival.core.temperature;

/**
 * Quanto frio um jogador está sentindo, a partir do ambiente e da proteção que ele carrega.
 *
 * <p>Noite, altitude e tempestade derrubam a temperatura do lugar em vez de somar frio direto:
 * assim uma noite num bioma temperado continua confortável, e só esfria o que já era frio.
 */
public final class Coldness {
    /** Acima desta temperatura de lugar o jogador não sente frio nenhum. */
    public static final double COMFORT_TEMPERATURE = 0.5;
    /** Queda a partir do conforto que já corresponde ao frio máximo. */
    public static final double SEVERE_TEMPERATURE_DROP = 1.0;
    public static final double NORMAL_BODY_TEMPERATURE = 37.0;
    public static final double FROZEN_BODY_TEMPERATURE = 30.0;
    /** Frio líquido abaixo disto é "estável" no termômetro. */
    private static final double TREND_DEADBAND = 0.02;
    /** Frio líquido acima disto é "caindo rápido". */
    private static final double FAST_TREND = 0.5;

    private Coldness() {
    }

    /** Temperatura sentida no lugar, na escala do Minecraft. */
    public static double feltTemperature(ColdReading reading, ColdTuning tuning) {
        double temperature = reading.biomeTemperature()
                - Math.max(0, reading.blocksAboveSeaLevel()) * tuning.altitudeDropPerBlock();
        if (reading.night()) {
            temperature -= tuning.nightDrop();
        }
        if (reading.exposedToStorm()) {
            temperature -= tuning.stormDrop();
        }
        if (reading.wet()) {
            temperature -= tuning.wetDrop();
        }
        return temperature;
    }

    /** Frio do lugar, de 0 (confortável) para cima; 1 é o frio que a proteção máxima precisa cobrir. */
    public static double chill(ColdReading reading, ColdTuning tuning) {
        double drop = COMFORT_TEMPERATURE - feltTemperature(reading, tuning);
        return Math.max(0.0, drop / SEVERE_TEMPERATURE_DROP);
    }

    /** Proteção contra o frio, na mesma escala de {@link #chill}. */
    public static double warmth(ColdReading reading, ColdTuning tuning) {
        double warmth = Math.clamp(reading.heatProximity(), 0.0, 1.0) * tuning.heatWarmth()
                + Math.max(0.0, reading.insulation());
        return reading.sheltered() ? warmth + tuning.shelterWarmth() : warmth;
    }

    /**
     * Frio líquido, de −1 (calor de sobra) a 1 (frio extremo). Positivo esfria o jogador e negativo
     * o aquece, na mesma velocidade.
     */
    public static double severity(ColdReading reading, ColdTuning tuning) {
        return Math.clamp(chill(reading, tuning) - warmth(reading, tuning), -1.0, 1.0);
    }

    /** Quanto a exposição (0 a 1) muda num tick, para que o frio máximo leve {@code secondsToFreeze}. */
    public static double exposurePerTick(double severity, int secondsToFreeze) {
        return severity / (secondsToFreeze * 20.0);
    }

    /**
     * Exposição traduzida para o contador de congelamento do vanilla, que desenha a vinheta de gelo.
     * Fica sempre um tick abaixo do máximo: o vanilla só aplica o dano dele quando o contador chega
     * ao fim, e o dano do frio é nosso.
     */
    public static int frozenTicks(double exposure, int ticksRequiredToFreeze) {
        return (int) (Math.clamp(exposure, 0.0, 1.0) * (ticksRequiredToFreeze - 1));
    }

    /** Temperatura do corpo, em °C, para o termômetro: 37 aquecido, 30 congelado. */
    public static double bodyTemperature(double exposure) {
        return NORMAL_BODY_TEMPERATURE - Math.clamp(exposure, 0.0, 1.0) * (NORMAL_BODY_TEMPERATURE - FROZEN_BODY_TEMPERATURE);
    }

    /** Para onde a temperatura do corpo está indo. */
    public enum Trend { FALLING_FAST, FALLING, STABLE, RISING, RISING_FAST }

    public static Trend trend(double exposure, double severity) {
        if (severity > 0 && exposure >= 1.0 || severity < 0 && exposure <= 0.0 || Math.abs(severity) < TREND_DEADBAND) {
            return Trend.STABLE;
        }
        if (severity > 0) {
            return severity > FAST_TREND ? Trend.FALLING_FAST : Trend.FALLING;
        }
        return -severity > FAST_TREND ? Trend.RISING_FAST : Trend.RISING;
    }
}
