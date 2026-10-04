package dev.madebyfelipe.iceagesurvival.core.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BonusPointsTest {
    @Test
    void spendingMovesOnePointFromTheBalanceToTheStat() {
        BonusPoints points = BonusPoints.NONE.grant(BonusPoints.INCUBATOR_GRANT)
                .spend(Stat.HEALTH, Stat.wildScalableStats())
                .spend(Stat.HEALTH, Stat.wildScalableStats())
                .spend(Stat.ARMOR, Stat.wildScalableStats());
        assertEquals(7, points.unspent());
        assertEquals(2, points.spent().get(Stat.HEALTH));
        assertEquals(1, points.spent().get(Stat.ARMOR));
        assertEquals(3, points.spent().total());
    }

    @Test
    void cannotSpendWithoutBalance() {
        assertFalse(BonusPoints.NONE.canSpend(Stat.ATTACK, Stat.wildScalableStats()));
        assertThrows(IllegalArgumentException.class, () -> BonusPoints.NONE.spend(Stat.ATTACK, Stat.wildScalableStats()));
    }

    @Test
    void speedAndFlightOfANonFlyerAreNotEligible() {
        BonusPoints points = BonusPoints.NONE.grant(1);
        assertFalse(points.canSpend(Stat.SPEED, Stat.wildScalableStats(true)));
        assertFalse(points.canSpend(Stat.FLIGHT_STAMINA, Stat.wildScalableStats(false)));
        assertEquals(1, points.spend(Stat.FLIGHT_STAMINA, Stat.wildScalableStats(true)).spent().get(Stat.FLIGHT_STAMINA));
    }

    @Test
    void plusAddsStatByStat() {
        StatPoints sum = StatPoints.NONE.with(Stat.HEALTH, 2).plus(StatPoints.NONE.with(Stat.HEALTH, 3).with(Stat.ARMOR, 1));
        assertEquals(5, sum.get(Stat.HEALTH));
        assertEquals(1, sum.get(Stat.ARMOR));
        assertEquals(7, sum.level());
    }
}
