package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente: alguém começou a recarregar (D58). O cliente toca a animação de recarga da arma e, se for ele
 * mesmo, abaixa a arma na primeira pessoa até o fim da espera. Vai para quem rastreia o atirador e para ele mesmo.
 */
public record FirearmReloadPayload(int shooterId, Firearm gun, boolean mainHand) {
    private static Consumer<FirearmReloadPayload> clientHandler = payload -> { };

    public static void encode(FirearmReloadPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeEnum(message.gun);
        buf.writeBoolean(message.mainHand);
    }

    public static FirearmReloadPayload decode(FriendlyByteBuf buf) {
        return new FirearmReloadPayload(buf.readVarInt(), buf.readEnum(Firearm.class), buf.readBoolean());
    }

    public static void setClientHandler(Consumer<FirearmReloadPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(FirearmReloadPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
