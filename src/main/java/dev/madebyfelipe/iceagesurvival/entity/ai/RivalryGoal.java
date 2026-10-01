package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Disputa de território entre predadores rivais (tag {@code rivals}): o T-Rex não tolera o
 * Alossauro por perto, o Smilodon e o lobo-terrível brigam pela mesma caça.
 *
 * <p>Ver o rival estressa. O mais forte ({@link PrehistoricCreature#dominance()}) ruge e vai para
 * cima — mais provável quanto mais estressado ou faminto estiver; o mais fraco que é encarado
 * desiste e vai embora, ou, encurralado e estressado, revida. A briga para quando um dos dois
 * cai à metade da vida ({@link PrehistoricCreature#yieldTo}): rival não briga até a morte.
 */
public class RivalryGoal extends Goal {
    private static final int SCAN_INTERVAL = 60;
    /** Chance de desafiar, por varredura, um rival mais fraco no raio, quando calmo. */
    private static final double BASE_CHALLENGE_CHANCE = 0.25;

    private final PrehistoricCreature creature;
    private int scanCooldown;
    @Nullable
    private PrehistoricCreature rival;

    public RivalryGoal(PrehistoricCreature creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.TARGET));
        scanCooldown = creature.getRandom().nextInt(SCAN_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (creature.isTame() || creature.isBaby() || creature.isUnconscious() || creature.isVehicle()
                || creature.getTarget() != null || creature.yieldingFrom() != null || --scanCooldown > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL + creature.getRandom().nextInt(20);
        double radius = creature.ecology().rivalRadius();
        PrehistoricCreature nearest = null;
        double best = Double.MAX_VALUE;
        for (PrehistoricCreature other : creature.level().getEntitiesOfClass(PrehistoricCreature.class,
                creature.getBoundingBox().inflate(radius, 8.0, radius),
                other -> creature.isRival(other) && !other.isUnconscious() && other.yieldingFrom() == null)) {
            double distance = creature.distanceTo(other);
            if (distance < best) {
                best = distance;
                nearest = other;
            }
        }
        if (nearest == null) {
            return false;
        }
        // Rival no território incomoda, por segundo de varredura.
        creature.addStress(Stress.Event.RIVAL_SEEN, SCAN_INTERVAL / 20.0);
        if (creature.dominance() < nearest.dominance()) {
            return false;
        }
        double chance = BASE_CHALLENGE_CHANCE * Stress.perceptionMultiplier(creature.stress())
                * (creature.isSated() ? 0.5 : 1.0);
        if (creature.getRandom().nextDouble() >= chance) {
            return false;
        }
        rival = nearest;
        return true;
    }

    @Override
    public void start() {
        creature.threatDisplay();
        creature.setTarget(rival);
        rival.challengedBy(creature);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void stop() {
        rival = null;
    }
}
