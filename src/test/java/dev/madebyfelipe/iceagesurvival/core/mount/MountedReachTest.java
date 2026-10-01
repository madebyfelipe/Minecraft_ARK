package dev.madebyfelipe.iceagesurvival.core.mount;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MountedReachTest {
    @Test
    void biggerMountsReachFarther() {
        double smilodon = MountedReach.reach(1.3);
        double rex = MountedReach.reach(2.7);
        assertTrue(smilodon >= 5.0, "Smilodon alcança " + smilodon);
        assertTrue(rex >= 6.8, "T-Rex alcança " + rex);
        assertTrue(MountedReach.breakDepth(2.7) >= 4.5, "T-Rex quebra pouco à frente");
    }

    @Test
    void biteConeCoversTheFrontAndSidesButNotBehind() {
        assertTrue(MountedReach.inBiteCone(0, 1, 0, 5));
        assertTrue(MountedReach.inBiteCone(0, 1, 3, 2), "diagonal à frente");
        assertFalse(MountedReach.inBiteCone(0, 1, 0, -5), "atrás");
        assertFalse(MountedReach.inBiteCone(0, 1, 5, -0.5), "de lado, um pouco para trás");
        assertTrue(MountedReach.inBiteCone(0, 1, 0, 0), "em cima");
    }
}
