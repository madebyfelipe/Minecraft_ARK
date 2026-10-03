package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CarcassTest {
    private static double volume(double width, double height) {
        return width * width * height;
    }

    @Test
    void aMealGrowsWithTheEater() {
        assertEquals(3, Carcass.mealPortions(volume(1.2, 2.3)), "Galimimo");
        assertEquals(8, Carcass.mealPortions(volume(2.7, 5.4)), "T-Rex");
        assertEquals(2, Carcass.mealPortions(volume(0.6, 0.9)), "Velociraptor: no mínimo 2");
        assertEquals(1.0 / 8, Carcass.portionFraction(volume(2.7, 5.4)), 1e-9);
    }

    @Test
    void theStrongerTakesOver() {
        assertTrue(Carcass.takesOver(1.5));
        assertFalse(Carcass.takesOver(1.0), "empate: quem já come fica");
        assertFalse(Carcass.takesOver(0.4));
    }

    @Test
    void butcheringGivesWhatIsLeft() {
        assertEquals(1.0, Carcass.butcherShare(30, 30), 1e-9);
        assertEquals(0.5, Carcass.butcherShare(3, 6), 1e-9);
        assertEquals(0.0, Carcass.butcherShare(0, 6), 1e-9);
        assertEquals(0.0, Carcass.butcherShare(3, 0), 1e-9, "não é carcaça");
    }
}
