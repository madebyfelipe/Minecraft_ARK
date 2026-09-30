package dev.madebyfelipe.iceagesurvival.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WhistleTest {
    @Test
    void eachWhistleChangesExactlyOneAxis() {
        for (Whistle whistle : Whistle.values()) {
            assertTrue(whistle.movement().isPresent() ^ whistle.stance().isPresent(), whistle.name());
        }
    }

    @Test
    void everyMovementAndStanceHasAWhistle() {
        for (Movement movement : Movement.values()) {
            assertTrue(java.util.Arrays.stream(Whistle.values()).anyMatch(w -> w.movement().orElse(null) == movement));
        }
        for (Stance stance : Stance.values()) {
            assertTrue(java.util.Arrays.stream(Whistle.values()).anyMatch(w -> w.stance().orElse(null) == stance));
        }
    }

    @Test
    void idsRoundTrip() {
        for (Movement movement : Movement.values()) {
            assertEquals(movement, Movement.byId(movement.id(), null));
        }
        for (Stance stance : Stance.values()) {
            assertEquals(stance, Stance.byId(stance.id(), null));
        }
        assertEquals(Stance.DEFEND, Stance.byId("nonsense", Stance.DEFEND));
    }

    @Test
    void stanceFlags() {
        assertTrue(Stance.DEFEND.defendsOwner() && Stance.DEFEND.fightsBack());
        assertTrue(Stance.NEUTRAL.fightsBack());
        assertFalse(Stance.NEUTRAL.defendsOwner());
        assertFalse(Stance.PASSIVE.fightsBack() || Stance.PASSIVE.fleesWhenHurt());
        assertTrue(Stance.FLEE.fleesWhenHurt());
        assertFalse(Stance.FLEE.fightsBack());
    }
}
