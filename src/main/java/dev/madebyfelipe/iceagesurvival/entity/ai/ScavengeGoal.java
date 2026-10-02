package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Carrion;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * O necrófago ({@code behavior.habits.scavenges}): com fome, vai até a carne crua largada no chão (tag
 * {@code iceagesurvival:carrion}) e come um pedaço — a sobra da caçada de outro predador ou do jogador. Não chega perto
 * se há um predador maior a menos de {@link Carrion#SAFE_RADIUS} da carne.
 */
public class ScavengeGoal extends Goal {
    private static final int SCAN_INTERVAL = 40;
    /** Desiste da carne que não alcança neste tempo. */
    private static final int GIVE_UP_TICKS = 300;

    private final PrehistoricCreature creature;
    private final double speed;
    @Nullable
    private ItemEntity meat;
    private int ticks;
    /** Espera até a próxima procura (contador, pelo mesmo motivo do {@link RestGoal}). */
    private int scanCooldown;

    public ScavengeGoal(PrehistoricCreature creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
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
        scanCooldown = SCAN_INTERVAL / 2;
        meat = nearestSafeMeat();
        return meat != null;
    }

    @Override
    public boolean canContinueToUse() {
        return meat != null && meat.isAlive() && ticks < GIVE_UP_TICKS && able() && safe(meat);
    }

    @Override
    public void start() {
        ticks = 0;
        creature.getNavigation().moveTo(meat, speed);
    }

    @Override
    public void tick() {
        ticks++;
        if (meat == null) {
            return;
        }
        creature.getLookControl().setLookAt(meat);
        if (creature.distanceTo(meat) <= Carrion.REACH) {
            meat.getItem().shrink(1);
            if (meat.getItem().isEmpty()) {
                meat.discard();
            }
            creature.eatCarrion();
            meat = null;
        } else if (ticks % 20 == 0) {
            creature.getNavigation().moveTo(meat, speed);
        }
    }

    @Override
    public void stop() {
        meat = null;
        creature.getNavigation().stop();
    }

    @Nullable
    private ItemEntity nearestSafeMeat() {
        ItemEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity item : creature.level().getEntitiesOfClass(ItemEntity.class,
                creature.getBoundingBox().inflate(Carrion.SEARCH_RADIUS, 6.0, Carrion.SEARCH_RADIUS),
                item -> item.isAlive() && item.getItem().is(ModTags.CARRION))) {
            double distance = creature.distanceTo(item);
            if (distance < bestDistance && safe(item)) {
                bestDistance = distance;
                best = item;
            }
        }
        return best;
    }

    /** Sem predador selvagem maior perto da carne. */
    private boolean safe(ItemEntity item) {
        double nearest = Double.POSITIVE_INFINITY;
        for (LivingEntity predator : creature.level().getEntitiesOfClass(LivingEntity.class,
                item.getBoundingBox().inflate(Carrion.SAFE_RADIUS),
                other -> other != creature && other.isAlive() && other.getType().is(ModTags.PREDATORS)
                        && !(other instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null)
                        && creature.sizeRatioOf(other) > 1.0)) {
            nearest = Math.min(nearest, predator.distanceTo(item));
        }
        return Carrion.safe(nearest);
    }
}
