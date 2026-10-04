package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Projetor de êxtase: a peça do campo da contenção que o Titanovenator larga ao morrer. Apontado para uma criatura a
 * até {@link #RANGE} blocos (sem parede no meio), a congela por {@link #FREEZE_TICKS}: parada, sem IA e invulnerável,
 * como o campo da base. Depois recarrega por {@link #COOLDOWN_TICKS}. Não pega em pessoas nem no Titanovenator.
 *
 * <p>O congelamento fica numa marca com o tempo de jogo em que acaba ({@link #TAG_UNTIL}), então sobrevive a salvar e
 * carregar o mundo. Números propostos, ajustáveis.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public class StasisProjectorItem extends Item {
    public static final double RANGE = 24.0;
    public static final int FREEZE_TICKS = 200;
    public static final int COOLDOWN_TICKS = 1200;
    public static final String TAG_UNTIL = IceAgeSurvival.MODID + ".stasis_until";
    private static final int MISS_COOLDOWN_TICKS = 10;

    public StasisProjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        LivingEntity target = aimed(player);
        if (target == null) {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.getCooldowns().addCooldown(this, MISS_COOLDOWN_TICKS);
            return InteractionResultHolder.fail(stack);
        }
        freeze(target, FREEZE_TICKS);
        beam((ServerLevel) level, player.getEyePosition(), target.position().add(0, target.getBbHeight() / 2, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        if (!player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** A criatura sob a mira, a até {@link #RANGE} e sem bloco no caminho; {@code null} se não há. */
    @Nullable
    static LivingEntity aimed(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(RANGE));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box,
                StasisProjectorItem::freezable);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Pega em qualquer criatura viva, menos pessoas e o Titanovenator. */
    public static boolean freezable(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && !(entity instanceof Player)
                && !(entity instanceof TitanovenatorBoss);
    }

    /** Congela por {@code ticks}; se já estava congelada, vale o que acabar mais tarde. */
    public static void freeze(LivingEntity target, int ticks) {
        long until = target.level().getGameTime() + ticks;
        long current = target.getPersistentData().getLong(TAG_UNTIL);
        target.getPersistentData().putLong(TAG_UNTIL, Math.max(until, current));
        hold(target);
    }

    public static boolean frozen(LivingEntity entity) {
        return entity.getPersistentData().contains(TAG_UNTIL);
    }

    private static void hold(LivingEntity target) {
        target.setInvulnerable(true);
        target.setDeltaMovement(Vec3.ZERO);
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
        }
    }

    /** Solta: se ainda estiver dentro do campo da base, o núcleo a prende de novo no próximo ciclo dele. */
    private static void release(LivingEntity target) {
        target.getPersistentData().remove(TAG_UNTIL);
        target.setInvulnerable(false);
        if (target instanceof Mob mob) {
            mob.setNoAi(false);
        }
        target.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL,
                0.8F, 1.6F);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !frozen(entity)) {
            return;
        }
        if (entity.level().getGameTime() >= entity.getPersistentData().getLong(TAG_UNTIL)) {
            release(entity);
            return;
        }
        hold(entity);
        if (entity.tickCount % 4 == 0 && entity.level() instanceof ServerLevel level) {
            float angle = entity.tickCount * 0.35F;
            double radius = entity.getBbWidth() * 0.7 + 0.3;
            for (int i = 0; i < 3; i++) {
                float a = angle + i * Mth.TWO_PI / 3;
                level.sendParticles(ParticleTypes.END_ROD, entity.getX() + Mth.cos(a) * radius,
                        entity.getY() + (entity.tickCount % 40) / 40.0 * entity.getBbHeight(),
                        entity.getZ() + Mth.sin(a) * radius, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void beam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = Math.max(2, (int) (step.length() * 2));
        for (int i = 1; i < points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.stasis_projector.tooltip",
                FREEZE_TICKS / 20, COOLDOWN_TICKS / 20).withStyle(ChatFormatting.GRAY));
    }
}
