package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class TamingTests {
    private static final String EMPTY = "empty";

    private static TestCreature spawn(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
    }

    /** Criatura derrubada pelo jogador, que passa a ser quem a domestica. */
    private static TestCreature knockedOutBy(GameTestHelper helper, Player tamer) {
        TestCreature creature = spawn(helper);
        creature.addTorpor(creature.maxTorpor(), tamer);
        return creature;
    }

    // ---- Torpor ----

    @GameTest(template = EMPTY)
    public static void torporBelowMaxDoesNotKnockOut(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.addTorpor(creature.maxTorpor() / 2);
        helper.assertTrue(!creature.isUnconscious(), "caiu com metade do torpor");
        helper.assertTrue(Math.abs(creature.torporFraction() - 0.5F) < 0.01F, "fração de torpor: " + creature.torporFraction());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fullTorporKnocksOut(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.addTorpor(creature.maxTorpor());
        helper.assertTrue(creature.isUnconscious(), "não caiu com torpor máximo");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void unconsciousCreatureCannotBeMoved(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.setDeltaMovement(0.4, 0.0, 0.4);
        creature.addTorpor(creature.maxTorpor());

        helper.assertTrue(creature.getDeltaMovement().horizontalDistanceSqr() == 0, "manteve o impulso ao cair");
        helper.assertTrue(!creature.isPushable(), "continua empurrável");
        creature.knockback(1.0, 1.0, 0.0);
        helper.assertTrue(creature.getDeltaMovement().horizontalDistanceSqr() == 0, "sofreu recuo inconsciente");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void torporDecaysUntilCreatureWakes(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.addTorpor(creature.maxTorpor());
        creature.setTorpor(1.5);
        helper.assertTrue(creature.isUnconscious(), "acordou antes do torpor zerar");
        helper.succeedWhen(() -> helper.assertTrue(!creature.isUnconscious() && creature.torpor() == 0, "ainda inconsciente"));
    }

    @GameTest(template = EMPTY)
    public static void tamedCreatureIgnoresTorpor(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.tame(helper.makeMockSurvivalPlayer());
        creature.addTorpor(creature.maxTorpor());
        helper.assertTrue(!creature.isUnconscious() && creature.torpor() == 0, "criatura domesticada acumulou torpor");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tranqArrowAppliesTorpor(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        // A área de teste tem teto de barreira logo acima; a flecha precisa nascer abaixo dele.
        Vec3 above = creature.position().add(0, 1.5, 0);
        TranqArrow arrow = new TranqArrow(helper.getLevel(), above.x, above.y, above.z,
                new ItemStack(ModItems.TRANQ_ARROW.get()), null);
        arrow.shoot(0, -1, 0, 1.5F, 0);
        helper.getLevel().addFreshEntity(arrow);
        helper.succeedWhen(() -> helper.assertTrue(creature.torpor() > 0, "flecha não aplicou torpor"));
    }

    @GameTest(template = EMPTY)
    public static void powerEnchantmentIncreasesArrowTorpor(GameTestHelper helper) {
        TestCreature plain = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 0, 2, 1);
        TestCreature boosted = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 2, 2, 1);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.POWER_ARROWS, 5);
        for (var entry : new Object[][] {{plain, null}, {boosted, bow}}) {
            TestCreature target = (TestCreature) entry[0];
            Vec3 above = target.position().add(0, 1.5, 0);
            TranqArrow arrow = new TranqArrow(helper.getLevel(), above.x, above.y, above.z,
                    new ItemStack(ModItems.TRANQ_ARROW.get()), (ItemStack) entry[1]);
            arrow.shoot(0, -1, 0, 1.5F, 0);
            helper.getLevel().addFreshEntity(arrow);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(plain.torpor() > 0, "flecha comum não aplicou torpor");
            helper.assertTrue(boosted.torpor() > 0, "flecha com Força não aplicou torpor");
            helper.assertTrue(boosted.torpor() > plain.torpor() * 2.0,
                    "Força V não aumentou o torpor: " + boosted.torpor() + " vs " + plain.torpor());
        });
    }

    // ---- Inventário e domesticação ----

    @GameTest(template = EMPTY)
    public static void wildInventoryOpensOnlyWhileUnconsciousAndOnlyForTheTamer(GameTestHelper helper) {
        Player tamer = helper.makeMockSurvivalPlayer();
        Player other = helper.makeMockSurvivalPlayer();
        TestCreature creature = spawn(helper);
        helper.assertTrue(!creature.canAccessInventory(tamer), "inventário de criatura acordada acessível");

        creature.addTorpor(creature.maxTorpor(), tamer);
        helper.assertTrue(creature.canAccessInventory(tamer), "quem derrubou não acessa o inventário");
        helper.assertTrue(!creature.canAccessInventory(other), "outro jogador acessa o inventário");

        creature.setTorpor(0);
        helper.assertTrue(creature.tamerUUID() == null, "acordar não liberou a criatura");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tamedInventoryIsOwnerOnly(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        TestCreature creature = spawn(helper);
        creature.tame(owner);
        helper.assertTrue(creature.canAccessInventory(owner), "dono sem acesso");
        helper.assertTrue(!creature.canAccessInventory(helper.makeMockSurvivalPlayer()), "estranho com acesso");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void awakeCreatureDoesNotEatFromInventory(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.inventory().addItem(new ItemStack(Items.CARROT, 8));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(creature.inventory().countItem(Items.CARROT) == 8, "criatura acordada comeu");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void eatsOneUnitPerInterval(GameTestHelper helper) {
        TestCreature creature = knockedOutBy(helper, helper.makeMockSurvivalPlayer());
        creature.inventory().addItem(new ItemStack(Items.BEETROOT, 8));
        // Intervalo da criatura de teste: 5 s. Em 3 s cabe exatamente uma refeição.
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(creature.inventory().countItem(Items.BEETROOT) == 7,
                    "sobraram " + creature.inventory().countItem(Items.BEETROOT));
            helper.assertTrue(creature.tamingProgress() > 0, "sem progresso após comer");
            helper.succeed();
        });
    }

    /** Sem comida no inventário, a criatura avisa (o painel mostra o alerta); com comida, o aviso some. */
    @GameTest(template = EMPTY)
    public static void warnsWhenTamingRunsOutOfFood(GameTestHelper helper) {
        TestCreature creature = knockedOutBy(helper, helper.makeMockSurvivalPlayer());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(creature.needsTamingFood(), "deveria pedir comida com o inventário vazio");
            creature.inventory().addItem(new ItemStack(Items.CARROT, 8));
            creature.inventory().addItem(new ItemStack(Items.STONE, 1));
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!creature.needsTamingFood(), "com cenoura no inventário não deveria pedir comida");
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY)
    public static void notWarnedWithoutATamer(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.addTorpor(creature.maxTorpor());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!creature.needsTamingFood(), "sem quem derrubou, não há domesticação para continuar");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void eatsTheBestFoodFirst(GameTestHelper helper) {
        TestCreature creature = knockedOutBy(helper, helper.makeMockSurvivalPlayer());
        creature.inventory().addItem(new ItemStack(Items.BEETROOT, 8));
        creature.inventory().addItem(new ItemStack(Items.CARROT, 8));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(creature.inventory().countItem(Items.CARROT) == 7, "não comeu o alimento preferido");
            helper.assertTrue(creature.inventory().countItem(Items.BEETROOT) == 8, "comeu o alimento pior primeiro");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void ignoresItemsThatAreNotFood(GameTestHelper helper) {
        TestCreature creature = knockedOutBy(helper, helper.makeMockSurvivalPlayer());
        creature.inventory().addItem(new ItemStack(Items.STONE, 8));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(creature.inventory().countItem(Items.STONE) == 8, "comeu pedra");
            helper.assertTrue(creature.tamingProgress() == 0, "progresso sem alimento");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 1600)
    public static void eatingPreferredFoodTamesForTheTamerWithBonusLevels(GameTestHelper helper) {
        Player tamer = helper.makeMockSurvivalPlayer();
        TestCreature creature = knockedOutBy(helper, tamer);
        int wildLevel = creature.creatureLevel();
        creature.inventory().addItem(new ItemStack(Items.CARROT, 64));

        helper.succeedWhen(() -> {
            if (!creature.isTame()) {
                // Mantém a criatura derrubada enquanto come, como o jogador faria.
                creature.setTorpor(creature.maxTorpor());
            }
            helper.assertTrue(creature.isTame(), "ainda não domesticada");
            helper.assertTrue(tamer.getUUID().equals(creature.getOwnerUUID()), "dono errado");
            helper.assertTrue(!creature.isUnconscious() && creature.torpor() == 0, "continua inconsciente");
            helper.assertTrue(creature.creatureLevel() == wildLevel + wildLevel / 2,
                    "nível " + creature.creatureLevel() + " a partir de " + wildLevel);
            helper.assertTrue(creature.affinity() == 50.0F, "afinidade " + creature.affinity());
            helper.assertTrue(creature.inventory().countItem(Items.CARROT) > 0, "sobra de comida sumiu");
        });
    }

    @GameTest(template = EMPTY)
    public static void tamingStateAndInventorySurviveSaveAndLoad(GameTestHelper helper) {
        Player tamer = helper.makeMockSurvivalPlayer();
        TestCreature original = knockedOutBy(helper, tamer);
        original.inventory().addItem(new ItemStack(Items.BEETROOT, 8));

        helper.runAfterDelay(45, () -> {
            TestCreature loaded = ModEntities.TEST_CREATURE.get().create(helper.getLevel());
            loaded.load(original.saveWithoutId(new CompoundTag()));

            helper.assertTrue(loaded.isUnconscious(), "inconsciência não persistiu");
            helper.assertTrue(loaded.torpor() == original.torpor(), "torpor não persistiu");
            helper.assertTrue(loaded.tamingProgress() == original.tamingProgress() && loaded.tamingProgress() > 0,
                    "progresso não persistiu: " + loaded.tamingProgress());
            helper.assertTrue(loaded.inventory().countItem(Items.BEETROOT) == 7, "inventário não persistiu");
            helper.assertTrue(tamer.getUUID().equals(loaded.tamerUUID()), "quem derrubou não persistiu");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void inventoryDropsOnDeath(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.inventory().addItem(new ItemStack(Items.CARROT, 5));
        creature.kill();
        helper.assertItemEntityPresent(Items.CARROT, new BlockPos(1, 2, 1), 3.0);
        helper.succeed();
    }

    // ---- Corpo ----

    @GameTest(template = EMPTY)
    public static void largeSpeciesStepUpFullBlocks(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        helper.assertTrue(smilodon.maxUpStep() >= 1.0F, "Smilodon não sobe um bloco");
        TestCreature small = spawn(helper);
        helper.assertTrue(small.maxUpStep() == 0.6F, "criatura pequena com degrau alterado");
        helper.succeed();
    }

    /** O localizador guarda a criatura domesticada com o dono certo, segue a posição e esquece a morta. */
    @GameTest(template = EMPTY)
    public static void locatorListsOwnedCreatures(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        TestCreature creature = spawn(helper);
        creature.tame(owner);
        dev.madebyfelipe.iceagesurvival.world.CreatureLocator.update(creature);
        var server = helper.getLevel().getServer();
        var mine = dev.madebyfelipe.iceagesurvival.world.CreatureLocator.ownedBy(server, owner.getUUID());
        helper.assertTrue(mine.stream().anyMatch(entry -> entry.creature().equals(creature.getUUID())
                        && entry.pos().equals(creature.blockPosition())), "a criatura domesticada deveria estar na lista do dono");
        helper.assertTrue(dev.madebyfelipe.iceagesurvival.world.CreatureLocator.ownedBy(server, java.util.UUID.randomUUID())
                .stream().noneMatch(entry -> entry.creature().equals(creature.getUUID())), "nem na de outro jogador");
        creature.kill();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(dev.madebyfelipe.iceagesurvival.world.CreatureLocator.ownedBy(server, owner.getUUID())
                    .stream().noneMatch(entry -> entry.creature().equals(creature.getUUID())), "morta, sai da lista");
            helper.succeed();
        });
    }

    /** Ferida, a domesticada aceita a comida da espécie da mão do dono e se cura com ela, sem esperar a fome. */
    @GameTest(template = EMPTY)
    public static void ownerHealsTamedCreatureWithItsFood(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        TestCreature creature = spawn(helper);
        creature.tame(owner);
        creature.setHealth(creature.getMaxHealth() * 0.2F);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 4));
        float before = creature.getHealth();
        creature.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        float afterFirst = creature.getHealth();
        helper.assertTrue(afterFirst > before, "a cenoura não curou");
        helper.assertTrue(owner.getMainHandItem().getCount() == 3, "a porção não foi gasta");
        helper.runAfterDelay(15, () -> {
            // Ferida, não espera o intervalo da fome (30 s): come de novo meio segundo depois.
            creature.mobInteract(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(creature.getHealth() > afterFirst, "ferida, deveria aceitar outra porção logo");
            helper.succeed();
        });
    }

    /** Domesticada e ferida, come sozinha a comida que estiver no inventário dela. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void tamedCreatureEatsFromInventoryToHeal(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.tame(helper.makeMockPlayer());
        creature.setHealth(creature.getMaxHealth() * 0.2F);
        creature.inventory().addItem(new ItemStack(Items.CARROT, 8));
        float before = creature.getHealth();
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(creature.getHealth() > before, "não se curou com a comida do inventário");
            helper.assertTrue(creature.inventory().countItem(Items.CARROT) < 8, "não comeu do inventário");
            helper.succeed();
        });
    }
}
