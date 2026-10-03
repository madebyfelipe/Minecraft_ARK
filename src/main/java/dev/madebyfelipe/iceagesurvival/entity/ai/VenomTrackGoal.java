package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.VenomBite;
import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Depois da mordida peçonhenta ({@link VenomBite}), a Megalania solta a presa e a segue pelo rastro, sem atacar, até
 * ela cair — como o dragão-de-komodo, que morde, recua e acompanha a vítima de longe provando o ar com a língua
 * (Fry et al. 2009; Bull et al. 2010).
 *
 * <ol>
 *   <li><b>Recua</b> por {@link #RETREAT_TICKS}: afasta-se {@link #RETREAT_DISTANCE} blocos da presa, que agora foge
 *   ou revida no vazio.</li>
 *   <li><b>Segue o rastro</b>: anda atrás dela sem chegar a menos de {@link #TRAIL_DISTANCE} blocos, e a cada
 *   {@link #TONGUE_INTERVAL} ticks prova o ar (gesto {@code tongueflick}).</li>
 *   <li>A presa caiu: a criatura do mod desmaiada pelo choque, a selvagem vai lá e termina; a domesticada para, porque
 *   a presa caiu no nome do dono, que a doma. O jogador e o mob vanilla morrem sangrando, e a Megalania come.</li>
 * </ol>
 *
 * <p>O rastro se perde quando a peçonha passa (então ela volta a perseguir e morde de novo), quando a vítima fica a mais
 * de {@link #LOSE_DISTANCE} blocos, ou quando a Megalania tem outro alvo (alguém a atacou).
 */
public class VenomTrackGoal extends Goal {
    /** Quanto tempo recua depois da mordida: 1,5 s, como a bicada da Kelenken. */
    public static final int RETREAT_TICKS = 30;
    /** Quanto se afasta no recuo, em blocos. */
    public static final double RETREAT_DISTANCE = 6.0;
    /** De quão perto segue a presa envenenada, em blocos. */
    public static final double TRAIL_DISTANCE = 8.0;
    /** Longe assim, o rastro se perde. */
    public static final double LOSE_DISTANCE = 64.0;
    /** A cada quantos ticks prova o ar com a língua enquanto segue. */
    public static final int TONGUE_INTERVAL = 60;
    private static final int REPATH_TICKS = 10;
    private static final int FINISH_ATTACK_INTERVAL = 20;

    private final PrehistoricCreature creature;
    private final double speed;
    private int repath;
    private int tongueCooldown;
    private int attackCooldown;

    public VenomTrackGoal(PrehistoricCreature creature, double speed) {
        this.creature = creature;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        VenomBite.Trail trail = VenomBite.trail(creature).orElse(null);
        if (trail == null) {
            return false;
        }
        if (!stillOnTrail(trail.victim())) {
            VenomBite.endTrail(creature);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = 0;
        tongueCooldown = TONGUE_INTERVAL / 2;
        attackCooldown = 0;
    }

    @Override
    public void stop() {
        creature.getNavigation().stop();
    }

    @Override
    public void tick() {
        VenomBite.Trail trail = VenomBite.trail(creature).orElse(null);
        if (trail == null) {
            return;
        }
        LivingEntity victim = trail.victim();
        creature.getLookControl().setLookAt(victim, 30.0F, 30.0F);
        if (retreating(trail)) {
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                Vec3 away = creature.position().subtract(victim.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, creature.getYRot() + 180.0F)
                        : away.normalize();
                Vec3 to = creature.position().add(away.scale(RETREAT_DISTANCE));
                creature.getNavigation().moveTo(to.x, to.y, to.z, speed * 1.4);
            }
            return;
        }
        if (fallen(victim)) {
            finish(victim);
            return;
        }
        if (--repath <= 0) {
            repath = REPATH_TICKS;
            if (creature.distanceTo(victim) > TRAIL_DISTANCE) {
                creature.getNavigation().moveTo(victim, speed);
            } else {
                creature.getNavigation().stop();
            }
        }
        if (--tongueCooldown <= 0) {
            tongueCooldown = TONGUE_INTERVAL;
            trail.flickTongue();
            creature.gesture("tongueflick");
        }
    }

    /** Se a Megalania está no recuo logo depois da mordida. */
    public static boolean retreating(PrehistoricCreature creature) {
        return VenomBite.trail(creature).map(trail -> retreatingAt(creature, trail)).orElse(false);
    }

    private boolean retreating(VenomBite.Trail trail) {
        return retreatingAt(creature, trail);
    }

    private static boolean retreatingAt(PrehistoricCreature creature, VenomBite.Trail trail) {
        return creature.level().getGameTime() - trail.bitAt() < RETREAT_TICKS;
    }

    /** A selvagem termina a presa caída; a domesticada a deixa para o dono domar. */
    private void finish(LivingEntity victim) {
        if (creature.isTame()) {
            VenomBite.endTrail(creature);
            return;
        }
        if (--repath <= 0) {
            repath = REPATH_TICKS;
            creature.getNavigation().moveTo(victim, speed * 1.2);
        }
        attackCooldown = Math.max(attackCooldown - 1, 0);
        if (attackCooldown <= 0 && creature.isWithinMeleeAttackRange(victim)) {
            attackCooldown = FINISH_ATTACK_INTERVAL;
            creature.swing(InteractionHand.MAIN_HAND);
            creature.doHurtTarget(victim);
        }
    }

    private static boolean fallen(LivingEntity victim) {
        return victim instanceof PrehistoricCreature downed && downed.isUnconscious() && !downed.isCorpse()
                && !downed.isTame();
    }

    private boolean stillOnTrail(LivingEntity victim) {
        if (!victim.isAlive() || victim.level() != creature.level() || creature.isVehicle()
                || creature.isUnconscious() || creature.isResting()
                || creature.distanceTo(victim) > LOSE_DISTANCE) {
            return false;
        }
        LivingEntity target = creature.getTarget();
        if (target != null && target != victim) {
            return false; // outro alvo: quem a atacou vem antes do rastro
        }
        // A caída segue no rastro até a selvagem terminá-la; a de pé, só enquanto a peçonha age.
        return fallen(victim) ? !creature.isTame() : VenomBite.isEnvenomed(victim);
    }
}
