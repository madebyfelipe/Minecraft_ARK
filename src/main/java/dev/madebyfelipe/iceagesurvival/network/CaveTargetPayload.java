package dev.madebyfelipe.iceagesurvival.network;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, quando o jogador pega o rastreador da caverna e quando o alvo muda: a boca da caverna da arena
 * mais próxima, ou vazio se não há nenhuma naquela dimensão.
 */
public record CaveTargetPayload(Optional<GlobalPos> mouth) {
    private static Consumer<CaveTargetPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<CaveTargetPayload> handler) {
        clientHandler = handler;
    }

    public static void encode(CaveTargetPayload message, FriendlyByteBuf buf) {
        buf.writeOptional(message.mouth, FriendlyByteBuf::writeGlobalPos);
    }

    public static CaveTargetPayload decode(FriendlyByteBuf buf) {
        return new CaveTargetPayload(buf.readOptional(FriendlyByteBuf::readGlobalPos));
    }

    public static void handle(CaveTargetPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
