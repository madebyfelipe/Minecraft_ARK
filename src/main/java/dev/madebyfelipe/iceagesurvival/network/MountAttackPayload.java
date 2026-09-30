package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Cliente → servidor: quem monta mandou a criatura morder. {@code targetId} é quem estava sob
 * a mira, ou {@link #NO_TARGET}: a mordida acontece do mesmo jeito.
 */
public record MountAttackPayload(int targetId) implements CustomPacketPayload {
    public static final int NO_TARGET = -1;

    public static final Type<MountAttackPayload> TYPE = new Type<>(IceAgeSurvival.id("mount_attack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MountAttackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MountAttackPayload::targetId,
            MountAttackPayload::new);

    @Override
    public Type<MountAttackPayload> type() {
        return TYPE;
    }

    public static void handle(MountAttackPayload payload, IPayloadContext context) {
        var player = context.player();
        if (player.getVehicle() instanceof PrehistoricCreature mount) {
            LivingEntity target = player.level().getEntity(payload.targetId()) instanceof LivingEntity living ? living : null;
            mount.attackAsMount(player, target);
        }
    }
}
