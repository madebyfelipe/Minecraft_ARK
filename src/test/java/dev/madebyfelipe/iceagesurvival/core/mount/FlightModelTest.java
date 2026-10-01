package dev.madebyfelipe.iceagesurvival.core.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlightModelTest {
    private static final FlightModel.Tuning TUNING = FlightModel.Tuning.forMaxSpeed(0.8);
    private static final FlightModel.Input FORWARD = new FlightModel.Input(1, 0, false, false);
    private static final FlightModel.Input NOTHING = new FlightModel.Input(0, 0, false, false);

    private static double accelerate(double speed, FlightModel.Input input, int ticks) {
        for (int i = 0; i < ticks; i++) {
            speed = FlightModel.nextSpeed(speed, input, TUNING);
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
        double after = accelerate(boosted, FORWARD, 200);
        assertEquals(0.8, after, 1e-9);
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
    void landsOnlyOnTheGroundAndNotWhileClimbing() {
        assertTrue(FlightModel.shouldLand(true, false, 10));
        assertFalse(FlightModel.shouldLand(false, false, 10));
        assertFalse(FlightModel.shouldLand(true, true, 10));
        assertFalse(FlightModel.shouldLand(true, false, -40), "olhando para cima decola de novo, não pousa");
    }
}
