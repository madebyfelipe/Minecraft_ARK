package dev.madebyfelipe.iceagesurvival.core.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.spawn.SpeciesSpacing.Claim;
import dev.madebyfelipe.iceagesurvival.core.spawn.SpeciesSpacing.Verdict;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpeciesSpacingTest {
    @Test
    void emptyWorldAcceptsANewGroup() {
        assertEquals(Verdict.NEW_GROUP, SpeciesSpacing.judge(List.of(), 0, 0, 300));
    }

    @Test
    void membersNextToTheirMarkBelongToTheGroup() {
        List<Claim> claims = List.of(new Claim(100, 100, 0));
        assertEquals(Verdict.SAME_GROUP, SpeciesSpacing.judge(claims, 104, 96, 300));
        assertEquals(Verdict.SAME_GROUP, SpeciesSpacing.judge(claims, 140, 140, 300), "manada espalhada");
    }

    @Test
    void anotherGroupWithinTheSpacingIsRefused() {
        List<Claim> claims = List.of(new Claim(0, 0, 0));
        assertEquals(Verdict.TOO_CLOSE, SpeciesSpacing.judge(claims, 150, 0, 300));
        assertEquals(Verdict.TOO_CLOSE, SpeciesSpacing.judge(claims, 0, 299, 300));
        assertEquals(Verdict.NEW_GROUP, SpeciesSpacing.judge(claims, 0, 300, 300));
        assertEquals(Verdict.NEW_GROUP, SpeciesSpacing.judge(claims, 250, 250, 300), "diagonal de 353 blocos");
    }

    @Test
    void theNearestMarkDecides() {
        List<Claim> claims = List.of(new Claim(0, 0, 0), new Claim(600, 0, 0));
        assertEquals(Verdict.SAME_GROUP, SpeciesSpacing.judge(claims, 590, 0, 300));
        assertEquals(Verdict.TOO_CLOSE, SpeciesSpacing.judge(claims, 300, 0, 400));
    }

    @Test
    void noSpacingNeverRefuses() {
        assertEquals(Verdict.NEW_GROUP, SpeciesSpacing.judge(List.of(new Claim(0, 0, 0)), 100, 0, 0));
    }

    @Test
    void marksExpireOnlyAfterTheGracePeriod() {
        Claim claim = new Claim(0, 0, 1000);
        assertFalse(SpeciesSpacing.expired(claim, 1000 + SpeciesSpacing.GRACE_TICKS));
        assertTrue(SpeciesSpacing.expired(claim, 1001 + SpeciesSpacing.GRACE_TICKS));
    }
}
