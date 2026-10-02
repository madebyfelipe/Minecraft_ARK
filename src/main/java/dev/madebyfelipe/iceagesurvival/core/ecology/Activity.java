package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Horário de atividade da fauna ({@code behavior.habits.activity}). O noturno (Ornitholestes, de órbitas enormes)
 * dorme escondido de dia e caça à noite; o diurno, o contrário. Domesticada, a criatura segue o dono a qualquer hora.
 *
 * <p>Hora do dia do Minecraft: 0 é o nascer do sol, 12 000 o pôr do sol, 24 000 o dia seguinte.
 * Sem classes do Minecraft (D10).
 */
public final class Activity {
    public static final long DAY_LENGTH = 24_000L;
    /** O noturno se recolhe logo depois do nascer do sol e acorda no pôr do sol. */
    public static final long SUNRISE = 0L;
    public static final long SUNSET = 12_000L;

    public enum Pattern {
        ALWAYS,
        NOCTURNAL,
        DIURNAL
    }

    private Activity() {
    }

    public static boolean isDaytime(long dayTime) {
        long time = Math.floorMod(dayTime, DAY_LENGTH);
        return time >= SUNRISE && time < SUNSET;
    }

    /** Se é hora de dormir para este padrão. */
    public static boolean rests(Pattern pattern, long dayTime) {
        return switch (pattern) {
            case ALWAYS -> false;
            case NOCTURNAL -> isDaytime(dayTime);
            case DIURNAL -> !isDaytime(dayTime);
        };
    }
}
