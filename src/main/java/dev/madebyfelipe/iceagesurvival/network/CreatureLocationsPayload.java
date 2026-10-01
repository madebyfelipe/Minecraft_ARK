package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Servidor → cliente: as criaturas domesticadas do jogador e a última posição conhecida de cada uma. */
public record CreatureLocationsPayload(List<CreatureLocator.Entry> entries) {
    /** Quem recebe; o cliente registra aqui a tela e a bússola. */
    private static Consumer<CreatureLocationsPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<CreatureLocationsPayload> handler) {
        clientHandler = handler;
    }

    public static void encode(CreatureLocationsPayload message, FriendlyByteBuf buf) {
        buf.writeCollection(message.entries, (out, entry) -> {
            out.writeUUID(entry.creature());
            out.writeUUID(entry.owner());
            out.writeResourceLocation(entry.type());
            out.writeUtf(entry.name(), 256);
            out.writeVarInt(entry.level());
            out.writeResourceKey(entry.dimension());
            out.writeBlockPos(entry.pos());
        });
    }

    public static CreatureLocationsPayload decode(FriendlyByteBuf buf) {
        return new CreatureLocationsPayload(buf.readList(in -> new CreatureLocator.Entry(in.readUUID(), in.readUUID(),
                in.readResourceLocation(), in.readUtf(256), in.readVarInt(),
                in.readResourceKey(Registries.DIMENSION), in.readBlockPos())));
    }

    public static void handle(CreatureLocationsPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
