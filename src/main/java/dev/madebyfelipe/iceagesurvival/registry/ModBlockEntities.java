package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.ChemistryBenchBlockEntity;
import dev.madebyfelipe.iceagesurvival.block.IncubatorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IceAgeSurvival.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<IncubatorBlockEntity>> INCUBATOR =
            BLOCK_ENTITIES.register("incubator", () -> BlockEntityType.Builder
                    .of(IncubatorBlockEntity::new, ModBlocks.INCUBATOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ChemistryBenchBlockEntity>> CHEMISTRY_BENCH =
            BLOCK_ENTITIES.register("chemistry_bench", () -> BlockEntityType.Builder
                    .of(ChemistryBenchBlockEntity::new, ModBlocks.CHEMISTRY_BENCH.get()).build(null));

    private ModBlockEntities() {
    }
}
