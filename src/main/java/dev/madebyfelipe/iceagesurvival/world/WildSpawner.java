package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.spawn.WildSpawnRules;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;

/**
 * Reposição de fauna selvagem em chunks já gerados.
 *
 * <p>Criaturas do mod são da categoria {@code CREATURE} do vanilla, que na prática só nasce
 * quando o terreno é gerado: num mundo já explorado, a fauna que morre não volta. Em vez de
 * mudar a categoria (o que faria as criaturas desaparecerem sozinhas), o mod repõe por conta
 * própria — uma tentativa por jogador a cada intervalo, num anel fora do campo de visão, com
 * teto de densidade por espécie vindo do bloco {@code spawn} do JSON da espécie.
 *
 * <p>Sem o teto, a reposição encheria o mundo; com ele, a densidade converge para
 * {@code max_nearby} indivíduos por raio de densidade em torno de onde o jogador andou.
 */
public final class WildSpawner {
    /** Quantas posições tentar antes de desistir da tentativa deste intervalo. */
    private static final int POSITION_ATTEMPTS = 12;
    /** Espalhamento horizontal dos membros de um grupo em relação à posição sorteada. */
    private static final int GROUP_SPREAD = 4;

    private WildSpawner() {
    }

    /** Uma espécie candidata à reposição, com a contagem do que já existe perto. */
    /** @param weight o peso da espécie no sorteio, neste bioma ({@link SpawnProfile#weightIn}) */
    public record Report(EntityType<?> type, SpawnProfile profile, int nearby, int weight) {
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // No servidor de testes, fauna nascendo sozinha invade as cenas (o Pteranodonte nasce em qualquer
        // bioma); os testes da reposição chamam trySpawnAround direto.
        if (event.phase != TickEvent.Phase.END || !ServerConfig.WILD_SPAWN_ENABLED.get()
                || event.getServer() instanceof GameTestServer) {
            return;
        }
        int interval = ServerConfig.WILD_SPAWN_INTERVAL_SECONDS.get() * 20;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.getGameTime() % interval != 0 || level.players().isEmpty()) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator()) {
                    trySpawnAround(level, player);
                }
            }
        }
    }

    /**
     * Uma tentativa de reposição em torno do jogador, com as distâncias da config.
     *
     * @return quantas criaturas nasceram
     */
    public static int trySpawnAround(ServerLevel level, ServerPlayer player) {
        return trySpawnAround(level, player,
                ServerConfig.WILD_SPAWN_MIN_DISTANCE.get(), ServerConfig.WILD_SPAWN_MAX_DISTANCE.get());
    }

    /** Com a região abaixo desta fração do teto, a reposição faz nascer vários grupos de uma vez. */
    private static final double SPARSE_FRACTION = 0.5;
    /** Grupos por tentativa com a região esparsa (um mundo recém-criado, uma área recém-explorada). */
    private static final int SPARSE_GROUPS = 2;

    /** Fração do teto da config que vale de fato: 36 de 72, com espaço para os territórios (era 0,7; depois 0,4). */
    public static final double POPULATION_FRACTION = 0.5;

    /** População-alvo reduzida para deixar espaço para os territórios da fauna. */
    public static int effectiveMaximumPopulation() {
        return (int) (ServerConfig.WILD_SPAWN_MAX_TOTAL.get() * POPULATION_FRACTION);
    }

    /** Como {@link #trySpawnAround(ServerLevel, ServerPlayer)}, com as distâncias dadas. */
    public static int trySpawnAround(ServerLevel level, ServerPlayer player, int minDistance, int maxDistance) {
        Object2IntMap<EntityType<?>> nearby = countNearby(level, player);
        Occupancy start = Occupancy.of(level, nearby);
        int groups = start.ground() < effectiveMaximumPopulation() * SPARSE_FRACTION
                || start.flyers() < WildSpawnRules.FLYER_CAP * SPARSE_FRACTION ? SPARSE_GROUPS : 1;
        RandomGenerator random = level.random::nextLong;
        int spawned = 0;
        for (int round = 0; round < groups; round++) {
            spawned += spawnOneGroup(level, player, nearby, minDistance, maxDistance, random);
        }
        return spawned;
    }

    /** As três categorias do teto: quem anda no chão (herbívoros dentro dele) e os voadores, à parte. */
    private record Occupancy(int ground, int herbivores, int flyers) {
        static Occupancy of(ServerLevel level, Object2IntMap<EntityType<?>> nearby) {
            int ground = 0;
            int herbivores = 0;
            int flyers = 0;
            for (var entry : nearby.object2IntEntrySet()) {
                int count = entry.getIntValue();
                if (isFlyer(level, entry.getKey())) {
                    flyers += count;
                } else {
                    ground += count;
                    if (!isCarnivore(level, entry.getKey())) {
                        herbivores += count;
                    }
                }
            }
            return new Occupancy(ground, herbivores, flyers);
        }

        /** Vagas que esta espécie ainda tem na categoria dela. */
        int room(ServerLevel level, EntityType<?> type) {
            if (isFlyer(level, type)) {
                return WildSpawnRules.FLYER_CAP - flyers;
            }
            int cap = effectiveMaximumPopulation();
            int groundRoom = cap - ground;
            return isCarnivore(level, type) ? groundRoom
                    : Math.min(groundRoom, WildSpawnRules.herbivoreRoom(cap, herbivores));
        }

        /** Se a vaga da categoria desta espécie já é disputada ({@link WildSpawnRules#contested}). */
        boolean contested(ServerLevel level, EntityType<?> type) {
            if (isFlyer(level, type)) {
                return WildSpawnRules.contested(flyers, WildSpawnRules.FLYER_CAP);
            }
            int cap = effectiveMaximumPopulation();
            return isCarnivore(level, type) ? WildSpawnRules.contested(ground, cap)
                    : WildSpawnRules.contested(herbivores, WildSpawnRules.herbivoreCap(cap));
        }
    }

    /**
     * Sorteia uma posição no anel e, nela, uma espécie do bioma daquele lugar — não do bioma em
     * que o jogador pisa: quem está na borda de uma tundra também vê a fauna da tundra.
     */
    private static int spawnOneGroup(ServerLevel level, ServerPlayer player, Object2IntMap<EntityType<?>> nearby,
                                     int minDistance, int maxDistance, RandomGenerator random) {
        Occupancy occupancy = Occupancy.of(level, nearby);
        int min = minDistance;
        int max = Math.max(min + 1, maxDistance);
        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            WildSpawnRules.Offset offset = WildSpawnRules.ringOffset(min, max, random);
            int x = player.getBlockX() + offset.x();
            int z = player.getBlockZ() + offset.z();
            if (!level.hasChunkAt(x, z)) {
                continue;
            }
            List<Report> reports = survey(level, new BlockPos(x, player.getBlockY(), z), nearby);
            if (reports.isEmpty()) {
                continue;
            }
            // Cada espécie disputa a vaga da categoria dela, com a regra de diversidade do sorteio.
            List<WildSpawnRules.Candidate> candidates = new ArrayList<>(reports.size());
            for (Report report : reports) {
                int weight = occupancy.room(level, report.type()) <= 0 ? 0
                        : WildSpawnRules.fairWeight(report.weight(), report.nearby(), report.profile().groupMax(),
                        occupancy.contested(level, report.type()));
                candidates.add(new WildSpawnRules.Candidate(weight, report.nearby(), report.profile().maxNearby()));
            }
            int chosen = WildSpawnRules.pick(candidates, random);
            if (chosen < 0) {
                continue;
            }
            Report report = reports.get(chosen);
            int room = Math.min(report.profile().maxNearby() - report.nearby(), occupancy.room(level, report.type()));
            int group = WildSpawnRules.groupSize(report.profile().groupMin(), report.profile().groupMax(), room, random);
            BlockPos origin = surfacePos(level, x, z, report.type());
            if (group <= 0 || origin == null || !canSpawnAt(level, report.type(), origin, report.profile())) {
                continue;
            }
            int spawned = 0;
            // Quem nasce junto é um bando, já com os membros definidos.
            PrehistoricCreature.Pack pack = new PrehistoricCreature.Pack(report.type(), java.util.UUID.randomUUID());
            for (int index = 0; index < group; index++) {
                BlockPos pos = index == 0 ? origin : nearbySurfacePos(level, origin, report.type(), report.profile(), random);
                if (pos != null && spawnAt(level, report.type(), pos, pack)) {
                    spawned++;
                }
            }
            nearby.mergeInt(report.type(), spawned, Integer::sum);
            return spawned;
        }
        return 0;
    }

    /**
     * Espécies que podem nascer no bioma em que o jogador está, com quantas delas já existem
     * no raio de densidade. Só as que ainda têm vaga.
     */
    public static List<Report> survey(ServerLevel level, ServerPlayer player) {
        return survey(level, player.blockPosition(), countNearby(level, player));
    }

    /** Espécies do bioma desta posição, com as contagens dadas. */
    private static List<Report> survey(ServerLevel level, BlockPos pos, Object2IntMap<EntityType<?>> nearby) {
        var biome = level.getBiome(pos);
        List<Report> reports = new ArrayList<>();
        for (var holder : ModEntities.LAND_CREATURES) {
            EntityType<?> type = holder.get();
            Optional<SpawnProfile> profile = Species.of(level.registryAccess(), type).flatMap(Species::spawn);
            if (profile.isEmpty() || !biome.is(profile.get().biomes())) {
                continue;
            }
            reports.add(new Report(type, profile.get(), nearby.getInt(type), profile.get().weightIn(biome)));
        }
        return reports;
    }

    /** Criaturas selvagens do mod, de todas as espécies, no raio de densidade do jogador: o que conta no teto. */
    public static int totalWildNearby(ServerLevel level, ServerPlayer player) {
        return countNearby(level, player).values().intStream().sum();
    }

    /**
     * Raio da contagem: o da config, mas sempre além de onde a reposição faz nascer — senão
     * quem nasceu na borda sai andando, deixa de contar e abre vaga para outro.
     */
    public static double densityRadius() {
        return Math.max(ServerConfig.WILD_SPAWN_DENSITY_RADIUS.get(), ServerConfig.WILD_SPAWN_MAX_DISTANCE.get() + 32);
    }

    /** Voador: a espécie voa ({@code mount.flying}) e fica no teto próprio dos voadores. */
    public static boolean isFlyer(ServerLevel level, EntityType<?> type) {
        return Species.of(level.registryAccess(), type).flatMap(Species::mount)
                .map(dev.madebyfelipe.iceagesurvival.species.MountProfile::flying).orElse(false);
    }

    /** Carnívoro: a espécie tem presa ({@code behavior.prey}). */
    public static boolean isCarnivore(ServerLevel level, EntityType<?> type) {
        return Species.of(level.registryAccess(), type).flatMap(Species::behavior)
                .flatMap(dev.madebyfelipe.iceagesurvival.species.BehaviorProfile::prey).isPresent();
    }

    /** Quantos herbívoros selvagens do chão há nestas contagens. */
    public static int herbivoresNearby(ServerLevel level, Object2IntMap<EntityType<?>> nearby) {
        int herbivores = 0;
        for (var entry : nearby.object2IntEntrySet()) {
            if (!isCarnivore(level, entry.getKey()) && !isFlyer(level, entry.getKey())) {
                herbivores += entry.getIntValue();
            }
        }
        return herbivores;
    }

    /** Uma varredura só, para todas as espécies: contagens de criaturas do mod perto do jogador. */
    private static Object2IntMap<EntityType<?>> countNearby(ServerLevel level, ServerPlayer player) {
        double radius = densityRadius();
        Object2IntMap<EntityType<?>> counts = new Object2IntOpenHashMap<>();
        AABB box = player.getBoundingBox().inflate(radius);
        for (PrehistoricCreature creature : level.getEntitiesOfClass(PrehistoricCreature.class, box,
                creature -> !creature.isTame())) {
            counts.mergeInt(creature.getType(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * Posição na superfície da coluna, já ajustada pelo tipo de colocação da espécie.
     *
     * @return null se o chunk não estiver carregado; a reposição nunca carrega chunk
     */
    @Nullable
    public static BlockPos surfacePos(ServerLevel level, int x, int z, EntityType<?> type) {
        if (!level.hasChunkAt(x, z)) {
            return null;
        }
        Heightmap.Types heightmap = SpawnPlacements.getHeightmapType(type);
        BlockPos top = new BlockPos(x, level.getHeight(heightmap, x, z), z);
        return top;
    }

    @Nullable
    private static BlockPos nearbySurfacePos(ServerLevel level, BlockPos origin, EntityType<?> type,
                                             SpawnProfile profile, RandomGenerator random) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = origin.getX() + random.nextInt(GROUP_SPREAD * 2 + 1) - GROUP_SPREAD;
            int z = origin.getZ() + random.nextInt(GROUP_SPREAD * 2 + 1) - GROUP_SPREAD;
            BlockPos pos = surfacePos(level, x, z, type);
            if (pos != null && canSpawnAt(level, type, pos, profile)) {
                return pos;
            }
        }
        return null;
    }

    /** As mesmas checagens do spawn natural do vanilla, mais o bioma da espécie. */
    public static boolean canSpawnAt(ServerLevel level, EntityType<?> type, BlockPos pos, SpawnProfile profile) {
        return level.getBiome(pos).is(profile.biomes())
                && SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.random)
                && level.noCollision(type.getDimensions().makeBoundingBox(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }

    /**
     * Faz nascer um indivíduo selvagem nesta posição, pelo mesmo caminho do spawn natural
     * (inclusive os eventos do Forge, para outros mods poderem barrar).
     *
     * @return se nasceu
     */
    public static boolean spawnAt(ServerLevel level, EntityType<?> type, BlockPos pos) {
        return spawnAt(level, type, pos, null);
    }

    /** Como {@link #spawnAt(ServerLevel, EntityType, BlockPos)}, no bando dado. */
    public static boolean spawnAt(ServerLevel level, EntityType<?> type, BlockPos pos,
                                  @Nullable PrehistoricCreature.Pack pack) {
        if (!(type.create(level) instanceof Mob mob)) {
            return false;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        if (!ForgeEventFactory.checkSpawnPosition(mob, level, MobSpawnType.NATURAL)) {
            mob.discard();
            return false;
        }
        ForgeEventFactory.onFinalizeSpawn(
                mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, pack, null);
        return level.addFreshEntity(mob);
    }
}
