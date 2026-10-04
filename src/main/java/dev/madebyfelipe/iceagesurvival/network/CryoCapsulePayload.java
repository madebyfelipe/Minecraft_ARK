package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.CryoCapsuleItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor: pela aba "Cápsulas" do menu da tecla O, guardar uma criatura por perto na primeira cápsula
 * vazia do inventário ({@code store}, {@code target} = id da entidade) ou soltar à frente do jogador a criatura da
 * cápsula num espaço do inventário ({@code target} = espaço). O servidor confere tudo de novo; repetir o pedido não
 * duplica nada, porque a cápsula já mudou.
 */
public record CryoCapsulePayload(boolean store, int target) {

    public static void encode(CryoCapsulePayload message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.store);
        buf.writeVarInt(message.target);
    }

    public static CryoCapsulePayload decode(FriendlyByteBuf buf) {
        return new CryoCapsulePayload(buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(CryoCapsulePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator()) {
                return;
            }
            Inventory inventory = player.getInventory();
            if (message.store()) {
                if (player.level().getEntity(message.target()) instanceof PrehistoricCreature creature) {
                    for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                        if (CryoCapsuleItem.isEmptyCapsule(inventory.getItem(slot))) {
                            CryoCapsuleItem.freeze(player, inventory.getItem(slot), creature);
                            break;
                        }
                    }
                }
            } else if (message.target() >= 0 && message.target() < inventory.getContainerSize()) {
                ItemStack capsule = inventory.getItem(message.target());
                CryoCapsuleItem.releaseInFront(player.serverLevel(), player, capsule);
            }
            inventory.setChanged();
        });
        context.setPacketHandled(true);
    }
}
