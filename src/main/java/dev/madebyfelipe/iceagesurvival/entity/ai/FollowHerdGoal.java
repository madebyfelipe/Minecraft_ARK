package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.Comparator;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Mantém uma criatura selvagem perto da manada. O líder é o indivíduo da mesma espécie com
 * o menor id por perto; os demais voltam para junto dele quando se afastam demais. Não há
 * estado compartilhado: cada membro resolve sozinho quem é o líder.
 */
public class FollowHerdGoal extends Goal {
    /** A busca pelo líder varre entidades, então é feita só de vez em quando. */
    private static final int SEARCH_INTERVAL_TICKS = 100;
    private static final int SEARCH_JITTER_TICKS = 40;
    private static final int REPATH_INTERVAL_TICKS = 20;
    /** O líder é procurado neste múltiplo do raio da manada. */
    private static final double SEARCH_RANGE_FACTOR = 3.0;

    private final PrehistoricCreature creature;
    private final double speedModifier;
    private final double herdRadius;
    @Nullable
    private PrehistoricCreature leader;
    private int nextSearchTick;
    private int repathCooldown;

    public FollowHerdGoal(PrehistoricCreature creature, double speedModifier, double herdRadius) {
        this.creature = creature;
        this.speedModifier = speedModifier;
        this.herdRadius = herdRadius;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (creature.isTame() || creature.tickCount < nextSearchTick) {
            return false;
        }
        nextSearchTick = creature.tickCount + SEARCH_INTERVAL_TICKS + creature.getRandom().nextInt(SEARCH_JITTER_TICKS);
        leader = findLeader();
        return leader != null && creature.distanceToSqr(leader) > herdRadius * herdRadius;
    }

    @Nullable
    private PrehistoricCreature findLeader() {
        double range = herdRadius * SEARCH_RANGE_FACTOR;
        return creature.level().getEntitiesOfClass(
                        PrehistoricCreature.class,
                        creature.getBoundingBox().inflate(range),
                        other -> other.getType() == creature.getType() && other.isAlive() && !other.isTame())
                .stream()
                .min(Comparator.comparingInt(Entity::getId))
                .filter(found -> found != creature)
                .orElse(null);
    }

    @Override
    public boolean canContinueToUse() {
        double close = herdRadius / 2;
        return leader != null && leader.isAlive() && !creature.isTame()
                && creature.distanceToSqr(leader) > close * close;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        if (leader != null && --repathCooldown <= 0) {
            repathCooldown = adjustedTickDelay(REPATH_INTERVAL_TICKS);
            creature.getNavigation().moveTo(leader, speedModifier);
        }
    }

    @Override
    public void stop() {
        leader = null;
        creature.getNavigation().stop();
    }
}
