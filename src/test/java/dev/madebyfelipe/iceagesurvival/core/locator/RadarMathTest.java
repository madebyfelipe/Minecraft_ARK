package dev.madebyfelipe.iceagesurvival.core.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RadarMathTest {
    @Test
    void targetAheadIsAtTheTop() {
        // Olhando para o sul (yaw 0), quem está ao sul (+Z) fica no topo do radar.
        assertEquals(0.0F, RadarMath.bearing(0, 10, 0.0F), 1e-4F);
        // Olhando para o norte, quem está ao norte (−Z) também.
        assertEquals(0.0F, RadarMath.bearing(0, -10, RadarMath.NORTH), 1e-4F);
    }

    @Test
    void rightHandSideIsClockwise() {
        // Olhando para o sul, o oeste (−X) fica à direita: 90° na tela.
        assertEquals(90.0F, RadarMath.bearing(-10, 0, 0.0F), 1e-4F);
        // E o leste (+X), à esquerda.
        assertEquals(-90.0F, RadarMath.bearing(10, 0, 0.0F), 1e-4F);
        // Atrás, embaixo.
        assertEquals(180.0F, Math.abs(RadarMath.bearing(0, -10, 0.0F)), 1e-4F);
    }

    @Test
    void cardinalsTurnWithTheView() {
        // Olhando para o leste, o norte fica à esquerda e o sul à direita.
        assertEquals(-90.0F, RadarMath.screenAngle(RadarMath.NORTH, RadarMath.EAST), 1e-4F);
        assertEquals(90.0F, RadarMath.screenAngle(RadarMath.SOUTH, RadarMath.EAST), 1e-4F);
        assertEquals(0.0F, RadarMath.screenAngle(RadarMath.EAST, RadarMath.EAST), 1e-4F);
        // O yaw do jogador não fica preso em ±180: dá voltas.
        assertEquals(0.0F, RadarMath.screenAngle(RadarMath.WEST, 90.0F + 720.0F), 1e-4F);
    }

    @Test
    void wrapStaysInHalfOpenRange() {
        assertEquals(180.0F, RadarMath.wrap(-180.0F), 1e-4F);
        assertEquals(180.0F, RadarMath.wrap(540.0F), 1e-4F);
        assertEquals(-170.0F, RadarMath.wrap(190.0F), 1e-4F);
        assertEquals(10.0F, RadarMath.wrap(-350.0F), 1e-4F);
    }

    @Test
    void radialGrowsFastNearbyAndStopsAtTheEdge() {
        assertEquals(0.0F, RadarMath.radial(0), 1e-6F);
        assertEquals(0.5F, RadarMath.radial(RadarMath.RANGE / 4), 1e-4F);
        assertEquals(1.0F, RadarMath.radial(RadarMath.RANGE), 1e-6F);
        assertEquals(1.0F, RadarMath.radial(RadarMath.RANGE * 10), 1e-6F);
        assertFalse(RadarMath.outOfRange(RadarMath.RANGE));
        assertTrue(RadarMath.outOfRange(RadarMath.RANGE + 1));
    }

    @Test
    void sweepLightsTheMarkerAndFadesUntilTheNextTurn() {
        assertEquals(1.0F, RadarMath.sweepGlow(40.0F, 40.0F), 1e-4F);
        // Logo depois de passar, ainda forte; meia volta depois, a um quarto.
        assertTrue(RadarMath.sweepGlow(50.0F, 40.0F) > 0.9F);
        assertEquals(0.25F, RadarMath.sweepGlow(220.0F, 40.0F), 1e-4F);
        // Ângulos negativos e a passagem pelo topo.
        assertEquals(RadarMath.sweepGlow(10.0F, -10.0F), RadarMath.sweepGlow(20.0F, 0.0F), 1e-4F);
        // Prestes a passar de novo, quase apagado.
        assertTrue(RadarMath.sweepGlow(39.0F, 40.0F) < 0.01F);
    }

    @Test
    void heightIgnoresSmallSteps() {
        assertEquals(0, RadarMath.heightDifference(1.4));
        assertEquals(0, RadarMath.heightDifference(-1.4));
        assertEquals(14, RadarMath.heightDifference(14.2));
        assertEquals(-30, RadarMath.heightDifference(-29.6));
    }
}
