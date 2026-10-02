package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.Foe;
import dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.Verdict;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApexDuelTest {
    private final UUID me = UUID.randomUUID();
    private final ApexDuel duel = new ApexDuel(me);

    @Test
    void theChallengerMayStrike() {
        assertEquals(Verdict.OK, duel.hitBy(me, null, null));
    }

    @Test
    void anotherPlayerCancels() {
        assertEquals(Verdict.CANCEL, duel.hitBy(UUID.randomUUID(), null, null));
    }

    @Test
    void upToThreeOfMyCreaturesThenAFourthCancels() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertEquals(Verdict.OK, duel.hitBy(null, a, me));
        assertEquals(Verdict.OK, duel.hitBy(null, b, me));
        assertEquals(Verdict.OK, duel.hitBy(null, a, me), "a mesma criatura de novo não conta duas vezes");
        assertEquals(Verdict.OK, duel.hitBy(null, c, me));
        assertEquals(Verdict.CANCEL, duel.hitBy(null, UUID.randomUUID(), me));
    }

    @Test
    void someoneElsesOrAWildCreatureCancels() {
        assertEquals(Verdict.CANCEL, duel.hitBy(null, UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(Verdict.CANCEL, new ApexDuel(me).hitBy(null, UUID.randomUUID(), null));
    }

    @Test
    void theEnvironmentDoesNotCount() {
        assertEquals(Verdict.OK, duel.hitBy(null, null, null));
    }

    @Test
    void theApexRoarsForFiveSecondsBeforeFighting() {
        for (int tick = 1; tick < ApexDuel.ROAR_TICKS; tick++) {
            assertTrue(duel.tickRoar(), "parou de rugir no tick " + tick);
        }
        assertFalse(duel.tickRoar());
        assertFalse(duel.roaring());
        assertEquals(100, ApexDuel.ROAR_TICKS, "a janela pedida é de 5 s");
    }

    @Test
    void itGoesForTheCreaturesUnlessTheChallengerIsAloneOrStrikes() {
        assertEquals(Foe.CREATURES, ApexDuel.foe(true, false));
        assertEquals(Foe.CHALLENGER, ApexDuel.foe(false, false));
        assertEquals(Foe.CHALLENGER, ApexDuel.foe(true, true));
    }
}
