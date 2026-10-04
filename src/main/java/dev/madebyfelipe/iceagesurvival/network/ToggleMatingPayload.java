package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Cliente → servidor: o dono liga ou desliga o acasalamento de uma criatura (tela de status). */
public record ToggleMatingPayload(int creatureId) {

    public static void encode(ToggleMatingPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.creatureId);
    }

    public static ToggleMatingPayload decode(FriendlyByteBuf buf) {
        return new ToggleMatingPayload(buf.readVarInt());
    }

    public static void handle(ToggleMatingPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null
                    && player.level().getEntity(message.creatureId()) instanceof PrehistoricCreature creature
                    && creature.canCommand(player) && !creature.isBaby() && creature.breedingProfile().isPresent()
                    && creature.distanceToSqr(player) <= CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE) {
                creature.setMatingEnabled(!creature.isMatingEnabled());
            }
        });
        context.setPacketHandled(true);
    }
}
