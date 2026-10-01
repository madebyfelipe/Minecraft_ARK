package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.Whistle;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Cliente → servidor: o jogador assobiou. {@code aimedId} é a criatura sob a mira, ou
 * {@link #NO_TARGET} para assobiar para todas as suas ao alcance.
 */
public record WhistlePayload(Whistle whistle, int aimedId) {
    public static final int NO_TARGET = -1;

    public static void encode(WhistlePayload message, FriendlyByteBuf buf) {
        buf.writeEnum(message.whistle);
        buf.writeVarInt(message.aimedId);
    }

    public static WhistlePayload decode(FriendlyByteBuf buf) {
        return new WhistlePayload(buf.readEnum(Whistle.class), buf.readVarInt());
    }

    public static void handle(WhistlePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                PrehistoricCreature aimed = player.level().getEntity(message.aimedId()) instanceof PrehistoricCreature creature
                        ? creature : null;
                CreatureCommands.whistle(player, message.whistle(), aimed);
            }
        });
        context.setPacketHandled(true);
    }
}
