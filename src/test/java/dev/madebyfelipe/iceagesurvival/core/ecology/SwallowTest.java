package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** O Quetzalcoatlus (2,2 × 4,5) engole inteira a presa pequena e só bica a maior. */
class SwallowTest {
    private static double ratio(double width, double height) {
        return ThreatResponse.sizeRatio(2.2, 4.5, width, height);
    }

    @Test
    void smallPreyFitsInTheBeak() {
        assertTrue(Swallow.swallows(ratio(0.7, 0.9), false), "dodô");
        assertTrue(Swallow.swallows(ratio(0.7, 1.1), false), "Velociraptor e Ornitholestes");
        assertTrue(Swallow.swallows(ratio(0.4, 0.5), false), "coelho");
        assertTrue(Swallow.swallows(ratio(0.4, 0.7), false), "galinha");
        assertTrue(Swallow.swallows(ratio(0.5, 0.5), false), "sapo");
        assertTrue(Swallow.swallows(ratio(0.7, 0.4), false), "salmão");
    }

    @Test
    void biggerPreyOnlyGetsThePeck() {
        assertFalse(Swallow.swallows(ratio(1.2, 2.3), false), "Galimimo");
        assertFalse(Swallow.swallows(ratio(0.9, 0.9), false), "porco");
        assertFalse(Swallow.swallows(ratio(0.9, 1.3), false), "ovelha");
        assertFalse(Swallow.swallows(ratio(1.6, 2.0), false), "Pteranodonte");
    }

    @Test
    void neverAYoungOne() {
        assertFalse(Swallow.swallows(ratio(0.35, 0.45), true), "filhote de dodô");
    }
}
