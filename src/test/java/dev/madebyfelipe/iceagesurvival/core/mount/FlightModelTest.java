package dev.madebyfelipe.iceagesurvival.core.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlightModelTest {
    private static final FlightModel.Tuning TUNING = FlightModel.Tuning.forMaxSpeed(0.8, 120.0);
    private static final FlightModel.Input FORWARD = new FlightModel.Input(1, 0, false, false);
    private static final FlightModel.Input NOTHING = new FlightModel.Input(0, 0, false, false);

    private static double accelerate(double speed, FlightModel.Input input, int ticks) {
        for (int i = 0; i < ticks; i++) {
            speed = FlightModel.nextSpeed(speed, 0.0F, input, TUNING);
        }
        return speed;
    }

    @Test
    void forwardAcceleratesUpToCruiseSpeed() {
        double speed = accelerate(0, FORWARD, 200);
        assertEquals(0.8, speed, 1e-9);
    }

    @Test
    void boostRaisesTheCeilingAndBleedsBackWhenReleased() {
        FlightModel.Input boost = new FlightModel.Input(1, 0, false, true);
        double boosted = accelerate(0, boost, 200);
        assertEquals(1.2, boosted, 1e-9);
        double after = accelerate(boosted, FORWARD, 400);
        assertEquals(0.8, after, 1e-3);
    }

    @Test
    void withoutInputItGlidesAndKeepsMostMomentum() {
        double after = accelerate(0.8, NOTHING, 20);
        assertTrue(after > 0.7 && after < 0.8, "planando deveria perder pouco: " + after);
    }

    @Test
    void backBrakesToAStop() {
        double after = accelerate(0.8, new FlightModel.Input(-1, 0, false, false), 40);
        assertEquals(0.0, after, 1e-9);
    }

    @Test
    void flyingFollowsTheCamera() {
        // yaw 0 = +Z; olhando reto, toda a velocidade é horizontal.
        FlightModel.Velocity level = FlightModel.velocity(0.8, 0, 0, FORWARD, TUNING);
        assertEquals(0.8, level.z(), 1e-9);
        assertEquals(0.0, level.y(), 1e-9);
        // Olhar 30° para baixo mergulha; para cima, sobe.
        assertTrue(FlightModel.velocity(0.8, 0, 30, FORWARD, TUNING).y() < -0.3);
        assertTrue(FlightModel.velocity(0.8, 0, -30, FORWARD, TUNING).y() > 0.3);
        // yaw 90 = −X.
        assertTrue(FlightModel.velocity(0.8, 90, 0, FORWARD, TUNING).x() < -0.79);
    }

    @Test
    void tooSlowLosesLiftAndFlappingClimbs() {
        FlightModel.Velocity hovering = FlightModel.velocity(0, 0, 0, NOTHING, TUNING);
        assertEquals(-TUNING.sinkRate(), hovering.y(), 1e-9);
        assertEquals(0.0, FlightModel.sink(0.8, TUNING), 1e-9);
        FlightModel.Velocity flapping = FlightModel.velocity(0, 0, 0, new FlightModel.Input(0, 0, true, false), TUNING);
        assertTrue(flapping.y() > 0, "bater as asas parado deveria subir");
    }

    @Test
    void landsOnlyOnTheGroundSlowAndNotWhileClimbing() {
        assertTrue(FlightModel.shouldLand(true, false, 10, 0.3, TUNING));
        assertFalse(FlightModel.shouldLand(false, false, 10, 0.3, TUNING));
        assertFalse(FlightModel.shouldLand(true, true, 10, 0.3, TUNING));
        assertFalse(FlightModel.shouldLand(true, false, -40, 0.3, TUNING), "olhando para cima decola de novo, não pousa");
        assertFalse(FlightModel.shouldLand(true, false, 10, 1.2, TUNING), "rápida, raspa o chão em vez de pousar");
    }

    @Test
    void divingTradesHeightForSpeedUpToTheCeiling() {
        double speed = 0.8;
        for (int i = 0; i < 400; i++) {
            speed = FlightModel.nextSpeed(speed, 70.0F, NOTHING, TUNING);
        }
        assertTrue(speed > 1.5, "mergulho deveria passar bem do cruzeiro: " + speed);
        assertTrue(speed <= TUNING.diveSpeed() + 1e-9, "passou do teto do mergulho: " + speed);
    }

    @Test
    void pullingOutOfADiveKeepsTheMomentumForAWhile() {
        // Rasante: saindo do mergulho a 1,7, nivelado, ainda passa do cruzeiro dois segundos depois.
        double speed = 1.7;
        for (int i = 0; i < 40; i++) {
            speed = FlightModel.nextSpeed(speed, 0.0F, FORWARD, TUNING);
        }
        assertTrue(speed > 1.0, "o embalo do mergulho sumiu rápido demais: " + speed);
        for (int i = 0; i < 400; i++) {
            speed = FlightModel.nextSpeed(speed, 0.0F, FORWARD, TUNING);
        }
        assertEquals(0.8, speed, 1e-3);
    }

    @Test
    void climbingCostsSpeed() {
        double speed = FlightModel.nextSpeed(0.8, -60.0F, NOTHING, TUNING);
        assertTrue(speed < 0.78, "subir íngreme deveria custar velocidade: " + speed);
    }

    @Test
    void turnsGraduallyAndWiderWhenFast() {
        // 120°/s = 6° por tick no cruzeiro; a câmera virou 90°, a montaria vira 6.
        assertEquals(6.0F, FlightModel.nextYaw(0.0F, 90.0F, 0.8, TUNING), 1e-4);
        assertEquals(3.0F, FlightModel.nextYaw(0.0F, 90.0F, 1.6, TUNING), 1e-4);
        // Pelo lado mais curto, atravessando ±180.
        assertEquals(-176.0F, FlightModel.nextYaw(178.0F, -170.0F, 0.8, TUNING), 1e-4);
        // Perto do alvo, chega nele sem passar.
        assertEquals(2.0F, FlightModel.nextYaw(0.0F, 2.0F, 0.8, TUNING), 1e-4);
    }

    @Test
    void pitchFollowsTheCameraGraduallyAndIsCapped() {
        assertEquals(4.5F, FlightModel.nextPitch(0.0F, 60.0F, TUNING), 1e-4);
        float pitch = 0.0F;
        for (int i = 0; i < 100; i++) {
            pitch = FlightModel.nextPitch(pitch, 90.0F, TUNING);
        }
        assertEquals(FlightModel.MAX_PITCH, pitch, 1e-4);
    }
}
