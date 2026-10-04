package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * O slot do analisador: todo jogador tem o aparelho sempre, fora do inventário, e ele nunca se perde. Segurando a
 * tecla do analisador, o cliente pede {@link #draw}: um analisador de verdade entra na mão selecionada e o item que
 * estava ali fica guardado nos dados do jogador. Soltando, {@link #stow} tira o analisador e devolve o item.
 *
 * <p>O servidor também guarda de volta sozinho quando a mão deixa de ser o analisador (trocou o slot da barra, trocou
 * de mão), quando abre um container, ao morrer (antes de soltar o inventário), ao sair e ao entrar (se o jogo caiu com
 * o aparelho na mão), e ao tentar jogar o aparelho fora. Fora desse uso, qualquer analisador no inventário some: ele
 * só existe no slot. Nada dele cai ao morrer, nem entra em moldura ou na mão de um allay.
 */
public final class AnalyzerSlot {
    /** Nos dados do jogador, enquanto o analisador está na mão: o slot da barra e o item guardado dele. */
    public static final String HOLD_TAG = "iceagesurvival.analyzer_hold";

    private AnalyzerSlot() {
    }

    public static boolean isHolding(ServerPlayer player) {
        return player.getPersistentData().contains(HOLD_TAG);
    }

    /** Põe o analisador na mão selecionada, guardando o item que estava nela. */
    public static void draw(ServerPlayer player) {
        if (isHolding(player) || player.isSpectator() || !player.isAlive()
                || player.containerMenu != player.inventoryMenu) {
            return;
        }
        int slot = player.getInventory().selected;
        ItemStack stashed = player.getInventory().items.get(slot);
        CompoundTag hold = new CompoundTag();
        hold.putInt("slot", slot);
        hold.put("item", stashed.save(new CompoundTag()));
        player.getPersistentData().put(HOLD_TAG, hold);
        player.getInventory().items.set(slot, new ItemStack(ModItems.ANALYZER.get()));
    }

    /** Tira o analisador de onde estiver e devolve o item guardado ao slot dele (ou a outro, se o slot foi ocupado). */
    public static void stow(ServerPlayer player) {
        if (!isHolding(player)) {
            return;
        }
        CompoundTag hold = player.getPersistentData().getCompound(HOLD_TAG);
        player.getPersistentData().remove(HOLD_TAG);
        if (player.isUsingItem() && player.getUseItem().is(ModItems.ANALYZER.get())) {
            player.stopUsingItem();
        }
        removeAnalyzers(player, null);
        int slot = hold.getInt("slot");
        ItemStack stashed = ItemStack.of(hold.getCompound("item"));
        if (stashed.isEmpty()) {
            return;
        }
        if (player.getInventory().items.get(slot).isEmpty()) {
            player.getInventory().items.set(slot, stashed);
        } else if (!player.getInventory().add(stashed)) {
            player.drop(stashed, false);
        }
    }

    /** Confere o uso a cada tick: a mão ainda é o analisador? Senão, guarda. E apaga analisadores fora do slot. */
    public static void check(ServerPlayer player) {
        if (isHolding(player)) {
            int slot = player.getPersistentData().getCompound(HOLD_TAG).getInt("slot");
            ItemStack held = player.getInventory().items.get(slot);
            if (player.getInventory().selected != slot || !held.is(ModItems.ANALYZER.get())
                    || player.containerMenu != player.inventoryMenu || !player.isAlive()) {
                stow(player);
            } else {
                removeAnalyzers(player, held);
            }
        } else {
            removeAnalyzers(player, null);
        }
    }

    /** Apaga do inventário, da grade de criação e do cursor todo analisador que não seja {@code keep}. */
    private static void removeAnalyzers(ServerPlayer player, ItemStack keep) {
        player.getInventory().clearOrCountMatchingItems(
                stack -> stack.is(ModItems.ANALYZER.get()) && stack != keep, -1,
                player.inventoryMenu.getCraftSlots());
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            check(player);
        }
    }

    /** Antes de soltar o inventário: o item guardado volta e cai como os outros; o analisador não. */
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stow(player);
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> drop.getItem().is(ModItems.ANALYZER.get()));
    }

    /** Jogar fora o analisador só o guarda de volta. */
    public static void onToss(ItemTossEvent event) {
        ItemEntity tossed = event.getEntity();
        if (tossed.getItem().is(ModItems.ANALYZER.get())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer player) {
                stow(player);
            }
        }
    }

    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stow(player);
        }
    }

    /** Moldura e allay pegariam o aparelho da mão. */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getItemStack().is(ModItems.ANALYZER.get())
                && (event.getTarget() instanceof ItemFrame || event.getTarget() instanceof Allay)) {
            event.setCanceled(true);
        }
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stow(player);
            check(player);
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stow(player);
        }
    }
}
