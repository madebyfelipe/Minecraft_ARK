package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Fôlego de voo (atributo {@code flight_stamina}, em segundos): voando gasta, pousada recarrega. Subir custa o
 * dobro; planar em descida quase nada. Esgotada, só plana; volta a decolar com uma reserva mínima.
 *
 * <p>Os custos são da espécie ({@link Costs}, bloco {@code mount.flight}); o padrão é o do Pteranodonte. Numa
 * térmica ({@link Thermals}) a subida que vem do ar quente não conta como bater as asas: o Quetzalcoatlus, planador
 * de térmica como o condor (Habib), sobe em círculos gastando o fôlego de quem plana.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class FlightStamina {
    /** Acima disto (blocos/tick) a montaria está subindo. */
    public static final double CLIMB_DY = 0.02;
    /** Segundos de fôlego gastos por segundo subindo. */
    public static final double CLIMB_RATE = 2.0;
    /** Segundos de fôlego gastos por segundo voando nivelada. */
    public static final double CRUISE_RATE = 1.0;
    /** Segundos de fôlego gastos por segundo planando em descida. */
    public static final double GLIDE_RATE = 0.2;

    /**
     * Quanto cada jeito de voar gasta, em segundos de fôlego por segundo.
     *
     * @param climb  subindo por conta própria (batendo as asas, ou trocando velocidade por altura)
     * @param cruise voando nivelada, fora de térmica
     * @param glide  planando em descida, ou sustentada por uma térmica
     */
    public record Costs(double climb, double cruise, double glide) {
        /** O voo de hoje (o do Pteranodonte): subir custa o dobro, planar em descida um quinto. */
        public static final Costs DEFAULT = new Costs(CLIMB_RATE, CRUISE_RATE, GLIDE_RATE);
    }

    private FlightStamina() {
    }

    /** Quanto fôlego (segundos por segundo) gasta voando com esta variação de altura por tick, com os custos padrão. */
    public static double drainRate(double dyPerTick) {
        return drainRate(dyPerTick, 0.0, Costs.DEFAULT);
    }

    /**
     * Quanto fôlego (segundos por segundo) gasta voando com esta variação de altura por tick.
     *
     * @param thermalLift o quanto a térmica sobe a montaria por tick ({@link Thermals#lift}); 0 fora dela. Essa parte
     *                    da subida é de graça: só o que passa dela é subida batendo as asas, e quem se sustenta numa
     *                    térmica sem bater as asas paga o custo de planar
     */
    public static double drainRate(double dyPerTick, double thermalLift, Costs costs) {
        double lift = Math.max(0.0, thermalLift);
        double powered = dyPerTick - lift;
        if (powered > CLIMB_DY) {
            return costs.climb();
        }
        if (powered < -CLIMB_DY || lift > 0.0) {
            return costs.glide();
        }
        return costs.cruise();
    }

    /** Esgotada no zero; só deixa de estar quando o fôlego volta a {@code takeoff}. */
    public static boolean exhausted(boolean wasExhausted, float fraction, float takeoff) {
        if (fraction <= 0.0F) {
            return true;
        }
        return wasExhausted && fraction < takeoff;
    }
}
