package dev.madebyfelipe.iceagesurvival.network;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

/** Servidor → cliente, ao entrar no mundo e a cada registro novo: as espécies que o jogador já escaneou. */
public record DinoFilePayload(List<ResourceLocation> registered) {
    private static Consumer<DinoFilePayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<DinoFilePayload> handler) {
        clientHandler = handler;
    }

    public static void encode(DinoFilePayload message, FriendlyByteBuf buf) {
        buf.writeCollection(message.registered, FriendlyByteBuf::writeResourceLocation);
    }

    public static DinoFilePayload decode(FriendlyByteBuf buf) {
        return new DinoFilePayload(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public static void handle(DinoFilePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
