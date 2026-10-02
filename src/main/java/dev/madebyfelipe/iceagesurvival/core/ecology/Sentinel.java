package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Sentinela ({@code behavior.habits.sentinel_radius}): a criatura domesticada sempre em alerta avisa o dono quando
 * um predador selvagem chega perto, antes de o predador notar o jogador. Um aviso por predador a cada
 * {@link #COOLDOWN_TICKS}.
 * Sem classes do Minecraft (D10).
 */
public final class Sentinel {
    public static final long COOLDOWN_TICKS = 60L * 20L;
    /** O dono precisa estar a até esta distância do sentinela para ouvir o aviso. */
    public static final double OWNER_RANGE = 64.0;

    /** Os oito rumos, a partir do norte (-Z do Minecraft), no sentido horário. */
    public enum Direction {
        NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private Sentinel() {
    }

    /** Avisa de novo sobre este predador: nunca avisou, ou o último aviso já passou da espera. */
    public static boolean shouldWarn(long lastWarned, long now) {
        return lastWarned == Long.MIN_VALUE || now - lastWarned >= COOLDOWN_TICKS;
    }

    /** O rumo do deslocamento (dx, dz) no mapa: o norte é -Z e o leste é +X. */
    public static Direction direction(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        int sector = (int) Math.floorMod(Math.round(degrees / 45.0), 8L);
        return Direction.values()[sector];
    }
}
