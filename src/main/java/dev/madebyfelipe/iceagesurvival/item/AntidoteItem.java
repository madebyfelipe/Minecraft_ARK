package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.entity.VenomBite;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Antídoto: carvão ativado num frasco, feito na mesa química. Bebido, ou dado a uma criatura (clique nela), cura a
 * peçonha da Megalania; devolve o frasco vazio.
 */
public class AntidoteItem extends Item {
    private static final int DRINK_TICKS = 32;

    public AntidoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (!level.isClientSide) {
            VenomBite.cure(drinker);
        }
        if (drinker instanceof ServerPlayer player) {
            net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
        }
        Player player = drinker instanceof Player p ? p : null;
        if (player != null && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (player != null && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    /** Dado a uma criatura envenenada: cura na hora, sem beber. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!VenomBite.isEnvenomed(target)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            VenomBite.cure(target);
            target.playSound(SoundEvents.GENERIC_DRINK, 0.8F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                    player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
                }
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.antidote.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
