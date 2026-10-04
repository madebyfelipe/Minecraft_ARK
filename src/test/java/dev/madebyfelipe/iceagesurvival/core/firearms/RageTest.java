package dev.madebyfelipe.iceagesurvival.core.firearms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RageTest {
    @Test
    void manyHitsInAShortTimeEnrage() {
        Rage rage = new Rage();
        for (int i = 0; i < Rage.THRESHOLD - 1; i++) {
            assertFalse(rage.hit(i * 2, true));
        }
        assertFalse(rage.enraged(10));
        assertTrue(rage.hit(10, true));
        assertTrue(rage.enraged(11));
        assertTrue(rage.enraged(10 + Rage.DURATION_TICKS - 1));
        assertFalse(rage.enraged(10 + Rage.DURATION_TICKS));
    }

    @Test
    void spacedShotsNeverEnrage() {
        Rage rage = new Rage();
        // Um tiro por segundo (o ritmo do canhão sólido): a janela de 3 s nunca junta 6.
        for (int i = 0; i < 40; i++) {
            assertFalse(rage.hit(i * 20L, true));
        }
    }

    @Test
    void theAntiTankRifleDoesNotProvoke() {
        Rage rage = new Rage();
        for (int i = 0; i < 20; i++) {
            assertFalse(rage.hit(i, false));
        }
        assertFalse(rage.enraged(20));
    }

    @Test
    void hitsDuringRageDoNotExtendIt() {
        Rage rage = new Rage();
        for (int i = 0; i < Rage.THRESHOLD; i++) {
            rage.hit(i, true);
        }
        long until = rage.enragedUntil();
        for (int i = 0; i < 30; i++) {
            assertFalse(rage.hit(10 + i, true));
        }
        assertTrue(until == rage.enragedUntil());
        // Acabada a fúria, recomeça do zero: um acerto só não enfurece de novo.
        assertFalse(rage.hit(until + 1, true));
        assertFalse(rage.enraged(until + 2));
    }

    @Test
    void idleAfterTheWindowPasses() {
        Rage rage = new Rage();
        rage.hit(0, true);
        assertFalse(rage.idle(10));
        assertTrue(rage.idle(Rage.WINDOW_TICKS));
    }
}
