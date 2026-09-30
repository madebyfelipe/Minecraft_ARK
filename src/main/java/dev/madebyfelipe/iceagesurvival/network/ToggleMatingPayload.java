package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente → servidor: o dono liga ou desliga o acasalamento de uma criatura (tela de status). */
public record ToggleMatingPayload(int creatureId) implements CustomPacketPayload {
    public static final Type<ToggleMatingPayload> TYPE = new Type<>(IceAgeSurvival.id("toggle_mating"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMatingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToggleMatingPayload::creatureId,
            ToggleMatingPayload::new);

    @Override
    public Type<ToggleMatingPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleMatingPayload payload, IPayloadContext context) {
        var player = context.player();
        if (player.level().getEntity(payload.creatureId()) instanceof PrehistoricCreature creature
                && creature.isOwner(player) && !creature.isBaby() && creature.breedingProfile().isPresent()
                && creature.distanceToSqr(player) <= CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE) {
            creature.setMatingEnabled(!creature.isMatingEnabled());
        }
    }
}
