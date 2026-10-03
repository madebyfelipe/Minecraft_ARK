package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Fishing;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.core.mount.LeapTakeoff;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Voo da criatura selvagem de uma espécie voadora: de tempos em tempos decola, plana em pontos sorteados a 10–26
 * blocos do chão e depois pousa. Decola na hora quando é ferida, quando cai na água ou quando está no ar sem voar
 * (carregada no meio do voo).
 *
 * <p>A velocidade é posta a cada tick, sem gravidade, como no voo montado; a navegação do chão fica
 * parada. Não é interrompível: no ar, nenhum goal de chão (fuga, manada) pode assumir o movimento.
 *
 * <p>O que muda com a espécie ({@code mount.flight} e {@code behavior}); sem nada disso, é o voo do Pteranodonte:
 * <ul>
 *   <li><b>Decolagem por salto</b> ({@code leap_height}): agacha e salta parada antes de bater as asas
 *   ({@link LeapTakeoff}); sem céu aberto acima, não decola — fica no chão, vulnerável. Pousa no campo aberto, onde
 *   consegue decolar de novo.</li>
 *   <li><b>Térmica</b> ({@code thermal_lift}): de dia, sem chuva e sobre terra, sobe em círculos sem bater as asas e
 *   depois plana até a próxima, como o condor.</li>
 *   <li><b>Caçador a pé</b> (com presa ou pesca): com fome, pousa logo para caçar e não decola à toa; saciado, volta a
 *   voar entre um campo e outro.</li>
 *   <li><b>Diurno</b> ({@code habits.activity}): à noite não decola, salvo fugindo, e pousa se anoitecer.</li>
 *   <li><b>Cautelosa</b> ({@code wariness}): estressada pela ameaça, foge para o ar.</li>
 * </ul>
 */
public class WildFlightGoal extends Goal {
    /** Chance por tick de decolar em repouso: em média a cada ~20 s no chão. */
    private static final int TAKEOFF_CHANCE = 400;
    /** Fôlego mínimo para decolar sem motivo. */
    private static final float REST_TAKEOFF_STAMINA = 0.6F;
    /** Com o fôlego abaixo disto, procura onde pousar. */
    private static final float LAND_STAMINA = 0.25F;
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
    /** Pousando, a esta altura do chão já recolhe as asas. */
    private static final double LANDING_DROP = 0.5;
    /** Quanto da diferença para a velocidade desejada é corrigida por tick: curvas suaves. */
    private static final double STEERING = 0.15;
    private static final float MAX_TURN_DEGREES = 8.0F;
    /** Fugindo, voa ao menos isto antes de pousar pela fome ou pela noite: 10 s. */
    private static final int ESCAPE_FLIGHT_TICKS = 200;
    /** Céu fechado acima: só confere de novo depois deste tempo. */
    private static final int SKY_RECHECK_TICKS = 20;
    /** Térmica: raio do círculo em que sobe e a velocidade em volta dele. */
    private static final double CIRCLE_RADIUS = 10.0;
    private static final double CIRCLE_SPEED = 0.4;
    /** Quanto tempo sobe em cada térmica: 10 a 20 s. */
    private static final int MIN_CIRCLE_TICKS = 200;
    private static final int EXTRA_CIRCLE_TICKS = 200;
    /** Abaixo desta subida (perto do teto da térmica) não vale circular. */
    private static final double MIN_CIRCLE_LIFT = 0.005;
    /** Planando de uma térmica à outra, desce este tanto por bloco andado. */
    private static final double GLIDE_SLOPE = 0.15;

    private final PrehistoricCreature creature;
    private Vec3 waypoint = Vec3.ZERO;
    private int remainingTicks;
    private boolean landing;
    /** Decolagem por salto: ticks agachada que faltam e a velocidade vertical do salto (0 = não está saltando). */
    private int crouchTicks;
    private double leapSpeed;
    /** Ticks de fuga que ainda faltam: até lá, nem a fome nem a noite a fazem pousar. */
    private int escapeTicks;
    /** Subindo numa térmica: em volta de {@link #circleCenter} por mais {@link #circleTicks}. */
    private boolean circling;
    private Vec3 circleCenter = Vec3.ZERO;
    private int circleTicks;
    /** Até este tick a decolagem por salto nem confere o céu (estava fechado). */
    private int skyBlockedUntil;

    public WildFlightGoal(PrehistoricCreature creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!creature.canFlyWild()) {
            return false;
        }
        if (creature.isFlightExhausted()) {
            return false; // sem fôlego: fica no chão até recuperar
        }
        boolean airborne = !creature.onGround() && creature.fallDistance > 1.5F;
        if (creature.isFlying() || airborne) {
            return true;
        }
        // Fugindo (ferida, ameaçada) ou na água, decola com o que tiver; à toa, só com fôlego de sobra — e nunca com
        // fome (caça a pé) nem na hora de dormir.
        boolean takeOff = escaping() || inDeepWater()
                || !keepsToTheGround() && creature.flightStaminaFraction() >= REST_TAKEOFF_STAMINA
                && creature.getRandom().nextInt(TAKEOFF_CHANCE) == 0;
        return takeOff && skyIsOpen();
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
        circling = false;
        escapeTicks = escaping() ? ESCAPE_FLIGHT_TICKS : 0;
        MountProfile.FlightStyle style = creature.flightStyle();
        if (style.leaps() && creature.onGround()) {
            // Decola parada: agacha e salta; as asas só batem no topo do salto.
            crouchTicks = LeapTakeoff.CROUCH_TICKS;
            leapSpeed = LeapTakeoff.launchSpeed(style.leapHeight());
            creature.setDeltaMovement(Vec3.ZERO);
        } else {
            crouchTicks = 0;
            leapSpeed = 0.0;
            Vec3 movement = creature.getDeltaMovement();
            creature.setDeltaMovement(movement.x, Math.max(movement.y, 0.5), movement.z);
        }
        pickCruiseWaypoint();
        if (crouchTicks == 0) {
            startCircling();
        }
    }

    @Override
    public void stop() {
        creature.setWildFlying(false);
        crouchTicks = 0;
        leapSpeed = 0.0;
        circling = false;
    }

    @Override
    public void tick() {
        if (crouchTicks > 0 || leapSpeed > 0.0) {
            tickLeap();
            return;
        }
        if (escapeTicks > 0) {
            escapeTicks--;
        }
        if (!landing && creature.flightStaminaFraction() < LAND_STAMINA) {
            remainingTicks = 0; // cansada: pousa antes de esgotar
        }
        if (!landing && wantsToLand()) {
            remainingTicks = 0; // com fome, pousa para caçar; anoiteceu, pousa para dormir
        }
        if (!landing && --remainingTicks <= 0) {
            circling = false;
            landing = pickLandingWaypoint();
            if (!landing) {
                remainingTicks = 200;
            }
        }
        Vec3 position = creature.position();
        if (!landing) {
            if (circling) {
                tickCircling();
                return;
            }
            double ground = groundY(position.x, position.z);
            if (position.distanceToSqr(waypoint) < 16.0 || creature.horizontalCollision) {
                if (!creature.horizontalCollision && startCircling()) {
                    tickCircling();
                    return;
                }
                pickCruiseWaypoint();
                if (creature.horizontalCollision) {
                    waypoint = waypoint.add(0.0, 6.0, 0.0);
                }
            } else if (position.y < ground + FLOOR_CLEARANCE && waypoint.y < ground + FLOOR_CLEARANCE * 2) {
                waypoint = new Vec3(waypoint.x, ground + MIN_ALTITUDE, waypoint.z);
            }
        } else if (creature.onGround() || justAboveDryGround(position)) {
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
        steer(desired);
    }

    /** Leva a velocidade em direção à desejada, com curva suave, e vira o corpo para onde vai. */
    private void steer(Vec3 desired) {
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

    // ---- Decolagem por salto ----

    /**
     * Agachada, parada; depois o salto vertical, sem avanço. No topo (ou batendo a cabeça), começa o voo: bate as asas
     * até o primeiro ponto do cruzeiro, e só lá em cima procura a térmica.
     */
    private void tickLeap() {
        creature.getNavigation().stop();
        creature.resetFallDistance();
        if (crouchTicks > 0) {
            crouchTicks--;
            creature.setDeltaMovement(0.0, Math.min(0.0, creature.getDeltaMovement().y), 0.0);
            return;
        }
        if (creature.verticalCollision && !creature.onGround()) {
            leapSpeed = 0.0; // bateu a cabeça: o salto acaba ali
        }
        if (leapSpeed > 0.0) {
            creature.setDeltaMovement(0.0, leapSpeed, 0.0);
            leapSpeed = LeapTakeoff.nextSpeed(leapSpeed);
        }
        if (leapSpeed < 0.0) {
            leapSpeed = 0.0;
        }
    }

    // ---- Térmica ----

    /**
     * Começa a subir em círculos, se a espécie usa térmicas e há uma aqui ({@link PrehistoricCreature#thermalLift()}).
     * O ponto em que está fica na borda do círculo, que gira para o lado de dentro da curva.
     */
    private boolean startCircling() {
        if (creature.flightStyle().thermalLift() <= 0.0 || creature.thermalLift() <= MIN_CIRCLE_LIFT) {
            return false;
        }
        Vec3 velocity = creature.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        Vec3 heading = horizontal > 0.02 ? new Vec3(velocity.x / horizontal, 0.0, velocity.z / horizontal)
                : Vec3.directionFromRotation(0.0F, creature.getYRot());
        // À esquerda do rumo: girando no sentido anti-horário visto de cima, o rumo já é a tangente.
        Vec3 left = new Vec3(heading.z, 0.0, -heading.x);
        circleCenter = creature.position().add(left.scale(CIRCLE_RADIUS));
        circleTicks = MIN_CIRCLE_TICKS + creature.getRandom().nextInt(EXTRA_CIRCLE_TICKS + 1);
        circling = true;
        return true;
    }

    /** Em volta do centro, na velocidade de circular, subindo o que a térmica dá — sem bater as asas. */
    private void tickCircling() {
        double lift = creature.thermalLift();
        if (--circleTicks <= 0 || lift <= MIN_CIRCLE_LIFT || creature.horizontalCollision) {
            // Acabou a térmica (ou o tempo nela): plana até a próxima.
            circling = false;
            pickCruiseWaypoint();
            steer(waypoint.subtract(creature.position()).normalize().scale(CRUISE_SPEED));
            return;
        }
        Vec3 position = creature.position();
        double dx = position.x - circleCenter.x;
        double dz = position.z - circleCenter.z;
        double radius = Math.sqrt(dx * dx + dz * dz);
        Vec3 desired;
        if (radius < 1.0E-3) {
            desired = new Vec3(CIRCLE_SPEED, lift, 0.0);
        } else {
            // A tangente do giro, mais uma correção para voltar ao raio.
            double tx = dz / radius;
            double tz = -dx / radius;
            double pull = (CIRCLE_RADIUS - radius) / radius * 0.1;
            desired = new Vec3(tx * CIRCLE_SPEED + dx * pull, lift, tz * CIRCLE_SPEED + dz * pull);
        }
        steer(desired);
    }

    // ---- Quando decolar e quando pousar ----

    /** Ferida agora, ou estressada pela ameaça (só quem tem cautela, {@code wariness}): foge para o ar. */
    private boolean escaping() {
        boolean hurt = creature.hurtTime > 0 && creature.getLastHurtByMob() != null;
        boolean threatened = creature.wariness().isPresent() && creature.mood().atLeast(Stress.Mood.STRESSED);
        return hurt || threatened;
    }

    /**
     * Caiu na água. O pescador ({@code habits.fishing}) vadeia a água rasa de propósito: só a água mais funda que a de
     * pesca o tira de lá.
     */
    private boolean inDeepWater() {
        if (!creature.isInWater()) {
            return false;
        }
        return creature.fishing().isEmpty() || creature.getFluidHeight(FluidTags.WATER) > Fishing.MAX_DEPTH + 0.5;
    }

    /** Fica no chão sem motivo para voar: caçador a pé com fome, ou na hora de dormir. */
    private boolean keepsToTheGround() {
        return creature.restsNow() || hungryHunter();
    }

    /** Pousa para caçar ou para dormir, passado o susto da fuga. */
    private boolean wantsToLand() {
        return escapeTicks <= 0 && keepsToTheGround() && !escaping();
    }

    /** Caçador a pé (tem presa ou pesca) e com fome. */
    private boolean hungryHunter() {
        boolean hunts = creature.behavior().flatMap(BehaviorProfile::prey).isPresent() || creature.fishing().isPresent();
        return hunts && creature.hungerDrive() != Hunger.Drive.SATED;
    }

    /** Quem decola por salto precisa de céu aberto ({@link PrehistoricCreature#hasOpenSkyForTakeoff()}). */
    private boolean skyIsOpen() {
        if (!creature.flightStyle().leaps()) {
            return true;
        }
        if (creature.tickCount < skyBlockedUntil) {
            return false;
        }
        if (creature.hasOpenSkyForTakeoff()) {
            return true;
        }
        skyBlockedUntil = creature.tickCount + SKY_RECHECK_TICKS;
        return false;
    }

    // ---- Pontos do voo ----

    /**
     * Um ponto no ar, em volta de onde está (ou do território, se tiver um). O planador de térmica, saindo de uma,
     * plana para baixo até a próxima em vez de subir batendo as asas.
     */
    private void pickCruiseWaypoint() {
        Vec3 center = creature.hasRestriction() ? Vec3.atCenterOf(creature.getRestrictCenter()) : creature.position();
        double angle = creature.getRandom().nextDouble() * Math.PI * 2.0;
        double radius = 12.0 + creature.getRandom().nextDouble() * (WANDER_RANGE - 12.0);
        double x = center.x + Math.cos(angle) * radius;
        double z = center.z + Math.sin(angle) * radius;
        double ground = groundY(x, z);
        double y = ground + MIN_ALTITUDE + creature.getRandom().nextInt(EXTRA_ALTITUDE + 1);
        if (creature.flightStyle().thermalLift() > 0.0 && creature.thermalLift() > MIN_CIRCLE_LIFT) {
            double run = Math.sqrt((x - creature.getX()) * (x - creature.getX()) + (z - creature.getZ()) * (z - creature.getZ()));
            y = Math.max(ground + MIN_ALTITUDE, Math.min(y, creature.getY() - run * GLIDE_SLOPE));
        }
        waypoint = new Vec3(x, Math.min(y, creature.level().getMaxBuildHeight() - 4), z);
    }

    /**
     * Um ponto de pouso em terra firme perto de onde está. Quem decola por salto só pousa no campo aberto, sem copa
     * por cima: debaixo das árvores não conseguiria decolar de novo.
     *
     * @return falso se só achou água; o voo continua um pouco e tenta de novo
     */
    private boolean pickLandingWaypoint() {
        boolean openField = creature.flightStyle().leaps();
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = creature.getX() + creature.getRandom().nextInt(33) - 16;
            double z = creature.getZ() + creature.getRandom().nextInt(33) - 16;
            BlockPos top = BlockPos.containing(x, groundY(x, z), z);
            if (creature.level().getFluidState(top).isEmpty() && creature.level().getFluidState(top.below()).isEmpty()
                    && (!openField || openGround(top))) {
                waypoint = new Vec3(x, top.getY(), z);
                return true;
            }
        }
        return false;
    }

    /** Chão nu: nem copa por cima, nem a própria copa como chão. */
    private boolean openGround(BlockPos top) {
        int noLeaves = creature.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top.getX(), top.getZ());
        return noLeaves == top.getY() && !creature.level().getBlockState(top.below()).is(BlockTags.LEAVES);
    }

    /**
     * Pousando, a menos de {@link #LANDING_DROP} bloco de terra firme: recolhe as asas e cai o resto. Sem isso, perto do
     * ponto de pouso a velocidade mínima a fazia girar em volta dele rente ao chão, sem nunca encostar.
     */
    private boolean justAboveDryGround(Vec3 position) {
        double ground = groundY(position.x, position.z);
        return position.y - ground < LANDING_DROP
                && creature.level().getFluidState(BlockPos.containing(position.x, ground - 1.0, position.z)).isEmpty();
    }

    private double groundY(double x, double z) {
        return creature.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }
}
