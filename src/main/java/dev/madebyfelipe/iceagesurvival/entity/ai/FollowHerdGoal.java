package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.Comparator;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Mantém o bando junto: o líder é o membro do bando com o menor id, e os demais gravitam para perto
 * dele — voltam quando passam de pouco mais da metade do raio da manada e só param bem perto. O
 * líder é procurado até {@link PrehistoricCreature#GROUP_RANGE}, então quem se desgarrou numa fuga
 * ou numa briga volta para o próprio bando, não para o grupo da espécie que estiver mais perto.
 */
public class FollowHerdGoal extends Goal {
    /** A busca pelo líder varre entidades, então é feita só de vez em quando. */
    private static final int SEARCH_INTERVAL_TICKS = 40;
    private static final int SEARCH_JITTER_TICKS = 20;
    /** Fração do raio da manada a partir da qual volta para junto do líder. */
    private static final double RETURN_FRACTION = 0.6;
    /** Fração do raio em que para de seguir: bem perto do líder. */
    private static final double SETTLE_FRACTION = 0.3;
    private static final int REPATH_INTERVAL_TICKS = 20;

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
        double leave = herdRadius * RETURN_FRACTION;
        return leader != null && creature.distanceToSqr(leader) > leave * leave;
    }

    @Nullable
    private PrehistoricCreature findLeader() {
        return creature.groupMembers(PrehistoricCreature.GROUP_RANGE).stream()
                .min(Comparator.comparingInt(Entity::getId))
                .filter(found -> found.getId() < creature.getId())
                .orElse(null);
    }

    @Override
    public boolean canContinueToUse() {
        double close = herdRadius * SETTLE_FRACTION;
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
