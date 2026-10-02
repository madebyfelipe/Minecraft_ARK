package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Arma tranquilizante de disparo imediato: o rifle e a besta de dardos. Sem puxar como o arco: clique e o dardo
 * sai reto e rápido, com torpor fixo ({@code multiplier} × o da flecha tranquilizante bem puxada). Depois, uma
 * espera de recarga. A munição vem da outra mão ou do inventário.
 */
public class TranqGunItem extends Item {
    private final double torporMultiplier;
    private final float speed;
    private final float inaccuracy;
    private final int reloadTicks;
    private final Predicate<ItemStack> ammo;
    private final SoundEvent sound;

    public TranqGunItem(Properties properties, double torporMultiplier, float speed, float inaccuracy, int reloadTicks,
                        Predicate<ItemStack> ammo, SoundEvent sound) {
        super(properties);
        this.torporMultiplier = torporMultiplier;
        this.speed = speed;
        this.inaccuracy = inaccuracy;
        this.reloadTicks = reloadTicks;
        this.ammo = ammo;
        this.sound = sound;
    }

    /** Torpor de cada disparo. */
    public double torpor() {
        return ServerConfig.TRANQ_ARROW_TORPOR.get() * torporMultiplier;
    }

    public int reloadTicks() {
        return reloadTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        ItemStack round = findAmmo(player);
        boolean creative = player.getAbilities().instabuild;
        if (round.isEmpty() && !creative) {
            return InteractionResultHolder.fail(gun);
        }
        ItemStack fired = round.isEmpty() ? new ItemStack(ModItems.TRANQ_DART.get()) : round.copyWithCount(1);
        if (!level.isClientSide) {
            TranqArrow dart = new TranqArrow(level, player, fired, torpor());
            dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
            dart.pickup = creative ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
            level.addFreshEntity(dart);
            gun.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
            if (!creative) {
                round.shrink(1);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.3F + 0.9F));
        player.getCooldowns().addCooldown(this, reloadTicks);
        return InteractionResultHolder.sidedSuccess(gun, level.isClientSide);
    }

    /** Munição: a da outra mão primeiro, depois a do inventário. */
    private ItemStack findAmmo(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (ammo.test(held)) {
                return held;
            }
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ammo.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
