package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.cryo.CryoStorage;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor, pela aba "Criogenia" do menu da tecla O: pedir a lista das criaturas congeladas ({@code LIST}),
 * guardar uma criatura própria por perto ({@code STORE}, {@code entity} = id da entidade) ou soltar à frente do
 * jogador uma criatura congelada ({@code RELEASE}, {@code entry} = id da entrada). A resposta é sempre a lista nova
 * ({@link CryoListPayload}). O servidor confere tudo de novo; repetir o pedido não duplica nada, porque a criatura
 * ou a entrada já mudou.
 */
public record CryoCapsulePayload(Action action, int entity, UUID entry) {
    private static final UUID NONE = new UUID(0L, 0L);

    public enum Action {
        LIST, STORE, RELEASE
    }

    public static CryoCapsulePayload list() {
        return new CryoCapsulePayload(Action.LIST, -1, NONE);
    }

    public static CryoCapsulePayload store(int entity) {
        return new CryoCapsulePayload(Action.STORE, entity, NONE);
    }

    public static CryoCapsulePayload release(UUID entry) {
        return new CryoCapsulePayload(Action.RELEASE, -1, entry);
    }

    public static void encode(CryoCapsulePayload message, FriendlyByteBuf buf) {
        buf.writeEnum(message.action);
        buf.writeVarInt(message.entity);
        buf.writeUUID(message.entry);
    }

    public static CryoCapsulePayload decode(FriendlyByteBuf buf) {
        return new CryoCapsulePayload(buf.readEnum(Action.class), buf.readVarInt(), buf.readUUID());
    }

    public static void handle(CryoCapsulePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!player.isSpectator()) {
                if (message.action() == Action.STORE
                        && player.level().getEntity(message.entity()) instanceof PrehistoricCreature creature) {
                    CryoStorage.freeze(player, creature);
                } else if (message.action() == Action.RELEASE) {
                    CryoStorage.releaseInFront(player, message.entry());
                }
            }
            CryoStorage.sync(player);
        });
        context.setPacketHandled(true);
    }
}
