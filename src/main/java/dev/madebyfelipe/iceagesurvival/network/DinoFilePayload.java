package dev.madebyfelipe.iceagesurvival.network;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, ao entrar no mundo e a cada registro novo: as espécies que o jogador já escaneou e quantos
 * registros ele já recuperou em cada série.
 */
public record DinoFilePayload(List<ResourceLocation> registered, Map<String, Integer> records) {
    private static Consumer<DinoFilePayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<DinoFilePayload> handler) {
        clientHandler = handler;
    }

    public static void encode(DinoFilePayload message, FriendlyByteBuf buf) {
        buf.writeCollection(message.registered, FriendlyByteBuf::writeResourceLocation);
        buf.writeMap(message.records, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    public static DinoFilePayload decode(FriendlyByteBuf buf) {
        return new DinoFilePayload(buf.readList(FriendlyByteBuf::readResourceLocation),
                Map.copyOf(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)));
    }

    public static void handle(DinoFilePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
