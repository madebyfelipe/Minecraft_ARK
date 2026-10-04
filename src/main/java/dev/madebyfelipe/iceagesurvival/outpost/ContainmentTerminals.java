package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.TerminalScreenPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * As sessões abertas nos consoles da contenção, no servidor: uma por pessoa, presa ao console onde ela clicou e ao
 * núcleo que ele opera. Cada linha que chega vai para o {@link ContainmentShell}; o servidor confere que a sessão
 * existe, que é desse console e que a pessoa continua a até {@link #REACH} blocos dele. Só o servidor desliga o campo.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public final class ContainmentTerminals {
    public static final double REACH = 8.0;
    /** Até onde o console procura o núcleo que opera. */
    static final int CORE_SEARCH_RADIUS = 32;
    static final int CORE_SEARCH_HEIGHT = 8;

    private record Open(BlockPos terminal, BlockPos core, ContainmentShell.Session session) {
    }

    private static final Map<UUID, Open> SESSIONS = new HashMap<>();

    private ContainmentTerminals() {
    }

    /** O núcleo mais próximo do console, ou {@code null} se não há nenhum ao alcance. */
    @Nullable
    public static BlockPos findCore(Level level, BlockPos console) {
        if (level.getBlockState(console).is(Outposts.CONTAINMENT_CORE.get())) {
            return console;
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -CORE_SEARCH_RADIUS; dx <= CORE_SEARCH_RADIUS; dx++) {
            for (int dz = -CORE_SEARCH_RADIUS; dz <= CORE_SEARCH_RADIUS; dz++) {
                for (int dy = -CORE_SEARCH_HEIGHT; dy <= CORE_SEARCH_HEIGHT; dy++) {
                    cursor.setWithOffset(console, dx, dy, dz);
                    if (level.getBlockState(cursor).is(Outposts.CONTAINMENT_CORE.get())) {
                        double distance = cursor.distSqr(console);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Clique no console (ou no núcleo): abre uma sessão nova, como convidado, e liga a tela. */
    public static void open(ServerPlayer player, BlockPos terminal) {
        ServerLevel level = player.serverLevel();
        BlockPos core = findCore(level, terminal);
        ContainmentCoreBlockEntity entity = core == null ? null
                : level.getBlockEntity(core) instanceof ContainmentCoreBlockEntity found ? found : null;
        if (entity == null) {
            player.displayClientMessage(Component.translatable("block.iceagesurvival.containment_console.no_core"),
                    true);
            return;
        }
        ContainmentShell.Session session = new ContainmentShell.Session();
        SESSIONS.put(player.getUUID(), new Open(terminal.immutable(), core, session));
        level.playSound(null, terminal, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.4F, 1.8F);
        ModPayloads.sendToPlayer(player, new TerminalScreenPayload(terminal, true, false, false,
                ContainmentShell.boot(entity.status()), session.prompt(), false));
    }

    /** Uma linha digitada; devolve a resposta (os testes leem por aqui) ou {@code null} se a sessão não vale. */
    @Nullable
    public static ContainmentShell.Reply command(ServerPlayer player, BlockPos terminal, String line) {
        Open open = SESSIONS.get(player.getUUID());
        if (open == null || !open.terminal().equals(terminal)) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        boolean near = player.distanceToSqr(terminal.getX() + 0.5, terminal.getY() + 0.5, terminal.getZ() + 0.5)
                <= REACH * REACH;
        ContainmentCoreBlockEntity entity = level.getBlockEntity(open.core()) instanceof ContainmentCoreBlockEntity
                found ? found : null;
        if (!near || entity == null) {
            SESSIONS.remove(player.getUUID());
            ModPayloads.sendToPlayer(player, new TerminalScreenPayload(terminal, false, false, true,
                    List.of("conexão perdida."), "", false));
            return null;
        }
        ContainmentShell.Reply reply = ContainmentShell.run(open.session(), line, entity.status());
        if (reply.action() == ContainmentShell.Action.SHUTDOWN) {
            entity.shutdown(level);
        }
        boolean exit = reply.action() == ContainmentShell.Action.EXIT;
        if (exit) {
            SESSIONS.remove(player.getUUID());
        }
        level.playSound(null, terminal, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.15F, 2.0F);
        ModPayloads.sendToPlayer(player, new TerminalScreenPayload(terminal, false,
                reply.action() == ContainmentShell.Action.CLEAR, exit, reply.lines(), reply.prompt(), reply.secret()));
        return reply;
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
