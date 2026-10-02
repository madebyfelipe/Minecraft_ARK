package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TargetPriorityTest {
    /** Porte de um jogador diante de um T-Rex (colisão 0,6 × 1,8 contra 2,7 × 5,4). */
    private static final double PLAYER_VS_REX = ThreatResponse.sizeRatio(2.7, 5.4, 0.6, 1.8);

    @Test
    void aRivalRexOutranksTheProvokingPlayer() {
        double player = TargetPriority.danger(PLAYER_VS_REX, 1, true, 3.0);
        double rival = TargetPriority.danger(1.0, 1, true, 10.0);
        assertTrue(TargetPriority.outranks(rival, player), "rival " + rival + " × jogador " + player);
    }

    @Test
    void aPackOutranksASingleThreatOfTheSameSize() {
        assertTrue(TargetPriority.danger(1.0, 3, false, 10.0) > TargetPriority.danger(1.0, 1, false, 10.0));
    }

    @Test
    void closerAndAttackingIsMoreImmediate() {
        assertTrue(TargetPriority.danger(1.0, 1, false, 4.0) > TargetPriority.danger(1.0, 1, false, 20.0));
        assertTrue(TargetPriority.danger(1.0, 1, true, 10.0) > TargetPriority.danger(1.0, 1, false, 10.0));
    }

    @Test
    void aSmallDistantCreatureDoesNotOutrankThePlayerAttackingNow() {
        double player = TargetPriority.danger(1.0, 1, true, 2.0);
        double dodo = TargetPriority.danger(0.3, 1, false, 14.0);
        assertTrue(!TargetPriority.outranks(dodo, player));
    }
}
