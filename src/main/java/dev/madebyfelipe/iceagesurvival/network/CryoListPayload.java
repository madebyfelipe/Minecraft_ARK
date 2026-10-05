package dev.madebyfelipe.iceagesurvival.network;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

/** Servidor → cliente: as criaturas congeladas do jogador, para a aba "Criogenia" do menu da tecla O. */
public record CryoListPayload(List<Row> rows) {
    /** Uma criatura congelada: id da entrada (o que o menu pede para soltar), espécie, nome e nível. */
    public record Row(UUID entry, ResourceLocation type, String name, int level) {
    }

    /** Quem recebe; a tela da criogenia se registra aqui. */
    private static Consumer<CryoListPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<CryoListPayload> handler) {
        clientHandler = handler;
    }

    public static void encode(CryoListPayload message, FriendlyByteBuf buf) {
        buf.writeCollection(message.rows, (out, row) -> {
            out.writeUUID(row.entry());
            out.writeResourceLocation(row.type());
            out.writeUtf(row.name(), 256);
            out.writeVarInt(row.level());
        });
    }

    public static CryoListPayload decode(FriendlyByteBuf buf) {
        return new CryoListPayload(buf.readList(in -> new Row(in.readUUID(), in.readResourceLocation(),
                in.readUtf(256), in.readVarInt())));
    }

    public static void handle(CryoListPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
