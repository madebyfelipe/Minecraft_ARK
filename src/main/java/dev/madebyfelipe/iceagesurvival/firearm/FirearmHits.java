package dev.madebyfelipe.iceagesurvival.firearm;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.firearms.Ballistics;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.core.firearms.Rage;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.FirearmProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.joml.Vector3f;

/**
 * O acerto de um tiro (D58): o dano convertido do DC2 ({@link Ballistics}), a fúria ({@link Rage}) e o tranco.
 *
 * <ul>
 *   <li>Criatura do mod com o bloco {@code firearm}: o dano tira a mesma fração da vida base que no DC2 e usa o tipo
 *   {@code iceagesurvival:firearm_dc2}, que ignora a armadura (o couro do DC2 já está na conta).</li>
 *   <li>Os outros (jogadores, mobs, o Titanovenator): a escala do raptor e o tipo {@code iceagesurvival:firearm}, em
 *   que a armadura vale. No Titanovenator o couro blindado da D57 segura quase tudo — como o Giganotossauro do DC2,
 *   que nenhuma arma derruba.</li>
 *   <li>Sem a pausa de invulnerabilidade do vanilla entre tiros (as automáticas acertariam um tiro em cada dois).</li>
 *   <li>Os dois tipos não empurram ({@code no_knockback}): o tranco é daqui, por arma, menor nos grandes (resistência
 *   a empurrão), e nenhum na fúria (o bicho enfurecido não cambaleia).</li>
 * </ul>
 */
public final class FirearmHits {
    public static final ResourceKey<DamageType> FIREARM =
            ResourceKey.create(Registries.DAMAGE_TYPE, IceAgeSurvival.id("firearm"));
    public static final ResourceKey<DamageType> FIREARM_DC2 =
            ResourceKey.create(Registries.DAMAGE_TYPE, IceAgeSurvival.id("firearm_dc2"));

    /** De quanto em quanto tempo o bicho enfurecido solta a fumaça vermelha. */
    private static final int RAGE_PARTICLE_TICKS = 4;
    private static final DustParticleOptions RAGE_DUST = new DustParticleOptions(new Vector3f(0.85F, 0.08F, 0.04F), 1.8F);

    /** A fúria de cada criatura baleada recentemente; esquecida quando passa. */
    private static final Map<LivingEntity, Rage> RAGES = new HashMap<>();

    private FirearmHits() {
    }

    /**
     * Aplica um tiro.
     *
     * @param distance distância do atirador (ou da boca do canhão) ao ponto atingido
     * @param from     de onde o tiro veio, para a direção do tranco
     * @return se o golpe entrou
     */
    public static boolean hit(ServerLevel level, @Nullable Entity shooter, LivingEntity target, Firearm gun,
                              double distance, Vec3 from) {
        Optional<FirearmProfile> profile = profile(target);
        Ballistics.Target ballistic = profile.map(p -> new Ballistics.Target(baseHealth(target), p.dc2Health(),
                p.armored(), target.getMaxHealth())).orElseGet(Ballistics.Target::generic);
        long now = level.getGameTime();
        Rage rage = target instanceof PrehistoricCreature ? RAGES.computeIfAbsent(target, t -> new Rage()) : null;
        boolean enraged = rage != null && rage.enraged(now);
        float damage = (float) Ballistics.damage(gun, distance, ballistic, enraged);

        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(profile.isPresent() ? FIREARM_DC2 : FIREARM), shooter);
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(source, damage);
        if (!hurt) {
            return false;
        }
        if (!enraged && target.isAlive()) {
            kick(target, gun, distance, from);
        }
        if (rage != null && rage.hit(now, gun.buildsRage()) && target.isAlive()) {
            enrage(level, target);
        }
        return true;
    }

    /** O bloco {@code firearm} da espécie, se o alvo é uma criatura do mod que o tem. */
    private static Optional<FirearmProfile> profile(LivingEntity target) {
        return target instanceof PrehistoricCreature creature
                ? creature.species().flatMap(Species::firearm) : Optional.empty();
    }

    /** Vida base da espécie, no nível 1: é ela que casa com a vida do bicho do DC2. */
    private static double baseHealth(LivingEntity target) {
        return target instanceof PrehistoricCreature creature
                ? creature.species().map(species -> species.stats().entry(Stat.HEALTH).base())
                        .orElse((double) target.getMaxHealth())
                : target.getMaxHealth();
    }

    /** O tranco do tiro, na direção dele; os grandes (resistência a empurrão 1) não se mexem. */
    private static void kick(LivingEntity target, Firearm gun, double distance, Vec3 from) {
        double strength = switch (gun) {
            case HANDGUN -> 0.12;
            case SUBMACHINE_GUN -> 0.05;
            case HEAVY_MACHINE_GUN -> 0.12;
            case SHOTGUN -> distance <= 5.0 ? 0.55 : 0.3;
            case SOLID_CANNON -> 0.6;
            case ANTI_TANK_RIFLE -> 0.9;
        };
        strength *= 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (strength <= 0.01) {
            return;
        }
        Vec3 push = target.position().subtract(from).multiply(1.0, 0.0, 1.0);
        if (push.lengthSqr() < 1.0E-6) {
            return;
        }
        push = push.normalize().scale(strength);
        target.push(push.x, Math.min(0.25, strength * 0.4), push.z);
        target.hurtMarked = true;
    }

    /** Entrou em fúria: um rosnado, a nuvem de raiva e, até passar, a fumaça vermelha. */
    private static void enrage(ServerLevel level, LivingEntity target) {
        Vec3 at = target.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, at.x, target.getEyeY(), at.z, 6,
                target.getBbWidth() * 0.4, 0.3, target.getBbWidth() * 0.4, 0.0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), Firearms.RAGE.get(), SoundSource.HOSTILE,
                1.4F, 0.8F + level.getRandom().nextFloat() * 0.2F);
    }

    /** Se a criatura está enfurecida agora (para os testes e o HUD). */
    public static boolean enraged(LivingEntity target) {
        Rage rage = RAGES.get(target);
        return rage != null && rage.enraged(target.level().getGameTime());
    }

    /** A fumaça vermelha dos enfurecidos e a limpeza de quem já esqueceu os tiros. */
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || RAGES.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<LivingEntity, Rage>> entries = RAGES.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<LivingEntity, Rage> entry = entries.next();
            LivingEntity target = entry.getKey();
            if (target.level() != level) {
                continue;
            }
            if (target.isRemoved() || !target.isAlive() || entry.getValue().idle(now)) {
                entries.remove();
                continue;
            }
            if (entry.getValue().enraged(now) && now % RAGE_PARTICLE_TICKS == 0) {
                double width = target.getBbWidth() * 0.5;
                level.sendParticles(RAGE_DUST, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                        target.getZ(), 4, width, target.getBbHeight() * 0.3, width, 0.0);
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        RAGES.clear();
    }
}
