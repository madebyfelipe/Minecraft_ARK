package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FlightInputPayload(int mountId, boolean ascend) {

    public static void encode(FlightInputPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.mountId);
        buf.writeBoolean(message.ascend);
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
                    && mount.isSaddled()
                    && mount.isFlightMount()) {
                mount.setFlightInput(message.ascend());
            }
        });
        context.setPacketHandled(true);
    }
}
