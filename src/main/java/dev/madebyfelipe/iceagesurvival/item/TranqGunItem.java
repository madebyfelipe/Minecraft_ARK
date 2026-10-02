package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.client.GunClientExtensions;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Arma tranquilizante de disparo imediato: a besta de dardos (e, por {@link TranqRifleItem}, o rifle). Sem puxar como
 * o arco: clique e o dardo sai reto e rápido, com torpor fixo ({@code multiplier} × o da flecha tranquilizante bem
 * puxada). Depois, uma espera de recarga. A munição vem da outra mão ou do inventário.
 *
 * <p>O clique não balança o braço ({@link InteractionResultHolder#consume}): no lugar, a pose de mira, o coice e a
 * arma abaixada durante a recarga ({@link GunClientExtensions}).
 */
public class TranqGunItem extends Item {
    /** Avisado no cliente a cada disparo do jogador local (o coice começa sem esperar o servidor). */
    private static BiConsumer<Player, InteractionHand> clientShotHook = (player, hand) -> { };

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

    public static void setClientShotHook(BiConsumer<Player, InteractionHand> hook) {
        clientShotHook = hook;
    }

    /** Torpor de cada disparo. */
    public double torpor() {
        return ServerConfig.TRANQ_ARROW_TORPOR.get() * torporMultiplier;
    }

    public int reloadTicks() {
        return reloadTicks;
    }

    /** Velocidade do dardo (blocos/tick); no rifle, que não tem dardo voando, só entra na conta do dano. */
    protected float speed() {
        return speed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        ItemStack round = findAmmo(player);
        boolean creative = player.getAbilities().instabuild;
        if (round.isEmpty() && !creative) {
            return InteractionResultHolder.fail(gun);
        }
        if (level instanceof ServerLevel server) {
            ItemStack fired = round.isEmpty() ? new ItemStack(ModItems.TRANQ_DART.get()) : round.copyWithCount(1);
            fire(server, player, hand, fired, creative);
            gun.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
            if (!creative) {
                round.shrink(1);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        } else {
            clientShotHook.accept(player, hand);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.3F + 0.9F));
        player.getCooldowns().addCooldown(this, reloadTicks);
        // consume, não success: o clique não balança o braço; a animação é a da arma.
        return InteractionResultHolder.consume(gun);
    }

    /**
     * O disparo no servidor. A besta lança o dardo, que voa e se recolhe do chão.
     *
     * @param fired a munição gasta, uma unidade
     */
    protected void fire(ServerLevel level, Player player, InteractionHand hand, ItemStack fired, boolean creative) {
        TranqArrow dart = new TranqArrow(level, player, fired, torpor());
        dart.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, inaccuracy);
        dart.pickup = creative ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        level.addFreshEntity(dart);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(GunClientExtensions.INSTANCE);
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
