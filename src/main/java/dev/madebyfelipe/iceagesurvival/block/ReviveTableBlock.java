package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Revive a criatura do implante, ao custo de um diamante. Ver {@link ReviveTableBlockEntity}. */
public class ReviveTableBlock extends StationBlock {
    public ReviveTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type() {
        return ModBlockEntities.REVIVE_TABLE;
    }
}
