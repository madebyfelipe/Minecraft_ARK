package dev.madebyfelipe.iceagesurvival.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

/**
 * Criaturas compartilhadas pelo time vanilla ({@code /team}): quem está no time atual do dono comanda as criaturas
 * dele sem virar dono. O time do dono é procurado pelo nome, então vale com o dono fora do servidor; sair do time
 * revoga na hora.
 */
public final class CreatureTeams {
    private CreatureTeams() {
    }

    /** Nome do jogador: online, senão o do cache de perfis do servidor, senão {@code fallback}. */
    public static String playerName(MinecraftServer server, UUID player, String fallback) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return online.getScoreboardName();
        }
        var cache = server.getProfileCache();
        String cached = cache == null ? null : cache.get(player).map(profile -> profile.getName()).orElse(null);
        return cached != null ? cached : fallback;
    }

    /** Time vanilla do jogador, ou nulo; o nome pode ser vazio se ele nunca foi visto. */
    @Nullable
    public static PlayerTeam teamOf(MinecraftServer server, UUID player, String fallbackName) {
        String name = playerName(server, player, fallbackName);
        return name.isEmpty() ? null : server.getScoreboard().getPlayersTeam(name);
    }

    /** Se {@code player} está no mesmo time do dono {@code owner}. */
    public static boolean sameTeam(MinecraftServer server, UUID owner, String ownerName, Player player) {
        Team team = player.getTeam();
        PlayerTeam ownerTeam = teamOf(server, owner, ownerName);
        return team != null && ownerTeam != null && team.getName().equals(ownerTeam.getName());
    }
}
