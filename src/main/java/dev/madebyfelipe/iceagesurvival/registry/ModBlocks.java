package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.BlackFruitLeavesBlock;
import dev.madebyfelipe.iceagesurvival.block.ChemistryBenchBlock;
import dev.madebyfelipe.iceagesurvival.block.IncubatorBlock;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IceAgeSurvival.MODID);

    public static final DeferredBlock<BlackFruitLeavesBlock> BLACK_FRUIT_LEAVES = BLOCKS.register(
            "black_fruit_leaves",
            () -> new BlackFruitLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES)));

    /** Cresce na árvore de fruta-negra, a mesma que nasce no mundo. */
    public static final TreeGrower BLACK_FRUIT_TREE_GROWER = new TreeGrower(
            IceAgeSurvival.MODID + ":black_fruit",
            Optional.empty(),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, IceAgeSurvival.id("black_fruit_tree"))),
            Optional.empty());

    public static final DeferredBlock<SaplingBlock> BLACK_FRUIT_SAPLING = BLOCKS.register(
            "black_fruit_sapling",
            () -> new SaplingBlock(BLACK_FRUIT_TREE_GROWER, BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING)));

    public static final DeferredBlock<IncubatorBlock> INCUBATOR = BLOCKS.register(
            "incubator",
            () -> new IncubatorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5F)));

    public static final DeferredBlock<ChemistryBenchBlock> CHEMISTRY_BENCH = BLOCKS.register(
            "chemistry_bench",
            () -> new ChemistryBenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)));

    private ModBlocks() {
    }
}
