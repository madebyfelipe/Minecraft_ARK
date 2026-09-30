package dev.madebyfelipe.iceagesurvival.block;

import com.mojang.serialization.MapCodec;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import java.util.function.Supplier;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Choca os ovos das criaturas. Ver {@link IncubatorBlockEntity}. */
public class IncubatorBlock extends StationBlock {
    public static final MapCodec<IncubatorBlock> CODEC = simpleCodec(IncubatorBlock::new);

    public IncubatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends StationBlockEntity>> type() {
        return ModBlockEntities.INCUBATOR;
    }
}
