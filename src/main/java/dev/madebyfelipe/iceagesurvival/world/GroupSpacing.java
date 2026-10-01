package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.core.spawn.SpeciesSpacing;
import dev.madebyfelipe.iceagesurvival.core.spawn.SpeciesSpacing.Claim;
import dev.madebyfelipe.iceagesurvival.core.spawn.SpeciesSpacing.Verdict;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;

/**
 * As marcas de grupo ({@link SpeciesSpacing}) de cada espécie com {@code spawn.spacing}, nos dados da
 * dimensão. É o que deixa uma só manada de Brontossauro e um só T-Rex a cada 300 blocos.
 *
 * <ul>
 *   <li><b>Nascer:</b> a regra de colocação ({@code checkSurfaceSpawnRules}) só aceita um grupo novo
 *   longe de toda marca, e marca o lugar; os outros membros caem perto dela e passam. Vale para a
 *   geração do terreno e para a reposição.</li>
 *   <li><b>Acompanhar:</b> a cada varredura, marca com o lugar carregado anda para onde o grupo está;
 *   sem ninguém do grupo ali, some depois do prazo. Cada indivíduo selvagem também renova a sua de
 *   tempos em tempos, e cria uma se não houver (mundos de antes desta regra).</li>
 *   <li><b>Mundos antigos:</b> uma criatura da geração do terreno conferida pela primeira vez perto
 *   demais de outro grupo some, como a fauna fora da zona de perigo.</li>
 * </ul>
 *
 * <p>A geração do terreno roda fora da thread do servidor: os dados ficam num cache aberto na carga da
 * dimensão e os métodos são sincronizados.
 */
public final class GroupSpacing extends SavedData {
    private static final String NAME = "iceagesurvival_group_spacing";
    private static final int SWEEP_INTERVAL = 200;
    private static final Map<ServerLevel, GroupSpacing> OPEN = new ConcurrentHashMap<>();

    private final Map<ResourceLocation, List<Claim>> claims = new HashMap<>();

    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            OPEN.put(level, level.getDataStorage().computeIfAbsent(GroupSpacing::load, GroupSpacing::new, NAME));
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            OPEN.remove(level);
        }
    }

    /** Os dados da dimensão, ou null se ela ainda não terminou de carregar. */
    @Nullable
    public static GroupSpacing of(ServerLevel level) {
        return OPEN.get(level);
    }

    /** Espaçamento da espécie em blocos; 0 = sem regra. */
    public static int spacing(ServerLevel level, EntityType<?> type) {
        return Species.of(level.registryAccess(), type).flatMap(Species::spawn).map(SpawnProfile::spacing).orElse(0);
    }

    /**
     * Regra de colocação: pode nascer aqui? Um grupo novo marca o lugar na hora, para os membros
     * seguintes do mesmo grupo passarem e um grupo vizinho, não.
     */
    public static boolean permitsSpawn(ServerLevel level, EntityType<?> type, BlockPos pos) {
        int spacing = spacing(level, type);
        GroupSpacing data = spacing > 0 ? of(level) : null;
        return data == null || data.admit(key(type), pos, spacing, level.getGameTime(), false) != Verdict.TOO_CLOSE;
    }

    /**
     * Um indivíduo selvagem que já existe: renova a marca do grupo dele, ou cria uma.
     *
     * @return {@link Verdict#TOO_CLOSE} se ele está perto demais de outro grupo; a marca foi criada
     *         mesmo assim, e quem chama decide se ele fica (manada migrando) ou some (mundo antigo)
     */
    public static Verdict report(ServerLevel level, EntityType<?> type, BlockPos pos, boolean claimEvenIfClose) {
        int spacing = spacing(level, type);
        GroupSpacing data = spacing > 0 ? of(level) : null;
        if (data == null) {
            return Verdict.NEW_GROUP;
        }
        return data.admit(key(type), pos, spacing, level.getGameTime(), claimEvenIfClose);
    }

    private synchronized Verdict admit(ResourceLocation species, BlockPos pos, int spacing, long now,
                                       boolean claimEvenIfClose) {
        List<Claim> list = claims.computeIfAbsent(species, ignored -> new ArrayList<>());
        Verdict verdict = SpeciesSpacing.judge(list, pos.getX(), pos.getZ(), spacing);
        if (verdict == Verdict.SAME_GROUP) {
            int nearest = SpeciesSpacing.nearest(list, pos.getX(), pos.getZ());
            list.set(nearest, new Claim(list.get(nearest).x(), list.get(nearest).z(), now));
        } else if (verdict == Verdict.NEW_GROUP || claimEvenIfClose) {
            list.add(new Claim(pos.getX(), pos.getZ(), now));
        }
        setDirty();
        return verdict;
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            GroupSpacing data = of(level);
            if (data != null && level.getGameTime() % SWEEP_INTERVAL == 0) {
                data.sweep(level);
            }
        }
    }

    /** Move cada marca carregada para onde o grupo está; apaga a de grupo que sumiu. */
    private synchronized void sweep(ServerLevel level) {
        long now = level.getGameTime();
        boolean changed = false;
        for (var entry : claims.entrySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
            List<Claim> list = entry.getValue();
            for (int index = list.size() - 1; index >= 0; index--) {
                Claim claim = list.get(index);
                BlockPos at = new BlockPos(claim.x(), level.getSeaLevel(), claim.z());
                if (!level.isPositionEntityTicking(at)) {
                    continue;
                }
                PrehistoricCreature member = findMember(level, type, claim);
                if (member != null) {
                    list.set(index, new Claim(member.getBlockX(), member.getBlockZ(), now));
                    changed = true;
                } else if (SpeciesSpacing.expired(claim, now)) {
                    list.remove(index);
                    changed = true;
                }
            }
        }
        if (changed) {
            setDirty();
        }
    }

    @Nullable
    private static PrehistoricCreature findMember(ServerLevel level, EntityType<?> type, Claim claim) {
        double radius = SpeciesSpacing.GROUP_RADIUS;
        AABB box = new AABB(claim.x() - radius, level.getMinBuildHeight(), claim.z() - radius,
                claim.x() + radius, level.getMaxBuildHeight(), claim.z() + radius);
        List<PrehistoricCreature> found = level.getEntitiesOfClass(PrehistoricCreature.class, box,
                creature -> creature.getType() == type && creature.countsForGroupSpacing());
        return found.isEmpty() ? null : found.get(0);
    }

    private static ResourceLocation key(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    private static GroupSpacing load(CompoundTag tag) {
        GroupSpacing data = new GroupSpacing();
        for (String species : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(species);
            if (id == null) {
                continue;
            }
            List<Claim> list = new ArrayList<>();
            for (Tag element : tag.getList(species, Tag.TAG_COMPOUND)) {
                CompoundTag claim = (CompoundTag) element;
                list.add(new Claim(claim.getInt("X"), claim.getInt("Z"), claim.getLong("Seen")));
            }
            data.claims.put(id, list);
        }
        return data;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        claims.forEach((species, list) -> {
            ListTag entries = new ListTag();
            for (Claim claim : list) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("X", claim.x());
                entry.putInt("Z", claim.z());
                entry.putLong("Seen", claim.lastSeen());
                entries.add(entry);
            }
            tag.put(species.toString(), entries);
        });
        return tag;
    }
}
