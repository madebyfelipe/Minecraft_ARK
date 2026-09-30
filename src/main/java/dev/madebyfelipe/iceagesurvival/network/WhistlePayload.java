package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.Whistle;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Cliente → servidor: o jogador assobiou. {@code aimedId} é a criatura sob a mira, ou
 * {@link #NO_TARGET} para assobiar para todas as suas ao alcance.
 */
public record WhistlePayload(Whistle whistle, int aimedId) implements CustomPacketPayload {
    public static final int NO_TARGET = -1;
    public static final Type<WhistlePayload> TYPE = new Type<>(IceAgeSurvival.id("whistle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhistlePayload> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(Whistle.class), WhistlePayload::whistle,
            ByteBufCodecs.VAR_INT, WhistlePayload::aimedId,
            WhistlePayload::new);

    @Override
    public Type<WhistlePayload> type() {
        return TYPE;
    }

    public static void handle(WhistlePayload payload, IPayloadContext context) {
        var player = context.player();
        PrehistoricCreature aimed = player.level().getEntity(payload.aimedId()) instanceof PrehistoricCreature creature
                ? creature : null;
        CreatureCommands.whistle(player, payload.whistle(), aimed);
    }
}
