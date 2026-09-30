package dev.madebyfelipe.iceagesurvival.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class CreatureOrderTest {
    @Test
    void cyclingVisitsEveryOrderAndReturnsToTheStart() {
        EnumSet<CreatureOrder> seen = EnumSet.noneOf(CreatureOrder.class);
        CreatureOrder order = CreatureOrder.FOLLOW;
        for (int i = 0; i < CreatureOrder.values().length; i++) {
            seen.add(order);
            order = order.next();
        }
        assertEquals(EnumSet.allOf(CreatureOrder.class), seen);
        assertEquals(CreatureOrder.FOLLOW, order);
    }

    @Test
    void idRoundTripsAndUnknownFallsBack() {
        for (CreatureOrder order : CreatureOrder.values()) {
            assertEquals(order, CreatureOrder.byId(order.id(), null));
        }
        assertEquals(CreatureOrder.DEFEND, CreatureOrder.byId("nonsense", CreatureOrder.DEFEND));
    }

    @Test
    void onlyDefendProtectsTheOwnerAndStayAndFleeNeverFight() {
        assertTrue(CreatureOrder.DEFEND.defendsOwner());
        assertFalse(CreatureOrder.FOLLOW.defendsOwner());
        assertFalse(CreatureOrder.STAY.fightsBack());
        assertFalse(CreatureOrder.FLEE.fightsBack());
        assertFalse(CreatureOrder.STAY.followsOwner());
        assertTrue(CreatureOrder.FLEE.followsOwner());
    }
}
