package dev.madebyfelipe.iceagesurvival.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** O dono da armadilha de urso e até quando ela segura a vítima atual. */
public class BearTrapBlockEntity extends DefenseOwnerBlockEntity {
    private long releaseAt;

    public BearTrapBlockEntity(BlockPos pos, BlockState state) {
        super(DefenseBlocks.BEAR_TRAP_ENTITY.get(), pos, state);
    }

    public void hold(long until) {
        releaseAt = until;
        setChanged();
    }

    /** O tick do jogo em que a vítima atual se solta. */
    public long releaseAt() {
        return releaseAt;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, BearTrapBlockEntity trap) {
        if (state.getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.HOLDING && level.getGameTime() >= trap.releaseAt) {
            level.setBlock(pos, state.setValue(BearTrapBlock.STAGE, BearTrapBlock.Stage.SPRUNG), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("ReleaseAt", releaseAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        releaseAt = tag.getLong("ReleaseAt");
    }
}
