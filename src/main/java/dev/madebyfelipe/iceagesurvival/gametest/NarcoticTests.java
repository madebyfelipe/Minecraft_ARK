package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.BlackFruitLeavesBlock;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class NarcoticTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void narcoticRaisesTorporWithoutDamage(GameTestHelper helper) {
        TestCreature creature = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
        creature.addTorpor(creature.maxTorpor());
        creature.setTorpor(1.0);
        float health = creature.getHealth();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NARCOTIC.get(), 3));

        creature.mobInteract(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(creature.torpor() > 1.0, "torpor não subiu: " + creature.torpor());
        helper.assertTrue(creature.getHealth() == health, "narcótico causou dano");
        helper.assertTrue(player.getMainHandItem().getCount() == 2, "narcótico não foi consumido");
        helper.assertTrue(creature.tamingProgress() == 0, "narcótico contou como alimento");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void narcoticDoesNothingToAnAwakeCreature(GameTestHelper helper) {
        TestCreature creature = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NARCOTIC.get(), 3));

        creature.mobInteract(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(creature.torpor() == 0 && player.getMainHandItem().getCount() == 3,
                "criatura acordada aceitou narcótico");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void recipesAreLoaded(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(IceAgeSurvival.id("narcotic")).isPresent(), "receita do narcótico ausente");
        var arrow = recipes.byKey(IceAgeSurvival.id("tranq_arrow")).orElseThrow();
        boolean usesNarcotic = arrow.value().getIngredients().stream()
                .anyMatch(ingredient -> ingredient.test(new ItemStack(ModItems.NARCOTIC.get())));
        helper.assertTrue(usesNarcotic, "flecha tranquilizante não usa narcótico");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ripeLeavesGiveFruitAndBecomeUnripe(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.BLACK_FRUIT_LEAVES.get().defaultBlockState()
                .setValue(BlackFruitLeavesBlock.RIPE, true)
                .setValue(LeavesBlock.PERSISTENT, true));

        helper.useBlock(pos, helper.makeMockPlayer(GameType.SURVIVAL));

        helper.assertItemEntityPresent(ModItems.BLACK_FRUIT.get(), pos, 2.0);
        helper.assertTrue(!helper.getBlockState(pos).getValue(BlackFruitLeavesBlock.RIPE), "folhas continuam maduras");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void unripeLeavesGiveNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.BLACK_FRUIT_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));
        helper.useBlock(pos, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertItemEntityNotPresent(ModItems.BLACK_FRUIT.get(), pos, 2.0);
        helper.succeed();
    }

    @GameTest(template = EMPTY, skyAccess = true)
    public static void blackFruitTreeFeatureGrowsATree(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(1, 1, 1, Blocks.DIRT);
        BlockPos base = helper.absolutePos(new BlockPos(1, 2, 1));
        ConfiguredFeature<?, ?> tree = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, IceAgeSurvival.id("black_fruit_tree")));

        helper.assertTrue(tree.place(level, level.getChunkSource().getGenerator(), level.random, base),
                "árvore não foi colocada");
        helper.assertTrue(level.getBlockState(base).is(Blocks.SPRUCE_LOG), "sem tronco na base");
        boolean hasLeaves = false;
        for (int y = 2; y <= 6 && !hasLeaves; y++) {
            hasLeaves = level.getBlockState(base.above(y)).is(ModBlocks.BLACK_FRUIT_LEAVES.get());
        }
        helper.assertTrue(hasLeaves, "sem folhas de fruta-negra no topo");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void treeIsRegisteredForWorldGeneration(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .containsKey(IceAgeSurvival.id("black_fruit_tree")), "placed feature ausente");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void largeSpeciesResistKnockback(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        helper.assertTrue(smilodon.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1.0,
                "Smilodon sem resistência a recuo");
        smilodon.knockback(1.0, 1.0, 0.0);
        helper.assertTrue(smilodon.getDeltaMovement().horizontalDistanceSqr() == 0, "Smilodon foi arremessado");

        TestCreature small = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
        helper.assertTrue(small.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.0,
                "criatura pequena ganhou resistência");
        helper.succeed();
    }
}
