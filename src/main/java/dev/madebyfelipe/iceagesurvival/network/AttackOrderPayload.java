package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente → servidor: o jogador quer que suas criaturas ataquem um alvo. */
public record AttackOrderPayload(int targetId) implements CustomPacketPayload {
    public static final Type<AttackOrderPayload> TYPE = new Type<>(IceAgeSurvival.id("attack_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttackOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AttackOrderPayload::targetId,
            AttackOrderPayload::new);

    @Override
    public Type<AttackOrderPayload> type() {
        return TYPE;
    }

    public static void handle(AttackOrderPayload payload, IPayloadContext context) {
        if (context.player().level().getEntity(payload.targetId()) instanceof LivingEntity target) {
            CreatureCommands.orderAttack(context.player(), target);
        }
    }
}
