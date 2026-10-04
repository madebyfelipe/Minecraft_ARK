package dev.madebyfelipe.iceagesurvival.network;

import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor → cliente: os atributos de uma criatura, para a tela de status. Os pontos de
 * atributo só existem no servidor, por isso vêm aqui e não pelos dados sincronizados.
 *
 * @param points     pontos por atributo, na ordem de {@link Stat#values()}
 * @param values     valor final por atributo, na mesma ordem
 * @param baseValues valor com zero pontos, para mostrar o ganho
 * @param ownerName  nome do dono, ou vazio se ele nunca foi visto por este servidor
 * @param mutations  mutações acumuladas na linhagem
 * @param healthGene se carrega o gene de mutação de vida
 * @param gestation  fração da gestação, ou −1 se não estiver prenhe
 * @param maturation fração do crescimento; 1 = adulto
 * @param bonusUnspent pontos distribuíveis ainda sem atributo (os da incubadora)
 */
public record CreatureStatusPayload(int creatureId, int[] points, double[] values, double[] baseValues,
                                    float health, double torpor, float affinity, String ownerName,
                                    int mutations, boolean healthGene, float gestation, float maturation,
                                    int bonusUnspent) {

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
                values[stat.ordinal()] = species.stats().value(stat, creature.statPoints())
                        * (stat == Stat.SPEED ? creature.genome().speedMultiplier() : 1.0);
                baseValues[stat.ordinal()] = species.stats().value(stat, 0);
            }
        });
        Player owner = creature.getOwnerUUID() == null ? null : creature.level().getPlayerByUUID(creature.getOwnerUUID());
        String ownerName = owner != null ? owner.getGameProfile().getName() : "";
        return new CreatureStatusPayload(creature.getId(), points, values, baseValues,
                creature.getHealth(), creature.torpor(), creature.affinity(), ownerName,
                creature.genome().totalMutations(), creature.genome().healthGene(),
                creature.gestationProgress(), creature.maturationProgress(), creature.bonusPoints().unspent());
    }

    public static CreatureStatusPayload decode(FriendlyByteBuf buf) {
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
                buf.readFloat(), buf.readDouble(), buf.readFloat(), buf.readUtf(),
                buf.readVarInt(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public static void encode(CreatureStatusPayload message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.creatureId);
        for (int i = 0; i < message.points.length; i++) {
            buf.writeVarInt(message.points[i]);
            buf.writeDouble(message.values[i]);
            buf.writeDouble(message.baseValues[i]);
        }
        buf.writeFloat(message.health);
        buf.writeDouble(message.torpor);
        buf.writeFloat(message.affinity);
        buf.writeUtf(message.ownerName);
        buf.writeVarInt(message.mutations);
        buf.writeBoolean(message.healthGene);
        buf.writeFloat(message.gestation);
        buf.writeFloat(message.maturation);
        buf.writeVarInt(message.bonusUnspent);
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

    public static void setClientHandler(java.util.function.Consumer<CreatureStatusPayload> handler) {
        clientHandler = handler;
    }

    public static void handle(CreatureStatusPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> clientHandler.accept(message));
        context.setPacketHandled(true);
    }
}
