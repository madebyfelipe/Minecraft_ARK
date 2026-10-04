package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.item.AnalyzerSlot;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente → servidor: a tecla do analisador foi apertada ({@code held}) ou solta. Ver {@link AnalyzerSlot}. */
public record AnalyzerHoldPayload(boolean held) {
    public static void encode(AnalyzerHoldPayload message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.held);
    }

    public static AnalyzerHoldPayload decode(FriendlyByteBuf buf) {
        return new AnalyzerHoldPayload(buf.readBoolean());
    }

    public static void handle(AnalyzerHoldPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (message.held()) {
                AnalyzerSlot.draw(player);
            } else {
                AnalyzerSlot.stow(player);
            }
        });
        context.setPacketHandled(true);
    }
}
