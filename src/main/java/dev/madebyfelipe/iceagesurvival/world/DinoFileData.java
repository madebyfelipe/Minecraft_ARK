package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.network.DinoFilePayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * A DINO FILE de cada jogador: as espécies que ele já escaneou com o Analisador, na ordem do registro, e quantos
 * registros militares já recuperou nos terminais dos postos (eles se destravam sempre na mesma ordem, então basta a
 * contagem; o texto fica no manual do cliente). Fica nos dados do Overworld (vale para o servidor todo) e não se perde
 * ao morrer.
 */
public final class DinoFileData extends SavedData {
    private static final String NAME = "iceagesurvival_dino_file";

    private final Map<UUID, Set<ResourceLocation>> registered = new HashMap<>();
    private final Map<UUID, Integer> records = new HashMap<>();

    @Nullable
    private static DinoFileData get(@Nullable MinecraftServer server) {
        ServerLevel overworld = server == null ? null : server.overworld();
        return overworld == null ? null
                : overworld.getDataStorage().computeIfAbsent(DinoFileData::load, DinoFileData::new, NAME);
    }

    /** Registra a espécie para o jogador; {@code true} se é registro novo. Manda a lista nova ao cliente. */
    public static boolean register(ServerPlayer player, ResourceLocation species) {
        DinoFileData data = get(player.getServer());
        if (data == null) {
            return false;
        }
        boolean added = data.registered.computeIfAbsent(player.getUUID(), id -> new LinkedHashSet<>()).add(species);
        if (added) {
            data.setDirty();
            sync(player);
        }
        return added;
    }

    /** As espécies que o jogador já registrou, na ordem. */
    public static List<ResourceLocation> registered(ServerPlayer player) {
        DinoFileData data = get(player.getServer());
        return data == null ? List.of() : List.copyOf(data.registered.getOrDefault(player.getUUID(), Set.of()));
    }

    /** Destrava o próximo registro militar do jogador; devolve quantos ele tem agora. Manda a DINO FILE ao cliente. */
    public static int unlockRecord(ServerPlayer player) {
        DinoFileData data = get(player.getServer());
        if (data == null) {
            return 0;
        }
        int count = data.records.merge(player.getUUID(), 1, Integer::sum);
        data.setDirty();
        sync(player);
        return count;
    }

    /** Quantos registros militares o jogador já recuperou. */
    public static int records(ServerPlayer player) {
        DinoFileData data = get(player.getServer());
        return data == null ? 0 : data.records.getOrDefault(player.getUUID(), 0);
    }

    public static void sync(ServerPlayer player) {
        ModPayloads.sendToPlayer(player, new DinoFilePayload(registered(player), records(player)));
    }

    private static DinoFileData load(CompoundTag tag) {
        DinoFileData data = new DinoFileData();
        for (Tag element : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            if (!entry.hasUUID("Id")) {
                continue;
            }
            Set<ResourceLocation> species = new LinkedHashSet<>();
            for (Tag id : entry.getList("Species", Tag.TAG_STRING)) {
                ResourceLocation location = ResourceLocation.tryParse(id.getAsString());
                if (location != null) {
                    species.add(location);
                }
            }
            data.registered.put(entry.getUUID("Id"), species);
            if (entry.getInt("Records") > 0) {
                data.records.put(entry.getUUID("Id"), entry.getInt("Records"));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        Set<UUID> ids = new LinkedHashSet<>(registered.keySet());
        ids.addAll(records.keySet());
        for (UUID id : ids) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            ListTag list = new ListTag();
            registered.getOrDefault(id, Set.of()).forEach(location -> list.add(StringTag.valueOf(location.toString())));
            entry.put("Species", list);
            entry.putInt("Records", records.getOrDefault(id, 0));
            players.add(entry);
        }
        tag.put("Players", players);
        return tag;
    }
}
