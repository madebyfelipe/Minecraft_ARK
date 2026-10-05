package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.world.CreatureCall;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Cliente → servidor: pelo botão "Chamar" do menu da tecla O, trazer a criatura escolhida para o lado do jogador. */
public record CallCreaturePayload(UUID creature) {

    public static void encode(CallCreaturePayload message, FriendlyByteBuf buf) {
        buf.writeUUID(message.creature);
    }

    public static CallCreaturePayload decode(FriendlyByteBuf buf) {
        return new CallCreaturePayload(buf.readUUID());
    }

    public static void handle(CallCreaturePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null && !context.getSender().isSpectator()) {
                CreatureCall.request(context.getSender(), message.creature());
            }
        });
        context.setPacketHandled(true);
    }
}
