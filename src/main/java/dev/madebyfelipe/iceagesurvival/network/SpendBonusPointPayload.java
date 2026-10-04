package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor: o dono põe um ponto distribuível num atributo (tela de status). O servidor
 * confere dono, distância, saldo e se o atributo recebe pontos, e responde com o status novo.
 */
public record SpendBonusPointPayload(int creatureId, Stat stat) {

    public static void encode(SpendBonusPointPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.creatureId);
        buf.writeEnum(message.stat);
    }

    public static SpendBonusPointPayload decode(FriendlyByteBuf buf) {
        return new SpendBonusPointPayload(buf.readVarInt(), buf.readEnum(Stat.class));
    }

    public static void handle(SpendBonusPointPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.level().getEntity(message.creatureId()) instanceof PrehistoricCreature creature
                    && creature.isOwner(player)
                    && creature.distanceToSqr(player) <= CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE
                    && creature.spendBonusPoint(message.stat())) {
                ModPayloads.sendToPlayer(player, CreatureStatusPayload.of(creature));
            }
        });
        context.setPacketHandled(true);
    }
}
