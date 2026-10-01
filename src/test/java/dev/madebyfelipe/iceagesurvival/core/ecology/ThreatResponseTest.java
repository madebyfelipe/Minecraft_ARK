package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Situation;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Tuning;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;

class ThreatResponseTest {
    /** O Elasmotério: alerta a 14, investe a 4, blefa metade das vezes. */
    private static final Tuning RHINO = new Tuning(14, 4, 0.55, 0.35, 0.04, 0.5, 14);
    private static final Tuning DODO = new Tuning(8, 0, 0, 0, 0, 0.4, 12);
    private static final DoubleSupplier LUCKY = () -> 0.0;
    private static final DoubleSupplier UNLUCKY = () -> 0.99;

    private static Situation at(double distance) {
        return new Situation(distance, false, false, false, false, 0.2);
    }

    private static Situation hunter(double distance, double sizeRatio, int predators) {
        return new Situation(distance, false, false, false, false, sizeRatio, false, predators, 0.0);
    }

    @Test
    void farAwayIsIgnored() {
        assertEquals(Reaction.IGNORE, ThreatResponse.react(at(20), RHINO, UNLUCKY));
    }

    @Test
    void sneakingHalvesTheSenses() {
        Situation sneaking = new Situation(10, false, true, false, false, 0.2);
        assertEquals(Reaction.IGNORE, ThreatResponse.react(sneaking, RHINO, UNLUCKY));
        assertEquals(Reaction.ALERT, ThreatResponse.react(at(10), RHINO, UNLUCKY));
    }

    @Test
    void standingStillAtADistanceOnlyGetsAStare() {
        assertEquals(Reaction.ALERT, ThreatResponse.react(at(10), RHINO, UNLUCKY));
    }

    @Test
    void tooCloseAlwaysCharges() {
        assertEquals(Reaction.CHARGE, ThreatResponse.react(at(3), RHINO, LUCKY));
        assertEquals(Reaction.CHARGE, ThreatResponse.react(at(3), RHINO, UNLUCKY));
    }

    @Test
    void surprisedAtCloseRangeChargesWithoutWarning() {
        Situation surprised = new Situation(5.5, false, false, true, false, 0.2);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(surprised, RHINO, UNLUCKY));
        Situation noticedEarlier = new Situation(5.5, false, false, false, false, 0.2);
        assertEquals(Reaction.ALERT, ThreatResponse.react(noticedEarlier, RHINO, UNLUCKY));
    }

    @Test
    void approachingCloseIsBluffOrCharge() {
        Situation pushing = new Situation(7, true, false, false, false, 0.2);
        assertEquals(Reaction.BLUFF, ThreatResponse.react(pushing, RHINO, LUCKY));
        assertEquals(Reaction.CHARGE, ThreatResponse.react(pushing, RHINO, UNLUCKY));
    }

    @Test
    void approachingFromFarSometimesMakesItBackOff() {
        Situation approaching = new Situation(12, true, false, false, false, 0.2);
        assertEquals(Reaction.RETREAT, ThreatResponse.react(approaching, RHINO, () -> 0.2));
        assertEquals(Reaction.ALERT, ThreatResponse.react(approaching, RHINO, UNLUCKY));
    }

    @Test
    void aMotherWithCalfChargesWithoutBluffingAndHearsFurther() {
        Situation withCalf = new Situation(8, false, false, false, true, 0.2);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(withCalf, RHINO, LUCKY));
        Tuning shortSighted = new Tuning(6, 3, 0.5, 0.5, 0, 0.5, 14);
        assertEquals(Reaction.IGNORE, ThreatResponse.react(at(10), shortSighted, UNLUCKY));
        assertEquals(Reaction.ALERT, ThreatResponse.react(new Situation(10, false, false, false, true, 0.2),
                shortSighted, UNLUCKY));
    }

    @Test
    void somethingMuchBiggerIsFledFrom() {
        Situation rex = new Situation(10, false, false, false, true, 3.0);
        assertEquals(Reaction.FLEE, ThreatResponse.react(rex, RHINO, LUCKY));
    }

    @Test
    void speciesWithoutChargeAlwaysFlee() {
        assertEquals(Reaction.FLEE, ThreatResponse.react(at(5), DODO, LUCKY));
        assertEquals(Reaction.IGNORE, ThreatResponse.react(at(9), DODO, LUCKY));
    }

    @Test
    void sometimesItChargesOutOfNowhere() {
        assertEquals(Reaction.CHARGE, ThreatResponse.react(at(12), RHINO, () -> 0.01));
    }

    @Test
    void largerPreyIntimidatesASmallHunterWithoutBeingHuntedFirst() {
        Tuning largeHerbivore = new Tuning(36, 3, 0.7, 0.4, 0.02, 0.5, 36);
        assertEquals(Reaction.BLUFF, ThreatResponse.reactToHunter(hunter(12, 0.2, 1),
                largeHerbivore, 1, () -> 0.5));
        assertEquals(Reaction.CHARGE, ThreatResponse.reactToHunter(hunter(12, 0.2, 1),
                largeHerbivore, 1, () -> 0.9));
    }

    @Test
    void hunterPackCanOutmatchPreyEvenWhenEachPredatorIsSmaller() {
        assertEquals(Reaction.FLEE, ThreatResponse.reactToHunter(hunter(12, 0.4, 4),
                RHINO, 1, LUCKY));
    }

    @Test
    void herdCanFaceALargerHunterTogether() {
        assertEquals(Reaction.CHARGE, ThreatResponse.reactToHunter(hunter(8, 1.2, 1),
                RHINO, 2, UNLUCKY));
    }

    @Test
    void predatorFleesAHerbivoreThatOutmatchesIt() {
        assertEquals(Reaction.FLEE,
                ThreatResponse.reactToIntimidation(hunter(8, 2.5, 1), 1, 1, false, true),
                "bem maior, só encarando: corre, mesmo caçando");
        assertEquals(Reaction.FLEE,
                ThreatResponse.reactToIntimidation(hunter(8, 0.5, 1), 1, 4, false, true),
                "manada de quatro contra um predador");
    }

    @Test
    void comparableHerbivoreOnlyDetersASatedPredatorWithACharge() {
        assertEquals(Reaction.ALERT,
                ThreatResponse.reactToIntimidation(hunter(8, 1.2, 1), 1, 1, false, false),
                "porte parecido, só bufando: o predador encara de volta");
        assertEquals(Reaction.RETREAT,
                ThreatResponse.reactToIntimidation(hunter(8, 1.2, 1), 1, 1, true, false),
                "saciado, a investida veio: sai andando");
        assertFalse(ThreatResponse.deters(1.2, true, true), "com fome ou caçando, segura a posição");
        assertFalse(ThreatResponse.deters(1.62, true, true), "três Alossauros famintos contra dois mamutes");
    }

    @Test
    void aStrongPackIsNotDeterredByASingleHerbivore() {
        // Elasmotério contra quatro Velociraptores: ~5,9 ÷ 4.
        double power = ThreatResponse.confrontationPower(5.9, 1, 4);
        assertFalse(ThreatResponse.deters(power, false, false));
        assertFalse(ThreatResponse.deters(power, true, true));
        assertTrue(ThreatResponse.deters(power, true, false));
        assertFalse(ThreatResponse.deters(ThreatResponse.confrontationPower(5.9, 1, 3), false, true),
                "três ainda seguram o bufo");
        assertTrue(ThreatResponse.deters(ThreatResponse.confrontationPower(5.9, 1, 2), false, true),
                "dois, não");
    }
}
