package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Quem percebe quem primeiro. O carnívoro sente a presa de mais longe do que ela o percebe: o faro
 * de caça ({@code hunt_radius}) é sempre maior que o maior alerta ({@code alert_radius} ou
 * {@code calf_radius}) das presas da dieta dele, por {@link #MARGIN} blocos pelo menos. É essa folga
 * que deixa o predador rondar a manada sem ser visto antes do bote ({@code StalkGoal}).
 *
 * <p>Quem espreita é percebido só a {@link #STALKER_FACTOR} do raio de alerta — como o jogador
 * agachado. Sem classes do Minecraft (D10).
 */
public final class Perception {
    /** Folga mínima entre o faro do carnívoro e o alerta da presa, em blocos. */
    public static final double MARGIN = 8.0;
    /** Espreitando, o predador é percebido a esta fração do raio de alerta da presa. */
    public static final double STALKER_FACTOR = 0.5;

    private Perception() {
    }

    /** O menor faro de caça que respeita a regra diante de uma presa com este alerta. */
    public static double minimumHuntRadius(double preyAlertRadius) {
        return preyAlertRadius + MARGIN;
    }

    /** O faro que vale: o declarado, ou o mínimo da regra se o declarado ficar abaixo. */
    public static double huntRadius(double declared, double largestPreyAlert) {
        return Math.max(declared, largestPreyAlert > 0.0 ? minimumHuntRadius(largestPreyAlert) : 0.0);
    }

    /** Se o faro declarado já cumpre a regra diante desta presa. */
    public static boolean respects(double declared, double preyAlertRadius) {
        return declared >= minimumHuntRadius(preyAlertRadius);
    }

    /** Até onde a presa percebe um predador que a espreita, dado o raio de alerta dela agora. */
    public static double stalkerNoticeRadius(double alertRadius) {
        return alertRadius * STALKER_FACTOR;
    }
}
