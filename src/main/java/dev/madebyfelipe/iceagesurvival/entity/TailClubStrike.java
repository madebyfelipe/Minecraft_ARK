package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.core.ecology.TailClub;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import javax.annotation.Nullable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * A clava da cauda do Anquilossauro em jogo ({@code wariness.defense: tail_club}; a regra é {@link TailClub}).
 *
 * <ul>
 *   <li><b>Reflexo:</b> a cada {@value #SCAN_INTERVAL} ticks, acordado e sem ninguém em cima, golpeia o inimigo mais
 *   perto que esteja no arco de trás e ao alcance da cauda — sem precisar tê-lo notado: o predador que espreita pelas
 *   costas leva a clavada antes do bote. Inimigo da selvagem: o alvo dela, o jogador (agachado, só a metade do
 *   alcance), o predador selvagem e os monstros. Da domesticada: só o alvo dela.</li>
 *   <li><b>Golpe:</b> o golpe da espécie ({@code doHurtTarget}, que toca a animação da cauda) e a perna quebrada
 *   ({@link ModEffects#BROKEN_LEG}) em quem não é da mesma espécie nem da tag {@code leg_break_immune}.</li>
 *   <li><b>Duelo:</b> entre dois da mesma espécie o golpe é no flanco e não leva o rival abaixo de
 *   {@link TailClub#DUEL_FLOOR} da vida ({@link #onHurt}).</li>
 * </ul>
 *
 * Virar a cauda para a ameaça é da cautela ({@code WaryGoal}) e da briga com alvo ({@code TailGuardGoal}).
 */
public final class TailClubStrike {
    static final int SCAN_INTERVAL = 5;
    /** Diferença de altura máxima, além dos corpos, para a cauda alcançar. */
    private static final double VERTICAL_SLACK = 1.0;

    private TailClubStrike() {
    }

    /** A espécie se defende com a clava da cauda. */
    public static boolean has(PrehistoricCreature creature) {
        return creature.wariness().map(WarinessProfile::tailClub).orElse(false);
    }

    /** O reflexo, no {@code aiStep} do servidor. */
    static void tick(PrehistoricCreature creature) {
        if (creature.tailCooldown > 0) {
            creature.tailCooldown--;
            return;
        }
        if (creature.tickCount % SCAN_INTERVAL != 0 || creature.isBaby() || creature.isUnconscious()
                || creature.isVehicle() || creature.isResting() || !has(creature)) {
            return;
        }
        LivingEntity foe = nearestFoeInReach(creature);
        if (foe != null) {
            strike(creature, foe);
        }
    }

    /**
     * Gira o corpo, no máximo {@link TailClub#TURN_PER_TICK} por tick, para pôr a cauda na ameaça. Parado: quem gira
     * é o corpo inteiro, e a cabeça vai junto.
     */
    public static void turnTail(PrehistoricCreature creature, LivingEntity threat) {
        float wanted = TailClub.braceYaw(threat.getX() - creature.getX(), threat.getZ() - creature.getZ());
        float yaw = TailClub.turn(creature.getYRot(), wanted, TailClub.TURN_PER_TICK);
        creature.getNavigation().stop();
        creature.setYRot(yaw);
        creature.setYBodyRot(yaw);
        creature.setYHeadRot(yaw);
    }

    /** Golpeia com a cauda: o golpe da espécie, e a recarga. */
    public static void strike(PrehistoricCreature creature, LivingEntity foe) {
        creature.tailCooldown = TailClub.SWING_COOLDOWN_TICKS;
        creature.doHurtTarget(foe);
    }

    /** Depois de um golpe que acertou ({@code doHurtTarget}): a perna quebrada. */
    static void afterHit(PrehistoricCreature creature, LivingEntity target) {
        if (target.getType() == creature.getType() || target.getType().is(ModTags.LEG_BREAK_IMMUNE)) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.BROKEN_LEG.get(), TailClub.LEG_BREAK_TICKS), creature);
    }

    @Nullable
    static LivingEntity nearestFoeInReach(PrehistoricCreature creature) {
        double reach = TailClub.REACH + 1.0;
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity other : creature.level().getEntitiesOfClass(LivingEntity.class,
                creature.getBoundingBox().inflate(reach, VERTICAL_SLACK + 1.0, reach),
                other -> isFoe(creature, other))) {
            double edge = edgeDistance(creature, other);
            if (edge < best && inReach(creature, other)) {
                best = edge;
                nearest = other;
            }
        }
        return nearest;
    }

    /** O alvo está no arco de trás e ao alcance da cauda agora. */
    public static boolean inReach(PrehistoricCreature creature, LivingEntity other) {
        boolean vertical = other.getY() < creature.getY() + creature.getBbHeight() + VERTICAL_SLACK
                && other.getY() + other.getBbHeight() > creature.getY() - VERTICAL_SLACK;
        double factor = other instanceof Player player && player.isShiftKeyDown()
                ? creature.wariness().map(WarinessProfile::sneakFactor).orElse(1.0) : 1.0;
        return vertical && TailClub.strikes(creature.getYRot(), other.getX() - creature.getX(),
                other.getZ() - creature.getZ(), edgeDistance(creature, other), factor);
    }

    /** Do centro do animal até a borda do outro, no plano. */
    public static double edgeDistance(PrehistoricCreature creature, LivingEntity other) {
        double dx = other.getX() - creature.getX();
        double dz = other.getZ() - creature.getZ();
        return Math.max(0.0, Math.sqrt(dx * dx + dz * dz) - other.getBbWidth() / 2.0);
    }

    private static boolean isFoe(PrehistoricCreature creature, LivingEntity other) {
        if (other == creature || !other.isAlive() || other.isSpectator() || creature.isAlliedTo(other)
                || other instanceof PrehistoricCreature body && body.isCorpse()) {
            return false;
        }
        if (other == creature.getTarget()) {
            return true;
        }
        if (creature.isTame()) {
            return false;
        }
        if (other instanceof Player player) {
            return !player.isCreative() && creature.wariness().map(WarinessProfile::players).orElse(true);
        }
        if (other instanceof PrehistoricCreature wild) {
            if (wild.isTame() || wild.isUnconscious() || wild.isBaby() || wild.getType() == creature.getType()) {
                return false;
            }
            boolean hunter = wild.behavior().flatMap(BehaviorProfile::prey).isPresent();
            return hunter || isConfiguredThreat(creature, other);
        }
        return other instanceof Enemy || isConfiguredThreat(creature, other);
    }

    private static boolean isConfiguredThreat(PrehistoricCreature creature, LivingEntity other) {
        return creature.wariness().flatMap(WarinessProfile::threats).map(other.getType()::is).orElse(false);
    }

    /** Duelo da mesma espécie, entre selvagens: a clavada no flanco não leva o rival abaixo do piso. */
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !(event.getSource().getEntity() instanceof PrehistoricCreature attacker)
                || event.getSource().getDirectEntity() != attacker || attacker.getType() != target.getType()
                || attacker.isTame() || target instanceof PrehistoricCreature rival && rival.isTame()
                || !has(attacker)) {
            return;
        }
        event.setAmount(TailClub.duelDamage(event.getAmount(), target.getHealth(), target.getMaxHealth()));
    }
}
