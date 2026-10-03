package dev.madebyfelipe.iceagesurvival.core.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * O voo do planador de térmica (o Quetzalcoatlus): embala devagar, sobe na térmica sem bater as asas e gasta quase
 * nada planando, enquanto o padrão (o Pteranodonte) continua igual.
 */
class SoaringFlightTest {
    private static final FlightModel.Input FORWARD = new FlightModel.Input(1, 0, false, false);
    private static final FlightModel.Input NOTHING = new FlightModel.Input(0, 0, false, false);
    private static final FlightModel.Input FLAPPING = new FlightModel.Input(0, 0, true, false);
    /** Custos do Quetzalcoatlus: subir batendo as asas é caro, planar quase de graça. */
    private static final FlightStamina.Costs QUETZAL = new FlightStamina.Costs(3.0, 1.0, 0.05);

    private static int ticksToCruise(FlightModel.Tuning tuning) {
        double speed = 0.0;
        int ticks = 0;
        while (speed < tuning.maxSpeed() - 1e-9 && ticks < 10_000) {
            speed = FlightModel.nextSpeed(speed, 0.0F, FORWARD, tuning);
            ticks++;
        }
        return ticks;
    }

    @Test
    void theDefaultTuningKeepsThePteranodonAcceleration() {
        FlightModel.Tuning before = FlightModel.Tuning.forMaxSpeed(0.85, 120.0);
        FlightModel.Tuning explicit = FlightModel.Tuning.forMaxSpeed(0.85, 120.0, FlightModel.DEFAULT_ACCELERATION_TICKS);
        assertEquals(before, explicit);
        assertEquals(0.85 / 25.0, before.acceleration(), 1e-12);
        assertEquals(25, ticksToCruise(before));
    }

    @Test
    void aSlowAcceleratorTakesItsSecondsToReachCruise() {
        FlightModel.Tuning quetzal = FlightModel.Tuning.forMaxSpeed(1.1, 50.0, 80.0);
        assertEquals(80, ticksToCruise(quetzal));
        assertTrue(ticksToCruise(quetzal) > ticksToCruise(FlightModel.Tuning.forMaxSpeed(0.85, 120.0)),
                "o Quetzalcoatlus deveria embalar mais devagar que o Pteranodonte");
    }

    @Test
    void aThermalLiftsTheGliderWithoutFlapping() {
        FlightModel.Tuning tuning = FlightModel.Tuning.forMaxSpeed(1.1, 50.0, 80.0);
        FlightModel.Velocity still = FlightModel.velocity(1.1, 0, 0, NOTHING, tuning);
        FlightModel.Velocity soaring = FlightModel.velocity(1.1, 0, 0, NOTHING, tuning, 0.06);
        assertEquals(0.0, still.y(), 1e-9);
        assertEquals(0.06, soaring.y(), 1e-9);
        assertEquals(still.x(), soaring.x(), 1e-12);
        assertEquals(still.z(), soaring.z(), 1e-12);
        // Sem térmica, a sobrecarga é a mesma de antes.
        assertEquals(FlightModel.velocity(0.2, 0, 0, FLAPPING, tuning).y(),
                FlightModel.velocity(0.2, 0, 0, FLAPPING, tuning, 0.0).y(), 1e-12);
    }

    @Test
    void thermalsOnlyByDayWithoutRainOverLand() {
        assertTrue(Thermals.active(true, false, false));
        assertTrue(!Thermals.active(false, false, false), "à noite não há térmica");
        assertTrue(!Thermals.active(true, true, false), "na chuva não há térmica");
        assertTrue(!Thermals.active(true, false, true), "sobre a água não há térmica");
    }

    @Test
    void theThermalFadesToItsCeiling() {
        assertEquals(0.06, Thermals.lift(0.06, 0.0, 48.0), 1e-12);
        assertEquals(0.03, Thermals.lift(0.06, 24.0, 48.0), 1e-12);
        assertEquals(0.0, Thermals.lift(0.06, 48.0, 48.0), 1e-12);
        assertEquals(0.0, Thermals.lift(0.06, 90.0, 48.0), 1e-12);
        assertEquals(0.0, Thermals.lift(0.0, 5.0, 48.0), 1e-12, "espécie sem térmica");
    }

    @Test
    void theDefaultCostsAreThePteranodons() {
        FlightStamina.Costs costs = FlightStamina.Costs.DEFAULT;
        for (double dy : new double[] {0.3, 0.021, 0.0, -0.019, -0.3}) {
            assertEquals(FlightStamina.drainRate(dy), FlightStamina.drainRate(dy, 0.0, costs), 1e-12);
        }
        assertEquals(2.0, costs.climb(), 1e-12);
        assertEquals(1.0, costs.cruise(), 1e-12);
        assertEquals(0.2, costs.glide(), 1e-12);
    }

    @Test
    void glidingCostsFarLessThanFlapping() {
        double flapping = FlightStamina.drainRate(0.3, 0.0, QUETZAL);
        double gliding = FlightStamina.drainRate(-0.1, 0.0, QUETZAL);
        assertEquals(3.0, flapping, 1e-12);
        assertEquals(0.05, gliding, 1e-12);
        assertTrue(gliding < FlightStamina.drainRate(-0.1), "planar deveria custar menos que no Pteranodonte");
    }

    @Test
    void risingOnAThermalCostsTheGlideNotTheClimb() {
        // Sobe 0,06 por tick com a térmica de 0,06: é a térmica que sobe, não as asas.
        assertEquals(0.05, FlightStamina.drainRate(0.06, 0.06, QUETZAL), 1e-12);
        // Nivelada dentro da térmica também é planar.
        assertEquals(0.05, FlightStamina.drainRate(0.0, 0.06, QUETZAL), 1e-12);
        // Subindo além da térmica, as asas batem.
        assertEquals(3.0, FlightStamina.drainRate(0.3, 0.06, QUETZAL), 1e-12);
        // Fora da térmica, subir 0,06 por tick é bater as asas.
        assertEquals(3.0, FlightStamina.drainRate(0.06, 0.0, QUETZAL), 1e-12);
    }
}
