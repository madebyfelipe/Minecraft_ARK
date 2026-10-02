package dev.madebyfelipe.iceagesurvival.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Marca os blocos de defesa: nenhuma criatura os quebra esbarrando ou mordendo; só os gigantes, na madeira. */
public interface DefenseBlock {
    /** Se é de madeira e cede aos golpes dos gigantes ({@link DefenseBlocks#WALL_BREAKERS}). Pedra e armadilha, não. */
    default boolean yieldsToGiants() {
        return false;
    }

    /**
     * A posição que guarda os golpes recebidos: o próprio bloco no muro; a parte mestra no portão, para que golpes
     * em partes diferentes somem no mesmo portão.
     */
    default BlockPos damageKey(BlockGetter level, BlockPos pos, BlockState state) {
        return pos;
    }
}
