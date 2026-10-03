package dev.madebyfelipe.iceagesurvival.core.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Decolagem por salto: a velocidade do salto alcança a altura pedida, com a gravidade e o arrasto do vanilla. */
class LeapTakeoffTest {
    @Test
    void theLaunchSpeedReachesTheRequestedHeight() {
        for (double height : new double[] {1.0, 2.5, 4.0}) {
            double speed = LeapTakeoff.launchSpeed(height);
            assertEquals(height, LeapTakeoff.apexHeight(speed), 1e-6, "altura do salto de " + height);
        }
    }

    @Test
    void theQuetzalcoatlusVaultsAboutTwoAndAHalfBlocksInUnderASecond() {
        double speed = LeapTakeoff.launchSpeed(2.5);
        assertTrue(speed > 0.5 && speed < 0.75, "velocidade do salto: " + speed);
        int ticks = LeapTakeoff.leapTicks(speed);
        assertTrue(ticks >= 6 && ticks <= 12, "ticks do salto: " + ticks);
    }

    @Test
    void theApexMatchesAPlainVanillaJump() {
        // O pulo do jogador (0,42 blocos/tick) passa de pouco mais de 1 bloco.
        double height = LeapTakeoff.apexHeight(0.42);
        assertTrue(height > 1.1 && height < 1.35, "altura do pulo do jogador: " + height);
    }

    @Test
    void noLeapNoSpeed() {
        assertEquals(0.0, LeapTakeoff.launchSpeed(0.0), 0.0);
        assertEquals(0.0, LeapTakeoff.apexHeight(0.0), 0.0);
        assertEquals(0, LeapTakeoff.leapTicks(0.0));
    }
}
