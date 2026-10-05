package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Botão "Chamar" do menu da tecla O: a criatura aparece ao lado de quem chamou, a qualquer distância na mesma
 * dimensão. Carregada, vem na hora. Num chunk descarregado, um ticket temporário carrega a última posição conhecida
 * ({@link CreatureLocator}) e ela vem assim que entrar no mundo; se não aparecer em {@link #WAIT_TICKS}, avisa.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public final class CreatureCall {
    /** Quanto esperar a criatura de um chunk descarregado entrar no mundo. */
    static final int WAIT_TICKS = 20 * 5;
    private static final TicketType<ChunkPos> TICKET =
            TicketType.create(IceAgeSurvival.MODID + "_call", Comparator.comparingLong(ChunkPos::toLong), WAIT_TICKS);

    /** Chamadas esperando a criatura carregar: criatura → quem chamou e até quando. */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(UUID player, long deadline) {
    }

    private CreatureCall() {
    }

    /** Pedido do jogador. O servidor confere que a criatura é dele (ou do time) e está na mesma dimensão. */
    public static void request(ServerPlayer player, UUID creatureId) {
        MinecraftServer server = player.server;
        Optional<CreatureLocator.Entry> entry = CreatureLocator.visibleTo(server, player).stream()
                .filter(candidate -> candidate.creature().equals(creatureId)).findFirst();
        if (entry.isEmpty()) {
            return;
        }
        Component name = Component.literal(entry.get().name());
        if (!entry.get().dimension().equals(player.level().dimension())) {
            player.displayClientMessage(Component.translatable("iceagesurvival.call.other_dimension", name), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.getEntity(creatureId) instanceof PrehistoricCreature creature && creature.isAlive()) {
            bring(player, creature);
            return;
        }
        ChunkPos chunk = new ChunkPos(entry.get().pos());
        level.getChunkSource().addRegionTicket(TICKET, chunk, 2, chunk);
        PENDING.put(creatureId, new Pending(player.getUUID(), server.getTickCount() + WAIT_TICKS));
        player.displayClientMessage(Component.translatable("iceagesurvival.call.waiting", name), true);
    }

    /**
     * Traz a criatura para o lado do jogador, se houver espaço para o corpo dela.
     *
     * @return se ela veio
     */
    public static boolean bring(ServerPlayer player, PrehistoricCreature creature) {
        if (!creature.canCommand(player) || creature.level() != player.level()) {
            return false;
        }
        Vec3 spot = findSpot(player, creature);
        if (spot == null) {
            player.displayClientMessage(Component.translatable("iceagesurvival.call.no_room", creature.getName()),
                    true);
            return false;
        }
        creature.ejectPassengers();
        creature.stopRiding();
        creature.getNavigation().stop();
        creature.clearMoveOrder();
        creature.teleportTo(spot.x, spot.y, spot.z);
        creature.setYRot(player.getYRot() + 180.0F);
        creature.dropLeash(true, true);
        player.level().playSound(null, creature.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL,
                0.6F, 0.8F);
        player.displayClientMessage(Component.translatable("iceagesurvival.call.arrived", creature.getName()), true);
        return true;
    }

    /**
     * Um lugar no chão perto do jogador onde o corpo da criatura cabe: primeiro à frente dele, depois girando em
     * volta, e um pouco mais longe se não couber.
     */
    @Nullable
    static Vec3 findSpot(ServerPlayer player, PrehistoricCreature creature) {
        ServerLevel level = player.serverLevel();
        double half = creature.getBbWidth() / 2.0;
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        for (double distance : new double[] {1.5 + half, 3.0 + half}) {
            for (int step = 0; step < 8; step++) {
                // À frente, depois alternando lados: 0, +45, -45, +90, -90...
                double angle = yaw + Mth.HALF_PI / 2.0 * ((step + 1) / 2) * (step % 2 == 0 ? 1 : -1);
                Vec3 ahead = player.position().add(-Math.sin(angle) * distance, 0.0, Math.cos(angle) * distance);
                BlockPos column = BlockPos.containing(ahead);
                for (int dy = 2; dy >= -3; dy--) {
                    BlockPos feet = column.offset(0, dy, 0);
                    if (level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                        continue;
                    }
                    Vec3 spot = new Vec3(ahead.x, feet.getY(), ahead.z);
                    AABB box = creature.getBoundingBox().move(spot.subtract(creature.position()));
                    if (level.noCollision(creature, box) && !level.containsAnyLiquid(box)) {
                        return spot;
                    }
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Pending> call = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(call.getValue().player());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (player.serverLevel().getEntity(call.getKey()) instanceof PrehistoricCreature creature
                    && creature.isAlive()) {
                iterator.remove();
                bring(player, creature);
            } else if (server.getTickCount() > call.getValue().deadline()) {
                iterator.remove();
                player.displayClientMessage(Component.translatable("iceagesurvival.call.failed"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
