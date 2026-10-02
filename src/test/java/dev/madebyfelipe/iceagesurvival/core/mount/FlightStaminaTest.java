package dev.madebyfelipe.iceagesurvival.core.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlightStaminaTest {
    @Test
    void climbingCostsDoubleAndGlidingAlmostNothing() {
        assertEquals(2.0, FlightStamina.drainRate(0.3), 1e-9);
        assertEquals(1.0, FlightStamina.drainRate(0.0), 1e-9);
        assertEquals(0.2, FlightStamina.drainRate(-0.3), 1e-9);
    }

    @Test
    void exhaustedAtZeroUntilTheTakeoffReserve() {
        assertTrue(FlightStamina.exhausted(false, 0.0F, 0.3F));
        assertTrue(FlightStamina.exhausted(true, 0.2F, 0.3F), "recuperando ainda não decola");
        assertFalse(FlightStamina.exhausted(true, 0.3F, 0.3F));
        assertFalse(FlightStamina.exhausted(false, 0.1F, 0.3F), "com pouco fôlego, mas sem ter esgotado, ainda voa");
    }
}
