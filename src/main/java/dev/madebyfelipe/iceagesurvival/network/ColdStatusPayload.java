package dev.madebyfelipe.iceagesurvival.network;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, uma vez por segundo: o frio do jogador, para o termômetro.
 *
 * @param exposure de 0 (aquecido) a 1 (congelado)
 * @param severity frio líquido do lugar, de −1 a 1: positivo esfria, negativo aquece
 * @param wet      se está na água ou na chuva
 */
public record ColdStatusPayload(float exposure, float severity, boolean wet) {

    private static Consumer<ColdStatusPayload> clientHandler = payload -> { };

    public static void encode(ColdStatusPayload message, FriendlyByteBuf buf) {
        buf.writeFloat(message.exposure);
        buf.writeFloat(message.severity);
        buf.writeBoolean(message.wet);
    }

    public static ColdStatusPayload decode(FriendlyByteBuf buf) {
        return new ColdStatusPayload(buf.readFloat(), buf.readFloat(), buf.readBoolean());
    }

    public static void setClientHandler(Consumer<ColdStatusPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(ColdStatusPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
