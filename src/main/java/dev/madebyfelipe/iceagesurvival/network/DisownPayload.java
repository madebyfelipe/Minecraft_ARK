package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor: o dono deserda a criatura (tela de status, depois de confirmar). O servidor confere que é o
 * dono de verdade e que ela está ao alcance dos comandos.
 */
public record DisownPayload(int creatureId) {

    public static void encode(DisownPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.creatureId);
    }

    public static DisownPayload decode(FriendlyByteBuf buf) {
        return new DisownPayload(buf.readVarInt());
    }

    public static void handle(DisownPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null
                    && player.level().getEntity(message.creatureId()) instanceof PrehistoricCreature creature
                    && creature.distanceToSqr(player) <= CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE
                    && creature.disown(player)) {
                player.displayClientMessage(Component.translatable("iceagesurvival.disown.done", creature.getName()),
                        true);
            }
        });
        context.setPacketHandled(true);
    }
}
