package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Prepara narcóticos, estimulantes e flechas tranquilizantes em lote. Ver {@link ChemistryBenchBlockEntity}. */
public class ChemistryBenchBlock extends StationBlock {
    public ChemistryBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type() {
        return ModBlockEntities.CHEMISTRY_BENCH;
    }
}
