package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Cliente → servidor: o jogador quer ver os atributos de uma criatura sua. A resposta é um
 * {@link CreatureStatusPayload}; a tela aberta repete o pedido para se manter atualizada.
 */
public record StatusRequestPayload(int creatureId) implements CustomPacketPayload {
    public static final Type<StatusRequestPayload> TYPE = new Type<>(IceAgeSurvival.id("status_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatusRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StatusRequestPayload::creatureId,
            StatusRequestPayload::new);

    @Override
    public Type<StatusRequestPayload> type() {
        return TYPE;
    }

    public static void handle(StatusRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.level().getEntity(payload.creatureId()) instanceof PrehistoricCreature creature)) {
            return;
        }
        if (!creature.isOwner(player)
                || creature.distanceToSqr(player) > CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE) {
            player.displayClientMessage(Component.translatable("iceagesurvival.status.not_yours"), true);
            return;
        }
        PacketDistributor.sendToPlayer(player, CreatureStatusPayload.of(creature));
    }
}
