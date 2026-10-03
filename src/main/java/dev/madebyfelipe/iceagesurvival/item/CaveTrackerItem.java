package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.network.CaveTargetPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.registry.ModStructures;
import dev.madebyfelipe.iceagesurvival.world.cave.ArenaCaveStructure;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Rastreador da caverna (D47): feito com os troféus de apex, é um aparelho de mão. Segurado em qualquer das mãos,
 * mostra no radar do canto a direção e a distância até a boca da caverna da arena mais próxima, e o ícone vira uma
 * bússola apontando para ela. Não é jogado nem gasto.
 *
 * <p>O servidor acha a caverna ({@code findNearestMapStructure} com a tag {@code #iceagesurvival:arena_cave}) e a boca
 * exata ({@link ArenaCaveStructure#entranceAt}), guarda a busca até o jogador andar {@link #RESEARCH_DISTANCE} blocos
 * ou trocar de dimensão e manda o alvo ao cliente ao pegar o rastreador e quando ele muda.
 */
public class CaveTrackerItem extends Item {
    /** Raio de busca em chunks; os anéis concêntricos são achados por lista, então serve qualquer valor grande. */
    public static final int SEARCH_RADIUS_CHUNKS = 100;
    /** Andou isto (na horizontal) desde a última busca, procura de novo: a mais próxima só muda numa viagem longa. */
    public static final int RESEARCH_DISTANCE = 256;
    /** De quanto em quanto tempo, com o rastreador na mão, o servidor confere se o alvo mudou. */
    private static final int CHECK_TICKS = 20;

    private static final Map<UUID, Signal> SIGNALS = new HashMap<>();

    public CaveTrackerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.cave_tracker.tooltip").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isHolding(Player player) {
        return player.getMainHandItem().getItem() instanceof CaveTrackerItem
                || player.getOffhandItem().getItem() instanceof CaveTrackerItem;
    }

    /** A boca da caverna da arena mais próxima de {@code from} nesta dimensão; {@code null} se não há nenhuma. */
    @Nullable
    public static BlockPos nearestMouth(ServerLevel level, BlockPos from) {
        BlockPos ring = level.findNearestMapStructure(ModStructures.ARENA_CAVES, from, SEARCH_RADIUS_CHUNKS, false);
        if (ring == null) {
            return null;
        }
        return ArenaCaveStructure.entranceAt(level.getChunkSource().getGenerator(), level,
                level.getChunkSource().randomState(), new ChunkPos(ring));
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Signal signal = SIGNALS.get(player.getUUID());
        if (!isHolding(player)) {
            if (signal != null) {
                signal.sent = null; // ao pegar de novo, manda outra vez
            }
            return;
        }
        if (signal == null) {
            signal = new Signal();
            SIGNALS.put(player.getUUID(), signal);
        }
        if (signal.sent != null && player.tickCount % CHECK_TICKS != 0) {
            return;
        }
        Optional<GlobalPos> target = signal.target(player);
        if (!target.equals(signal.sent)) {
            signal.sent = target;
            ModPayloads.sendToPlayer(player, new CaveTargetPayload(target));
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SIGNALS.remove(event.getEntity().getUUID());
    }

    /** A última busca de um jogador e o que o cliente dele já sabe. */
    private static final class Signal {
        @Nullable
        private ResourceKey<Level> dimension;
        private BlockPos origin = BlockPos.ZERO;
        private Optional<GlobalPos> mouth = Optional.empty();
        /** O último alvo mandado; {@code null} quando o cliente ainda não tem o de agora. */
        @Nullable
        private Optional<GlobalPos> sent;

        Optional<GlobalPos> target(ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            BlockPos here = player.blockPosition();
            long dx = here.getX() - origin.getX();
            long dz = here.getZ() - origin.getZ();
            if (!level.dimension().equals(dimension) || dx * dx + dz * dz > (long) RESEARCH_DISTANCE * RESEARCH_DISTANCE) {
                dimension = level.dimension();
                origin = here;
                mouth = Optional.ofNullable(nearestMouth(level, here)).map(pos -> GlobalPos.of(level.dimension(), pos));
            }
            return mouth;
        }
    }
}
