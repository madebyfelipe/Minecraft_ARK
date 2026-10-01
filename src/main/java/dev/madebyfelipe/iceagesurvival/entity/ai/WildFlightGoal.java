package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Voo da criatura selvagem de uma espécie voadora (o Pteranodonte): de tempos em tempos decola,
 * plana em pontos sorteados a 10–26 blocos do chão e depois pousa. Decola na hora quando é ferida,
 * quando cai na água ou quando está no ar sem voar (carregada no meio do voo).
 *
 * <p>A velocidade é posta a cada tick, sem gravidade, como no voo montado; a navegação do chão fica
 * parada. Não é interrompível: no ar, nenhum goal de chão (fuga, manada) pode assumir o movimento.
 */
public class WildFlightGoal extends Goal {
    /** Chance por tick de decolar em repouso: em média a cada ~20 s no chão. */
    private static final int TAKEOFF_CHANCE = 400;
    private static final int MIN_FLIGHT_TICKS = 600;
    private static final int EXTRA_FLIGHT_TICKS = 1200;
    private static final int MIN_ALTITUDE = 10;
    private static final int EXTRA_ALTITUDE = 16;
    /** Altura mínima acima do chão em cruzeiro; abaixo dela o ponto de destino sobe. */
    private static final int FLOOR_CLEARANCE = 5;
    private static final int WANDER_RANGE = 48;
    /** Velocidade desejada em blocos/tick; o arrasto do ar deixa o cruzeiro em ~2/3 disso. */
    private static final double CRUISE_SPEED = 0.6;
    private static final double LANDING_SPEED = 0.3;
    /** Quanto da diferença para a velocidade desejada é corrigida por tick: curvas suaves. */
    private static final double STEERING = 0.15;
    private static final float MAX_TURN_DEGREES = 8.0F;

    private final PrehistoricCreature creature;
    private Vec3 waypoint = Vec3.ZERO;
    private int remainingTicks;
    private boolean landing;

    public WildFlightGoal(PrehistoricCreature creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!creature.canFlyWild()) {
            return false;
        }
        boolean airborne = !creature.onGround() && creature.fallDistance > 1.5F;
        boolean hurt = creature.hurtTime > 0 && creature.getLastHurtByMob() != null;
        return creature.isFlying() || airborne || hurt || creature.isInWater()
                || creature.getRandom().nextInt(TAKEOFF_CHANCE) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return creature.isFlying() && creature.canFlyWild();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        creature.getNavigation().stop();
        creature.setWildFlying(true);
        remainingTicks = MIN_FLIGHT_TICKS + creature.getRandom().nextInt(EXTRA_FLIGHT_TICKS);
        landing = false;
        Vec3 movement = creature.getDeltaMovement();
        creature.setDeltaMovement(movement.x, Math.max(movement.y, 0.5), movement.z);
        pickCruiseWaypoint();
    }

    @Override
    public void stop() {
        creature.setWildFlying(false);
    }

    @Override
    public void tick() {
        if (!landing && --remainingTicks <= 0) {
            landing = pickLandingWaypoint();
            if (!landing) {
                remainingTicks = 200;
            }
        }
        Vec3 position = creature.position();
        if (!landing) {
            double ground = groundY(position.x, position.z);
            if (position.distanceToSqr(waypoint) < 16.0 || creature.horizontalCollision) {
                pickCruiseWaypoint();
                if (creature.horizontalCollision) {
                    waypoint = waypoint.add(0.0, 6.0, 0.0);
                }
            } else if (position.y < ground + FLOOR_CLEARANCE && waypoint.y < ground + FLOOR_CLEARANCE * 2) {
                waypoint = new Vec3(waypoint.x, ground + MIN_ALTITUDE, waypoint.z);
            }
        } else if (creature.onGround()) {
            creature.setWildFlying(false);
            return;
        } else if (creature.isInWater()) {
            // Na água não pousa: sobe de novo e tenta outro lugar daqui a pouco.
            landing = false;
            remainingTicks = 200;
            pickCruiseWaypoint();
        }

        Vec3 toTarget = waypoint.subtract(position);
        double distance = toTarget.length();
        double speed = landing ? Math.min(LANDING_SPEED, Math.max(0.12, distance * 0.05)) : CRUISE_SPEED;
        Vec3 desired = distance > 1.0E-4 ? toTarget.scale(speed / distance) : Vec3.ZERO;
        Vec3 velocity = creature.getDeltaMovement().lerp(desired, STEERING);
        creature.setDeltaMovement(velocity);

        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal > 0.02) {
            float targetYaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
            float yaw = Mth.approachDegrees(creature.getYRot(), targetYaw, MAX_TURN_DEGREES);
            float pitch = (float) (-Mth.atan2(velocity.y, horizontal) * Mth.RAD_TO_DEG);
            creature.setYRot(yaw);
            creature.setXRot(Mth.clamp(pitch, -45.0F, 60.0F));
            creature.yBodyRot = creature.yHeadRot = yaw;
        }
        creature.resetFallDistance();
    }

    /** Um ponto no ar, em volta de onde está (ou do território, se tiver um). */
    private void pickCruiseWaypoint() {
        Vec3 center = creature.hasRestriction() ? Vec3.atCenterOf(creature.getRestrictCenter()) : creature.position();
        double angle = creature.getRandom().nextDouble() * Math.PI * 2.0;
        double radius = 12.0 + creature.getRandom().nextDouble() * (WANDER_RANGE - 12.0);
        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;
        double y = groundY(x, z) + MIN_ALTITUDE + creature.getRandom().nextInt(EXTRA_ALTITUDE + 1);
        waypoint = new Vec3(x, Math.min(y, creature.level().getMaxBuildHeight() - 4), z);
    }

    /**
     * Um ponto de pouso em terra firme perto de onde está.
     *
     * @return falso se só achou água; o voo continua um pouco e tenta de novo
     */
    private boolean pickLandingWaypoint() {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = creature.getX() + creature.getRandom().nextInt(33) - 16;
            double z = creature.getZ() + creature.getRandom().nextInt(33) - 16;
            BlockPos top = BlockPos.containing(x, groundY(x, z), z);
            if (creature.level().getFluidState(top).isEmpty() && creature.level().getFluidState(top.below()).isEmpty()) {
                waypoint = new Vec3(x, top.getY(), z);
                return true;
            }
        }
        return false;
    }

    private double groundY(double x, double z) {
        return creature.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }
}
