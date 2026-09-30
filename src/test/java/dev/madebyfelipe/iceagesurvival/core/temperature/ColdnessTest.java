package dev.madebyfelipe.iceagesurvival.core.temperature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColdnessTest {
    private static final ColdTuning TUNING = new ColdTuning(0.002, 0.2, 0.25, 0.25, 0.8, 0.3);

    /** Temperaturas base reais do Minecraft, para os casos serem os do jogo. */
    private static final double PLAINS = 0.8;
    private static final double TAIGA = 0.25;
    private static final double SNOWY_PLAINS = 0.0;
    private static final double SNOWY_TAIGA = -0.5;

    private static ColdReading at(double biomeTemperature) {
        return new ColdReading(biomeTemperature, 0, false, false, false, 0.0, 0.0, false);
    }

    @Test
    void warmBiomesDoNotChillAtAll() {
        assertEquals(0.0, Coldness.chill(at(PLAINS), TUNING), 1e-9);
        assertEquals(0.0, Coldness.chill(at(Coldness.COMFORT_TEMPERATURE), TUNING), 1e-9);
    }

    @Test
    void chillGrowsAsTheBiomeGetsColder() {
        assertEquals(0.25, Coldness.chill(at(TAIGA), TUNING), 1e-9);
        assertEquals(0.5, Coldness.chill(at(SNOWY_PLAINS), TUNING), 1e-9);
        assertEquals(1.0, Coldness.chill(at(SNOWY_TAIGA), TUNING), 1e-9);
    }

    @Test
    void nightAndStormBiteMuchLessInAWarmBiome() {
        // Porque derrubam a temperatura do lugar em vez de somar frio: numa planície ainda sobra
        // folga até o conforto, numa planície nevada não.
        ColdReading warmNight = new ColdReading(PLAINS, 0, true, true, false, 0.0, 0.0, false);
        ColdReading coldNight = new ColdReading(SNOWY_PLAINS, 0, true, true, false, 0.0, 0.0, false);
        assertEquals(0.15, Coldness.chill(warmNight, TUNING), 1e-9);
        assertEquals(0.95, Coldness.chill(coldNight, TUNING), 1e-9);
    }

    @Test
    void aClearDayInAWarmBiomeChillsNothing() {
        ColdReading day = new ColdReading(PLAINS, 0, false, false, false, 0.0, 0.0, false);
        assertEquals(0.0, Coldness.chill(day, TUNING), 1e-9);
    }

    @Test
    void altitudeChillsAndDepthDoesNot() {
        ColdReading peak = new ColdReading(SNOWY_PLAINS, 150, false, false, false, 0.0, 0.0, false);
        assertEquals(0.8, Coldness.chill(peak, TUNING), 1e-9);

        ColdReading cave = new ColdReading(SNOWY_PLAINS, -50, false, false, false, 0.0, 0.0, false);
        assertEquals(Coldness.chill(at(SNOWY_PLAINS), TUNING), Coldness.chill(cave, TUNING), 1e-9,
                "descer não pode esquentar por conta da altitude");
    }

    @Test
    void warmthAddsUpFromClothesShelterAndFire() {
        ColdReading bare = new ColdReading(SNOWY_TAIGA, 0, false, false, false, 0.0, 0.0, false);
        assertEquals(0.0, Coldness.warmth(bare, TUNING), 1e-9);

        ColdReading dressed = new ColdReading(SNOWY_TAIGA, 0, false, false, false, 0.0, 0.8, false);
        assertEquals(0.8, Coldness.warmth(dressed, TUNING), 1e-9);

        ColdReading camp = new ColdReading(SNOWY_TAIGA, 0, false, false, true, 1.0, 0.8, false);
        assertEquals(1.85, Coldness.warmth(camp, TUNING), 1e-9);
    }

    @Test
    void farHeatWarmsLessThanCloseHeat() {
        ColdReading close = new ColdReading(SNOWY_TAIGA, 0, false, false, false, 1.0, 0.0, false);
        ColdReading far = new ColdReading(SNOWY_TAIGA, 0, false, false, false, 0.25, 0.0, false);
        assertTrue(Coldness.warmth(close, TUNING) > Coldness.warmth(far, TUNING));
        assertTrue(Coldness.warmth(far, TUNING) > 0.0);
    }

    @Test
    void severityIsChillMinusWarmth() {
        ColdReading dressedInSnow = new ColdReading(SNOWY_PLAINS, 0, false, false, false, 0.0, 0.4, false);
        assertEquals(0.1, Coldness.severity(dressedInSnow, TUNING), 1e-9);
    }

    @Test
    void severityStaysWithinMinusOneAndOne() {
        ColdReading deadly = new ColdReading(-20.0, 300, true, true, false, 0.0, 0.0, true);
        assertEquals(1.0, Coldness.severity(deadly, TUNING), 1e-9);

        ColdReading cozy = new ColdReading(PLAINS, 0, false, false, true, 1.0, 0.8, false);
        assertEquals(-1.0, Coldness.severity(cozy, TUNING), 1e-9);
    }

    @Test
    void theConfiguredTimeToFreezeIsTheTimeItTakes() {
        int seconds = 120;
        double exposure = 0.0;
        for (int tick = 0; tick < seconds * 20; tick++) {
            exposure += Coldness.exposurePerTick(1.0, seconds);
        }
        assertEquals(1.0, exposure, 1e-9);
    }

    @Test
    void warmingIsAsFastAsFreezing() {
        assertEquals(-Coldness.exposurePerTick(1.0, 60), Coldness.exposurePerTick(-1.0, 60), 1e-12);
    }

    @Test
    void frozenTicksNeverReachTheVanillaMaximum() {
        // Chegar ao máximo entregaria o dano do congelamento ao vanilla, que o soma ao nosso.
        assertEquals(139, Coldness.frozenTicks(1.0, 140));
        assertEquals(139, Coldness.frozenTicks(3.0, 140));
        assertEquals(0, Coldness.frozenTicks(0.0, 140));
        assertEquals(0, Coldness.frozenTicks(-1.0, 140));
        assertEquals(69, Coldness.frozenTicks(0.5, 140));
    }

    @Test
    void aFireTurnsASnowyNightAround() {
        ColdReading exposed = new ColdReading(SNOWY_PLAINS, 0, true, false, false, 0.0, 0.0, false);
        assertTrue(Coldness.severity(exposed, TUNING) > 0, "noite nevada a céu aberto deveria esfriar");

        ColdReading byTheFire = new ColdReading(SNOWY_PLAINS, 0, true, false, true, 1.0, 0.0, false);
        assertTrue(Coldness.severity(byTheFire, TUNING) < 0, "abrigo com fogo deveria aquecer");
    }

    @Test
    void wetClothesChillMore() {
        ColdReading dry = new ColdReading(TAIGA, 0, false, false, false, 0.0, 0.0, false);
        ColdReading wet = new ColdReading(TAIGA, 0, false, false, false, 0.0, 0.0, true);
        assertEquals(0.55, Coldness.chill(wet, TUNING), 1e-9);
        assertTrue(Coldness.chill(wet, TUNING) > Coldness.chill(dry, TUNING));
    }

    @Test
    void furKeepsASnowyTaigaDayBearable() {
        // Pele completa (4 × 0,35) cobre o frio máximo; couro completo (4 × 0,2) não.
        ColdReading fur = new ColdReading(SNOWY_TAIGA, 0, false, false, false, 0.0, 1.4, false);
        ColdReading leather = new ColdReading(SNOWY_TAIGA, 0, false, false, false, 0.0, 0.8, false);
        assertTrue(Coldness.severity(fur, TUNING) <= 0);
        assertTrue(Coldness.severity(leather, TUNING) > 0);
    }

    @Test
    void thermometerReadsBodyTemperatureAndTrend() {
        assertEquals(37.0, Coldness.bodyTemperature(0.0), 1e-9);
        assertEquals(30.0, Coldness.bodyTemperature(1.0), 1e-9);
        assertEquals(Coldness.Trend.FALLING_FAST, Coldness.trend(0.3, 0.9));
        assertEquals(Coldness.Trend.FALLING, Coldness.trend(0.3, 0.2));
        assertEquals(Coldness.Trend.RISING, Coldness.trend(0.3, -0.2));
        assertEquals(Coldness.Trend.STABLE, Coldness.trend(0.0, -0.8), "já aquecido não sobe mais");
        assertEquals(Coldness.Trend.STABLE, Coldness.trend(1.0, 0.8), "já congelado não cai mais");
    }
}
