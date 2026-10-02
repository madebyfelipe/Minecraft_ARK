package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.primal.DryingRackBlock;
import dev.madebyfelipe.iceagesurvival.primal.KilnBlock;
import dev.madebyfelipe.iceagesurvival.primal.PrimalStationBlockEntity;
import dev.madebyfelipe.iceagesurvival.primal.PrimalStations;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * As estações primitivas (porte do Primal Stage): grelha, forno de olaria, varal de secagem, tora de corte e bigorna de
 * pedra. Sem tela: o item entra com o clique direito na face de trabalho, fica à vista, e sai pronto — pelo tempo
 * (grelha, forno, varal) ou pelos golpes (tora, bigorna).
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class PrimalStationTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "primal_stations";
    /** Folga de ticks além do tempo de uma receita de grelha ou forno (30 s). */
    private static final int COOK_CHECK = 660;

    /** Um jogador de sobrevivência com {@code stack} na mão principal. */
    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** O clique direito do jogador na face {@code face} do bloco em {@code rel}, com a mão principal. */
    private static InteractionResult click(GameTestHelper helper, Player player, BlockPos rel, Direction face) {
        BlockPos abs = helper.absolutePos(rel);
        Vec3 at = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return helper.getLevel().getBlockState(abs).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(at, face, abs, false));
    }

    private static PrimalStationBlockEntity station(GameTestHelper helper, BlockPos rel) {
        var blockEntity = helper.getBlockEntity(rel);
        helper.assertTrue(blockEntity instanceof PrimalStationBlockEntity, "sem estação em " + rel + ": " + blockEntity);
        return (PrimalStationBlockEntity) blockEntity;
    }

    /** Quantos de {@code item} há na estação. */
    private static int countIn(PrimalStationBlockEntity station, Item item) {
        int total = 0;
        for (int slot = 0; slot < station.size(); slot++) {
            if (station.getItem(slot).is(item)) {
                total += station.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static boolean empty(PrimalStationBlockEntity station) {
        for (int slot = 0; slot < station.size(); slot++) {
            if (!station.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Recolhe os itens soltos perto da estação em {@code rel}: quantos de {@code item} saltaram dela. */
    private static int collectDropped(GameTestHelper helper, BlockPos rel, Item item) {
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(rel)).inflate(1.5), drop -> drop.getItem().is(item));
        int total = 0;
        for (ItemEntity drop : drops) {
            total += drop.getItem().getCount();
            drop.discard();
        }
        return total;
    }

    private static int countInInventory(Player player, Item item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                total += player.getInventory().getItem(slot).getCount();
            }
        }
        return total;
    }

    private static <T extends Recipe<net.minecraft.world.Container>> ItemStack resultOf(GameTestHelper helper,
            RecipeType<T> type, Item input) {
        ServerLevel level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(type, new SimpleContainer(new ItemStack(input)), level)
                .map(recipe -> recipe.getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    // ---- Dados ----

    /** Os blocos existem e as receitas de cada estação carregam dos dados. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void theStationsAndTheirRecipesLoad(GameTestHelper helper) {
        helper.assertTrue(PrimalStations.DRYING_RACKS.size() == PrimalStations.WOODS.size()
                && PrimalStations.DRYING_RACKS.containsKey("oak") && PrimalStations.DRYING_RACKS.containsKey("warped"),
                "um varal por madeira: " + PrimalStations.DRYING_RACKS.keySet());
        var manager = helper.getLevel().getRecipeManager();
        for (RecipeType<?> type : List.of(PrimalStations.GRILL_RECIPE.get(), PrimalStations.KILN_RECIPE.get(),
                PrimalStations.DRYING_RECIPE.get(), PrimalStations.CUTTING_RECIPE.get(),
                PrimalStations.FORGING_RECIPE.get())) {
            @SuppressWarnings("unchecked")
            var recipes = manager.getAllRecipesFor((RecipeType<Recipe<net.minecraft.world.Container>>) type);
            helper.assertFalse(recipes.isEmpty(), "nenhuma receita carregou para " + type);
        }
        helper.assertTrue(resultOf(helper, PrimalStations.GRILL_RECIPE.get(), Items.BEEF).is(Items.COOKED_BEEF),
                "grelha: carne crua deveria virar carne assada");
        helper.assertTrue(resultOf(helper, PrimalStations.KILN_RECIPE.get(), Items.RAW_IRON_BLOCK).is(Items.IRON_BLOCK),
                "forno: bloco de ferro bruto deveria virar bloco de ferro");
        helper.assertTrue(resultOf(helper, PrimalStations.KILN_RECIPE.get(), Items.SAND).is(Items.GLASS),
                "forno: areia deveria virar vidro");
        helper.assertTrue(resultOf(helper, PrimalStations.DRYING_RECIPE.get(), Items.BEEF).is(ModItems.JERKY.get()),
                "varal: carne crua deveria virar charque");
        helper.assertTrue(resultOf(helper, PrimalStations.DRYING_RECIPE.get(), ModItems.DODO_MEAT.get())
                .is(ModItems.JERKY.get()), "varal: carne de dodô deveria virar charque");
        ItemStack planks = resultOf(helper, PrimalStations.CUTTING_RECIPE.get(), Items.OAK_LOG);
        helper.assertTrue(planks.is(Items.OAK_PLANKS) && planks.getCount() > 4,
                "tora: a tora deveria render mais tábuas que a mesa de trabalho: " + planks);
        helper.assertTrue(resultOf(helper, PrimalStations.CUTTING_RECIPE.get(), Items.OAK_PLANKS).is(Items.STICK),
                "tora: tábua deveria virar gravetos");
        helper.assertTrue(resultOf(helper, PrimalStations.FORGING_RECIPE.get(), Items.GRAVEL).is(Items.FLINT),
                "bigorna: cascalho deveria virar sílex");
        helper.assertTrue(resultOf(helper, PrimalStations.GRILL_RECIPE.get(), Items.STICK).isEmpty(),
                "a grelha não deveria ter receita para graveto");
        // O forno de olaria é feito de tijolos de forno.
        var kiln = manager.byKey(IceAgeSurvival.id("kiln")).orElse(null);
        helper.assertTrue(kiln != null, "sem receita do forno de olaria");
        helper.assertTrue(kiln.getResultItem(helper.getLevel().registryAccess()).is(PrimalStations.KILN.get().asItem()),
                "a receita do forno não faz o forno");
        helper.assertTrue(kiln.getIngredients().stream()
                        .anyMatch(ingredient -> ingredient.test(new ItemStack(PrimalStations.KILN_BRICKS.get()))),
                "o forno deveria ser feito de tijolos de forno");
        helper.succeed();
    }

    // ---- Grelha ----

    /**
     * Carne crua na grelha sobre a fogueira acesa vira carne assada com o tempo; na grelha fria, fica crua. Graveto,
     * sem receita, é recusado.
     */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = COOK_CHECK + 40)
    public static void theGrillCooksOverAFire(GameTestHelper helper) {
        BlockPos hot = new BlockPos(0, 2, 0);
        BlockPos cold = new BlockPos(2, 1, 2);
        helper.setBlock(hot.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(hot, PrimalStations.PRIMITIVE_GRILL.get());
        helper.setBlock(cold.below(), Blocks.STONE);
        helper.setBlock(cold, PrimalStations.PRIMITIVE_GRILL.get());

        Player cook = holding(helper, new ItemStack(Items.STICK));
        helper.assertTrue(click(helper, cook, hot, Direction.UP) == InteractionResult.PASS, "aceitou um graveto");
        helper.assertTrue(empty(station(helper, hot)) && cook.getMainHandItem().getCount() == 1,
                "o graveto foi para a grelha");

        cook.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 3));
        helper.assertTrue(click(helper, cook, hot, Direction.UP).consumesAction(), "recusou carne crua");
        helper.assertTrue(click(helper, cook, cold, Direction.UP).consumesAction(), "a grelha fria recusou carne crua");
        helper.assertTrue(cook.getMainHandItem().getCount() == 1, "cada clique deveria pôr uma carne só");
        helper.assertTrue(countIn(station(helper, hot), Items.BEEF) == 1, "a carne não ficou na grelha");

        helper.runAtTickTime(200, () -> helper.assertTrue(countIn(station(helper, hot), Items.BEEF) == 1,
                "assou cedo demais"));
        helper.runAtTickTime(COOK_CHECK, () -> {
            helper.assertTrue(countIn(station(helper, hot), Items.COOKED_BEEF) == 1, "a carne não assou no fogo");
            helper.assertTrue(countIn(station(helper, cold), Items.BEEF) == 1, "a grelha fria assou a carne");
            Player taker = holding(helper, ItemStack.EMPTY);
            helper.assertTrue(click(helper, taker, hot, Direction.UP).consumesAction(), "não deu para tirar a carne");
            helper.assertTrue(countInInventory(taker, Items.COOKED_BEEF) == 1, "a carne assada não veio para a mão");
            helper.assertTrue(empty(station(helper, hot)), "a grelha continuou com a carne");
            helper.succeed();
        });
    }

    // ---- Forno de olaria ----

    /** O forno carrega pela boca (a face da frente), não por cima; com fogo embaixo, a areia vira vidro. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = COOK_CHECK + 40)
    public static void theKilnFiresSandIntoGlass(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(pos, PrimalStations.KILN.get().defaultBlockState().setValue(KilnBlock.FACING, Direction.NORTH));
        Player potter = holding(helper, new ItemStack(Items.SAND, 2));
        helper.assertFalse(click(helper, potter, pos, Direction.UP).consumesAction(), "carregou por cima");
        helper.assertTrue(empty(station(helper, pos)), "a areia entrou por cima");
        potter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertFalse(click(helper, potter, pos, Direction.NORTH).consumesAction(), "aceitou um graveto");
        potter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SAND, 2));
        helper.assertTrue(click(helper, potter, pos, Direction.NORTH).consumesAction(), "recusou areia pela boca");
        helper.assertTrue(countIn(station(helper, pos), Items.SAND) == 1, "a areia não ficou no forno");
        helper.assertFalse(click(helper, potter, pos, Direction.NORTH).consumesAction(),
                "o forno cheio aceitou mais areia");
        helper.runAtTickTime(COOK_CHECK, () -> {
            helper.assertTrue(countIn(station(helper, pos), Items.GLASS) == 1, "a areia não virou vidro");
            Player taker = holding(helper, ItemStack.EMPTY);
            helper.assertTrue(click(helper, taker, pos, Direction.NORTH).consumesAction(), "não deu para tirar o vidro");
            helper.assertTrue(countInInventory(taker, Items.GLASS) == 1, "o vidro não veio para a mão");
            helper.succeed();
        });
    }

    // ---- Varal de secagem ----

    /**
     * De dia a carne crua seca no varal e vira charque; à noite não seca. O tempo do varal é passado tick a tick
     * dentro do teste (a hora é do mundo inteiro e volta ao que era no fim).
     */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void theDryingRackMakesJerkyByDay(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, PrimalStations.DRYING_RACKS.get("spruce").get());
        ServerLevel level = helper.getLevel();
        var state = level.getBlockState(helper.absolutePos(pos));
        helper.assertTrue(state.getTicker(level, PrimalStations.DRYING_RACK_ENTITY.get()) != null,
                "o varal não trabalha com o tempo");
        Player hunter = holding(helper, new ItemStack(Items.WHEAT));
        helper.assertFalse(click(helper, hunter, pos, Direction.UP).consumesAction(), "aceitou trigo");
        hunter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 2));
        helper.assertTrue(click(helper, hunter, pos, Direction.UP).consumesAction(), "recusou carne crua");
        PrimalStationBlockEntity rack = station(helper, pos);

        long savedTime = level.getDayTime();
        try {
            level.setDayTime(1_000L);
            helper.assertTrue(DryingRackBlock.isDaylight(level), "1000 deveria ser dia");
            int ticks = 0;
            while (countIn(rack, ModItems.JERKY.get()) == 0 && ticks < 24_000) {
                rack.serverTick();
                ticks++;
            }
            helper.assertTrue(countIn(rack, ModItems.JERKY.get()) == 1, "um dia inteiro e a carne não secou");
            helper.assertTrue(ticks > 20, "a carne secou na hora: " + ticks + " ticks");

            level.setDayTime(18_000L);
            helper.assertFalse(DryingRackBlock.isDaylight(level), "meia-noite deveria ser noite");
            helper.assertTrue(click(helper, hunter, pos, Direction.UP).consumesAction(), "recusou a segunda carne");
            for (int i = 0; i < ticks + 200; i++) {
                rack.serverTick();
            }
            helper.assertTrue(countIn(rack, Items.BEEF) == 1, "a carne secou à noite");
            helper.assertTrue(countIn(rack, ModItems.JERKY.get()) == 1, "o charque sumiu");
        } finally {
            level.setDayTime(savedTime);
        }
        helper.succeed();
    }

    // ---- Tora de corte ----

    /**
     * A tora vai para a tora de corte e os golpes do machado a viram tábuas, que saltam dela; o machado se gasta.
     * Pedregulho, sem receita, é recusado; golpe com o que não é machado não conta.
     */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void theCuttingLogSplitsLogsWithAnAxe(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, PrimalStations.CUTTING_LOG.get());
        PrimalStationBlockEntity log = station(helper, pos);
        Player woodcutter = holding(helper, new ItemStack(Items.COBBLESTONE));
        helper.assertFalse(click(helper, woodcutter, pos, Direction.UP).consumesAction(), "aceitou pedregulho");
        woodcutter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_LOG));
        helper.assertTrue(click(helper, woodcutter, pos, Direction.UP).consumesAction(), "recusou a tora");
        helper.assertTrue(countIn(log, Items.OAK_LOG) == 1, "a tora não ficou em cima");

        // Um graveto na mão não corta.
        woodcutter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        click(helper, woodcutter, pos, Direction.UP);
        helper.assertTrue(countIn(log, Items.OAK_LOG) == 1 && collectDropped(helper, pos, Items.OAK_PLANKS) == 0,
                "um graveto cortou a tora");

        ItemStack axe = new ItemStack(Items.STONE_AXE);
        woodcutter.setItemInHand(InteractionHand.MAIN_HAND, axe);
        int hits = 0;
        while (!empty(log) && hits < 20) {
            helper.assertTrue(click(helper, woodcutter, pos, Direction.UP).consumesAction(), "o golpe não contou");
            hits++;
        }
        helper.assertTrue(empty(log), "20 golpes e a tora continua lá");
        int planks = collectDropped(helper, pos, Items.OAK_PLANKS);
        helper.assertTrue(planks > 4, "deveria render mais tábuas que a mesa de trabalho: " + planks);
        helper.assertTrue(axe.getDamageValue() == hits, "o machado deveria gastar 1 por golpe: " + axe.getDamageValue()
                + " de " + hits + " golpes");

        // Tábua vira gravetos.
        woodcutter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(click(helper, woodcutter, pos, Direction.UP).consumesAction(), "recusou a tábua");
        woodcutter.setItemInHand(InteractionHand.MAIN_HAND, axe);
        for (int i = 0; i < 20 && !empty(log); i++) {
            click(helper, woodcutter, pos, Direction.UP);
        }
        helper.assertTrue(collectDropped(helper, pos, Items.STICK) > 2,
                "uma tábua deveria render mais gravetos que na mesa de trabalho");
        helper.succeed();
    }

    /** De mão vazia, a tora de corte devolve o que está em cima. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void theCuttingLogGivesTheItemBack(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, PrimalStations.CUTTING_LOG.get());
        Player woodcutter = holding(helper, new ItemStack(Items.BIRCH_LOG));
        helper.assertTrue(click(helper, woodcutter, pos, Direction.UP).consumesAction(), "recusou a tora de bétula");
        helper.assertTrue(woodcutter.getMainHandItem().isEmpty(), "a tora não saiu da mão");
        helper.assertTrue(click(helper, woodcutter, pos, Direction.UP).consumesAction(), "não devolveu a tora");
        helper.assertTrue(countInInventory(woodcutter, Items.BIRCH_LOG) == 1, "a tora não voltou");
        helper.assertTrue(empty(station(helper, pos)), "a tora continuou em cima");
        helper.succeed();
    }

    // ---- Bigorna de pedra ----

    /** Cascalho na bigorna, marteladas de picareta, e sai sílex. Comida é recusada; o machado não martela. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void theStoneAnvilKnapsGravelIntoFlint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, PrimalStations.STONE_ANVIL.get());
        PrimalStationBlockEntity anvil = station(helper, pos);
        Player smith = holding(helper, new ItemStack(Items.BEEF));
        helper.assertFalse(click(helper, smith, pos, Direction.UP).consumesAction(), "aceitou carne crua");
        smith.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GRAVEL));
        helper.assertTrue(click(helper, smith, pos, Direction.UP).consumesAction(), "recusou o cascalho");
        helper.assertTrue(countIn(anvil, Items.GRAVEL) == 1, "o cascalho não ficou na bigorna");

        smith.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_AXE));
        click(helper, smith, pos, Direction.UP);
        helper.assertTrue(countIn(anvil, Items.GRAVEL) == 1 && collectDropped(helper, pos, Items.FLINT) == 0,
                "o machado martelou");

        ItemStack pickaxe = new ItemStack(Items.WOODEN_PICKAXE);
        smith.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        int hits = 0;
        while (!empty(anvil) && hits < 20) {
            helper.assertTrue(click(helper, smith, pos, Direction.UP).consumesAction(), "a martelada não contou");
            hits++;
            if (!empty(anvil)) {
                helper.assertTrue(collectDropped(helper, pos, Items.FLINT) == 0, "saiu sílex antes do último golpe");
            }
        }
        helper.assertTrue(empty(anvil), "20 marteladas e o cascalho continua lá");
        helper.assertTrue(hits > 1, "o sílex deveria pedir mais de um golpe");
        helper.assertTrue(collectDropped(helper, pos, Items.FLINT) >= 1, "não saltou sílex da bigorna");
        helper.assertTrue(pickaxe.getDamageValue() == hits, "a picareta deveria gastar 1 por golpe");
        helper.succeed();
    }
}
