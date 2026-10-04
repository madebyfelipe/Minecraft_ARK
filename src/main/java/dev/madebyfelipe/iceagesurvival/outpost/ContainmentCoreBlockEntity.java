package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * O campo de êxtase da base. Na primeira vez que roda, procura os {@link StasisGeneratorBlock geradores} em volta
 * (até {@link #SCAN_RADIUS} na horizontal) e guarda onde estão. Enquanto algum estiver de pé:
 * <ul>
 *   <li>faz nascer o espécime ({@link #SPECIMEN}, o {@link TitanovenatorBoss}) no centro, uma vez, com o covil ali;</li>
 *   <li>congela toda criatura (não jogador) a até {@link #FIELD_RADIUS} do núcleo: sem IA, sem movimento e
 *   invulnerável, marcada com {@link #CONTAINED_TAG};</li>
 *   <li>mostra o campo com partículas.</li>
 * </ul>
 * Caído o último gerador, solta de uma vez tudo o que segurava, com aviso e rugido; dali em diante o núcleo não faz
 * mais nada. Quem foi preso continua marcado: a morte dele destrava o dossiê ({@link Outposts#onContainedDeath}).
 */
public class ContainmentCoreBlockEntity extends BlockEntity {
    /** O espécime que a base guarda; enquanto a espécie não existir no jogo, o núcleo segura o que for posto ali. */
    public static final ResourceLocation SPECIMEN = IceAgeSurvival.id("titanovenator");
    public static final String CONTAINED_TAG = "iceagesurvival.contained";
    public static final double FIELD_RADIUS = 6.0;
    static final int SCAN_RADIUS = 24;
    /** Raio do covil do Titanovenator em volta do núcleo: o salão do hangar. */
    static final int LAIR_RADIUS = 24;
    static final int SCAN_HEIGHT = 8;
    static final int CHECK_INTERVAL = 10;

    private final List<BlockPos> generators = new ArrayList<>();
    private final Set<UUID> held = new HashSet<>();
    private boolean scanned;
    private boolean specimenSpawned;
    private boolean released;

    public ContainmentCoreBlockEntity(BlockPos pos, BlockState state) {
        super(Outposts.CONTAINMENT_CORE_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ContainmentCoreBlockEntity core) {
        if (level instanceof ServerLevel server && server.getGameTime() % CHECK_INTERVAL == 0) {
            core.tick(server);
        }
    }

    public boolean released() {
        return released;
    }

    public int generatorCount() {
        return generators.size();
    }

    /** Quantos geradores ainda estão de pé. */
    public int standingGenerators() {
        return level == null ? 0 : (int) generators.stream()
                .filter(pos -> level.getBlockState(pos).is(Outposts.STASIS_GENERATOR.get())).count();
    }

    void tick(ServerLevel level) {
        if (released) {
            return;
        }
        if (!scanned) {
            scan(level);
        }
        if (standingGenerators() == 0) {
            release(level);
            return;
        }
        if (!specimenSpawned) {
            spawnSpecimen(level);
        }
        hold(level);
        showField(level);
    }

    private void scan(ServerLevel level) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                for (int dy = -SCAN_HEIGHT; dy <= SCAN_HEIGHT; dy++) {
                    cursor.setWithOffset(worldPosition, dx, dy, dz);
                    if (level.getBlockState(cursor).is(Outposts.STASIS_GENERATOR.get())) {
                        generators.add(cursor.immutable());
                    }
                }
            }
        }
        scanned = true;
        setChanged();
    }

    private void spawnSpecimen(ServerLevel level) {
        // O Titanovenator nasce preso ao salão da base como covil: solto, caça ali dentro e volta se for atraído longe.
        TitanovenatorBoss boss = TitanovenatorBoss.summon(level, worldPosition.above(), LAIR_RADIUS);
        if (boss != null) {
            specimenSpawned = true;
            setChanged();
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(SPECIMEN);
        if (type == null || !ForgeRegistries.ENTITY_TYPES.containsKey(SPECIMEN)) {
            return;
        }
        Entity specimen = type.create(level);
        if (specimen == null) {
            return;
        }
        specimen.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                level.random.nextFloat() * 360.0F, 0.0F);
        if (specimen instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.STRUCTURE, null, null);
        }
        level.addFreshEntity(specimen);
        specimenSpawned = true;
        setChanged();
    }

    private AABB field() {
        return new AABB(worldPosition).inflate(FIELD_RADIUS);
    }

    private void hold(ServerLevel level) {
        Vec3 center = Vec3.atBottomCenterOf(worldPosition);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, field(),
                entity -> !(entity instanceof Player) && entity.isAlive()
                        && entity.position().distanceTo(center) <= FIELD_RADIUS + entity.getBbWidth() / 2)) {
            freeze(entity);
        }
        for (UUID id : held) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                freeze(living);
            }
        }
    }

    private void freeze(LivingEntity entity) {
        if (held.add(entity.getUUID())) {
            setChanged();
        }
        entity.getPersistentData().putBoolean(CONTAINED_TAG, true);
        entity.setInvulnerable(true);
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private void showField(ServerLevel level) {
        double y = worldPosition.getY() + 0.2 + level.random.nextDouble() * 4.0;
        for (int i = 0; i < 24; i++) {
            double angle = i * Mth.TWO_PI / 24;
            level.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5 + Math.cos(angle) * FIELD_RADIUS, y,
                    worldPosition.getZ() + 0.5 + Math.sin(angle) * FIELD_RADIUS, 1, 0, 0, 0, 0);
        }
    }

    private void release(ServerLevel level) {
        released = true;
        for (UUID id : held) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living) {
                living.setInvulnerable(false);
                if (living instanceof Mob mob) {
                    mob.setNoAi(false);
                }
            }
        }
        level.playSound(null, worldPosition, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0F, 0.6F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(64))) {
            player.displayClientMessage(Component.translatable("block.iceagesurvival.containment_core.released"), true);
        }
        setChanged();
    }

    @Nullable
    private static BlockPos readPos(Tag tag) {
        return tag instanceof CompoundTag compound ? NbtUtils.readBlockPos(compound) : null;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        generators.clear();
        for (Tag element : tag.getList("Generators", Tag.TAG_COMPOUND)) {
            BlockPos pos = readPos(element);
            if (pos != null) {
                generators.add(pos);
            }
        }
        held.clear();
        for (Tag element : tag.getList("Held", Tag.TAG_INT_ARRAY)) {
            held.add(NbtUtils.loadUUID(element));
        }
        scanned = tag.getBoolean("Scanned");
        specimenSpawned = tag.getBoolean("SpecimenSpawned");
        released = tag.getBoolean("Released");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        generators.forEach(pos -> list.add(NbtUtils.writeBlockPos(pos)));
        tag.put("Generators", list);
        ListTag ids = new ListTag();
        held.forEach(id -> ids.add(NbtUtils.createUUID(id)));
        tag.put("Held", ids);
        tag.putBoolean("Scanned", scanned);
        tag.putBoolean("SpecimenSpawned", specimenSpawned);
        tag.putBoolean("Released", released);
    }
}
