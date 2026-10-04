package dev.madebyfelipe.iceagesurvival.firearm;

import dev.madebyfelipe.iceagesurvival.client.firearm.FirearmClientExtensions;
import dev.madebyfelipe.iceagesurvival.core.firearms.Ballistics;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Uma arma de fogo do estilo Dino Crisis 2 (D58), com modelo 3D GeckoLib ({@code tools/gen_dc2_weapons.py}).
 *
 * <p>Pente no próprio item ({@link #ROUNDS_TAG}); a munição é da família da arma, um item por tiro. Semiautomática:
 * um tiro por clique, com a espera {@link Firearm#interval()}. Automática: segurar o botão atira sem parar. Vazio o
 * pente, recarrega sozinha se houver munição; a tecla de recarga ({@code FirearmsClient}) completa o pente antes.
 * Durante a recarga a arma não atira (a espera do item). O tiro em si é do servidor ({@link FirearmShots}); o cliente
 * só faz o coice e a animação na hora do clique.
 */
public class FirearmItem extends Item implements GeoItem {
    public static final String ROUNDS_TAG = "Rounds";
    public static final String CONTROLLER = "main";
    /** Usado sem parar enquanto o botão está apertado (a automática). */
    private static final int USE_DURATION = 72000;

    /** Avisado no cliente a cada tiro do jogador local (coice, animação e chute da câmera sem esperar o servidor). */
    private static ShotHook clientShotHook = (player, hand, stack, gun) -> { };

    private final Firearm gun;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    @FunctionalInterface
    public interface ShotHook {
        void shot(Player player, InteractionHand hand, ItemStack stack, Firearm gun);
    }

    public FirearmItem(Properties properties, Firearm gun) {
        super(properties.stacksTo(1));
        this.gun = gun;
    }

    public static void setClientShotHook(ShotHook hook) {
        clientShotHook = hook;
    }

    public Firearm gun() {
        return gun;
    }

    public static int rounds(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(ROUNDS_TAG) : 0;
    }

    public static void setRounds(ItemStack stack, int rounds) {
        stack.getOrCreateTag().putInt(ROUNDS_TAG, rounds);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (rounds(stack) <= 0) {
            if (!level.isClientSide) {
                FirearmShots.reload(player, hand);
            }
            return InteractionResultHolder.fail(stack);
        }
        shoot(level, player, hand, stack);
        if (gun.automatic()) {
            player.startUsingItem(hand);
        }
        // consume, não success: o clique não balança o braço; a animação é a da arma.
        return InteractionResultHolder.consume(stack);
    }

    /** A automática: enquanto o botão está apertado, um tiro a cada {@link Firearm#interval()}. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player) || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        InteractionHand hand = player.getUsedItemHand();
        if (rounds(stack) <= 0) {
            player.stopUsingItem();
            if (!level.isClientSide) {
                FirearmShots.reload(player, hand);
            }
            return;
        }
        shoot(level, player, hand, stack);
    }

    private void shoot(Level level, Player player, InteractionHand hand, ItemStack stack) {
        player.getCooldowns().addCooldown(this, gun.interval());
        if (level instanceof ServerLevel server) {
            int left = rounds(stack) - 1;
            setRounds(stack, left);
            FirearmShots.fire(server, player, hand, gun);
            if (left <= 0) {
                if (player.isUsingItem()) {
                    player.stopUsingItem();
                }
                FirearmShots.reloadAfterLastShot(player, hand);
            }
        } else {
            clientShotHook.shot(player, hand, stack, gun);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server) {
            GeoItem.getOrAssignId(stack, server);
        }
    }

    /** O pente e o id do GeckoLib mudam a cada tiro: isso não deve fazer a arma abaixar e subir na mão. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    /** A barra do item mostra o pente: âmbar cheio, vermelho no fim. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return rounds(stack) < gun.magazine();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(rounds(stack), 0, gun.magazine()) / gun.magazine());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return rounds(stack) * 4 <= gun.magazine() ? 0xE0402A : 0xF2B036;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.firearm.magazine", rounds(stack), gun.magazine())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceagesurvival.firearm.ammo",
                Firearms.ammo(gun.ammo()).getDescription()).withStyle(ChatFormatting.GRAY));
        // Os tiros para matar do DC2: o raptor marrom, o raptor vermelho e o Alossauro (couro).
        tooltip.add(Component.translatable("item.iceagesurvival.firearm.hits",
                Ballistics.hitsToKill(gun, 10.0, 800, false), Ballistics.hitsToKill(gun, 10.0, 1600, false),
                Ballistics.hitsToKill(gun, 10.0, 5000, true)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.iceagesurvival." + gun.id() + ".tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Chamado ainda no construtor de {@link Item}: a extensão guarda o item e só lê a arma ao desenhar. */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new FirearmClientExtensions(this));
    }

    // ---- GeckoLib: em repouso, sem animação; o tiro e a recarga são disparados (triggerAnim) pelo cliente ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = "animation." + gun.id() + ".";
        controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim("fire", RawAnimation.begin().thenPlay(prefix + "fire"))
                .triggerableAnim("reload", RawAnimation.begin().thenPlay(prefix + "reload")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
