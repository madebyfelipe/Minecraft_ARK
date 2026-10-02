package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Território do bando, contra qualquer criatura do mod — herbívoros inclusive. Em volta de cada membro
 * selvagem há um raio de defesa ({@link PrehistoricCreature#defendRadius()}); quem entra é intruso.
 *
 * <ul>
 *   <li><b>Outro bando da mesma espécie:</b> guerra. Os dois bandos se atacam até um fugir (cada um
 *   foge no próprio {@code flee_health_fraction}) ou morrer.</li>
 *   <li><b>Outra espécie, mais fraca</b> (porte × quantos lutam juntos): o bando ameaça e investe; o
 *   intruso selvagem cede e vai embora, o domesticado é perseguido até sair do território.</li>
 *   <li><b>Outra espécie, mais forte:</b> quem cede é esta — entrar no território de um bando mais
 *   forte é desencorajado, e ela sai antes de apanhar.</li>
 * </ul>
 *
 * <p>O predador com fome diante da própria presa fica de fora: aí quem decide é a caça
 * ({@link HuntGoal}), não o território. O jogador também fica de fora: a reação a ele é a da
 * cautela de cada espécie ({@link WaryGoal}). Domesticadas montadas não contam como intrusas.
 */
public class TerritoryGoal extends Goal {
    private static final int SCAN_INTERVAL = 50;
    private static final double VERTICAL_REACH = 6.0;

    private final PrehistoricCreature creature;
    private int scanCooldown;
    @Nullable
    private PrehistoricCreature intruder;
    private boolean war;

    public TerritoryGoal(PrehistoricCreature creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.TARGET));
        scanCooldown = creature.getRandom().nextInt(SCAN_INTERVAL);
    }

    @Override
    public boolean canUse() {
        if (creature.isTame() || creature.isBaby() || creature.isUnconscious() || creature.isVehicle()
                || creature.getTarget() != null || creature.yieldingFrom() != null || creature.groupId() == null
                || --scanCooldown > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL + creature.getRandom().nextInt(20);
        PrehistoricCreature found = nearestIntruder();
        if (found == null) {
            return false;
        }
        war = isWar(found);
        if (war) {
            intruder = found;
            return true;
        }
        double intruderPower = ThreatResponse.confrontationPower(creature.sizeRatioOf(found),
                found.fightingGroup(), creature.fightingGroup());
        if (intruderPower < 1.0) {
            intruder = found;
            return true;
        }
        // O outro bando é mais forte: esta é a intrusa. Sai antes de apanhar.
        creature.yieldTo(found);
        return false;
    }

    @Nullable
    private PrehistoricCreature nearestIntruder() {
        double radius = creature.defendRadius();
        PrehistoricCreature nearest = null;
        double best = Double.MAX_VALUE;
        for (PrehistoricCreature other : creature.level().getEntitiesOfClass(PrehistoricCreature.class,
                creature.getBoundingBox().inflate(radius, VERTICAL_REACH, radius), this::isIntruder)) {
            double distance = creature.distanceToSqr(other);
            if (distance < best && distance <= radius * radius) {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }

    private boolean isIntruder(PrehistoricCreature other) {
        if (other == creature || !other.isAlive() || other.isUnconscious() || other.isBaby()
                || creature.sameGroup(other) || other.yieldingFrom() == creature
                || other.isTame() && other.isVehicle()) {
            return false;
        }
        if (!other.isTame() && other.groupId() == null) {
            return false; // ainda sem bando (acabou de nascer): espera a adoção
        }
        return !huntingGround(other);
    }

    /** Predador com fome e a própria presa: quem decide é a caça, dos dois lados. */
    private boolean huntingGround(PrehistoricCreature other) {
        return eats(creature, other) || eats(other, creature);
    }

    private static boolean eats(PrehistoricCreature hunter, PrehistoricCreature prey) {
        return !hunter.isTame() && hunter.hungerDrive() != Hunger.Drive.SATED
                && hunter.behavior().flatMap(BehaviorProfile::prey).map(prey.getType()::is).orElse(false);
    }

    private boolean isWar(PrehistoricCreature other) {
        return other.getType() == creature.getType() && !other.isTame();
    }

    @Override
    public void start() {
        if (intruder == null) {
            return;
        }
        creature.threatDisplay();
        creature.defendTerritoryAgainst(intruder);
        if (war) {
            // Guerra entre bandos: os dois lados entram na briga.
            creature.rallyGroupAgainst(intruder);
            intruder.defendTerritoryAgainst(creature);
            intruder.rallyGroupAgainst(creature);
        } else if (!intruder.isTame()) {
            intruder.yieldTo(creature);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void stop() {
        intruder = null;
    }
}
