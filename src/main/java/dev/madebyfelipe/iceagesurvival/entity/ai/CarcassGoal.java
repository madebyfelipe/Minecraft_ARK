package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Carcass;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.entity.CreatureAction;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Comer da carcaça ({@link Carcass}): o carnívoro ou o carniceiro com fome fareja a carcaça a
 * {@value Carcass#SMELL_RADIUS} blocos, vai até ela e come uma porção por bocada até matar a fome. Se quem está comendo
 * é mais forte (porte × grupo), espera a sobra a {@value Carcass#WAIT_DISTANCE} blocos; se é mais fraco, toma o lugar —
 * o outro vê e vai esperar.
 */
public class CarcassGoal extends Goal {
    private static final int SCAN_INTERVAL = 40;
    /** Perto assim da carcaça, come. */
    private static final double REACH = 2.5;
    /** Desiste da carcaça que não alcança neste tempo. */
    private static final int GIVE_UP_TICKS = 600;

    private final PrehistoricCreature creature;
    private final double speed;
    @Nullable
    private PrehistoricCreature carcass;
    private int ticks;
    private int biteCooldown;
    private int scanCooldown;
    private boolean waiting;

    public CarcassGoal(PrehistoricCreature creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        scanCooldown = creature.getRandom().nextInt(SCAN_INTERVAL);
    }

    private boolean able() {
        return !creature.isTame() && !creature.isBaby() && !creature.isUnconscious() && !creature.isVehicle()
                && creature.getTarget() == null && creature.yieldingFrom() == null && !creature.restsNow()
                && creature.hungerDrive() != Hunger.Drive.SATED;
    }

    @Override
    public boolean canUse() {
        if (--scanCooldown > 0 || !able()) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL;
        carcass = nearestCarcass();
        return carcass != null;
    }

    @Override
    public boolean canContinueToUse() {
        return carcass != null && carcass.hasCarcassMeat() && able() && (waiting || ticks < GIVE_UP_TICKS);
    }

    @Override
    public void start() {
        ticks = 0;
        biteCooldown = 0;
        waiting = false;
        creature.setFeedingOn(carcass);
        creature.getNavigation().moveTo(carcass, speed);
    }

    @Override
    public void stop() {
        creature.setFeedingOn(null);
        creature.getNavigation().stop();
        carcass = null;
        waiting = false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (carcass == null) {
            return;
        }
        ticks++;
        creature.getLookControl().setLookAt(carcass, 30.0F, 30.0F);
        PrehistoricCreature stronger = strongerFeeder();
        if (stronger != null) {
            // Quem está comendo é mais forte: espera a sobra, longe o bastante para não brigar.
            waiting = true;
            creature.setFeedingOn(null);
            if (creature.distanceTo(carcass) < Carcass.WAIT_DISTANCE - 2.0 || creature.getNavigation().isDone()) {
                Vec3 away = DefaultRandomPos.getPosAway(creature, (int) Carcass.WAIT_DISTANCE, 4, stronger.position());
                if (away != null && creature.distanceTo(carcass) < Carcass.WAIT_DISTANCE - 2.0) {
                    creature.getNavigation().moveTo(away.x, away.y, away.z, speed);
                }
            }
            return;
        }
        waiting = false;
        creature.setFeedingOn(carcass);
        if (gap() > REACH) {
            if (ticks % 20 == 1 || creature.getNavigation().isDone()) {
                creature.getNavigation().moveTo(carcass, speed);
            }
            return;
        }
        creature.getNavigation().stop();
        if (--biteCooldown <= 0) {
            biteCooldown = Carcass.BITE_INTERVAL_TICKS;
            if (carcass.biteCarcass()) {
                creature.eatCarcassPortion(Carcass.portionFraction(volume(creature)));
                creature.startAction(CreatureAction.EAT, Carcass.BITE_INTERVAL_TICKS);
            }
        }
    }

    /** Quem come desta carcaça agora e é mais forte que esta criatura (de outro grupo). */
    @Nullable
    private PrehistoricCreature strongerFeeder() {
        for (PrehistoricCreature other : creature.level().getEntitiesOfClass(PrehistoricCreature.class,
                carcass.getBoundingBox().inflate(REACH + 4.0), other -> other != creature
                        && other.feedingOn() == carcass && other.isAlive() && !other.isUnconscious()
                        && !creature.sameGroup(other))) {
            double power = ThreatResponse.confrontationPower(creature.sizeRatioOf(other), other.fightingGroup(),
                    creature.fightingGroup());
            if (Carcass.takesOver(power)) {
                return other;
            }
        }
        return null;
    }

    @Nullable
    private PrehistoricCreature nearestCarcass() {
        PrehistoricCreature nearest = null;
        double best = Double.MAX_VALUE;
        for (PrehistoricCreature candidate : creature.level().getEntitiesOfClass(PrehistoricCreature.class,
                creature.getBoundingBox().inflate(Carcass.SMELL_RADIUS, 16.0, Carcass.SMELL_RADIUS),
                PrehistoricCreature::hasCarcassMeat)) {
            if (candidate.getType() == creature.getType()) {
                continue; // não come o da própria espécie
            }
            double distance = creature.distanceToSqr(candidate);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private double gap() {
        return Math.max(0.0, creature.distanceTo(carcass) - (creature.getBbWidth() + carcass.getBbWidth()) / 2.0);
    }

    private static double volume(PrehistoricCreature creature) {
        return creature.getBbWidth() * creature.getBbWidth() * creature.getBbHeight();
    }
}
