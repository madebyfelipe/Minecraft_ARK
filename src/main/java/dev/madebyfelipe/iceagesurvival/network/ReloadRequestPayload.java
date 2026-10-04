package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.firearm.FirearmShots;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente → servidor: a tecla de recarga foi apertada (D58). O servidor completa o pente da arma na mão. */
public record ReloadRequestPayload() {
    public static void encode(ReloadRequestPayload message, FriendlyByteBuf buf) {
    }

    public static ReloadRequestPayload decode(FriendlyByteBuf buf) {
        return new ReloadRequestPayload();
    }

    public static void handle(ReloadRequestPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && !player.isSpectator()) {
                FirearmShots.reloadHeld(player);
            }
        });
        context.setPacketHandled(true);
    }
}
