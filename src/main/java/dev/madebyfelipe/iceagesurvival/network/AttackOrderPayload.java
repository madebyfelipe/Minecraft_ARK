package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Cliente → servidor: o jogador quer que suas criaturas ataquem um alvo. */
public record AttackOrderPayload(int targetId) {

    public static void encode(AttackOrderPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.targetId);
    }

    public static AttackOrderPayload decode(FriendlyByteBuf buf) {
        return new AttackOrderPayload(buf.readVarInt());
    }

    public static void handle(AttackOrderPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null
                    && context.getSender().level().getEntity(message.targetId()) instanceof LivingEntity target) {
                CreatureCommands.orderAttack(context.getSender(), target);
            }
        });
        context.setPacketHandled(true);
    }
}
