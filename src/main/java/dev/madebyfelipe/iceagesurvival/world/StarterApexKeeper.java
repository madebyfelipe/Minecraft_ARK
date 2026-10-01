package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.spawn.DangerZones;
import dev.madebyfelipe.iceagesurvival.core.spawn.StarterApex;
import dev.madebyfelipe.iceagesurvival.core.spawn.WildSpawnRules;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import java.util.UUID;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Mantém o apex garantido perto do spawn ({@link StarterApex}): um T-Rex com território próprio
 * entre 220 e 297 blocos do spawn do mundo. Fica nos dados do Overworld qual é o apex e quando
 * outro pode chegar.
 *
 * <p>Como a reposição, nunca carrega chunk: o apex nasce quando algum jogador já tem aquele trecho
 * do anel carregado, e não à vista dele.
 */
public final class StarterApexKeeper extends SavedData {
    private static final String NAME = "iceagesurvival_starter_apex";
    private static final int CHECK_INTERVAL = 100;
    private static final int POSITION_ATTEMPTS = 16;

    @Nullable
    private UUID apex;
    @Nullable
    private BlockPos lastKnown;
    private long respawnAt;

    private static StarterApexKeeper get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(StarterApexKeeper::load, StarterApexKeeper::new, NAME);
    }

    private static StarterApexKeeper load(CompoundTag tag) {
        StarterApexKeeper keeper = new StarterApexKeeper();
        keeper.apex = tag.hasUUID("Apex") ? tag.getUUID("Apex") : null;
        keeper.lastKnown = tag.contains("LastKnown") ? NbtUtils.readBlockPos(tag.getCompound("LastKnown")) : null;
        keeper.respawnAt = tag.getLong("RespawnAt");
        return keeper;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (apex != null) {
            tag.putUUID("Apex", apex);
        }
        if (lastKnown != null) {
            tag.put("LastKnown", NbtUtils.writeBlockPos(lastKnown));
        }
        tag.putLong("RespawnAt", respawnAt);
        return tag;
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ServerConfig.STARTER_APEX_ENABLED.get()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        if (level == null || level.getGameTime() % CHECK_INTERVAL != 0 || level.players().isEmpty()) {
            return;
        }
        get(level).tick(level);
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            StarterApexKeeper keeper = get(level);
            if (event.getEntity().getUUID().equals(keeper.apex)) {
                keeper.release(level);
            }
        }
    }

    /** O apex atual morreu, sumiu ou foi domesticado: outro vem depois da espera. */
    private void release(ServerLevel level) {
        apex = null;
        respawnAt = level.getGameTime() + StarterApex.RESPAWN_TICKS;
        setDirty();
    }

    private void tick(ServerLevel level) {
        if (apex != null) {
            Entity entity = level.getEntity(apex);
            if (entity instanceof PrehistoricCreature creature && creature.isAlive()) {
                if (creature.isTame()) {
                    release(level);
                } else if (!creature.blockPosition().equals(lastKnown)) {
                    lastKnown = creature.blockPosition();
                    setDirty();
                }
                return;
            }
            // Fora dos chunks carregados, segue vivo. Com alguém ali do lado e ele não está: sumiu.
            if (lastKnown == null || !level.hasChunkAt(lastKnown) || level.getNearestPlayer(lastKnown.getX(),
                    lastKnown.getY(), lastKnown.getZ(), StarterApex.LOST_CHECK_RADIUS, false) == null) {
                return;
            }
            apex = null;
            setDirty();
        }
        if (StarterApex.due(apex != null, level.getGameTime(), respawnAt)) {
            spawn(level, level.random::nextLong);
        }
    }

    /** Tenta pôr o apex num ponto do anel com chunk carregado e chão firme. */
    private void spawn(ServerLevel level, RandomGenerator random) {
        BlockPos center = level.getSharedSpawnPos();
        EntityType<?> type = ModEntities.TYRANNOSAURUS.get();
        for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
            WildSpawnRules.Offset offset = WildSpawnRules.ringOffset(StarterApex.MIN_DISTANCE, StarterApex.MAX_DISTANCE, random);
            int x = center.getX() + offset.x();
            int z = center.getZ() + offset.z();
            if (!level.hasChunkAt(x, z) || !StarterApex.validCenter(
                    DangerZones.horizontalDistance(x, z, center.getX(), center.getZ()))) {
                continue;
            }
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()
                    || !level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                    || !level.noCollision(type.getDimensions().makeBoundingBox(x + 0.5, pos.getY(), z + 0.5))
                    || level.getNearestPlayer(x, pos.getY(), z, StarterApex.MIN_PLAYER_GAP, false) != null) {
                continue;
            }
            if (!(type.create(level) instanceof PrehistoricCreature creature)) {
                return;
            }
            creature.moveTo(x + 0.5, pos.getY(), z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            creature.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            creature.setPersistenceRequired();
            if (!level.addFreshEntity(creature)) {
                return;
            }
            creature.setTerritory(pos, StarterApex.TERRITORY_RADIUS);
            apex = creature.getUUID();
            lastKnown = pos;
            setDirty();
            return;
        }
    }
}
