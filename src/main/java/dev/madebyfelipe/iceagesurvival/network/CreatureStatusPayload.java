package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Servidor → cliente: os atributos de uma criatura, para a tela de status. Os pontos de
 * atributo só existem no servidor, por isso vêm aqui e não pelos dados sincronizados.
 *
 * @param points     pontos por atributo, na ordem de {@link Stat#values()}
 * @param values     valor final por atributo, na mesma ordem
 * @param baseValues valor com zero pontos, para mostrar o ganho
 * @param ownerName  nome do dono, ou vazio se ele nunca foi visto por este servidor
 */
public record CreatureStatusPayload(int creatureId, int[] points, double[] values, double[] baseValues,
                                    float health, double torpor, float affinity, String ownerName)
        implements CustomPacketPayload {
    public static final Type<CreatureStatusPayload> TYPE = new Type<>(IceAgeSurvival.id("creature_status"));

    public static final StreamCodec<FriendlyByteBuf, CreatureStatusPayload> STREAM_CODEC =
            StreamCodec.ofMember(CreatureStatusPayload::write, CreatureStatusPayload::read);

    /** Quem recebe; o cliente registra aqui como abrir ou atualizar a tela. */
    private static java.util.function.Consumer<CreatureStatusPayload> clientHandler = payload -> { };

    public static CreatureStatusPayload of(PrehistoricCreature creature) {
        Stat[] stats = Stat.values();
        int[] points = new int[stats.length];
        double[] values = new double[stats.length];
        double[] baseValues = new double[stats.length];
        creature.species().ifPresent(species -> {
            for (Stat stat : stats) {
                points[stat.ordinal()] = creature.statPoints().get(stat);
                values[stat.ordinal()] = species.stats().value(stat, creature.statPoints());
                baseValues[stat.ordinal()] = species.stats().value(stat, 0);
            }
        });
        Player owner = creature.getOwnerUUID() == null ? null : creature.level().getPlayerByUUID(creature.getOwnerUUID());
        String ownerName = owner != null ? owner.getGameProfile().getName() : "";
        return new CreatureStatusPayload(creature.getId(), points, values, baseValues,
                creature.getHealth(), creature.torpor(), creature.affinity(), ownerName);
    }

    private static CreatureStatusPayload read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int count = Stat.values().length;
        int[] points = new int[count];
        double[] values = new double[count];
        double[] baseValues = new double[count];
        for (int i = 0; i < count; i++) {
            points[i] = buf.readVarInt();
            values[i] = buf.readDouble();
            baseValues[i] = buf.readDouble();
        }
        return new CreatureStatusPayload(id, points, values, baseValues,
                buf.readFloat(), buf.readDouble(), buf.readFloat(), buf.readUtf());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(creatureId);
        for (int i = 0; i < points.length; i++) {
            buf.writeVarInt(points[i]);
            buf.writeDouble(values[i]);
            buf.writeDouble(baseValues[i]);
        }
        buf.writeFloat(health);
        buf.writeDouble(torpor);
        buf.writeFloat(affinity);
        buf.writeUtf(ownerName);
    }

    public int points(Stat stat) {
        return points[stat.ordinal()];
    }

    public double value(Stat stat) {
        return values[stat.ordinal()];
    }

    public double baseValue(Stat stat) {
        return baseValues[stat.ordinal()];
    }

    @Override
    public Type<CreatureStatusPayload> type() {
        return TYPE;
    }

    public static void setClientHandler(java.util.function.Consumer<CreatureStatusPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(CreatureStatusPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
