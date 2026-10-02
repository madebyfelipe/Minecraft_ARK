package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * O golpe próprio de cada caçador, pela biologia real:
 * <ul>
 *   <li><b>Emboscada</b> (Smilodon, 160–280 kg, patas dianteiras mais fortes que as de qualquer felino vivo): a
 *   arrancada mais rápida dos três, curta; o primeiro golpe agarra e prende a presa.</li>
 *   <li><b>Salto</b> (Utahraptor, caçador de bando): pula sobre a presa a média distância.</li>
 *   <li><b>Bicada</b> (Kelenken, a maior ave-terrível): o bico fundido ao crânio golpeia como uma marreta (parte do
 *   dano ignora armadura) e a ave recua em seguida — golpe e recua. Num escudo erguido, o bico trava: a fraqueza.</li>
 * </ul>
 * Sem classes do Minecraft (D10).
 */
public final class HuntSpecials {
    /** Emboscada: +50% de velocidade por 4 s a partir do bote. */
    public static final double AMBUSH_SPEED_BONUS = 0.5;
    public static final int AMBUSH_TICKS = 80;
    /** O agarrão: lentidão forte (nível 5) por 2 s. */
    public static final int GRAB_TICKS = 40;
    public static final int GRAB_AMPLIFIER = 4;
    /** Bicada: fração do dano que ignora armadura, somada ao golpe. */
    public static final double BEAK_PIERCE = 0.3;
    /** A bicada que bate num escudo trava o bico: a ave fica parada e sem atacar por 2 s. */
    public static final int BEAK_STUCK_TICKS = 40;
    /** Recuo depois da bicada, em ticks. */
    public static final int RETREAT_TICKS = 30;
    /** Salto: só a esta distância da presa (perto demais, morde; longe demais, corre). */
    public static final double LEAP_MIN = 3.0;
    public static final double LEAP_MAX = 10.0;
    public static final double LEAP_SPEED = 0.9;
    public static final double LEAP_LIFT = 0.45;

    private HuntSpecials() {
    }

    public static boolean inLeapRange(double distance) {
        return distance >= LEAP_MIN && distance <= LEAP_MAX;
    }

    /** Velocidade do salto na direção (dx, dz) da presa: {x, y, z}. */
    public static double[] leapVelocity(double dx, double dz) {
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-6) {
            return new double[] {0.0, LEAP_LIFT, 0.0};
        }
        return new double[] {dx / length * LEAP_SPEED, LEAP_LIFT, dz / length * LEAP_SPEED};
    }

    /** O dano extra da bicada, que ignora armadura. */
    public static double beakPierce(double attackDamage) {
        return attackDamage * BEAK_PIERCE;
    }
}
