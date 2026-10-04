package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor: o jogador quer ver os atributos de uma criatura sua. A resposta é um
 * {@link CreatureStatusPayload}; a tela aberta repete o pedido para se manter atualizada.
 */
public record StatusRequestPayload(int creatureId) {
    public static void encode(StatusRequestPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.creatureId);
    }

    public static StatusRequestPayload decode(FriendlyByteBuf buf) {
        return new StatusRequestPayload(buf.readVarInt());
    }

    public static void handle(StatusRequestPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || !(player.level().getEntity(message.creatureId()) instanceof PrehistoricCreature creature)) {
                return;
            }
            if (!creature.canCommand(player)
                    || creature.distanceToSqr(player)
                    > CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE) {
                player.displayClientMessage(Component.translatable("iceagesurvival.status.not_yours"), true);
                return;
            }
            ModPayloads.sendToPlayer(player, CreatureStatusPayload.of(creature));
        });
        context.setPacketHandled(true);
    }
}
