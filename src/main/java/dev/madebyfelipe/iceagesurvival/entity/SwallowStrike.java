package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.core.ecology.Swallow;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * Engole inteira ({@code hunt_special: swallow}), o golpe do Quetzalcoatlus: caçador a pé como a cegonha e o
 * calau-de-chão, apanha a presa pequena com o bico e a engole de uma vez (Witton &amp; Naish 2008, 2015).
 *
 * <ul>
 *   <li>O golpe do selvagem numa presa da dieta dele ({@code behavior.prey}) que caiba no bico ({@link Swallow}): a
 *   presa some, sem cair nada, com o gesto {@code eat}, uma nuvem de poeira e o som de engolir. Vale meia refeição, como o peixe
 *   da pesca, e sacia o bando.</li>
 *   <li>A presa maior, a domesticada, o filhote e quem não é presa (o jogador, quem o atacou) levam só a bicada.</li>
 * </ul>
 *
 * <p>O engolir troca o golpe inteiro, antes do dano: se a bicada viesse primeiro, ela já mataria a presa pequena e
 * derrubaria o que ela carrega. Por isso o golpe é interceptado no {@link LivingAttackEvent}, que o cancela.
 */
public final class SwallowStrike {
    private SwallowStrike() {
    }

    /** O bote da caçada ({@link PrehistoricCreature#onPounce}): nada a mais — o caçador a pé chega andando. */
    public static void onPounce(PrehistoricCreature hunter, LivingEntity target) {
    }

    /**
     * O golpe que acertou ({@code doHurtTarget}). A presa que cabia no bico já foi engolida antes do dano
     * ({@link #onAttack}); a que chega aqui é grande demais e leva só a bicada.
     */
    public static void onStrike(PrehistoricCreature hunter, LivingEntity target) {
    }

    /** Se este golpe do caçador engole a presa inteira em vez de bicar. */
    public static boolean swallows(PrehistoricCreature hunter, LivingEntity target) {
        if (hunter.isTame() || hunter.isBaby() || hunter.isUnconscious() || !target.isAlive()
                || hunter.behavior().map(BehaviorProfile::huntSpecial).orElse(BehaviorProfile.HuntSpecial.NONE)
                != BehaviorProfile.HuntSpecial.SWALLOW) {
            return false;
        }
        boolean prey = hunter.behavior().flatMap(BehaviorProfile::prey)
                .map(tag -> HuntGoal.isPrey(hunter, target, tag)).orElse(false);
        return prey && Swallow.swallows(hunter.sizeRatioOf(target), target.isBaby());
    }

    /** Engole: a presa some sem cair nada, com o gesto de comer, e a refeição sacia o bando. */
    public static void swallow(PrehistoricCreature hunter, LivingEntity target) {
        if (hunter.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(0.5), target.getZ(), 8,
                    target.getBbWidth() * 0.3, target.getBbHeight() * 0.3, target.getBbWidth() * 0.3, 0.02);
        }
        hunter.playSound(SoundEvents.PLAYER_BURP, 0.8F, 0.6F + hunter.getRandom().nextFloat() * 0.2F);
        target.remove(Entity.RemovalReason.KILLED);
        hunter.gesture("eat");
        // Meia refeição, como o peixe da pesca: mata metade da fome, a do bando junto (D26).
        hunter.eatFish();
    }

    /** O golpe corpo a corpo do caçador numa presa que cabe no bico vira o engolir: sem dano e sem queda de itens. */
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (target.level().isClientSide || !source.is(DamageTypes.MOB_ATTACK)
                || !(source.getDirectEntity() instanceof PrehistoricCreature hunter) || source.getEntity() != hunter
                || !swallows(hunter, target)) {
            return;
        }
        event.setCanceled(true);
        swallow(hunter, target);
    }
}
