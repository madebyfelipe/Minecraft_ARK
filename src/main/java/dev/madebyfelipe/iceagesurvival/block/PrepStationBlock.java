package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Apodrece carne e faz charque. Ver {@link PrepStationBlockEntity}. */
public class PrepStationBlock extends StationBlock {
    public PrepStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type() {
        return ModBlockEntities.PREP_STATION;
    }
}
