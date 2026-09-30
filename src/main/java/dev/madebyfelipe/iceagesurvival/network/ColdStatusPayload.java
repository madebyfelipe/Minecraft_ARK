package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor → cliente, uma vez por segundo: o frio do jogador, para o termômetro.
 *
 * @param exposure de 0 (aquecido) a 1 (congelado)
 * @param severity frio líquido do lugar, de −1 a 1: positivo esfria, negativo aquece
 * @param wet      se está na água ou na chuva
 */
public record ColdStatusPayload(float exposure, float severity, boolean wet) implements CustomPacketPayload {
    public static final Type<ColdStatusPayload> TYPE = new Type<>(IceAgeSurvival.id("cold_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ColdStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ColdStatusPayload::exposure,
            ByteBufCodecs.FLOAT, ColdStatusPayload::severity,
            ByteBufCodecs.BOOL, ColdStatusPayload::wet,
            ColdStatusPayload::new);

    private static Consumer<ColdStatusPayload> clientHandler = payload -> { };

    @Override
    public Type<ColdStatusPayload> type() {
        return TYPE;
    }

    public static void setClientHandler(Consumer<ColdStatusPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(ColdStatusPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
