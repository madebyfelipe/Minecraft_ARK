package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Cliente → servidor: o estado de voo da montaria aérea, decidido pela física de voo no cliente de
 * quem monta. O servidor só valida e espelha (tira a gravidade); a posição chega como a de qualquer veículo.
 */
public record FlightInputPayload(int mountId, boolean flying) {

    public static void encode(FlightInputPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.mountId);
        buf.writeBoolean(message.flying);
    }

    public static FlightInputPayload decode(FriendlyByteBuf buf) {
        return new FlightInputPayload(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(FlightInputPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.getVehicle() instanceof PrehistoricCreature mount
                    && mount.getId() == message.mountId()
                    && mount.getControllingPassenger() == player
                    && mount.isOwner(player)
                    && mount.isRideReady()
                    && mount.isFlightMount()) {
                mount.setFlying(message.flying());
            }
        });
        context.setPacketHandled(true);
    }
}
