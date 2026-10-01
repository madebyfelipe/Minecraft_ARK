package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Cliente → servidor: quem monta mandou a criatura morder. {@code targetId} é quem estava sob
 * a mira, ou {@link #NO_TARGET}: a mordida acontece do mesmo jeito.
 */
public record MountAttackPayload(int targetId) {
    public static final int NO_TARGET = -1;

    public static void encode(MountAttackPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.targetId);
    }

    public static MountAttackPayload decode(FriendlyByteBuf buf) {
        return new MountAttackPayload(buf.readVarInt());
    }

    public static void handle(MountAttackPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.getVehicle() instanceof PrehistoricCreature mount) {
                LivingEntity target = player.level().getEntity(message.targetId()) instanceof LivingEntity living
                        ? living : null;
                mount.attackAsMount(player, target);
            }
        });
        context.setPacketHandled(true);
    }
}
