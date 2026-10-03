package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente, quando o Analisador termina de escanear: o indivíduo lido, para a ANÁLISE da DINO FILE.
 *
 * @param entityId     a criatura no mundo do cliente (para o modelo da ficha)
 * @param species      o tipo de entidade
 * @param newEntry     se o scan abriu a ficha da espécie agora
 * @param state        {@link State}: selvagem, desmaiada, sua ou de outro
 * @param mood         o humor do selvagem ({@code calm}, {@code uneasy}, {@code stressed}, {@code panicked})
 * @param danger       0 (domesticada), 1 (selvagem que só se defende), 2 (selvagem que ataca ou caça)
 */
public record ScanResultPayload(int entityId, ResourceLocation species, boolean newEntry, String name, int level,
                                boolean female, float health, float maxHealth, float torpor, float attack,
                                float armor, State state, String owner, String mood, int danger) {
    public enum State { WILD, UNCONSCIOUS, YOURS, OTHERS }

    private static Consumer<ScanResultPayload> clientHandler = payload -> { };

    public static void setClientHandler(Consumer<ScanResultPayload> handler) {
        clientHandler = handler;
    }

    /** A leitura de {@code creature} feita por {@code player}. */
    public static ScanResultPayload of(ServerPlayer player, PrehistoricCreature creature, boolean newEntry) {
        State state = creature.isTame() ? creature.isOwner(player) ? State.YOURS : State.OTHERS
                : creature.isUnconscious() ? State.UNCONSCIOUS : State.WILD;
        LivingEntity owner = creature.isTame() ? creature.getOwner() : null;
        boolean predator = creature.behavior().map(b -> b.aggressive() || b.prey().isPresent()).orElse(false);
        int danger = creature.isTame() ? 0 : predator ? 2 : 1;
        return new ScanResultPayload(creature.getId(), BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()),
                newEntry, creature.getName().getString(), creature.creatureLevel(), creature.isFemale(),
                creature.getHealth(), creature.getMaxHealth(), creature.torporFraction(),
                (float) creature.getAttributeValue(Attributes.ATTACK_DAMAGE),
                (float) creature.getAttributeValue(Attributes.ARMOR), state,
                owner == null ? "" : owner.getName().getString(),
                creature.mood().name().toLowerCase(Locale.ROOT), danger);
    }

    public static void encode(ScanResultPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeResourceLocation(message.species);
        buf.writeBoolean(message.newEntry);
        buf.writeUtf(message.name, 256);
        buf.writeVarInt(message.level);
        buf.writeBoolean(message.female);
        buf.writeFloat(message.health);
        buf.writeFloat(message.maxHealth);
        buf.writeFloat(message.torpor);
        buf.writeFloat(message.attack);
        buf.writeFloat(message.armor);
        buf.writeEnum(message.state);
        buf.writeUtf(message.owner, 64);
        buf.writeUtf(message.mood, 32);
        buf.writeVarInt(message.danger);
    }

    public static ScanResultPayload decode(FriendlyByteBuf buf) {
        return new ScanResultPayload(buf.readVarInt(), buf.readResourceLocation(), buf.readBoolean(), buf.readUtf(256),
                buf.readVarInt(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readEnum(State.class), buf.readUtf(64), buf.readUtf(32), buf.readVarInt());
    }

    public static void handle(ScanResultPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
