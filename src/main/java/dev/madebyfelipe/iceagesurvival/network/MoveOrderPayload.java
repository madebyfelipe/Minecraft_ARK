package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Cliente → servidor: o jogador quer que suas criaturas ao alcance andem até o bloco mirado e fiquem lá. */
public record MoveOrderPayload(BlockPos destination) {

    public static void encode(MoveOrderPayload message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.destination);
    }

    public static MoveOrderPayload decode(FriendlyByteBuf buf) {
        return new MoveOrderPayload(buf.readBlockPos());
    }

    public static void handle(MoveOrderPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null && !context.getSender().isSpectator()) {
                CreatureCommands.orderMove(context.getSender(), message.destination());
            }
        });
        context.setPacketHandled(true);
    }
}
