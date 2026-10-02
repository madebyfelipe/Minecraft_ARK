package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.RifleTracerPayload;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/**
 * Rifle tranquilizante: tiro instantâneo (hitscan), sem entidade de projétil. Um raio sai do olho na direção do olhar,
 * para no primeiro bloco sólido e acerta a primeira criatura viva no caminho — inclusive pelas partes do More
 * Hitboxes, que contam como o corpo inteiro. O dardo é gasto (não sobra nada para recolher) e o que se vê é o
 * traçante ({@link RifleTracerPayload}).
 *
 * <p>Efeito igual ao do dardo voador de antes: torpor fixo {@link #torpor()} em criatura do mod que não seja apex,
 * creditado ao atirador, e o dano baixo do dardo.
 */
public class TranqRifleItem extends TranqGunItem {
    /**
     * Alcance do raio, em blocos. 96 = o dobro do tiro útil de um arco bem puxado (a flecha cai muito depois de uns 40
     * blocos), dentro do raio em que o cliente ainda vê as criaturas (rastreio de 10 chunks = 160 blocos) e curto o
     * bastante para a varredura de entidades ser barata a cada 2,5 s.
     */
    public static final double RANGE = 96.0;
    /** Dano base do dardo tranquilizante, igual ao de {@code TranqArrow}. */
    private static final double DART_BASE_DAMAGE = 0.5;
    /** Velocidade em que o dardo do rifle saía antes do hitscan; o dano é o que ele dava (0,5 × 6 = 3). */
    private static final float LEGACY_DART_SPEED = 6.0F;

    public TranqRifleItem(Properties properties, double torporMultiplier, int reloadTicks, Predicate<ItemStack> ammo,
                          SoundEvent sound) {
        super(properties, torporMultiplier, LEGACY_DART_SPEED, 0.0F, reloadTicks, ammo, sound);
    }

    /** Dano de cada tiro: o do dardo de antes (dano base × velocidade, arredondado para cima como a flecha). */
    public float damage() {
        return (float) Math.ceil(DART_BASE_DAMAGE * speed());
    }

    /** O resultado de um tiro: onde parou e quem foi atingido (o corpo, nunca a parte). */
    public record Shot(Vec3 end, @Nullable LivingEntity target, RifleTracerPayload.Impact impact) {
    }

    @Override
    protected void fire(ServerLevel level, Player player, InteractionHand hand, ItemStack fired, boolean creative) {
        Shot shot = trace(level, player, RANGE);
        LivingEntity target = shot.target();
        if (target != null && target.hurt(damageSource(level, player), damage())) {
            // Como o dardo: o torpor só entra se o golpe entrou (não em quem está invulnerável).
            if (target instanceof PrehistoricCreature creature && !creature.isApex()) {
                // O apex não cai com tranquilizante: só vencendo o desafio.
                creature.addTorpor(torpor(), player);
            }
        }
        ModPayloads.sendToTrackingAndSelf(player,
                new RifleTracerPayload(player.getId(), muzzle(player, hand), shot.end(), shot.impact()));
    }

    /**
     * Fonte do dano: tipo {@code arrow} (o mesmo do dardo de antes, projétil: o escudo bloqueia pela direção, Proteção
     * contra Projéteis vale, o Enderman desvia), causado e disparado pelo jogador. Quem feriu é o jogador, como com o
     * dardo: o juiz do duelo do apex, a perda de eficiência da domesticação, o alvo da criatura e o XP olham para ele.
     */
    private static DamageSource damageSource(Level level, Player player) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.ARROW), player);
    }

    /**
     * O raio do tiro, do olho na direção do olhar: para no primeiro bloco sólido (colisão, não a forma de mira — grama
     * alta e flores não seguram o tiro) e pega a criatura viva mais próxima antes dele. A montaria do atirador (e tudo
     * na mesma pilha de montaria) fica de fora, para quem atira montado não acertar o próprio animal.
     */
    public static Shot trace(Level level, Player shooter, double range) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 far = eye.add(shooter.getViewVector(1.0F).scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                shooter));
        boolean hitBlock = block.getType() != HitResult.Type.MISS;
        Vec3 end = hitBlock ? block.getLocation() : far;

        Entity ownStack = shooter.getRootVehicle();
        LivingEntity target = null;
        Vec3 at = end;
        double best = eye.distanceToSqr(end);
        // O filtro aceita o corpo das criaturas: o More Hitboxes tira da lista as partes cujo corpo não passa nele.
        Predicate<Entity> candidates = entity -> !entity.isSpectator()
                && (entity instanceof LivingEntity || entity instanceof PartEntity<?>);
        for (Entity candidate : level.getEntities(shooter, new AABB(eye, end).inflate(1.0), candidates)) {
            LivingEntity body = body(candidate);
            if (body == null || body == shooter || !body.isAlive() || !candidate.isPickable()
                    || body.getRootVehicle() == ownStack) {
                continue;
            }
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            Optional<Vec3> crossing = box.contains(eye) ? Optional.of(eye) : box.clip(eye, end);
            if (crossing.isPresent() && eye.distanceToSqr(crossing.get()) <= best) {
                best = eye.distanceToSqr(crossing.get());
                target = body;
                at = crossing.get();
            }
        }
        RifleTracerPayload.Impact impact = target != null ? RifleTracerPayload.Impact.ENTITY
                : hitBlock ? RifleTracerPayload.Impact.BLOCK : RifleTracerPayload.Impact.NONE;
        return new Shot(at, target, impact);
    }

    /** A boca do cano de quem atira com a arma nesta mão: à frente, para o lado da mão e abaixo do olho. */
    public static Vec3 muzzle(Player player, InteractionHand hand) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        GunKinematics.Offset offset = GunKinematics.muzzleOffset(player.getYRot(), player.getXRot(),
                arm == HumanoidArm.RIGHT ? 1 : -1);
        return player.getEyePosition().add(offset.x(), offset.y(), offset.z());
    }

    /** O corpo que leva o tiro: a própria criatura, ou o dono da parte do More Hitboxes. */
    @Nullable
    private static LivingEntity body(Entity entity) {
        Entity root = entity instanceof PartEntity<?> part ? part.getParent() : entity;
        return root instanceof LivingEntity living ? living : null;
    }
}
