package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Lutar ou fugir de quem chega perto: jogadores e os predadores da tag da espécie. A decisão é de
 * {@link ThreatResponse}; aqui ela vira movimento.
 *
 * <ul>
 *   <li><b>Alerta:</b> para, encara e bufa (som e gesto de ameaça). Avisa a manada.</li>
 *   <li><b>Recuo / fuga:</b> afasta-se andando, ou corre.</li>
 *   <li><b>Blefe:</b> investe e para a poucos blocos, bufando.</li>
 *   <li><b>Investida:</b> corre em linha quase reta (mira de novo a cada {@value #CHARGE_REAIM_TICKS}
 *       ticks, então dá para desviar), acerta com o golpe da espécie e volta a encarar. Às vezes
 *       segue brigando — o imprevisível. Espécies de manada com defesa em grupo chamam a manada.</li>
 * </ul>
 *
 * Filhote sempre foge. Domesticada, inconsciente, montada ou já com alvo (ferida: quem manda é o
 * revide), não roda.
 */
public class WaryGoal extends Goal {
    private static final int SCAN_INTERVAL = 10;
    private static final int DECISION_INTERVAL = 20;
    private static final int SNORT_INTERVAL = 50;
    private static final int CALM_DOWN_TICKS = 60;
    private static final int BLUFF_TICKS = 25;
    private static final double BLUFF_STOP_DISTANCE = 3.5;
    private static final int CHARGE_TICKS = 60;
    static final int CHARGE_REAIM_TICKS = 8;
    private static final int AFTER_CHARGE_COOLDOWN = 60;
    /** Depois de acertar a investida, chance de seguir brigando em vez de voltar a encarar. */
    private static final double KEEP_FIGHTING_CHANCE = 0.35;
    private static final double RETREAT_SPEED = 1.0;

    private final PrehistoricCreature creature;
    private final double calmSpeed;
    @Nullable
    private LivingEntity threat;
    private Reaction state = Reaction.IGNORE;
    private int stateTicks;
    private int decisionCooldown;
    private int snortCooldown;
    private int outOfRangeTicks;
    private int scanCooldown;
    private double lastDistance;
    private Vec3 chargeTarget = Vec3.ZERO;

    public WaryGoal(PrehistoricCreature creature, double calmSpeed) {
        this.creature = creature;
        this.calmSpeed = calmSpeed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable
    private WarinessProfile profile() {
        return creature.wariness().orElse(null);
    }

    private boolean able() {
        return !creature.isTame() && !creature.isUnconscious() && !creature.isVehicle()
                && creature.getTarget() == null && profile() != null;
    }

    @Override
    public boolean canUse() {
        if (!able()) {
            return false;
        }
        LivingEntity noticed = creature.takeNoticedThreat();
        if (noticed == null) {
            if (--scanCooldown > 0) {
                return false;
            }
            scanCooldown = SCAN_INTERVAL + creature.getRandom().nextInt(5);
            noticed = nearestThreat(profile());
        }
        if (noticed == null) {
            return false;
        }
        threat = noticed;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return able() && threat != null && threat.isAlive() && isThreat(threat, profile())
                && outOfRangeTicks < CALM_DOWN_TICKS;
    }

    @Override
    public void start() {
        state = Reaction.IGNORE;
        stateTicks = 0;
        decisionCooldown = 0;
        snortCooldown = 0;
        outOfRangeTicks = 0;
        lastDistance = gap(threat);
        // Primeira reação: pego de surpresa perto, investe na hora.
        decide(true);
        if (state == Reaction.ALERT || state == Reaction.BLUFF) {
            creature.threatDisplay();
            snortCooldown = SNORT_INTERVAL;
        }
        creature.alertHerdToThreat(threat);
    }

    @Override
    public void stop() {
        threat = null;
        state = Reaction.IGNORE;
        creature.getNavigation().stop();
        creature.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (threat == null) {
            return;
        }
        stateTicks++;
        WarinessProfile profile = profile();
        double distance = gap(threat);
        double radius = ThreatResponse.detectionRadius(profile.tuning(), sneaking(threat),
                creature.hasCalfNearby(profile.calfRadius()));
        outOfRangeTicks = distance > radius * 1.3 ? outOfRangeTicks + 1 : 0;

        switch (state) {
            case CHARGE -> tickCharge(profile, distance);
            case BLUFF -> tickBluff(profile, distance);
            case RETREAT, FLEE -> tickAway(profile);
            default -> tickAlert();
        }
        if (--decisionCooldown <= 0 && state != Reaction.CHARGE && state != Reaction.BLUFF) {
            decide(false);
        }
    }

    private void decide(boolean firstContact) {
        WarinessProfile profile = profile();
        double distance = gap(threat);
        boolean approaching = distance < lastDistance - 0.25;
        lastDistance = distance;
        decisionCooldown = DECISION_INTERVAL;
        var situation = new ThreatResponse.Situation(distance, approaching, sneaking(threat), firstContact,
                creature.hasCalfNearby(profile.calfRadius()), sizeRatio(threat));
        Reaction reaction = ThreatResponse.react(situation, profile.tuning(), creature.getRandom()::nextDouble);
        if (reaction == Reaction.IGNORE) {
            reaction = Reaction.ALERT;
        }
        // Filhote não enfrenta ninguém: corre (e a mãe, por perto, é quem investe).
        if (creature.isBaby()) {
            reaction = Reaction.FLEE;
        }
        enter(reaction);
    }

    private void enter(Reaction reaction) {
        if (reaction == state && reaction != Reaction.RETREAT && reaction != Reaction.FLEE) {
            return;
        }
        state = reaction;
        stateTicks = 0;
        switch (reaction) {
            case CHARGE, BLUFF -> {
                creature.setAggressive(true);
                creature.playAlert();
                aimCharge();
            }
            case RETREAT, FLEE -> moveAway(reaction == Reaction.FLEE ? profile().fleeSpeed() : RETREAT_SPEED * calmSpeed);
            default -> {
                creature.setAggressive(false);
                creature.getNavigation().stop();
            }
        }
    }

    private void tickAlert() {
        creature.getNavigation().stop();
        creature.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        if (--snortCooldown <= 0) {
            snortCooldown = SNORT_INTERVAL + creature.getRandom().nextInt(30);
            creature.threatDisplay();
        }
    }

    private void tickAway(WarinessProfile profile) {
        if (creature.getNavigation().isDone()) {
            moveAway(state == Reaction.FLEE ? profile.fleeSpeed() : RETREAT_SPEED * calmSpeed);
        }
    }

    private void moveAway(double speed) {
        Vec3 away = DefaultRandomPos.getPosAway(creature, 16, 7, threat.position());
        if (away != null) {
            creature.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
    }

    private void aimCharge() {
        // Mira um ponto além da ameaça: a investida passa por ela, não freia em cima.
        Vec3 direction = threat.position().subtract(creature.position()).multiply(1, 0, 1);
        if (direction.lengthSqr() < 1.0E-4) {
            direction = Vec3.directionFromRotation(0.0F, creature.getYRot());
        }
        chargeTarget = threat.position().add(direction.normalize().scale(3.0));
        creature.getNavigation().stop();
    }

    private void tickBluff(WarinessProfile profile, double distance) {
        creature.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        creature.getMoveControl().setWantedPosition(chargeTarget.x, chargeTarget.y, chargeTarget.z, profile.chargeSpeed());
        if (distance <= BLUFF_STOP_DISTANCE || stateTicks >= BLUFF_TICKS) {
            creature.getNavigation().stop();
            creature.threatDisplay();
            snortCooldown = SNORT_INTERVAL;
            enter(Reaction.ALERT);
            decisionCooldown = AFTER_CHARGE_COOLDOWN;
        }
    }

    private void tickCharge(WarinessProfile profile, double distance) {
        creature.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        if (stateTicks % CHARGE_REAIM_TICKS == 0) {
            aimCharge();
        }
        creature.getMoveControl().setWantedPosition(chargeTarget.x, chargeTarget.y, chargeTarget.z, profile.chargeSpeed());
        if (creature.getBoundingBox().inflate(0.6).intersects(threat.getBoundingBox())) {
            creature.doHurtTarget(threat);
            creature.alertHerd(threat);
            if (creature.getRandom().nextDouble() < KEEP_FIGHTING_CHANCE) {
                creature.setTarget(threat);
                return;
            }
            endCharge();
        } else if (stateTicks >= CHARGE_TICKS || creature.position().distanceToSqr(chargeTarget) < 1.0) {
            endCharge();
        }
    }

    private void endCharge() {
        creature.getNavigation().stop();
        enter(Reaction.ALERT);
        decisionCooldown = AFTER_CHARGE_COOLDOWN;
    }

    // ---- Quem é ameaça ----

    @Nullable
    private LivingEntity nearestThreat(WarinessProfile profile) {
        double reach = Math.max(profile.alertRadius(), profile.calfRadius());
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        boolean calf = creature.hasCalfNearby(profile.calfRadius());
        for (LivingEntity candidate : creature.level().getEntitiesOfClass(LivingEntity.class,
                creature.getBoundingBox().inflate(reach, 6.0, reach), other -> isThreat(other, profile))) {
            double distance = gap(candidate);
            double radius = ThreatResponse.detectionRadius(profile.tuning(), sneaking(candidate), calf);
            if (distance <= radius && distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private boolean isThreat(LivingEntity other, WarinessProfile profile) {
        if (other == creature || !other.isAlive() || other.getType() == creature.getType()) {
            return false;
        }
        if (other instanceof Player) {
            return profile.players() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(other);
        }
        if (other instanceof PrehistoricCreature predator && predator.isUnconscious()) {
            return false;
        }
        return profile.threats().map(other.getType()::is).orElse(false);
    }

    /** Distância entre as bordas dos corpos: os raios valem igual para um dodô e para um Brontossauro. */
    private double gap(LivingEntity other) {
        return Math.max(0.0, creature.distanceTo(other) - (creature.getBbWidth() + other.getBbWidth()) / 2.0);
    }

    private static boolean sneaking(LivingEntity threat) {
        return threat instanceof Player player && player.isShiftKeyDown();
    }

    /** Tamanho relativo: razão dos volumes de colisão elevada a 2/3, a escala de uma área. */
    private double sizeRatio(LivingEntity threat) {
        double own = creature.getBbWidth() * creature.getBbWidth() * creature.getBbHeight();
        double theirs = threat.getBbWidth() * threat.getBbWidth() * threat.getBbHeight();
        return own <= 0 ? 1.0 : Math.pow(theirs / own, 2.0 / 3.0);
    }
}
