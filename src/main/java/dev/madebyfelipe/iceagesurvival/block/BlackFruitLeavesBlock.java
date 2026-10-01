package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Folhas da árvore de fruta-negra. Enquanto presas a uma árvore viva, amadurecem com o
 * tempo; maduras, soltam frutas ao clique direito e voltam a amadurecer.
 */
public class BlackFruitLeavesBlock extends LeavesBlock {
    public static final BooleanProperty RIPE = BooleanProperty.create("ripe");
    /** Em média, um bloco amadurece após este número de ticks aleatórios (um a cada ~68 s). */
    private static final int RIPEN_CHANCE = 12;
    private static final int MIN_FRUIT = 1;
    private static final int MAX_FRUIT = 2;

    public BlackFruitLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RIPE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RIPE);
    }

    private static boolean canRipen(BlockState state) {
        // Folhas colocadas pelo jogador (persistentes) não pertencem a uma árvore viva.
        return !state.getValue(RIPE) && !state.getValue(PERSISTENT);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state) || canRipen(state);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        // O super remove o bloco quando a folha apodrece longe de um tronco.
        if (level.getBlockState(pos) == state && canRipen(state) && random.nextInt(RIPEN_CHANCE) == 0) {
            level.setBlock(pos, state.setValue(RIPE, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!state.getValue(RIPE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int count = MIN_FRUIT + level.random.nextInt(MAX_FRUIT - MIN_FRUIT + 1);
            popResource(level, pos, new ItemStack(ModItems.BLACK_FRUIT.get(), count));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F);
            level.setBlock(pos, state.setValue(RIPE, false), Block.UPDATE_CLIENTS);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
