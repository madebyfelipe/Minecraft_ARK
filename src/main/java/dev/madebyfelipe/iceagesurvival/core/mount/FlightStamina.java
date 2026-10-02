package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Fôlego de voo (atributo {@code flight_stamina}, em segundos): voando gasta, pousada recarrega. Subir custa o
 * dobro; planar em descida quase nada. Esgotada, só plana; volta a decolar com uma reserva mínima.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class FlightStamina {
    /** Acima disto (blocos/tick) a montaria está subindo. */
    public static final double CLIMB_DY = 0.02;
    /** Segundos de fôlego gastos por segundo subindo. */
    public static final double CLIMB_RATE = 2.0;
    /** Segundos de fôlego gastos por segundo planando em descida. */
    public static final double GLIDE_RATE = 0.2;

    private FlightStamina() {
    }

    /** Quanto fôlego (segundos por segundo) gasta voando com esta variação de altura por tick. */
    public static double drainRate(double dyPerTick) {
        if (dyPerTick > CLIMB_DY) {
            return CLIMB_RATE;
        }
        if (dyPerTick < -CLIMB_DY) {
            return GLIDE_RATE;
        }
        return 1.0;
    }

    /** Esgotada no zero; só deixa de estar quando o fôlego volta a {@code takeoff}. */
    public static boolean exhausted(boolean wasExhausted, float fraction, float takeoff) {
        if (fraction <= 0.0F) {
            return true;
        }
        return wasExhausted && fraction < takeoff;
    }
}
