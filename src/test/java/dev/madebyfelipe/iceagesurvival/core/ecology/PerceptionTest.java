package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerceptionTest {
    @Test
    void huntRadiusIsAlwaysLargerThanThePreyAlert() {
        // Faro de 40 diante de uma presa que alerta a 36: sobe para 36 + 8.
        assertEquals(44.0, Perception.huntRadius(40.0, 36.0), 1e-9);
        assertTrue(Perception.huntRadius(40.0, 36.0) > 36.0);
    }

    @Test
    void declaredRadiusAboveTheMinimumIsKept() {
        assertEquals(72.0, Perception.huntRadius(72.0, 36.0), 1e-9);
    }

    @Test
    void withoutWaryPreyTheDeclaredRadiusStands() {
        assertEquals(32.0, Perception.huntRadius(32.0, 0.0), 1e-9);
    }

    @Test
    void respectsNeedsTheFullMargin() {
        assertTrue(Perception.respects(44.0, 36.0));
        assertFalse(Perception.respects(43.9, 36.0));
    }

    @Test
    void aStalkerIsNoticedAtHalfTheAlert() {
        assertEquals(16.0, Perception.stalkerNoticeRadius(32.0), 1e-9);
    }
}
