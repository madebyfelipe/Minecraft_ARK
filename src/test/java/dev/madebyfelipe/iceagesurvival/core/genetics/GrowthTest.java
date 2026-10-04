package dev.madebyfelipe.iceagesurvival.core.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrowthTest {
    @Test
    void scaleGrowsLinearlyFromFortyPercent() {
        assertEquals(0.4F, Growth.scale(0.0F), 1e-6);
        assertEquals(0.7F, Growth.scale(0.5F), 1e-6);
        assertEquals(1.0F, Growth.scale(1.0F), 1e-6);
        assertEquals(0.4F, Growth.scale(-1.0F), 1e-6);
        assertEquals(1.0F, Growth.scale(2.0F), 1e-6);
    }

    @Test
    void juvenileLookOnlyInFirstHalf() {
        assertTrue(Growth.juvenileLook(0.0F));
        assertTrue(Growth.juvenileLook(0.49F));
        assertFalse(Growth.juvenileLook(0.5F));
        assertFalse(Growth.juvenileLook(1.0F));
    }

    @Test
    void progressFollowsVanillaAge() {
        assertEquals(0.0F, Growth.progress(-2400, 2400), 1e-6);
        assertEquals(0.75F, Growth.progress(-600, 2400), 1e-6);
        assertEquals(1.0F, Growth.progress(0, 2400), 1e-6);
        assertEquals(1.0F, Growth.progress(100, 2400), 1e-6);
        assertEquals(1.0F, Growth.progress(-10, 0), 1e-6);
        assertEquals(0.0F, Growth.progress(-5000, 2400), 1e-6);
    }
}
