package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.core.ecology.Fishing;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.entity.CreatureAction;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.FishingProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/**
 * O pescador ({@code behavior.habits.fishing}, regras em {@link Fishing}): vai até a água rasa e aberta, fica parado
 * olhando a água (gesto {@link CreatureAction#FISH}) de 6 a 15 s e dá o bote ({@link CreatureAction#FISH_STRIKE}).
 *
 * <ul>
 *   <li><b>Selvagem</b> ({@code tame} falso): com fome e na hora dela, procura água pescável a até {@code radius};
 *   apanhou, come ({@link PrehistoricCreature#eatFish()}) e continua enquanto tiver fome. Depois de
 *   {@link #MAX_STRIKES} botes vazios, procura outro ponto.</li>
 *   <li><b>Domesticada</b> ({@code tame} verdadeiro): com a ordem "Parar", pesca na água a até {@code tame_radius} de
 *   onde parou e guarda cada peixe no inventário; para quando o inventário enche. Fica acima do {@code Stay}.</li>
 * </ul>
 */
public class FishingGoal extends Goal {
    private enum Phase { APPROACH, WAIT, STRIKE, EAT }

    private static final int SCAN_INTERVAL = 40;
    /** Colunas sorteadas por procura, no raio grande do selvagem. */
    private static final int SAMPLES = 64;
    /** Até este raio a procura passa por todas as colunas. */
    private static final double FULL_SCAN_RADIUS = 8.0;
    /** A procura olha da altura dos pés + {@value #SCAN_UP} até - {@value #SCAN_DOWN}. */
    private static final int SCAN_UP = 3;
    private static final int SCAN_DOWN = 6;
    /** Desiste de chegar à água em 20 s. */
    private static final int APPROACH_GIVE_UP = 400;
    /** Botes vazios seguidos no mesmo ponto antes de procurar outro. */
    public static final int MAX_STRIKES = 6;
    /** Caminhos conferidos por procura, do ponto mais perto para o mais longe. */
    private static final int PATH_CHECKS = 3;

    private final PrehistoricCreature creature;
    private final double speed;
    private final boolean tame;
    @Nullable
    private BlockPos spot;
    @Nullable
    private BlockPos anchor;
    private Phase phase = Phase.APPROACH;
    private int phaseTicks;
    private int waitTicks;
    private int strikes;
    private int scanCooldown;

    public FishingGoal(PrehistoricCreature creature, double speed, boolean tame) {
        this.creature = creature;
        this.speed = speed;
        this.tame = tame;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        scanCooldown = creature.getRandom().nextInt(SCAN_INTERVAL);
    }

    private boolean able() {
        if (creature.fishing().isEmpty() || creature.isUnconscious() || creature.isBaby() || creature.isVehicle()
                || creature.isPassenger() || creature.isLeashed() || creature.getTarget() != null) {
            return false;
        }
        if (tame) {
            return creature.isTame() && creature.movement() == Movement.STAY
                    && Fishing.tameKeepsFishing(creature.hasRoomForFish());
        }
        return !creature.isTame() && creature.yieldingFrom() == null && !creature.restsNow()
                && creature.hungerDrive() != Hunger.Drive.SATED;
    }

    @Override
    public boolean canUse() {
        if (!able()) {
            if (tame && creature.movement() != Movement.STAY) {
                anchor = null;
            }
            return false;
        }
        if (--scanCooldown > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL / 2 + creature.getRandom().nextInt(SCAN_INTERVAL);
        FishingProfile profile = creature.fishing().orElseThrow();
        BlockPos center = creature.blockPosition();
        double radius = profile.radius();
        if (tame) {
            // Mandada parar aqui: pesca em volta deste lugar. Se a levaram para longe (montada, laçada), o lugar muda.
            if (anchor == null || !anchor.closerThan(center, profile.tameRadius() * 2.0 + 4.0)) {
                anchor = center;
            }
            center = anchor;
            radius = profile.tameRadius();
        }
        spot = findSpot(center, radius);
        return spot != null;
    }

    @Override
    public boolean canContinueToUse() {
        return spot != null && able() && fishable(creature.level(), spot)
                && !(phase == Phase.APPROACH && phaseTicks > APPROACH_GIVE_UP);
    }

    @Override
    public void start() {
        phase = Phase.APPROACH;
        phaseTicks = 0;
        strikes = 0;
        moveToSpot();
    }

    @Override
    public void stop() {
        creature.stopAction(CreatureAction.FISH);
        creature.getNavigation().stop();
        spot = null;
        scanCooldown = SCAN_INTERVAL;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (spot == null) {
            return;
        }
        phaseTicks++;
        if (phase != Phase.APPROACH) {
            creature.getNavigation().stop();
            creature.getLookControl().setLookAt(spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5);
        }
        switch (phase) {
            case APPROACH -> {
                if (arrived()) {
                    beginWait();
                } else if (creature.getNavigation().isDone() || phaseTicks % 40 == 0) {
                    moveToSpot();
                }
            }
            case WAIT -> {
                if (phaseTicks >= waitTicks) {
                    creature.beginFishingStrike();
                    setPhase(Phase.STRIKE);
                }
            }
            case STRIKE -> {
                if (phaseTicks >= Fishing.STRIKE_TICKS) {
                    strikes++;
                    if (creature.resolveFishingStrike(spot)) {
                        strikes = 0;
                        setPhase(Phase.EAT);
                    } else if (strikes >= MAX_STRIKES) {
                        spot = null; // nada aqui: procura outro ponto
                    } else {
                        beginWait();
                    }
                }
            }
            case EAT -> {
                if (phaseTicks >= (tame ? Fishing.STRIKE_TICKS : Fishing.EAT_TICKS)) {
                    beginWait();
                }
            }
        }
    }

    private void setPhase(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private void beginWait() {
        setPhase(Phase.WAIT);
        waitTicks = Fishing.waitTicks(creature.getRandom().nextDouble());
        creature.getNavigation().stop();
        creature.holdAction(CreatureAction.FISH);
    }

    private void moveToSpot() {
        creature.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, speed);
    }

    private boolean arrived() {
        double dx = creature.getX() - (spot.getX() + 0.5);
        double dz = creature.getZ() - (spot.getZ() + 0.5);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return horizontal <= Fishing.ARRIVE_DISTANCE + creature.getBbWidth() * 0.5
                || creature.getNavigation().isDone() && horizontal <= Fishing.ARRIVE_DISTANCE + creature.getBbWidth();
    }

    /** O ponto de pesca mais perto da criatura, alcançável, a até {@code radius} de {@code center}. */
    @Nullable
    private BlockPos findSpot(BlockPos center, double radius) {
        Level level = creature.level();
        List<BlockPos> found = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        if (radius <= FULL_SCAN_RADIUS) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        addSurface(level, center.offset(dx, 0, dz), found);
                    }
                }
            }
        } else {
            var random = creature.getRandom();
            for (int i = 0; i < SAMPLES; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = Math.sqrt(random.nextDouble()) * radius;
                addSurface(level, center.offset((int) Math.round(Math.cos(angle) * distance), 0,
                        (int) Math.round(Math.sin(angle) * distance)), found);
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(creature.blockPosition())));
        for (int i = 0; i < Math.min(PATH_CHECKS, found.size()); i++) {
            BlockPos pos = found.get(i);
            if (arrivedAt(pos)) {
                return pos;
            }
            Path path = creature.getNavigation().createPath(pos, 1);
            if (path != null && path.canReach()) {
                return pos;
            }
        }
        return null;
    }

    private boolean arrivedAt(BlockPos pos) {
        double dx = creature.getX() - (pos.getX() + 0.5);
        double dz = creature.getZ() - (pos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz) <= Fishing.ARRIVE_DISTANCE + creature.getBbWidth() * 0.5;
    }

    /** A superfície da água nesta coluna, perto da altura de {@code column}, se for pescável. */
    private static void addSurface(Level level, BlockPos column, List<BlockPos> out) {
        for (int dy = SCAN_UP; dy >= -SCAN_DOWN; dy--) {
            BlockPos pos = column.above(dy);
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                if (fishable(level, pos)) {
                    out.add(pos.immutable());
                }
                return; // a primeira água de cima para baixo é a superfície (ou água coberta)
            }
        }
    }

    /**
     * Se dá para pescar neste bloco de água: ar logo acima (gelo ou qualquer bloco por cima, não) e de 1 a
     * {@link Fishing#MAX_DEPTH} blocos de água até o fundo ({@link Fishing#fishable}).
     */
    public static boolean fishable(Level level, BlockPos water) {
        if (!level.getFluidState(water).is(FluidTags.WATER)) {
            return false;
        }
        BlockPos above = water.above();
        boolean open = level.getBlockState(above).isAir() && level.getFluidState(above).isEmpty();
        return Fishing.fishable(depth(level, water), open);
    }

    /** Blocos de água de {@code water} para baixo, contados até {@link Fishing#MAX_DEPTH} + 1. */
    public static int depth(Level level, BlockPos water) {
        int depth = 0;
        BlockPos pos = water;
        while (depth <= Fishing.MAX_DEPTH && level.getFluidState(pos).is(FluidTags.WATER)) {
            depth++;
            pos = pos.below();
        }
        return depth;
    }

    /** O ponto de pesca escolhido agora; nulo se não está pescando. */
    @Nullable
    public BlockPos spot() {
        return spot;
    }
}
