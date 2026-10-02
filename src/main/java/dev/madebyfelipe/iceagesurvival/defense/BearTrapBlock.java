package dev.madebyfelipe.iceagesurvival.defense;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Armadilha de urso: armada, morde o primeiro estranho que pisar (6 de dano) e o prende sem andar por 5 s — 2 s
 * se for um gigante ({@link DefenseBlocks#WALL_BREAKERS}). Depois solta e fica desarmada até o dono (ou um aliado)
 * rearmar com clique direito.
 */
public class BearTrapBlock extends OwnedDefenseBlock {
    public static final float DAMAGE = 6.0F;
    public static final int HOLD_TICKS = 100;
    public static final int GIANT_HOLD_TICKS = 40;
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    /** Preso: não anda nem pula; a queda segue devagar, como na teia. */
    private static final Vec3 HELD = new Vec3(1.0E-4, 0.05, 1.0E-4);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

    /** Armada; prendendo alguém; disparada (desarmada, esperando o dono). */
    public enum Stage implements StringRepresentable {
        ARMED, HOLDING, SPRUNG;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public BearTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.ARMED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BearTrapBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, DefenseBlocks.BEAR_TRAP_ENTITY.get(), BearTrapBlockEntity::serverTick);
    }

    /** Quanto tempo a armadilha segura esta vítima. */
    public static int holdTicks(Entity victim) {
        return victim instanceof Mob mob && DefenseDamage.isBreaker(mob) ? GIANT_HOLD_TICKS : HOLD_TICKS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
            return;
        }
        Stage stage = state.getValue(STAGE);
        if (stage == Stage.HOLDING) {
            // Nos dois lados: o cliente também segura o jogador que ele move, sem esperar a correção do servidor.
            if (!spares(level, pos, entity)) {
                entity.makeStuckInBlock(state, HELD);
            }
            return;
        }
        if (stage != Stage.ARMED || level.isClientSide || spares(level, pos, entity)
                || !(level.getBlockEntity(pos) instanceof BearTrapBlockEntity trap)) {
            return;
        }
        living.hurt(DefenseBlocks.trapDamage(level), DAMAGE);
        entity.makeStuckInBlock(state, HELD);
        trap.hold(level.getGameTime() + holdTicks(entity));
        level.setBlock(pos, state.setValue(STAGE, Stage.HOLDING), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (state.getValue(STAGE) != Stage.SPRUNG) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        DefenseOwnerBlockEntity owned = owned(level, pos);
        if (owned == null || !owned.isOwnerOrAlly(player)) {
            player.displayClientMessage(Component.translatable("message.iceagesurvival.bear_trap.not_owner"), true);
            return InteractionResult.CONSUME;
        }
        level.setBlock(pos, state.setValue(STAGE, Stage.ARMED), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }
}
