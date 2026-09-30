package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.CreatureOrder;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente → servidor: o jogador quer dar uma ordem a uma criatura. */
public record SetOrderPayload(int creatureId, CreatureOrder order) implements CustomPacketPayload {
    public static final Type<SetOrderPayload> TYPE = new Type<>(IceAgeSurvival.id("set_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetOrderPayload::creatureId,
            NeoForgeStreamCodecs.enumCodec(CreatureOrder.class), SetOrderPayload::order,
            SetOrderPayload::new);

    @Override
    public Type<SetOrderPayload> type() {
        return TYPE;
    }

    public static void handle(SetOrderPayload payload, IPayloadContext context) {
        if (context.player().level().getEntity(payload.creatureId()) instanceof PrehistoricCreature creature) {
            CreatureCommands.setOrder(context.player(), creature, payload.order());
        }
    }
}
