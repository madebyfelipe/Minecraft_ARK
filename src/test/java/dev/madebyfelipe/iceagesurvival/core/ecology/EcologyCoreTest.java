package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Situation;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Tuning;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Estresse, fome e escolha de presa: a parte da ecologia que não precisa do jogo. */
class EcologyCoreTest {
    private static final Tuning MAMMOTH = new Tuning(32, 4, 0.6, 0.2, 0.02, 0.5, 32);

    // ---- Estresse ----

    @Test
    void eventsStressAndTimeCalmsDown() {
        double stress = Stress.apply(0, Stress.Event.HURT, 1.0);
        assertEquals(25.0, stress, 1e-9);
        assertEquals(Stress.Mood.UNEASY, Stress.mood(stress));
        stress = Stress.apply(stress, Stress.Event.HERD_KILLED, 1.0);
        assertEquals(Stress.Mood.STRESSED, Stress.mood(stress));
        assertEquals(Stress.MAX, Stress.apply(90, Stress.Event.HUNTED, 1.0), 1e-9, "teto");
        // Em companhia acalma mais rápido que sozinho.
        assertTrue(Stress.decay(60, 10, true) < Stress.decay(60, 10, false));
        assertEquals(0.0, Stress.decay(10, 60, false), 1e-9);
    }

    @Test
    void temperamentScalesWhatStressesNotWhatCalms() {
        assertEquals(50.0, Stress.apply(0, Stress.Event.HURT, 2.0), 1e-9, "o dodô se assusta em dobro");
        assertEquals(12.5, Stress.apply(0, Stress.Event.HURT, 0.5), 1e-9, "o T-Rex, pela metade");
        assertEquals(10.0, Stress.apply(50, Stress.Event.FED, 2.0), 1e-9, "comer acalma igual");
    }

    @Test
    void stressWidensTheSensesAndMakesItUnpredictable() {
        assertEquals(1.0, Stress.perceptionMultiplier(0), 1e-9);
        assertEquals(2.0, Stress.perceptionMultiplier(100), 1e-9);
        assertTrue(Stress.unpredictability(80) > 3.0);
    }

    // ---- Reação com estresse e caçada ----

    @Test
    void stressedAnimalNoticesFromFarther() {
        Situation calm = new Situation(40, false, false, false, false, 0.5, false, 1, 0);
        Situation tense = new Situation(40, false, false, false, false, 0.5, false, 1, 60);
        assertEquals(Reaction.IGNORE, ThreatResponse.react(calm, MAMMOTH, () -> 0.99));
        assertEquals(Reaction.ALERT, ThreatResponse.react(tense, MAMMOTH, () -> 0.99));
    }

    @Test
    void huntedByAPackTheHerdRuns() {
        // Alossauros (menores que o mamute) em bando: a manada dispara em vez de encarar.
        Situation pack = new Situation(20, true, false, false, false, 0.4, true, 3, 30);
        assertEquals(Reaction.FLEE, ThreatResponse.react(pack, MAMMOTH, () -> 0.99));
        // Um lobo sozinho, bem menor: o mamute encara.
        Situation wolf = new Situation(20, false, false, false, false, 0.1, true, 1, 0);
        assertEquals(Reaction.ALERT, ThreatResponse.react(wolf, MAMMOTH, () -> 0.99));
    }

    @Test
    void panicMeansFlightUnlessTheCalfIsThere() {
        Situation panic = new Situation(10, false, false, false, false, 0.5, false, 1, 90);
        assertEquals(Reaction.FLEE, ThreatResponse.react(panic, MAMMOTH, () -> 0.99));
        Situation mother = new Situation(10, false, false, false, true, 0.5, false, 1, 90);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(mother, MAMMOTH, () -> 0.99));
    }

    // ---- Fome ----

    @Test
    void hungerGoesFromSatedToOpportunisticToHunting() {
        assertEquals(Hunger.Drive.SATED, Hunger.drive(100 * 20, 180, 360));
        assertEquals(Hunger.Drive.OPPORTUNISTIC, Hunger.drive(200 * 20, 180, 360));
        assertEquals(Hunger.Drive.HUNTING, Hunger.drive(400 * 20, 180, 360));
        long fresh = Hunger.spawnTicksSinceMeal(360, 0.99);
        assertEquals(Hunger.Drive.HUNTING, Hunger.drive(fresh, 180, 360), "parte dos recém-nascidos já sai com fome");
    }

    // ---- Escolha de presa ----

    private static HuntChoice.Prey adult(double distance, double size, boolean isolated) {
        return new HuntChoice.Prey(distance, size, false, 1.0, isolated);
    }

    @Test
    void soloHunterTakesTheStragglerNotTheHerd() {
        List<HuntChoice.Prey> herd = List.of(adult(10, 1.0, false), adult(30, 1.0, true));
        assertEquals(1, HuntChoice.choose(herd, 64, 1, Hunger.Drive.HUNTING));
    }

    @Test
    void packTakesOnBiggerPreyThanALoneHunter() {
        // Mamute ~2,4× o alossauro: um sozinho não encara; três, sim.
        List<HuntChoice.Prey> mammoths = List.of(adult(20, 2.4, false));
        assertEquals(-1, HuntChoice.choose(mammoths, 64, 1, Hunger.Drive.HUNTING));
        assertEquals(0, HuntChoice.choose(mammoths, 64, 3, Hunger.Drive.HUNTING));
    }

    @Test
    void calvesAndWoundedAreFavoured() {
        List<HuntChoice.Prey> options = List.of(
                adult(10, 0.5, true),
                new HuntChoice.Prey(25, 0.5, true, 1.0, false),
                new HuntChoice.Prey(25, 0.5, false, 0.2, true));
        int chosen = HuntChoice.choose(options, 64, 1, Hunger.Drive.HUNTING);
        assertTrue(chosen == 1 || chosen == 2, "escolheu a presa saudável e inteira: " + chosen);
    }

    @Test
    void satedIgnoresAndOpportunistOnlyTakesEasyCloseMeals() {
        List<HuntChoice.Prey> easy = List.of(new HuntChoice.Prey(8, 0.3, true, 1.0, true));
        List<HuntChoice.Prey> far = List.of(adult(40, 0.3, true));
        assertEquals(-1, HuntChoice.choose(easy, 64, 1, Hunger.Drive.SATED));
        assertEquals(0, HuntChoice.choose(easy, 64, 1, Hunger.Drive.OPPORTUNISTIC));
        assertEquals(-1, HuntChoice.choose(far, 64, 1, Hunger.Drive.OPPORTUNISTIC));
        assertEquals(0, HuntChoice.choose(far, 64, 1, Hunger.Drive.HUNTING));
    }
}
