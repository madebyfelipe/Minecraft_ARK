package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.effect.VenomEffect;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Peçonha ({@code hunt_special: venom}), a mordida da Megalania: morde, solta e segue o rastro da presa envenenada até
 * ela cair. É a caçada do dragão-de-komodo, o parente vivo mais próximo: dentes serrilhados que cortam fundo e
 * glândulas de peçonha anticoagulante e hipotensora — a presa foge, sangra e entra em choque, e o lagarto vem atrás
 * pelo faro, com a língua (Fry et al. 2009; Bull et al. 2010 sobre a mordida que corta e solta).
 *
 * <ul>
 *   <li>A mordida que acerta ({@link #onStrike}) envenena ({@link VenomEffect}) e abre o rastro: a Megalania recua um
 *   instante e depois segue a vítima sem atacar ({@code VenomTrackGoal}), até ela cair.</li>
 *   <li>A peçonha lembra quem mordeu: o sangramento que mata dá a presa ao caçador (ele come), e o choque da mordida
 *   de uma Megalania domesticada derruba a criatura <b>no nome do dono</b>, que a doma.</li>
 *   <li>O apex não é envenenado (D30: só cai no desafio), e a domesticada nem o ataca ({@link #onChangeTarget}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public final class VenomBite {
    /** O sangramento: tipo de dano por dados em {@code damage_type/venom.json} (atravessa armadura). */
    public static final ResourceKey<DamageType> VENOM_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, IceAgeSurvival.id("venom"));
    private static final String TAG_BY = IceAgeSurvival.MODID + ".venom_by";
    private static final String TAG_OWNER = IceAgeSurvival.MODID + ".venom_owner";

    /** O rastro aberto de cada Megalania: só no servidor, e se perde ao descarregar (ela volta a caçar do zero). */
    private static final Map<PrehistoricCreature, Trail> TRAILS = new WeakHashMap<>();

    private VenomBite() {
    }

    /** A presa envenenada que a Megalania segue, desde quando ({@code bitAt}, tempo de jogo). */
    public static final class Trail {
        private final LivingEntity victim;
        private final long bitAt;
        private int tongueFlicks;

        Trail(LivingEntity victim, long bitAt) {
            this.victim = victim;
            this.bitAt = bitAt;
        }

        public LivingEntity victim() {
            return victim;
        }

        public long bitAt() {
            return bitAt;
        }

        /** Quantas vezes a língua provou o ar neste rastro (gesto {@code tongueflick}). */
        public int tongueFlicks() {
            return tongueFlicks;
        }

        public void flickTongue() {
            tongueFlicks++;
        }
    }

    /** O bote da caçada ({@link PrehistoricCreature#onPounce}): nada a mais — a Megalania chega andando e morde. */
    public static void onPounce(PrehistoricCreature hunter, LivingEntity target) {
    }

    /**
     * A mordida que acertou ({@code doHurtTarget}): envenena e abre o rastro. A mordida que termina a presa já caída
     * não recomeça o rastro; o apex não leva peçonha.
     */
    public static void onStrike(PrehistoricCreature hunter, LivingEntity target) {
        if (hunter.level().isClientSide || !target.isAlive()) {
            return;
        }
        if (target instanceof PrehistoricCreature creature && (creature.isApex() || creature.isUnconscious())) {
            return;
        }
        envenom(hunter, target);
        if (!hunter.isTame()) {
            // Soltar a presa não é perder a caçada: sem isto, largar a perseguição contaria como fracasso.
            hunter.endHunt();
        }
        TRAILS.put(hunter, new Trail(target, hunter.level().getGameTime()));
    }

    /** Põe a peçonha na vítima, lembrando quem mordeu e, se a Megalania é domesticada, o dono dela. */
    public static void envenom(PrehistoricCreature hunter, LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(ModEffects.VENOM.get(), VenomEffect.DURATION_TICKS, 0), hunter);
        CompoundTag data = victim.getPersistentData();
        data.putUUID(TAG_BY, hunter.getUUID());
        if (hunter.isTame() && hunter.getOwnerUUID() != null) {
            data.putUUID(TAG_OWNER, hunter.getOwnerUUID());
        } else {
            data.remove(TAG_OWNER);
        }
    }

    /** O rastro que esta Megalania segue agora, se houver. */
    public static Optional<Trail> trail(PrehistoricCreature hunter) {
        return Optional.ofNullable(TRAILS.get(hunter));
    }

    /** Fecha o rastro: a presa caiu, morreu, ficou longe ou a peçonha passou. */
    public static void endTrail(PrehistoricCreature hunter) {
        TRAILS.remove(hunter);
    }

    /** O choque numa criatura do mod: soma torpor, no nome do dono da Megalania domesticada que mordeu. */
    public static void shock(PrehistoricCreature victim, double fractionOfMax) {
        if (victim.isApex()) {
            return;
        }
        victim.addTorpor(victim.maxTorpor() * fractionOfMax, owner(victim));
    }

    /** O sangramento no jogador e nos mobs vanilla; quem morre sangrando é presa de quem mordeu. */
    public static void bleed(LivingEntity victim, float amount) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Entity biter = biter(victim);
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(VENOM_DAMAGE), null, biter);
        victim.hurt(source, amount);
    }

    /** Tira a peçonha (o antídoto). */
    public static boolean cure(LivingEntity victim) {
        boolean had = victim.removeEffect(ModEffects.VENOM.get());
        victim.getPersistentData().remove(TAG_BY);
        victim.getPersistentData().remove(TAG_OWNER);
        return had;
    }

    public static boolean isEnvenomed(LivingEntity entity) {
        return entity.hasEffect(ModEffects.VENOM.get());
    }

    /** A Megalania que envenenou esta vítima, se ainda estiver carregada. */
    @Nullable
    private static Entity biter(LivingEntity victim) {
        CompoundTag data = victim.getPersistentData();
        if (!data.hasUUID(TAG_BY) || !(victim.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity biter = level.getEntity(data.getUUID(TAG_BY));
        return biter != null && biter.isAlive() ? biter : null;
    }

    /** O dono da Megalania domesticada que envenenou esta vítima, se estiver online. */
    @Nullable
    private static Player owner(LivingEntity victim) {
        CompoundTag data = victim.getPersistentData();
        if (!data.hasUUID(TAG_OWNER)) {
            return null;
        }
        UUID owner = data.getUUID(TAG_OWNER);
        return victim.level().getPlayerByUUID(owner);
    }

    /** Envenenado não regenera: o jogador e os mobs vanilla (nas criaturas do mod a peçonha é choque, não sangue). */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && !(entity instanceof PrehistoricCreature) && isEnvenomed(entity)) {
            event.setCanceled(true);
        }
    }

    /** A Megalania domesticada não ataca o apex: ele só se doma no desafio (D30), não derrubado pela peçonha. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof PrehistoricCreature hunter && hunter.isTame()
                && event.getNewTarget() instanceof PrehistoricCreature target && target.isApex()
                && hunter.behavior().map(BehaviorProfile::huntSpecial).orElse(BehaviorProfile.HuntSpecial.NONE)
                == BehaviorProfile.HuntSpecial.VENOM) {
            event.setCanceled(true);
        }
    }
}
