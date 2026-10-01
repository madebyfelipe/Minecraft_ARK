package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.BlackFruitLeavesBlock;
import dev.madebyfelipe.iceagesurvival.block.ChemistryBenchBlock;
import dev.madebyfelipe.iceagesurvival.block.IncubatorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IceAgeSurvival.MODID);

    public static final RegistryObject<BlackFruitLeavesBlock> BLACK_FRUIT_LEAVES = BLOCKS.register(
            "black_fruit_leaves",
            () -> new BlackFruitLeavesBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_LEAVES)));

    /** Cresce na árvore de fruta-negra, a mesma que nasce no mundo. */
    public static final AbstractTreeGrower BLACK_FRUIT_TREE_GROWER = new AbstractTreeGrower() {
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return ResourceKey.create(Registries.CONFIGURED_FEATURE, IceAgeSurvival.id("black_fruit_tree"));
        }
    };

    public static final RegistryObject<SaplingBlock> BLACK_FRUIT_SAPLING = BLOCKS.register(
            "black_fruit_sapling",
            () -> new SaplingBlock(BLACK_FRUIT_TREE_GROWER, BlockBehaviour.Properties.copy(Blocks.SPRUCE_SAPLING)));

    public static final RegistryObject<IncubatorBlock> INCUBATOR = BLOCKS.register(
            "incubator",
            () -> new IncubatorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.5F)));

    public static final RegistryObject<ChemistryBenchBlock> CHEMISTRY_BENCH = BLOCKS.register(
            "chemistry_bench",
            () -> new ChemistryBenchBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));

    private ModBlocks() {
    }
}
