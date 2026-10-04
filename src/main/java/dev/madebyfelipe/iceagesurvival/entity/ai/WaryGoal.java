package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.core.ecology.Perception;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TailClubStrike;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
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
 *   <li><b>Blefe:</b> investe e para a poucos blocos, bufando. É a resposta comum ao jogador que chega perto, e
 *       é o aviso: quem ignora e fica colado, ou volta a ficar nos próximos {@value #WARNING_TICKS} ticks, leva a
 *       investida.</li>
 *   <li><b>Investida:</b> corre em linha quase reta (mira de novo a cada {@value #CHARGE_REAIM_TICKS}
 *       ticks, então dá para desviar), acerta com o golpe da espécie e volta a encarar. Às vezes
 *       segue brigando — o imprevisível; contra o jogador, raramente. Espécies de manada com defesa em
 *       grupo chamam a manada.</li>
 *   <li><b>Clava</b> ({@code defense: tail_club}, o Anquilossauro): não blefa, não foge e não investe — no lugar
 *       disso gira de costas para a ameaça, e a cauda golpeia quem entra no arco de trás ({@link TailClubStrike}).</li>
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
    /** Quem investe ou persegue alguém pesa isto a mais na escolha da ameaça. */
    private static final double ACTIVE_DANGER = 1.5;
    /** Diferença de perigo abaixo da qual vale a mais próxima (e não se troca de ameaça). */
    private static final double DANGER_MARGIN = 1.2;
    private static final int DECISION_INTERVAL = 20;
    private static final int SNORT_INTERVAL = 50;
    private static final int CALM_DOWN_TICKS = 60;
    private static final int BLUFF_TICKS = 25;
    private static final double BLUFF_STOP_DISTANCE = 3.5;
    private static final int CHARGE_TICKS = 60;
    static final int CHARGE_REAIM_TICKS = 8;
    private static final int AFTER_CHARGE_COOLDOWN = 60;
    /** De costas para a ameaça, quanto tempo antes de decidir de novo (a clava). */
    private static final int BRACE_TICKS = 60;
    /** Depois de acertar a investida, chance de seguir brigando em vez de voltar a encarar. */
    private static final double KEEP_FIGHTING_CHANCE = 0.35;
    /**
     * O mesmo, contra o jogador. Seguir brigando faz dele o alvo da criatura, e sem território (os herbívoros não têm)
     * o {@code ChaseGoal} só larga o alvo quando ele morre ou some: com {@value #KEEP_FIGHTING_CHANCE} uma investida
     * em cada três virava perseguição. Quem feriu o animal continua sendo perseguido (o revide é de {@code hurt}).
     */
    private static final double KEEP_FIGHTING_VS_PLAYER_CHANCE = 0.1;
    /** Depois de blefar ou investir contra o jogador, por quanto tempo ele conta como avisado (15 s). */
    static final int WARNING_TICKS = 300;
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
    /** O jogador que já levou um blefe ou investida, e até quando isso vale como aviso. */
    @Nullable
    private UUID warnedPlayer;
    private long warnedUntil;

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
        // Dormindo, não nota ninguém: só o golpe a acorda (hurt tira o sono, e aí a cautela volta a valer).
        if (creature.isResting()) {
            return false;
        }
        // O aviso da manada (ou de quem encara) só vale se, para esta criatura, aquilo é ameaça.
        if (noticed != null && !isThreat(noticed, profile())) {
            noticed = null;
        }
        if (noticed == null) {
            if (--scanCooldown > 0) {
                return false;
            }
            scanCooldown = SCAN_INTERVAL + creature.getRandom().nextInt(5);
            noticed = biggestThreat(profile());
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
        creature.setConfronting(threat);
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
        creature.setConfronting(null);
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
        if (state != Reaction.CHARGE && --scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL + creature.getRandom().nextInt(5);
            switchToBiggerThreat(profile);
        }
        double distance = gap(threat);
        double radius = detectionRadius(threat, profile, guardingCalf(threat, profile));
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
        boolean guardingCalf = guardingCalf(threat, profile);
        var situation = new ThreatResponse.Situation(distance, approaching, sneaking(threat), firstContact,
                guardingCalf, creature.sizeRatioOf(threat), creature.isHunted(), attackers, creature.stress(),
                warned(threat));
        int defenders = creature.fightingGroup();
        Reaction reaction;
        if (isHunter(threat)) {
            reaction = ThreatResponse.reactToHunter(situation, profile.tuning(), defenders,
                    creature.getRandom()::nextDouble);
        } else if (threat instanceof PrehistoricCreature confronter) {
            reaction = ThreatResponse.reactToIntimidation(situation, defenders, confronter.fightingGroup(),
                    confronter.isAggressive(), committed());
        } else {
            reaction = ThreatResponse.react(situation, tuning(threat, profile), creature.getRandom()::nextDouble);
        }
        if (reaction == Reaction.IGNORE) {
            reaction = Reaction.ALERT;
        }
        // Filhote não enfrenta ninguém: corre (e a mãe, por perto, é quem investe).
        if (creature.isBaby()) {
            reaction = Reaction.FLEE;
        } else if (tailClub() && (reaction == Reaction.BLUFF || reaction == Reaction.FLEE)) {
            // A clava não blefa nem foge: lento e blindado, o Anquilossauro vira a cauda e fica (TailClubStrike).
            reaction = Reaction.CHARGE;
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
                if (threat instanceof Player player) {
                    warnedPlayer = player.getUUID();
                    warnedUntil = creature.level().getGameTime() + WARNING_TICKS;
                }
                creature.setAggressive(true);
                creature.playAlert();
                if (tailClub()) {
                    creature.getNavigation().stop();
                } else {
                    aimCharge();
                }
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
        if (tailClub()) {
            tickBrace();
            return;
        }
        creature.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        if (stateTicks % CHARGE_REAIM_TICKS == 0) {
            aimCharge();
        }
        creature.getMoveControl().setWantedPosition(chargeTarget.x, chargeTarget.y, chargeTarget.z, profile.chargeSpeed());
        if (creature.getBoundingBox().inflate(0.6).intersects(threat.getBoundingBox())) {
            creature.doHurtTarget(threat);
            creature.alertHerd(threat);
            double keepFighting = threat instanceof Player ? KEEP_FIGHTING_VS_PLAYER_CHANCE : KEEP_FIGHTING_CHANCE;
            if (creature.getRandom().nextDouble() < keepFighting) {
                creature.setTarget(threat);
                return;
            }
            endCharge();
        } else if (stateTicks >= CHARGE_TICKS || creature.position().distanceToSqr(chargeTarget) < 1.0) {
            endCharge();
        }
    }

    /**
     * A defesa da clava ({@code defense: tail_club}): em vez de investir, gira de costas para a ameaça; quem entra no
     * arco de trás leva a clavada ({@link TailClubStrike}, no reflexo). Fica assim até {@value #BRACE_TICKS} ticks e
     * volta a decidir.
     */
    private void tickBrace() {
        TailClubStrike.turnTail(creature, threat);
        if (stateTicks >= BRACE_TICKS) {
            enter(Reaction.ALERT);
            decisionCooldown = DECISION_INTERVAL;
        }
    }

    private boolean tailClub() {
        WarinessProfile profile = profile();
        return profile != null && profile.tailClub();
    }

    private void endCharge() {
        creature.getNavigation().stop();
        enter(Reaction.ALERT);
        decisionCooldown = AFTER_CHARGE_COOLDOWN;
    }

    // ---- Quem é ameaça ----

    /**
     * A ameaça que mais pesa no raio: a de maior porte relativo, e quem está investindo pesa mais
     * ({@value #ACTIVE_DANGER}×); a distância só desempata. Antes era a mais próxima, e o jogador que fugia de um
     * Tricerátopo em direção ao mamute virava o alvo do mamute, com o Tricerátopo logo atrás (pedido do Felipe,
     * 2026-10-03).
     */
    @Nullable
    private LivingEntity biggestThreat(WarinessProfile profile) {
        double reach = profile.reach() * Stress.perceptionMultiplier(creature.stress());
        List<LivingEntity> inRange = new ArrayList<>();
        double maxDanger = 0.0;
        for (LivingEntity candidate : creature.level().getEntitiesOfClass(LivingEntity.class,
                creature.getBoundingBox().inflate(reach, 6.0, reach), other -> isThreat(other, profile))) {
            if (gap(candidate) <= detectionRadius(candidate, profile, guardingCalf(candidate, profile))) {
                inRange.add(candidate);
                maxDanger = Math.max(maxDanger, danger(candidate));
            }
        }
        LivingEntity chosen = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : inRange) {
            double distance = gap(candidate);
            if (danger(candidate) * DANGER_MARGIN >= maxDanger && distance < bestDistance) {
                bestDistance = distance;
                chosen = candidate;
            }
        }
        return chosen;
    }

    /** Porte relativo da ameaça, maior se ela está investindo ou atrás de alguém. */
    private double danger(LivingEntity candidate) {
        double danger = creature.sizeRatioOf(candidate);
        if (candidate instanceof PrehistoricCreature wild
                && (isActivelyThreatening(wild) || wild.isPursuingPlayer())) {
            danger *= ACTIVE_DANGER;
        }
        return danger;
    }

    /** Encarando uma ameaça, troca por outra bem mais perigosa que apareceu no raio. */
    private void switchToBiggerThreat(WarinessProfile profile) {
        LivingEntity bigger = biggestThreat(profile);
        if (bigger == null || bigger == threat || danger(bigger) <= danger(threat) * DANGER_MARGIN) {
            return;
        }
        threat = bigger;
        creature.setConfronting(threat);
        outOfRangeTicks = 0;
        lastDistance = gap(threat);
        decide(false);
        creature.alertHerdToThreat(threat);
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
            boolean activeThreat = isActivelyThreatening(creature) || creature.isPursuingPlayer();
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

    /** Os números que valem contra esta ameaça: o jogador tem os dele (bloco {@code player}), o resto os gerais. */
    private static ThreatResponse.Tuning tuning(LivingEntity threat, WarinessProfile profile) {
        return profile.tuning(threat instanceof Player);
    }

    /** Filhote por perto, no raio de filhote que vale contra esta ameaça (o do jogador é menor). */
    private boolean guardingCalf(LivingEntity threat, WarinessProfile profile) {
        return creature.hasCalfNearby(tuning(threat, profile).calfRadius());
    }

    /**
     * Se esta ameaça já foi avisada. Só o jogador tem aviso a dar: uma criatura (ou um lobo do vanilla) que chega
     * colada leva a investida sem blefe, como antes.
     */
    private boolean warned(LivingEntity threat) {
        return !(threat instanceof Player player)
                || player.getUUID().equals(warnedPlayer) && creature.level().getGameTime() <= warnedUntil;
    }

    /** Distância entre as bordas dos corpos: os raios valem igual para um dodô e para um Brontossauro. */
    private double gap(LivingEntity other) {
        return Math.max(0.0, creature.distanceTo(other) - (creature.getBbWidth() + other.getBbWidth()) / 2.0);
    }

    /**
     * Até onde nota esta ameaça agora; quem espreita, só à metade ({@link Perception#STALKER_FACTOR}); a ameaça
     * escondida no sub-bosque, à fração da camuflagem.
     */
    private double detectionRadius(LivingEntity threat, WarinessProfile profile, boolean calf) {
        double radius = ThreatResponse.detectionRadius(tuning(threat, profile), sneaking(threat), calf, creature.stress());
        if (threat instanceof PrehistoricCreature stalker && stalker.isStalking()) {
            radius = Perception.stalkerNoticeRadius(radius);
        }
        return PrehistoricCreature.perceivedRadius(threat, radius);
    }

    private static boolean sneaking(LivingEntity threat) {
        return threat instanceof Player player && player.isShiftKeyDown();
    }
}
