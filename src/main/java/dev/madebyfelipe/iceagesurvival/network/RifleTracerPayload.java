package dev.madebyfelipe.iceagesurvival.network;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, a cada tiro do rifle: o traçante, da boca do cano ao ponto atingido. Vai para quem rastreia o
 * atirador e para ele mesmo. O tiro em si (raio, dano e torpor) é todo do servidor; isto é só o efeito.
 *
 * @param shooterId id da entidade que atirou (o cliente usa para o coice em terceira pessoa e, se for ele mesmo em
 *                  primeira pessoa, para recalcular a boca do cano na arma da tela)
 * @param start     boca do cano
 * @param end       onde o tiro parou
 * @param impact    o que foi atingido
 */
public record RifleTracerPayload(int shooterId, Vec3 start, Vec3 end, Impact impact) {

    /** O que o tiro atingiu: nada (alcance esgotado), um bloco ou uma criatura. */
    public enum Impact {
        NONE, BLOCK, ENTITY
    }

    private static Consumer<RifleTracerPayload> clientHandler = payload -> { };

    public static void encode(RifleTracerPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        writeVec(buf, message.start);
        writeVec(buf, message.end);
        buf.writeEnum(message.impact);
    }

    public static RifleTracerPayload decode(FriendlyByteBuf buf) {
        return new RifleTracerPayload(buf.readVarInt(), readVec(buf), readVec(buf), buf.readEnum(Impact.class));
    }

    public static void setClientHandler(Consumer<RifleTracerPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(RifleTracerPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }

    private static void writeVec(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
