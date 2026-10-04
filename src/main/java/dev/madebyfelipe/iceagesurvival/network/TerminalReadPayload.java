package dev.madebyfelipe.iceagesurvival.network;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, ao destravar (ou tentar) um registro: a série, quantos registros dela o jogador tem agora e se
 * desta vez veio um novo ({@code false}: ele já tinha lido este terminal).
 */
public record TerminalReadPayload(String series, int records, boolean newRecord) {
    private static Consumer<TerminalReadPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<TerminalReadPayload> handler) {
        clientHandler = handler;
    }

    public static void encode(TerminalReadPayload message, FriendlyByteBuf buf) {
        buf.writeUtf(message.series);
        buf.writeVarInt(message.records);
        buf.writeBoolean(message.newRecord);
    }

    public static TerminalReadPayload decode(FriendlyByteBuf buf) {
        return new TerminalReadPayload(buf.readUtf(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(TerminalReadPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
