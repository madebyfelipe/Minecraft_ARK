package dev.madebyfelipe.iceagesurvival.block;

import com.mojang.serialization.MapCodec;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.function.Supplier;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Prepara narcóticos, estimulantes e flechas tranquilizantes em lote. Ver {@link ChemistryBenchBlockEntity}. */
public class ChemistryBenchBlock extends StationBlock {
    public static final MapCodec<ChemistryBenchBlock> CODEC = simpleCodec(ChemistryBenchBlock::new);

    public ChemistryBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type() {
        return ModBlockEntities.CHEMISTRY_BENCH;
    }
}
