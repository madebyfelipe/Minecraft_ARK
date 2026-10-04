package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Onde estão as criaturas domesticadas de cada jogador, para o menu "localizar criatura". Fica nos
 * dados do Overworld (vale para o servidor todo) porque a criatura que interessa achar é justamente a
 * que ficou longe, num chunk descarregado, onde o servidor não a enxerga.
 *
 * <p>Cada criatura domesticada atualiza a própria entrada de tempos em tempos e ao sair do mundo
 * carregado; morta ou removida de vez, sai da lista.
 */
public final class CreatureLocator extends SavedData {
    private static final String NAME = "iceagesurvival_creature_locator";

    /** A última posição conhecida de uma criatura domesticada. */
    public record Entry(UUID creature, UUID owner, ResourceLocation type, String name, int level,
                        ResourceKey<Level> dimension, BlockPos pos) {
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    @Nullable
    private static CreatureLocator get(@Nullable MinecraftServer server) {
        ServerLevel overworld = server == null ? null : server.overworld();
        return overworld == null ? null
                : overworld.getDataStorage().computeIfAbsent(CreatureLocator::load, CreatureLocator::new, NAME);
    }

    /** Grava onde a criatura está; uma que não é mais domesticada sai da lista. */
    public static void update(PrehistoricCreature creature) {
        if (!(creature.level() instanceof ServerLevel level)) {
            return;
        }
        CreatureLocator locator = get(level.getServer());
        if (locator == null) {
            return;
        }
        UUID owner = creature.getOwnerUUID();
        if (!creature.isTame() || owner == null) {
            locator.forgetEntry(creature.getUUID());
            return;
        }
        Entry entry = new Entry(creature.getUUID(), owner, BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()),
                creature.getName().getString(), creature.creatureLevel(), level.dimension(), creature.blockPosition());
        if (!entry.equals(locator.entries.put(entry.creature(), entry))) {
            locator.setDirty();
        }
    }

    /** A criatura morreu ou foi removida de vez. */
    public static void forget(PrehistoricCreature creature) {
        if (creature.level() instanceof ServerLevel level) {
            CreatureLocator locator = get(level.getServer());
            if (locator != null) {
                locator.forgetEntry(creature.getUUID());
            }
        }
    }

    private void forgetEntry(UUID creature) {
        if (entries.remove(creature) != null) {
            setDirty();
        }
    }

    /**
     * As criaturas do jogador. A que está carregada vem com a posição de agora, não a da última
     * gravação.
     */
    public static List<Entry> ownedBy(MinecraftServer server, UUID owner) {
        return visible(server, owner, null);
    }

    /**
     * As criaturas do jogador e as dos donos que estão no mesmo time vanilla que ele agora ({@code /team}). A posse
     * não muda: a entrada continua com o dono de verdade.
     */
    public static List<Entry> visibleTo(MinecraftServer server, net.minecraft.world.entity.player.Player player) {
        return visible(server, player.getUUID(), player);
    }

    private static List<Entry> visible(MinecraftServer server, UUID owner,
                                       @Nullable net.minecraft.world.entity.player.Player teammate) {
        CreatureLocator locator = get(server);
        List<Entry> result = new ArrayList<>();
        if (locator == null) {
            return result;
        }
        Map<UUID, Boolean> allied = new HashMap<>();
        for (Entry entry : locator.entries.values()) {
            if (!entry.owner().equals(owner) && (teammate == null || !allied.computeIfAbsent(entry.owner(),
                    id -> dev.madebyfelipe.iceagesurvival.entity.CreatureTeams.sameTeam(server, id, "", teammate)))) {
                continue;
            }
            ServerLevel level = server.getLevel(entry.dimension());
            if (level != null && level.getEntity(entry.creature()) instanceof PrehistoricCreature live && live.isAlive()) {
                update(live);
                entry = locator.entries.getOrDefault(entry.creature(), entry);
                if (!entry.owner().equals(owner) && (teammate == null || !live.canCommand(teammate))) {
                    continue;
                }
            }
            result.add(entry);
        }
        return result;
    }

    /** A criatura, se estiver carregada em alguma dimensão. */
    @Nullable
    public static PrehistoricCreature findLoaded(MinecraftServer server, UUID creature) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(creature) instanceof PrehistoricCreature found && found.isAlive()) {
                return found;
            }
        }
        return null;
    }

    private static CreatureLocator load(CompoundTag tag) {
        CreatureLocator locator = new CreatureLocator();
        for (Tag element : tag.getList("Creatures", Tag.TAG_COMPOUND)) {
            CompoundTag data = (CompoundTag) element;
            ResourceLocation type = ResourceLocation.tryParse(data.getString("Type"));
            ResourceLocation dimension = ResourceLocation.tryParse(data.getString("Dimension"));
            if (type == null || dimension == null || !data.hasUUID("Id") || !data.hasUUID("Owner")) {
                continue;
            }
            Entry entry = new Entry(data.getUUID("Id"), data.getUUID("Owner"), type, data.getString("Name"),
                    data.getInt("Level"), ResourceKey.create(Registries.DIMENSION, dimension),
                    NbtUtils.readBlockPos(data.getCompound("Pos")));
            locator.entries.put(entry.creature(), entry);
        }
        return locator;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag data = new CompoundTag();
            data.putUUID("Id", entry.creature());
            data.putUUID("Owner", entry.owner());
            data.putString("Type", entry.type().toString());
            data.putString("Name", entry.name());
            data.putInt("Level", entry.level());
            data.putString("Dimension", entry.dimension().location().toString());
            data.put("Pos", NbtUtils.writeBlockPos(entry.pos()));
            list.add(data);
        }
        tag.put("Creatures", list);
        return tag;
    }
}
