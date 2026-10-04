package dev.madebyfelipe.iceagesurvival.network;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, quando o jogador pega o Receptor de Sinal e quando o alvo muda: o centro da base militar mais
 * próxima, ou vazio se não há nenhuma naquela dimensão.
 */
public record BaseSignalPayload(Optional<GlobalPos> base) {
    private static Consumer<BaseSignalPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<BaseSignalPayload> handler) {
        clientHandler = handler;
    }

    public static void encode(BaseSignalPayload message, FriendlyByteBuf buf) {
        buf.writeOptional(message.base, FriendlyByteBuf::writeGlobalPos);
    }

    public static BaseSignalPayload decode(FriendlyByteBuf buf) {
        return new BaseSignalPayload(buf.readOptional(FriendlyByteBuf::readGlobalPos));
    }

    public static void handle(BaseSignalPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
