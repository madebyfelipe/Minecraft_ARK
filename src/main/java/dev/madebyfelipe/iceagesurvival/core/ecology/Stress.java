package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Termômetro interno de desconforto de um animal selvagem, de 0 (calmo) a {@link #MAX} (em pânico).
 *
 * <p>Eventos de fora mexem nele: ver um predador, ser caçado, ser ferido, ver a manada apanhar,
 * um rival no território, gente por perto. Com o tempo ele volta ao repouso, mais rápido em
 * companhia da manada ou de barriga cheia. O humor que sai daí muda o comportamento: um animal
 * estressado percebe ameaças de mais longe, foge antes e é mais imprevisível; um predador
 * estressado ataca de mais longe.
 *
 * <p>{@code nervousness} é o temperamento da espécie: multiplica o que estressa (o dodô se assusta
 * com tudo; o T-Rex quase com nada). Sem classes do Minecraft (D10).
 */
public final class Stress {
    public static final double MAX = 100.0;
    /** Volta ao repouso por segundo, sozinho. */
    public static final double DECAY_PER_SECOND = 1.5;
    /** Volta ao repouso por segundo, em companhia da manada ou saciado. */
    public static final double COMFORT_DECAY_PER_SECOND = 3.0;

    public enum Mood {
        CALM(0.0), UNEASY(25.0), STRESSED(50.0), PANICKED(80.0);

        private final double threshold;

        Mood(double threshold) {
            this.threshold = threshold;
        }

        public double threshold() {
            return threshold;
        }

        public boolean atLeast(Mood other) {
            return ordinal() >= other.ordinal();
        }
    }

    /** O que mexe com o animal, e quanto (pontos; negativo acalma). */
    public enum Event {
        /** Por segundo com um predador ao alcance dos sentidos. */
        THREAT_SEEN(4.0),
        /** Por segundo com um jogador por perto (agachado conta menos, quem chama decide). */
        PLAYER_NEAR(1.5),
        /** Por segundo com um rival no território. */
        RIVAL_SEEN(3.0),
        /** Virou a presa de uma caçada. */
        HUNTED(30.0),
        /** Levou um golpe. */
        HURT(25.0),
        /** Um da manada apanhou por perto. */
        HERD_HURT(12.0),
        /** Um da manada morreu por perto. */
        HERD_KILLED(35.0),
        /** A presa escapou. */
        HUNT_FAILED(12.0),
        /** Perdeu uma disputa com um rival. */
        YIELDED(25.0),
        /** Abateu e comeu. */
        FED(-40.0);

        private final double amount;

        Event(double amount) {
            this.amount = amount;
        }

        public double amount() {
            return amount;
        }
    }

    private Stress() {
    }

    /** Aplica um evento. Os que estressam são multiplicados pelo temperamento; os que acalmam, não. */
    public static double apply(double stress, Event event, double nervousness) {
        double amount = event.amount() > 0 ? event.amount() * Math.max(0.0, nervousness) : event.amount();
        return clamp(stress + amount);
    }

    /** Como {@link #apply}, com um fator para eventos graduais (o jogador agachado, a distância). */
    public static double apply(double stress, Event event, double nervousness, double scale) {
        double amount = event.amount() * Math.max(0.0, scale);
        return clamp(stress + (amount > 0 ? amount * Math.max(0.0, nervousness) : amount));
    }

    /** Volta ao repouso em {@code seconds} segundos. */
    public static double decay(double stress, double seconds, boolean comforted) {
        double rate = comforted ? COMFORT_DECAY_PER_SECOND : DECAY_PER_SECOND;
        return clamp(stress - rate * seconds);
    }

    public static Mood mood(double stress) {
        Mood result = Mood.CALM;
        for (Mood mood : Mood.values()) {
            if (stress >= mood.threshold()) {
                result = mood;
            }
        }
        return result;
    }

    /** Alcance dos sentidos: de 1× (calmo) a 2× (em pânico). Estressado, nota tudo de longe. */
    public static double perceptionMultiplier(double stress) {
        return 1.0 + clamp(stress) / MAX;
    }

    /** Multiplica a chance de investir do nada: o animal estressado é imprevisível (até 4×). */
    public static double unpredictability(double stress) {
        return 1.0 + 3.0 * clamp(stress) / MAX;
    }

    private static double clamp(double stress) {
        return Math.max(0.0, Math.min(MAX, stress));
    }
}
