package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Filhote selvagem anda colado num adulto da espécie (a mãe, na prática: o adulto mais perto).
 * O {@code FollowParentGoal} do vanilla não serve: ele compara a classe Java, e todas as espécies
 * terrestres são {@code LandCreature} — o filhote de Elasmotério seguiria um T-Rex.
 */
public class FollowMotherGoal extends Goal {
    private static final double SEARCH_RADIUS = 16.0;
    private static final double KEEP_WITHIN = 4.0;
    private static final double STOP_AT = 2.5;

    private final PrehistoricCreature calf;
    private final double speed;
    @Nullable
    private PrehistoricCreature mother;
    private int repath;

    public FollowMotherGoal(PrehistoricCreature calf, double speed) {
        this.calf = calf;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!calf.isBaby() || calf.isTame() || calf.getTarget() != null || calf.getRandom().nextInt(10) != 0) {
            return false;
        }
        mother = calf.level().getEntitiesOfClass(PrehistoricCreature.class, calf.getBoundingBox().inflate(SEARCH_RADIUS),
                        adult -> adult.getType() == calf.getType() && !adult.isBaby() && !adult.isTame() && adult.isAlive())
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(calf), b.distanceToSqr(calf))).orElse(null);
        return mother != null && calf.distanceTo(mother) > KEEP_WITHIN;
    }

    @Override
    public boolean canContinueToUse() {
        return mother != null && mother.isAlive() && calf.isBaby() && calf.distanceTo(mother) > STOP_AT
                && calf.distanceTo(mother) < SEARCH_RADIUS * 1.5;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        mother = null;
    }

    @Override
    public void tick() {
        if (--repath <= 0 && mother != null) {
            repath = 10;
            calf.getNavigation().moveTo(mother, speed);
        }
    }
}
