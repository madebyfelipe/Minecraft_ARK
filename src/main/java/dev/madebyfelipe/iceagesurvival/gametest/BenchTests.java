package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.defense.bench.BenchKind;
import dev.madebyfelipe.iceagesurvival.defense.bench.BenchMenu;
import dev.madebyfelipe.iceagesurvival.defense.bench.BenchRecipe;
import dev.madebyfelipe.iceagesurvival.defense.bench.Benches;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Bancadas de Construção e de Armeiro: receitas por dados, lista por bancada e fabricação pelo inventário. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BenchTests {
    private static final String EMPTY = "empty";

    private static final List<String> DEFENSES = List.of("wood_wall", "stone_wall", "wood_gate", "stone_gate",
            "large_wood_gate", "large_stone_gate", "spike_trap", "thorn_palisade", "bear_trap", "foliage_cover");
    private static final List<String> WEAPONS = List.of("tranq_rifle", "tranq_crossbow", "tranq_dart");

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(IceAgeSurvival.id(path));
    }

    /** Um menu de bancada aberto por um jogador de sobrevivência, ao lado de uma bancada de verdade. */
    private static BenchMenu open(GameTestHelper helper, BenchKind kind, Player player) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, kind.block());
        return new BenchMenu(kind, 1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos)));
    }

    private static int indexOf(GameTestHelper helper, BenchMenu menu, String path) {
        ResourceLocation id = IceAgeSurvival.id(path);
        for (int i = 0; i < menu.recipes().size(); i++) {
            if (menu.recipes().get(i).getId().equals(id)) {
                return i;
            }
        }
        throw new net.minecraft.gametest.framework.GameTestAssertException("a bancada não lista " + path);
    }

    /** Cada defesa tem receita na Construção, cada arma na Armeiro, e as duas bancadas saem da mesa de trabalho. */
    @GameTest(template = EMPTY, batch = "benches")
    public static void benchRecipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var access = helper.getLevel().registryAccess();
        for (String defense : DEFENSES) {
            var recipe = recipes.byKey(IceAgeSurvival.id("bench/construction/" + defense));
            helper.assertTrue(recipe.isPresent(), "sem receita de bancada: " + defense);
            helper.assertTrue(recipe.get() instanceof BenchRecipe bench && bench.bench() == BenchKind.CONSTRUCTION
                    && bench.getResultItem(access).is(item(defense)), "receita errada: " + defense);
        }
        for (String weapon : WEAPONS) {
            var recipe = recipes.byKey(IceAgeSurvival.id("bench/armory/" + weapon));
            helper.assertTrue(recipe.isPresent(), "sem receita de bancada: " + weapon);
            helper.assertTrue(recipe.get() instanceof BenchRecipe bench && bench.bench() == BenchKind.ARMORY
                    && bench.getResultItem(access).is(item(weapon)), "receita errada: " + weapon);
        }
        for (Item bench : List.of(Benches.CONSTRUCTION_BENCH_ITEM.get(), Benches.ARMORY_BENCH_ITEM.get())) {
            helper.assertTrue(recipes.getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .anyMatch(recipe -> recipe.getResultItem(access).is(bench)), "bancada sem receita: " + bench);
        }
        helper.succeed();
    }

    /** Rifle, besta e dardo saíram da mesa de trabalho. */
    @GameTest(template = EMPTY, batch = "benches")
    public static void weaponsLeftTheCraftingTable(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var access = helper.getLevel().registryAccess();
        Set<Item> weapons = Set.of(ModItems.TRANQ_RIFLE.get(), ModItems.TRANQ_CROSSBOW.get(), ModItems.TRANQ_DART.get());
        recipes.getAllRecipesFor(RecipeType.CRAFTING).forEach(recipe -> helper.assertFalse(
                weapons.contains(recipe.getResultItem(access).getItem()), "ainda na mesa de trabalho: " + recipe.getId()));
        for (String weapon : WEAPONS) {
            helper.assertFalse(recipes.byKey(IceAgeSurvival.id(weapon)).isPresent(), "receita antiga ficou: " + weapon);
        }
        helper.succeed();
    }

    /** Clicar fabrica: tira os ingredientes do inventário e entrega o resultado. */
    @GameTest(template = EMPTY, batch = "benches")
    public static void craftingConsumesIngredientsAndDeliversTheResult(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().add(new ItemStack(Items.IRON_NUGGET));
        player.getInventory().add(new ItemStack(ModItems.NARCOTIC.get(), 2));
        BenchMenu menu = open(helper, BenchKind.ARMORY, player);
        int dart = indexOf(helper, menu, "bench/armory/tranq_dart");
        helper.assertTrue(menu.clickMenuButton(player, BenchMenu.buttonId(dart, false)), "deveria fabricar");
        var inventory = player.getInventory();
        helper.assertTrue(inventory.countItem(ModItems.TRANQ_DART.get()) == 4,
                "4 dardos: " + inventory.countItem(ModItems.TRANQ_DART.get()));
        helper.assertTrue(inventory.countItem(Items.IRON_NUGGET) == 0, "a pepita deveria sair");
        helper.assertTrue(inventory.countItem(ModItems.NARCOTIC.get()) == 1, "só um narcótico deveria sair");
        helper.succeed();
    }

    /** Shift + clique repete enquanto houver ingrediente. */
    @GameTest(template = EMPTY, batch = "benches")
    public static void shiftCraftsWhileThereAreIngredients(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().add(new ItemStack(Items.IRON_NUGGET, 3));
        player.getInventory().add(new ItemStack(ModItems.NARCOTIC.get(), 5));
        BenchMenu menu = open(helper, BenchKind.ARMORY, player);
        int dart = indexOf(helper, menu, "bench/armory/tranq_dart");
        helper.assertTrue(menu.clickMenuButton(player, BenchMenu.buttonId(dart, true)), "deveria fabricar");
        var inventory = player.getInventory();
        helper.assertTrue(inventory.countItem(ModItems.TRANQ_DART.get()) == 12,
                "12 dardos: " + inventory.countItem(ModItems.TRANQ_DART.get()));
        helper.assertTrue(inventory.countItem(Items.IRON_NUGGET) == 0 && inventory.countItem(ModItems.NARCOTIC.get()) == 2,
                "três pagamentos");
        helper.succeed();
    }

    /** Sem os ingredientes todos, nada é fabricado nem consumido. */
    @GameTest(template = EMPTY, batch = "benches")
    public static void withoutIngredientsNothingIsCrafted(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        BenchMenu menu = open(helper, BenchKind.ARMORY, player);
        int dart = indexOf(helper, menu, "bench/armory/tranq_dart");
        helper.assertFalse(menu.clickMenuButton(player, BenchMenu.buttonId(dart, false)), "inventário vazio fabricou");
        player.getInventory().add(new ItemStack(Items.IRON_NUGGET));
        helper.assertFalse(menu.clickMenuButton(player, BenchMenu.buttonId(dart, true)), "fabricou sem narcótico");
        var inventory = player.getInventory();
        helper.assertTrue(inventory.countItem(ModItems.TRANQ_DART.get()) == 0, "não deveria haver dardo");
        helper.assertTrue(inventory.countItem(Items.IRON_NUGGET) == 1, "a pepita não deveria ser gasta");
        helper.assertFalse(menu.clickMenuButton(player, BenchMenu.buttonId(menu.recipes().size(), false)),
                "botão fora da lista");
        helper.succeed();
    }

    /** Cada bancada lista só as receitas dela. */
    @GameTest(template = EMPTY, batch = "benches")
    public static void eachBenchListsOnlyItsOwnRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var access = helper.getLevel().registryAccess();
        List<BenchRecipe> construction = BenchMenu.recipesFor(recipes, BenchKind.CONSTRUCTION);
        List<BenchRecipe> armory = BenchMenu.recipesFor(recipes, BenchKind.ARMORY);
        helper.assertTrue(construction.stream().allMatch(recipe -> recipe.bench() == BenchKind.CONSTRUCTION),
                "receita alheia na Construção");
        helper.assertTrue(armory.stream().allMatch(recipe -> recipe.bench() == BenchKind.ARMORY),
                "receita alheia na Armeiro");
        for (String defense : DEFENSES) {
            helper.assertTrue(construction.stream().anyMatch(recipe -> recipe.getResultItem(access).is(item(defense))),
                    "a Construção não faz " + defense);
            helper.assertFalse(armory.stream().anyMatch(recipe -> recipe.getResultItem(access).is(item(defense))),
                    "a Armeiro faz " + defense);
        }
        for (String weapon : WEAPONS) {
            helper.assertTrue(armory.stream().anyMatch(recipe -> recipe.getResultItem(access).is(item(weapon))),
                    "a Armeiro não faz " + weapon);
            helper.assertFalse(construction.stream().anyMatch(recipe -> recipe.getResultItem(access).is(item(weapon))),
                    "a Construção faz " + weapon);
        }
        Player player = helper.makeMockSurvivalPlayer();
        helper.assertTrue(open(helper, BenchKind.ARMORY, player).recipes().equals(armory), "o menu mostra outra lista");
        helper.succeed();
    }
}
