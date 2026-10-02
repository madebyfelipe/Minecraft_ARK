package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.core.ecology.Perception;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
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
 * Filhote sempre foge. Domesticada, inconsciente ou montada, não roda. Com alvo, só reage a uma
 * ameaça selvagem que esteja investindo ou o encarando; a fuga interrompe a caça em curso.
 *
 * <p>Encarar, blefar ou investir contra outra criatura selvagem avisa a ela
 * ({@link PrehistoricCreature#intimidatedBy}). Sem isso o predador só via o herbívoro como ameaça
 * durante a investida (quando ele fica agressivo): o gesto de ameaça não o afastava, e ao fim da
 * investida ele parava de recuar e voltava à presa.
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
    /** Até onde corre de cada vez, fugindo. */
    private static final int FLEE_DISTANCE = 28;

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
                && profile() != null;
    }

    @Override
    public boolean canUse() {
        if (!able()) {
            return false;
        }
        LivingEntity noticed = creature.takeNoticedThreat();
        // O aviso da manada (ou de quem encara) só vale se, para esta criatura, aquilo é ameaça.
        if (noticed != null && !isThreat(noticed, profile())) {
            noticed = null;
        }
        if (noticed == null) {
            if (--scanCooldown > 0) {
                return false;
            }
            scanCooldown = SCAN_INTERVAL + creature.getRandom().nextInt(5);
            noticed = nearestThreat(profile());
        }
        if (noticed == null || (creature.getTarget() != null && !isActivelyThreatening(noticed))) {
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
        // Notado, quem espreitava perde a surpresa e dá o bote (StalkGoal).
        if (threat instanceof PrehistoricCreature stalker && stalker.isStalking()) {
            stalker.blowStalk();
        }
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
        double radius = detectionRadius(threat, profile, creature.hasCalfNearby(profile.calfRadius()));
        outOfRangeTicks = distance > radius * 1.3 && !creature.isHunted() ? outOfRangeTicks + 1 : 0;
        if (stateTicks % 20 == 0 && distance <= radius) {
            // A ameaça ali estressa: o predador muito, o jogador menos, e menos ainda agachado.
            if (threat instanceof Player) {
                creature.addStress(Stress.Event.PLAYER_NEAR, sneaking(threat) ? 0.3 : 1.0);
            } else {
                creature.addStress(Stress.Event.THREAT_SEEN);
            }
        }

        intimidate(radius, distance);
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
        int attackers = Math.max(creature.huntingPack(),
                threat instanceof PrehistoricCreature hunter ? hunter.fightingGroup() : 1);
        boolean guardingCalf = creature.hasCalfNearby(profile.calfRadius());
        var situation = new ThreatResponse.Situation(distance, approaching, sneaking(threat), firstContact,
                guardingCalf, creature.sizeRatioOf(threat), creature.isHunted(), attackers, creature.stress());
        int defenders = creature.fightingGroup();
        Reaction reaction;
        if (isHunter(threat)) {
            reaction = ThreatResponse.reactToHunter(situation, profile.tuning(), defenders,
                    creature.getRandom()::nextDouble);
        } else if (threat instanceof PrehistoricCreature confronter) {
            reaction = ThreatResponse.reactToIntimidation(situation, defenders, confronter.fightingGroup(),
                    confronter.isAggressive(), committed());
        } else {
            reaction = ThreatResponse.react(situation, profile.tuning(), creature.getRandom()::nextDouble);
        }
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
            case RETREAT, FLEE -> {
                if (creature.isHunting()) {
                    creature.setTarget(null);
                }
                moveAway(reaction == Reaction.FLEE ? profile().fleeSpeed() : RETREAT_SPEED * calmSpeed);
            }
            default -> {
                creature.setAggressive(false);
                creature.getNavigation().stop();
            }
        }
    }

    /**
     * Encarando de perto, blefando ou investindo contra outra criatura selvagem: ela fica sabendo.
     * Recuando ou fugindo, não.
     */
    private void intimidate(double radius, double distance) {
        if (!(threat instanceof PrehistoricCreature other) || other.isTame() || creature.isBaby()) {
            return;
        }
        boolean confronting = state == Reaction.CHARGE || state == Reaction.BLUFF
                || state == Reaction.ALERT && distance <= radius * ThreatResponse.CONFRONT_FRACTION;
        if (confronting) {
            other.intimidatedBy(creature);
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
        // Fugindo, refaz a rota a cada segundo: a ameaça se move, e a manada corre junto para longe dela.
        if (creature.getNavigation().isDone() || state == Reaction.FLEE && stateTicks % 20 == 0) {
            moveAway(state == Reaction.FLEE ? profile.fleeSpeed() : RETREAT_SPEED * calmSpeed);
        }
    }

    private void moveAway(double speed) {
        Vec3 away = DefaultRandomPos.getPosAway(creature, FLEE_DISTANCE, 7, threat.position());
        if (away == null) {
            // Sem ponto bom sorteado: corre em linha reta para longe.
            Vec3 direction = creature.position().subtract(threat.position()).multiply(1, 0, 1);
            if (direction.lengthSqr() < 1.0E-4) {
                direction = Vec3.directionFromRotation(0.0F, creature.getYRot());
            }
            away = creature.position().add(direction.normalize().scale(FLEE_DISTANCE));
        }
        creature.getNavigation().moveTo(away.x, away.y, away.z, speed);
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
        double reach = Math.max(profile.alertRadius(), profile.calfRadius())
                * Stress.perceptionMultiplier(creature.stress());
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        boolean calf = creature.hasCalfNearby(profile.calfRadius());
        for (LivingEntity candidate : creature.level().getEntitiesOfClass(LivingEntity.class,
                creature.getBoundingBox().inflate(reach, 6.0, reach), other -> isThreat(other, profile))) {
            double distance = gap(candidate);
            double radius = detectionRadius(candidate, profile, calf);
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
        if (other instanceof PrehistoricCreature predator
                && (predator.isUnconscious() || predator.isBaby() || predator.isTame())) {
            return false;
        }
        boolean configuredThreat = profile.threats().map(other.getType()::is).orElse(false);
        if (other instanceof PrehistoricCreature creature) {
            if (creature.isTame()) {
                return false;
            }
            boolean activeThreat = isActivelyThreatening(creature);
            if (isHunter(creature)) {
                boolean thisIsPrey = this.creature.behavior()
                        .map(behavior -> behavior.prey().isEmpty()).orElse(true);
                boolean huntsThisSpecies = creature.behavior().flatMap(BehaviorProfile::prey)
                        .map(tag -> this.creature.getType().is(tag)).orElse(false);
                return activeThreat || thisIsPrey || huntsThisSpecies;
            }
            return configuredThreat || activeThreat;
        }
        return configuredThreat;
    }

    private boolean isHunter(LivingEntity entity) {
        return entity instanceof PrehistoricCreature hunter && !hunter.isTame() && !hunter.isUnconscious()
                && hunter.behavior().flatMap(BehaviorProfile::prey).isPresent();
    }

    /**
     * Ameaça ativa: um caçador investindo, ou um animal que não caça confrontando esta criatura com
     * força para afastá-la. Investida de herbívoro contra quem não se impressiona (o bando que pode
     * abatê-lo, o predador faminto de força parecida) não conta: o porte individual sozinho fazia
     * cada raptor do bando fugir do Elasmotério.
     */
    private boolean isActivelyThreatening(LivingEntity entity) {
        if (!(entity instanceof PrehistoricCreature wild) || !wild.isAlive() || wild.isBaby() || wild.isTame()
                || wild.isUnconscious() || wild.wariness().isEmpty()) {
            return false;
        }
        if (isHunter(wild)) {
            return wild.isAggressive();
        }
        return (wild.isAggressive() || creature.isIntimidatedBy(wild)) && deterredBy(wild);
    }

    /**
     * Encarado por um animal que não caça: só conta como ameaça se o afastaria ({@link ThreatResponse#deters}).
     * Um bando que pode abater o Elasmotério não recua de um bufo.
     */
    private boolean deterredBy(PrehistoricCreature confronter) {
        double power = ThreatResponse.confrontationPower(creature.sizeRatioOf(confronter),
                confronter.fightingGroup(), creature.fightingGroup());
        return ThreatResponse.deters(power, confronter.isAggressive(), committed());
    }

    /** Com fome ou com alvo, o predador não cede fácil. */
    private boolean committed() {
        return creature.getTarget() != null || creature.hungerDrive() != Hunger.Drive.SATED;
    }

    /** Distância entre as bordas dos corpos: os raios valem igual para um dodô e para um Brontossauro. */
    private double gap(LivingEntity other) {
        return Math.max(0.0, creature.distanceTo(other) - (creature.getBbWidth() + other.getBbWidth()) / 2.0);
    }

    /** Até onde nota esta ameaça agora; quem espreita, só à metade ({@link Perception#STALKER_FACTOR}). */
    private double detectionRadius(LivingEntity threat, WarinessProfile profile, boolean calf) {
        double radius = ThreatResponse.detectionRadius(profile.tuning(), sneaking(threat), calf, creature.stress());
        return threat instanceof PrehistoricCreature stalker && stalker.isStalking()
                ? Perception.stalkerNoticeRadius(radius) : radius;
    }

    private static boolean sneaking(LivingEntity threat) {
        return threat instanceof Player player && player.isShiftKeyDown();
    }
}
