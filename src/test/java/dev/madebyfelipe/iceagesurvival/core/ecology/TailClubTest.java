package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A clava da cauda do Anquilossauro: golpeia atrás e no flanco (100° de arco), nunca pela frente; o corpo gira
 * devagar para pôr a cauda na ameaça; o duelo da mesma espécie não mata. Ângulo = {@code yRot} (0 olha +z, 90 olha −x).
 */
class TailClubTest {
    private static final double EPS = 1.0E-3;

    /** Direção (dx, dz) a {@code degrees} da linha de trás de quem olha +z (yaw 0), a {@code distance} blocos. */
    private static double[] fromRear(double degrees, double distance) {
        double rad = Math.toRadians(degrees);
        return new double[] {Math.sin(rad) * distance, -Math.cos(rad) * distance};
    }

    @Test
    void rightBehindIsZeroAndRightInFrontIs180() {
        assertEquals(0.0, TailClub.angleFromRear(0.0F, 0.0, -3.0), EPS, "olhando +z, atrás é −z");
        assertEquals(180.0, TailClub.angleFromRear(0.0F, 0.0, 3.0), EPS, "olhando +z, a frente é +z");
        assertEquals(0.0, TailClub.angleFromRear(90.0F, 3.0, 0.0), EPS, "olhando −x, atrás é +x");
        assertEquals(180.0, TailClub.angleFromRear(90.0F, -3.0, 0.0), EPS, "olhando −x, a frente é −x");
        assertEquals(90.0, TailClub.angleFromRear(0.0F, 3.0, 0.0), EPS, "de lado é 90°");
    }

    @Test
    void angleStaysBetween0And180() {
        for (int yaw = -540; yaw <= 540; yaw += 37) {
            for (int dir = 0; dir < 360; dir += 23) {
                double rad = Math.toRadians(dir);
                double angle = TailClub.angleFromRear(yaw, Math.cos(rad) * 5, Math.sin(rad) * 5);
                assertTrue(angle >= 0.0 && angle <= 180.0, "ângulo " + angle + " (yaw " + yaw + ", dir " + dir + ")");
            }
        }
    }

    @Test
    void flankAt45IsInsideTheArcAndAt60IsOutside() {
        double[] flank45 = fromRear(45, 3);
        double[] flank45Other = fromRear(-45, 3);
        double[] flank60 = fromRear(60, 3);
        assertEquals(45.0, TailClub.angleFromRear(0.0F, flank45[0], flank45[1]), EPS);
        assertTrue(TailClub.inRearArc(0.0F, flank45[0], flank45[1]), "flanco a 45° está no arco");
        assertTrue(TailClub.inRearArc(0.0F, flank45Other[0], flank45Other[1]), "o outro flanco a 45° também");
        assertFalse(TailClub.inRearArc(0.0F, flank60[0], flank60[1]), "flanco a 60° está fora do arco");
        assertTrue(TailClub.inRearArc(0.0F, 0.0, -2.0), "bem atrás está no arco");
        assertFalse(TailClub.inRearArc(0.0F, 0.0, 2.0), "à frente não está no arco");
        assertFalse(TailClub.inRearArc(0.0F, 2.0, 0.0), "de lado (90°) não está no arco");
    }

    @Test
    void theArcFollowsTheYaw() {
        // Olhando −x (yaw 90), atrás é +x: quem está em −z (atrás de quem olha +z) agora está de lado.
        assertTrue(TailClub.inRearArc(90.0F, 3.0, 0.5));
        assertFalse(TailClub.inRearArc(90.0F, 0.0, -3.0));
        // O yaw sem normalizar dá o mesmo resultado.
        assertEquals(TailClub.angleFromRear(90.0F, 3.0, 1.0), TailClub.angleFromRear(450.0F, 3.0, 1.0), EPS);
        assertEquals(TailClub.angleFromRear(90.0F, 3.0, 1.0), TailClub.angleFromRear(-270.0F, 3.0, 1.0), EPS);
    }

    @Test
    void strikesOnlyBehindAndWithinReach() {
        assertTrue(TailClub.strikes(0.0F, 0.0, -4.0, 4.0, 1.0), "atrás, a 4 blocos");
        assertTrue(TailClub.strikes(0.0F, 0.0, -4.5, TailClub.REACH, 1.0), "na borda do alcance");
        assertFalse(TailClub.strikes(0.0F, 0.0, -5.0, 4.6, 1.0), "além do alcance");
        assertFalse(TailClub.strikes(0.0F, 0.0, 1.5, 1.0, 1.0), "à frente, mesmo colado, não há golpe");
        double[] flank60 = fromRear(60, 2);
        assertFalse(TailClub.strikes(0.0F, flank60[0], flank60[1], 1.5, 1.0), "flanco fora do arco");
    }

    @Test
    void reachFactorShrinksTheReach() {
        // Agachado (sneak_factor 0,5): só a metade do alcance.
        assertTrue(TailClub.strikes(0.0F, 0.0, -2.0, 2.0, 0.5));
        assertFalse(TailClub.strikes(0.0F, 0.0, -3.0, 3.0, 0.5), "a 3 blocos, agachado, a cauda não chega");
        assertTrue(TailClub.strikes(0.0F, 0.0, -3.0, 3.0, 1.0), "em pé, chega");
    }

    @Test
    void yawTowardsFollowsTheMinecraftConvention() {
        assertEquals(0.0, wrap(TailClub.yawTowards(0.0, 1.0)), EPS, "+z");
        assertEquals(90.0, wrap(TailClub.yawTowards(-1.0, 0.0)), EPS, "−x");
        assertEquals(180.0, Math.abs(wrap(TailClub.yawTowards(0.0, -1.0))), EPS, "−z");
        assertEquals(-90.0, wrap(TailClub.yawTowards(1.0, 0.0)), EPS, "+x");
    }

    @Test
    void braceYawPutsTheTailOnTheTarget() {
        for (int dir = 0; dir < 360; dir += 15) {
            double rad = Math.toRadians(dir);
            double dx = Math.cos(rad) * 6;
            double dz = Math.sin(rad) * 6;
            float brace = TailClub.braceYaw(dx, dz);
            assertEquals(0.0, TailClub.angleFromRear(brace, dx, dz), EPS, "direção " + dir + "°");
            assertTrue(TailClub.inRearArc(brace, dx, dz));
        }
    }

    @Test
    void turnIsLimitedPerStep() {
        assertEquals(4.5, TailClub.turn(0.0F, 90.0F, TailClub.TURN_PER_TICK), EPS);
        assertEquals(-4.5, TailClub.turn(0.0F, -90.0F, TailClub.TURN_PER_TICK), EPS);
        assertEquals(3.0, TailClub.turn(0.0F, 3.0F, TailClub.TURN_PER_TICK), EPS, "perto, chega direto");
        assertEquals(5.5, TailClub.turn(10.0F, -100.0F, 4.5F), EPS);
    }

    @Test
    void turnTakesTheShortSideAcross180() {
        // De 170 para −170 são 20° pelo 180, não 340° pelo zero.
        assertEquals(174.5, TailClub.turn(170.0F, -170.0F, 4.5F), EPS);
        assertEquals(179.5, TailClub.turn(175.0F, -175.0F, 4.5F), EPS);
        assertEquals(-178.0, TailClub.turn(178.0F, -178.0F, 4.5F), EPS, "cruza o 180 e volta em (−180, 180]");
        assertEquals(178.0, TailClub.turn(-178.0F, 178.0F, 4.5F), EPS, "cruza o −180 pelo outro lado");
        assertEquals(-174.5, TailClub.turn(-170.0F, 170.0F, 4.5F), EPS);
    }

    @Test
    void turnResultIsAlwaysInRange() {
        for (int current = -720; current <= 720; current += 29) {
            for (int wanted = -720; wanted <= 720; wanted += 31) {
                float result = TailClub.turn(current, wanted, 4.5F);
                assertTrue(result > -180.0F && result <= 180.0F, "turn(" + current + ", " + wanted + ") = " + result);
            }
        }
    }

    @Test
    void repeatedTurnsReachTheBraceYaw() {
        float wanted = TailClub.braceYaw(0.0, 6.0); // a ameaça à frente: tem de dar meia-volta
        float yaw = 0.0F;
        int ticks = 0;
        while (TailClub.angleFromRear(yaw, 0.0, 6.0) > EPS && ticks < 100) {
            yaw = TailClub.turn(yaw, wanted, TailClub.TURN_PER_TICK);
            ticks++;
        }
        assertEquals(40, ticks, "meia-volta a 4,5°/tick leva 40 ticks");
    }

    @Test
    void duelDamageNeverTakesTheRivalBelowTheFloor() {
        float max = 380.0F;
        assertEquals(max - max * TailClub.DUEL_FLOOR, TailClub.duelDamage(1000.0F, max, max), EPS,
                "da vida cheia, no máximo até 40%");
        assertEquals(10.0F, TailClub.duelDamage(10.0F, max, max), EPS, "dano pequeno passa inteiro");
        assertEquals(8.0F, TailClub.duelDamage(50.0F, 160.0F, max), EPS, "perto do piso, só o que falta até ele");
    }

    @Test
    void duelDamageIsNeverNegative() {
        assertEquals(0.0F, TailClub.duelDamage(50.0F, 152.0F, 380.0F), EPS, "no piso, nada");
        assertEquals(0.0F, TailClub.duelDamage(50.0F, 100.0F, 380.0F), EPS, "abaixo do piso, nada (não negativo)");
        assertEquals(0.0F, TailClub.duelDamage(0.0F, 380.0F, 380.0F), EPS);
    }

    @Test
    void constantsMatchTheDesign() {
        assertEquals(50.0, TailClub.REAR_HALF_ARC, EPS);
        assertEquals(4.5, TailClub.REACH, EPS);
        assertEquals(30, TailClub.SWING_COOLDOWN_TICKS);
        assertEquals(4.5F, TailClub.TURN_PER_TICK, EPS);
        assertEquals(100, TailClub.LEG_BREAK_TICKS);
        assertEquals(0.6, TailClub.LEG_BREAK_SLOWDOWN, EPS);
        assertEquals(0.4, TailClub.DUEL_FLOOR, EPS);
    }

    private static double wrap(double degrees) {
        double w = degrees % 360.0;
        if (w > 180.0) {
            w -= 360.0;
        } else if (w <= -180.0) {
            w += 360.0;
        }
        return w;
    }
}
