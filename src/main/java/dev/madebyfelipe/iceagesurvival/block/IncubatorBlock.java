package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Choca os ovos das criaturas. Ver {@link IncubatorBlockEntity}. */
public class IncubatorBlock extends StationBlock {
    public IncubatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type() {
        return ModBlockEntities.INCUBATOR;
    }
}
