package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Hábitos do Ornitholestes: camuflagem, horário, carniça, sentinela e o agarrão. */
class HabitsTest {
    // ---- Camuflagem ----

    @Test
    void twoCoverBlocksAreEnoughToHide() {
        assertFalse(Camouflage.hidden(0), "chão limpo não esconde");
        assertFalse(Camouflage.hidden(1), "uma folha só não esconde");
        assertTrue(Camouflage.hidden(2));
        assertTrue(Camouflage.hidden(10), "cercado de mato por todos os lados");
    }

    @Test
    void theHiddenCreatureIsNoticedAtAFractionOfTheRadius() {
        assertEquals(10.0, Camouflage.perceivedRadius(20.0, 0.5, true), 1e-9);
        assertEquals(20.0, Camouflage.perceivedRadius(20.0, 0.5, false), 1e-9, "à vista: o raio inteiro");
        assertEquals(20.0, Camouflage.perceivedRadius(20.0, Camouflage.NONE, true), 1e-9,
                "sem camuflagem, esconder-se não muda nada");
    }

    // ---- Horário ----

    @Test
    void theDayGoesFromSunriseToSunset() {
        assertTrue(Activity.isDaytime(0));
        assertTrue(Activity.isDaytime(6_000));
        assertTrue(Activity.isDaytime(11_999));
        assertFalse(Activity.isDaytime(12_000), "o pôr do sol já é noite");
        assertFalse(Activity.isDaytime(18_000));
        assertFalse(Activity.isDaytime(23_999));
    }

    @Test
    void theHourRepeatsEveryDay() {
        assertTrue(Activity.isDaytime(24_000 * 5 + 6_000), "meio-dia do sexto dia");
        assertFalse(Activity.isDaytime(24_000 * 3 + 18_000), "meia-noite do quarto dia");
    }

    @Test
    void theNocturnalSleepsByDayAndTheDiurnalByNight() {
        assertTrue(Activity.rests(Activity.Pattern.NOCTURNAL, 6_000));
        assertFalse(Activity.rests(Activity.Pattern.NOCTURNAL, 18_000));
        assertFalse(Activity.rests(Activity.Pattern.DIURNAL, 6_000));
        assertTrue(Activity.rests(Activity.Pattern.DIURNAL, 18_000));
    }

    @Test
    void whoIsAlwaysActiveNeverSleepsByTheClock() {
        assertFalse(Activity.rests(Activity.Pattern.ALWAYS, 6_000));
        assertFalse(Activity.rests(Activity.Pattern.ALWAYS, 18_000));
    }

    // ---- Carniça ----

    @Test
    void theScavengerOnlyEatsFarFromABiggerPredator() {
        assertTrue(Carrion.safe(Double.POSITIVE_INFINITY), "sem predador por perto");
        assertTrue(Carrion.safe(Carrion.SAFE_RADIUS), "no limite já é seguro");
        assertTrue(Carrion.safe(30.0));
        assertFalse(Carrion.safe(15.9));
        assertFalse(Carrion.safe(2.0), "o T-Rex em cima da carne");
    }

    @Test
    void aPieceOfMeatIsHalfAMealWithinReach() {
        assertEquals(0.5, Carrion.MEAL_FRACTION, 1e-9);
        assertTrue(Carrion.SEARCH_RADIUS > Carrion.SAFE_RADIUS, "procura mais longe do que o raio de perigo");
    }

    // ---- Sentinela ----

    @Test
    void theSentinelWarnsOnceAMinutePerPredator() {
        assertTrue(Sentinel.shouldWarn(Long.MIN_VALUE, 0), "nunca avisou");
        assertTrue(Sentinel.shouldWarn(Long.MIN_VALUE, 5_000_000L));
        assertFalse(Sentinel.shouldWarn(1_000, 1_000), "acabou de avisar");
        assertFalse(Sentinel.shouldWarn(1_000, 1_000 + Sentinel.COOLDOWN_TICKS - 1));
        assertTrue(Sentinel.shouldWarn(1_000, 1_000 + Sentinel.COOLDOWN_TICKS), "passou um minuto");
        assertEquals(1_200L, Sentinel.COOLDOWN_TICKS);
    }

    @Test
    void theEightBearingsFollowTheMinecraftMap() {
        // Norte é -Z, leste é +X.
        assertEquals(Sentinel.Direction.NORTH, Sentinel.direction(0, -10));
        assertEquals(Sentinel.Direction.NORTHEAST, Sentinel.direction(10, -10));
        assertEquals(Sentinel.Direction.EAST, Sentinel.direction(10, 0));
        assertEquals(Sentinel.Direction.SOUTHEAST, Sentinel.direction(10, 10));
        assertEquals(Sentinel.Direction.SOUTH, Sentinel.direction(0, 10));
        assertEquals(Sentinel.Direction.SOUTHWEST, Sentinel.direction(-10, 10));
        assertEquals(Sentinel.Direction.WEST, Sentinel.direction(-10, 0));
        assertEquals(Sentinel.Direction.NORTHWEST, Sentinel.direction(-10, -10));
    }

    @Test
    void aBearingCoversFortyFiveDegrees() {
        // 20° a leste do norte ainda é norte; 30° já é nordeste.
        double toRadians = Math.PI / 180.0;
        assertEquals(Sentinel.Direction.NORTH,
                Sentinel.direction(Math.sin(20 * toRadians), -Math.cos(20 * toRadians)));
        assertEquals(Sentinel.Direction.NORTHEAST,
                Sentinel.direction(Math.sin(30 * toRadians), -Math.cos(30 * toRadians)));
        assertEquals(Sentinel.Direction.NORTH,
                Sentinel.direction(-Math.sin(20 * toRadians), -Math.cos(20 * toRadians)), "20° a oeste do norte");
    }

    @Test
    void theBearingKeyIsLowercase() {
        assertEquals("north", Sentinel.Direction.NORTH.key());
        assertEquals("southwest", Sentinel.Direction.SOUTHWEST.key());
    }

    // ---- Agarrão ----

    @Test
    void theGrabHoldsPreyUpToItsOwnSize() {
        assertTrue(HuntSpecials.grabs(0.2), "coelho");
        assertTrue(HuntSpecials.grabs(1.0), "do mesmo porte");
        assertFalse(HuntSpecials.grabs(1.01), "maior escapa");
        assertFalse(HuntSpecials.grabs(4.0), "vaca");
    }
}
