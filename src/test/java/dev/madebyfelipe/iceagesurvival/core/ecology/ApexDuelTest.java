package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
