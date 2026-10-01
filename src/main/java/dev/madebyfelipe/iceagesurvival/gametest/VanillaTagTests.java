package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Nossos itens e blocos nas tags do vanilla. No 1.20.1 as pastas são {@code tags/items} e
 * {@code tags/blocks}; os nomes do 1.21 ({@code tags/item}, {@code tags/block}) são ignorados sem
 * erro — foi assim que a flecha tranquilizante deixou de sair do arco depois do port.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class VanillaTagTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void tranqArrowIsAmmoForTheBow(GameTestHelper helper) {
        ItemStack arrow = new ItemStack(ModItems.TRANQ_ARROW.get());
        helper.assertTrue(arrow.is(ItemTags.ARROWS), "flecha tranquilizante fora de #minecraft:arrows");
        helper.assertTrue(((BowItem) Items.BOW).getAllSupportedProjectiles().test(arrow), "o arco não aceita a flecha");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void blackFruitTreeIsInTheVanillaTags(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.BLACK_FRUIT_LEAVES.get().defaultBlockState().is(BlockTags.LEAVES), "folhas fora de #leaves");
        helper.assertTrue(ModBlocks.BLACK_FRUIT_SAPLING.get().defaultBlockState().is(BlockTags.SAPLINGS), "muda fora de #saplings");
        helper.assertTrue(new ItemStack(ModItems.BLACK_FRUIT_SAPLING.get()).is(ItemTags.SAPLINGS), "item da muda fora de #saplings");
        helper.succeed();
    }
}
