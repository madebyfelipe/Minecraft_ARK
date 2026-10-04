package dev.madebyfelipe.iceagesurvival.outpost;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.containment.ContainmentShell;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * O campo de êxtase da base. Na primeira vez que roda, procura os {@link StasisGeneratorBlock emissores} em volta
 * (até {@link #SCAN_RADIUS} na horizontal) e guarda onde estão. Enquanto o campo estiver de pé:
 * <ul>
 *   <li>faz nascer o espécime ({@link #SPECIMEN}, o {@link TitanovenatorBoss}) no centro, uma vez, com o covil ali;</li>
 *   <li>congela toda criatura (não jogador) a até {@link #FIELD_RADIUS} do núcleo: sem IA, sem movimento e
 *   invulnerável, marcada com {@link #CONTAINED_TAG};</li>
 *   <li>o cliente desenha o cilindro de êxtase ({@code client/containment/StasisFieldRenderer}) com o estado que este
 *   bloco sincroniza.</li>
 * </ul>
 * O campo cai pelo console ({@link ContainmentShell}, comando {@code campo desligar} do operador): o
 * {@link #shutdown desligamento} começa um colapso de {@link #COLLAPSE_TICKS} (o campo pisca e encolhe, ainda
 * segurando) e então solta de uma vez tudo o que segurava, com aviso e rugido; dali em diante o núcleo não faz mais
 * nada. Os emissores são indestrutíveis; se mesmo assim sumirem todos (modo criativo), o campo cai na hora. Quem foi
 * preso continua marcado: a morte dele destrava o dossiê ({@link Outposts#onContainedDeath}).
 */
public class ContainmentCoreBlockEntity extends BlockEntity {
    /** O espécime que a base guarda; enquanto a espécie não existir no jogo, o núcleo segura o que for posto ali. */
    public static final ResourceLocation SPECIMEN = IceAgeSurvival.id("titanovenator");
    public static final String CONTAINED_TAG = "iceagesurvival.contained";
    public static final double FIELD_RADIUS = 6.0;
    /** Altura do cilindro de êxtase acima do núcleo: o Titanovenator tem 8,64 de altura. */
    public static final double FIELD_HEIGHT = 11.0;
    /** Duração do colapso do campo depois do comando, em ticks. */
    public static final int COLLAPSE_TICKS = 60;
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
    /** Tempo de jogo em que o console mandou desligar; −1 enquanto ninguém mandou. */
    private long shutdownAt = -1;

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

    /** Quantos emissores ainda estão de pé. */
    public int standingGenerators() {
        return level == null ? 0 : (int) generators.stream()
                .filter(pos -> level.getBlockState(pos).is(Outposts.STASIS_GENERATOR.get())).count();
    }

    /** Se o console já mandou desligar (o campo pode ainda estar no colapso). */
    public boolean shuttingDown() {
        return shutdownAt >= 0;
    }

    // ---- Para o cliente (sincronizado) ----

    /** Se o cilindro aparece: achados os emissores, até o fim do colapso. */
    public boolean fieldVisible() {
        return scanned && !released && !generators.isEmpty();
    }

    /** 0 com o campo firme; sobe até 1 durante o colapso. */
    public float collapse(float partialTick) {
        if (released) {
            return 1.0F;
        }
        if (shutdownAt < 0 || level == null) {
            return 0.0F;
        }
        return Mth.clamp((level.getGameTime() - shutdownAt + partialTick) / COLLAPSE_TICKS, 0.0F, 1.0F);
    }

    /** Onde ficam os emissores (posições absolutas). */
    public List<BlockPos> emitters() {
        return List.copyOf(generators);
    }

    // ---- Console ----

    /** O estado que o console mostra. */
    public ContainmentShell.Status status() {
        boolean holding = level != null && held.stream().map(id -> level instanceof ServerLevel server
                ? server.getEntity(id) : null).anyMatch(entity -> entity != null && entity.isAlive());
        return new ContainmentShell.Status(!released && shutdownAt < 0 && standingGenerators() > 0,
                !released && shutdownAt >= 0, standingGenerators(), generators.size(), !released && holding);
    }

    /** O console mandou desligar: começa o colapso; no fim dele, o campo solta tudo. */
    public void shutdown(ServerLevel level) {
        if (released || shutdownAt >= 0) {
            return;
        }
        if (!scanned) {
            scan(level);
        }
        shutdownAt = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 4.0F, 0.5F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(64))) {
            player.displayClientMessage(Component.translatable("block.iceagesurvival.containment_core.collapsing"),
                    true);
        }
        sync();
    }

    void tick(ServerLevel level) {
        if (released) {
            return;
        }
        if (!scanned) {
            scan(level);
        }
        if (standingGenerators() == 0 || shutdownAt >= 0 && level.getGameTime() >= shutdownAt + COLLAPSE_TICKS) {
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
        sync();
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

    /** Geada no chão do campo e faíscas nos emissores; o cilindro é desenhado no cliente. */
    private void showField(ServerLevel level) {
        double angle = level.random.nextDouble() * Mth.TWO_PI;
        double radius = FIELD_RADIUS * Math.sqrt(level.random.nextDouble());
        level.sendParticles(ParticleTypes.SNOWFLAKE, worldPosition.getX() + 0.5 + Math.cos(angle) * radius,
                worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5 + Math.sin(angle) * radius, 2, 0.3, 0.05, 0.3,
                0.005);
        if (shutdownAt >= 0 || level.random.nextInt(4) == 0) {
            for (BlockPos emitter : generators) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, emitter.getX() + 0.5, emitter.getY() + 1.1,
                        emitter.getZ() + 0.5, shutdownAt >= 0 ? 6 : 1, 0.2, 0.2, 0.2, 0.05);
            }
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
        sync();
    }

    /** Salva e manda o estado para quem vê o bloco (o cilindro depende dele). */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
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
        shutdownAt = tag.contains("ShutdownAt") ? tag.getLong("ShutdownAt") : -1;
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
        if (shutdownAt >= 0) {
            tag.putLong("ShutdownAt", shutdownAt);
        }
    }

    /** O cliente recebe o estado inteiro (são poucas posições e números). */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** O cilindro passa muito do bloco: o recorte de visão tem de considerar o campo e os emissores. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(SCAN_RADIUS + 2, FIELD_HEIGHT + 2, SCAN_RADIUS + 2);
    }
}
