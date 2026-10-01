package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente → servidor: o jogador quer a lista das criaturas dele, com onde cada uma está. A resposta é
 * um {@link CreatureLocationsPayload}. Com {@code highlight}, a criatura escolhida, se estiver
 * carregada, brilha por alguns segundos (o contorno do efeito Brilho aparece através das paredes).
 */
public record LocateRequestPayload(Optional<UUID> highlight) {
    private static final int HIGHLIGHT_TICKS = 20 * 15;

    public static void encode(LocateRequestPayload message, FriendlyByteBuf buf) {
        buf.writeOptional(message.highlight, FriendlyByteBuf::writeUUID);
    }

    public static LocateRequestPayload decode(FriendlyByteBuf buf) {
        return new LocateRequestPayload(buf.readOptional(FriendlyByteBuf::readUUID));
    }

    public static void handle(LocateRequestPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            message.highlight().ifPresent(id -> {
                PrehistoricCreature creature = CreatureLocator.findLoaded(player.server, id);
                if (creature != null && creature.isOwner(player)) {
                    creature.addEffect(new MobEffectInstance(MobEffects.GLOWING, HIGHLIGHT_TICKS, 0, false, false));
                }
            });
            ModPayloads.sendToPlayer(player,
                    new CreatureLocationsPayload(CreatureLocator.ownedBy(player.server, player.getUUID())));
        });
        context.setPacketHandled(true);
    }
}
