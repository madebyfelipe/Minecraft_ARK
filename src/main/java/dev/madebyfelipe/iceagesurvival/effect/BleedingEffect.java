package dev.madebyfelipe.iceagesurvival.effect;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sangramento: o corte dos dentes serrilhados do Giganotosaurus (D47). Os dentes dele eram lâminas finas e
 * serrilhadas, de cortar carne, não de esmagar osso — a mordida tinha cerca de um terço da força da do T-Rex, e a
 * presa morria pela perda de sangue (Therrien, Henderson e Ruff 2005, sobre a mandíbula de cortar dos carcarodontossaurídeos). Por isso o golpe não precisa ser forte: a ferida faz o
 * resto.
 *
 * <p>Diferente da peçonha da Megalania ({@link VenomEffect}): não é choque nem torpor, não impede a regeneração e
 * vale igual para o jogador e para as criaturas do mod — perde vida a cada segundo, atravessando a armadura (tipo de
 * dano {@code iceagesurvival:bleeding}, na tag {@code bypasses_armor}). Cada nível (amplificador + 1) soma:
 *
 * <ul>
 *   <li>jogador e mobs vanilla: {@link #PLAYER_DAMAGE_PER_LEVEL} de vida por segundo por nível;</li>
 *   <li>criaturas do mod, que têm de 10 a 100 vezes a vida de um jogador: {@link #CREATURE_FRACTION_PER_LEVEL} da
 *   vida máxima por segundo por nível, no mínimo {@link #PLAYER_DAMAGE_PER_LEVEL}.</li>
 * </ul>
 *
 * <p>A mordida empilha níveis e renova a duração ({@link #cut}); o próprio Giganotosaurus não sangra.
 */
public class BleedingEffect extends MobEffect {
    /** O tipo de dano: dados em {@code damage_type/bleeding.json} (atravessa armadura, sem empurrão). */
    public static final ResourceKey<DamageType> BLEEDING_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, IceAgeSurvival.id("bleeding"));
    /** Vida por segundo, por nível, no jogador e nos mobs vanilla: meio coração a cada dois segundos. */
    public static final float PLAYER_DAMAGE_PER_LEVEL = 0.5F;
    /** Fração da vida máxima por segundo, por nível, nas criaturas do mod. */
    public static final double CREATURE_FRACTION_PER_LEVEL = 0.004;

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0303);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        entity.hurt(source(level, null), damagePerSecond(entity, amplifier));
    }

    /** Quanto o sangramento tira por segundo desta vítima, neste nível. */
    public static float damagePerSecond(LivingEntity entity, int amplifier) {
        int levels = amplifier + 1;
        if (entity instanceof PrehistoricCreature) {
            return (float) Math.max(PLAYER_DAMAGE_PER_LEVEL,
                    entity.getMaxHealth() * CREATURE_FRACTION_PER_LEVEL) * levels;
        }
        return PLAYER_DAMAGE_PER_LEVEL * levels;
    }

    public static DamageSource source(ServerLevel level, @Nullable Entity cause) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(BLEEDING_DAMAGE), null, cause);
    }

    /**
     * Um corte: soma {@code levels} níveis ao sangramento da vítima, até {@code cap} níveis, e recomeça a duração.
     * Quem não pode sangrar (o próprio Giganotosaurus, os imunes a efeitos) fica como está.
     */
    public static void cut(LivingEntity victim, @Nullable Entity cutter, int levels, int cap, int durationTicks) {
        if (victim.level().isClientSide || !victim.isAlive()) {
            return;
        }
        MobEffect bleeding = ModEffects.BLEEDING.get();
        MobEffectInstance current = victim.getEffect(bleeding);
        int amplifier = dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials.stackedBleedAmplifier(
                current == null ? -1 : current.getAmplifier(), levels, cap);
        int duration = Math.max(durationTicks, current == null ? 0 : current.getDuration());
        MobEffectInstance next = new MobEffectInstance(bleeding, duration, amplifier);
        if (!victim.canBeAffected(next)) {
            return;
        }
        if (current != null) {
            // Sem tirar antes, o vanilla só aceita a instância nova se ela for mais forte ou mais longa.
            victim.removeEffect(bleeding);
        }
        victim.addEffect(next, cutter);
    }

    public static boolean isBleeding(LivingEntity entity) {
        return entity.hasEffect(ModEffects.BLEEDING.get());
    }
}
