package dev.madebyfelipe.iceagesurvival.primal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Estação primitiva sem tela: os itens ficam à vista em cima dela (sincronizados para o renderer do
 * cliente) e entram e saem no clique. Clique com um item que tem receita põe um dele no espaço
 * mirado (ou no primeiro livre); clique de mão vazia, ou agachado, tira.
 *
 * <p>A decisão do clique roda nos dois lados, com os itens e as receitas que o cliente já recebeu,
 * para o braço balançar só quando algo acontece; só o servidor muda o estado.
 */
public abstract class PrimalStationBlockEntity extends BlockEntity {
    protected final NonNullList<ItemStack> items;

    protected PrimalStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public int size() {
        return items.size();
    }

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /** Os itens, para derrubar quando o bloco quebra. */
    public NonNullList<ItemStack> items() {
        return items;
    }

    /** O item tem receita nesta estação. */
    public abstract boolean accepts(ItemStack stack);

    /** Um tick de servidor; as estações de golpe não fazem nada com o tempo. */
    public void serverTick() {
    }

    /** Chamado depois que um item entra no espaço {@code slot}. */
    protected void onInserted(int slot) {
    }

    /** Chamado depois que o item do espaço {@code slot} sai. */
    protected void onRemoved(int slot) {
    }

    /** O clique de um jogador mirando o espaço {@code aimed}. */
    public InteractionResult interact(Player player, InteractionHand hand, int aimed) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = level == null || level.isClientSide;
        if (!held.isEmpty() && !player.isSecondaryUseActive() && accepts(held)) {
            int slot = freeSlot(aimed);
            if (slot >= 0) {
                if (!client) {
                    insert(slot, player.getAbilities().instabuild ? held.copyWithCount(1) : held.split(1));
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                return InteractionResult.sidedSuccess(client);
            }
        }
        if (held.isEmpty() || player.isSecondaryUseActive()) {
            int slot = filledSlot(aimed);
            if (slot >= 0) {
                if (!client) {
                    ItemStack taken = take(slot);
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                    level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                            0.8F + level.random.nextFloat() * 0.4F);
                }
                return InteractionResult.sidedSuccess(client);
            }
        }
        return InteractionResult.PASS;
    }

    /** Põe um item no espaço, sem conferir receita (o clique já conferiu). */
    public void insert(int slot, ItemStack stack) {
        items.set(slot, stack);
        onInserted(slot);
        changed();
    }

    /** Tira o item do espaço. */
    public ItemStack take(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        onRemoved(slot);
        changed();
        return stack;
    }

    /** O espaço mirado se estiver livre; senão o primeiro livre; -1 se todos ocupados. */
    protected int freeSlot(int aimed) {
        if (aimed >= 0 && aimed < items.size() && items.get(aimed).isEmpty()) {
            return aimed;
        }
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** O espaço mirado se tiver item; senão o primeiro com item; -1 se todos vazios. */
    protected int filledSlot(int aimed) {
        if (aimed >= 0 && aimed < items.size() && !items.get(aimed).isEmpty()) {
            return aimed;
        }
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Salva e manda os itens ao cliente, para o renderer. */
    protected void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items, true);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
