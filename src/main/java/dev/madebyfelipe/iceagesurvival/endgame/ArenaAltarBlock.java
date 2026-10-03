package dev.madebyfelipe.iceagesurvival.endgame;

import dev.madebyfelipe.iceagesurvival.entity.GiganotosaurusBoss;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * O altar no centro da arena da caverna (D47). O tributo — a cabeça-troféu de um apex (T-Rex ou Espinossauro), como
 * no desafio do apex (D30) — chama o Giganotosaurus: o altar consome a cabeça, um rugido ecoa por
 * {@link #SUMMON_DELAY_TICKS} e o boss aparece ao lado, ligado a este altar. Enquanto o boss dele vive (ou o rugido
 * ainda ecoa), o altar recusa outro tributo; vencido o boss, chama de novo.
 *
 * <p>Indestrutível em sobrevivência (propriedades do bedrock): nem picareta, nem explosão, nem o Wither. A frente da
 * caverna põe o bloco na arena.
 */
public class ArenaAltarBlock extends Block {
    /** O rugido do tributo ecoando: o boss ainda não apareceu, mas o altar já está ocupado. */
    public static final BooleanProperty SUMMONING = BooleanProperty.create("summoning");
    /** Quanto dura o rugido antes de o boss aparecer: 3 s. */
    public static final int SUMMON_DELAY_TICKS = 60;
    /** Até onde o altar procura o boss dele vivo. */
    private static final double BOSS_SEARCH_RADIUS = 128.0;
    /** A que distância do altar o boss aparece (o altar fica no meio da arena; o boss, ao lado). */
    public static final int SPAWN_DISTANCE = 6;
    /** Quem está a até esta distância do altar ouve o aviso do tributo. */
    private static final double MESSAGE_RADIUS = 48.0;
    private static final ResourceLocation ROAR = ResourceLocation.fromNamespaceAndPath("jurassicreborn",
            "giganotosaurus_roar");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(3, 4, 3, 13, 12, 13),
            Block.box(1, 12, 1, 15, 16, 15));

    public ArenaAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SUMMONING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUMMONING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** As cabeças-troféu que valem de tributo: as dos apex ({@code #iceagesurvival:apex}). */
    public static boolean isTribute(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (var holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(ModTags.APEX)) {
            Item trophy = PrehistoricCreature.trophyFor(holder.value());
            if (trophy != null && trophy != Items.AIR && stack.is(trophy)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!isTribute(held)) {
            if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && !isTribute(player.getOffhandItem())) {
                player.displayClientMessage(Component.translatableWithFallback("iceagesurvival.altar.needs_tribute",
                        "The altar asks for the trophy head of an apex: Tyrannosaurus or Spinosaurus."), true);
            }
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(SUMMONING) || linkedBoss(level, pos) != null) {
            player.displayClientMessage(Component.translatableWithFallback("iceagesurvival.altar.busy",
                    "The Giganotosaurus of this altar still lives."), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.setBlock(pos, state.setValue(SUMMONING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, SUMMON_DELAY_TICKS);
        level.playSound(null, pos, roarSound(), SoundSource.HOSTILE, 6.0F, 0.8F);
        level.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0F, 0.6F);
        Component message = Component.translatableWithFallback("iceagesurvival.altar.tribute",
                "The tribute is accepted. Something huge roars in the depths...");
        for (Player nearby : level.players()) {
            if (nearby.distanceToSqr(Vec3.atCenterOf(pos)) <= MESSAGE_RADIUS * MESSAGE_RADIUS) {
                nearby.displayClientMessage(message, true);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SUMMONING)) {
            return;
        }
        level.setBlock(pos, state.setValue(SUMMONING, false), Block.UPDATE_ALL);
        summon(level, pos);
    }

    /** O boss ligado a este altar, vivo, se houver. */
    @Nullable
    public static GiganotosaurusBoss linkedBoss(Level level, BlockPos pos) {
        List<GiganotosaurusBoss> bosses = level.getEntitiesOfClass(GiganotosaurusBoss.class,
                new AABB(pos).inflate(BOSS_SEARCH_RADIUS), boss -> boss.isAlive() && pos.equals(boss.altar()));
        return bosses.isEmpty() ? null : bosses.get(0);
    }

    /** Põe o boss ao lado do altar, ligado a ele. */
    @Nullable
    public static GiganotosaurusBoss summon(ServerLevel level, BlockPos pos) {
        Entity created = ModEntities.GIGANOTOSAURUS.get().create(level);
        if (!(created instanceof GiganotosaurusBoss boss)) {
            if (created != null) {
                created.discard();
            }
            return null;
        }
        Vec3 spot = spawnSpot(level, pos, boss);
        boss.moveTo(spot.x, spot.y, spot.z, level.random.nextFloat() * 360.0F, 0.0F);
        boss.setAltar(pos);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null, null);
        level.addFreshEntity(boss);
        boss.threatDisplay();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, spot.x, spot.y + 1.0, spot.z, 60, 2.0, 1.5, 2.0, 0.02);
        return boss;
    }

    /**
     * Onde o boss aparece: a {@link #SPAWN_DISTANCE} blocos do altar, no primeiro dos oito rumos com chão e espaço
     * para o corpo inteiro; sem nenhum, em cima do altar.
     */
    public static Vec3 spawnSpot(ServerLevel level, BlockPos altar, Entity boss) {
        EntityType<?> type = boss.getType();
        int d = SPAWN_DISTANCE;
        int diagonal = (int) Math.round(SPAWN_DISTANCE / Math.sqrt(2.0));
        int[][] offsets = {{0, -d}, {d, 0}, {0, d}, {-d, 0},
                {diagonal, -diagonal}, {diagonal, diagonal}, {-diagonal, diagonal}, {-diagonal, -diagonal}};
        for (int[] offset : offsets) {
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos feet = altar.offset(offset[0], dy, offset[1]);
                Vec3 spot = Vec3.atBottomCenterOf(feet);
                if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                        && level.noCollision(type.getAABB(spot.x, spot.y, spot.z))) {
                    return spot;
                }
            }
        }
        return Vec3.atBottomCenterOf(altar.above());
    }

    private static SoundEvent roarSound() {
        SoundEvent roar = BuiltInRegistries.SOUND_EVENT.get(ROAR);
        return roar != null ? roar : SoundEvents.RAVAGER_ROAR;
    }

    /** O rugido ecoando: fumaça e cinza saindo do altar. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SUMMONING)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.1,
                    pos.getZ() + random.nextDouble(), 0.0, 0.05, 0.0);
        }
        level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    /** Nem o Wither nem o dragão quebram o altar. */
    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }
}
