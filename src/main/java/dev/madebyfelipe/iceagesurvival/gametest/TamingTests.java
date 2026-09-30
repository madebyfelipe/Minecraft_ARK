package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class TamingTests {
    private static final String EMPTY = "empty";

    private static TestCreature spawn(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
    }

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
        creature.tame(helper.makeMockPlayer(GameType.SURVIVAL));
        creature.addTorpor(creature.maxTorpor());
        helper.assertTrue(!creature.isUnconscious() && creature.torpor() == 0, "criatura domesticada acumulou torpor");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void awakeCreatureCannotBeFed(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 64));
        creature.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 64, "criatura acordada comeu");
        helper.assertTrue(creature.tamingProgress() == 0, "progresso sem estar inconsciente");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void feedingRespectsTheInterval(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        creature.addTorpor(creature.maxTorpor());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEETROOT, 64));
        creature.mobInteract(player, InteractionHand.MAIN_HAND);
        creature.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 63, "comeu duas vezes sem esperar");
        helper.assertTrue(creature.tamingProgress() > 0, "sem progresso após comer");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 1600)
    public static void feedingPreferredFoodTamesAndGrantsBonusLevels(GameTestHelper helper) {
        TestCreature creature = spawn(helper);
        int wildLevel = creature.creatureLevel();
        creature.addTorpor(creature.maxTorpor());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CARROT, 64));

        helper.succeedWhen(() -> {
            if (!creature.isTame()) {
                // Mantém a criatura derrubada enquanto come, como o jogador faria.
                creature.setTorpor(creature.maxTorpor());
                creature.mobInteract(player, InteractionHand.MAIN_HAND);
            }
            helper.assertTrue(creature.isTame(), "ainda não domesticada");
            helper.assertTrue(player.getUUID().equals(creature.getOwnerUUID()), "dono errado");
            helper.assertTrue(!creature.isUnconscious() && creature.torpor() == 0, "continua inconsciente");
            helper.assertTrue(creature.creatureLevel() == wildLevel + wildLevel / 2,
                    "nível " + creature.creatureLevel() + " a partir de " + wildLevel);
            helper.assertTrue(creature.affinity() == 50.0F, "afinidade " + creature.affinity());
        });
    }

    @GameTest(template = EMPTY)
    public static void unconsciousStateAndProgressSurviveSaveAndLoad(GameTestHelper helper) {
        TestCreature original = spawn(helper);
        original.addTorpor(original.maxTorpor());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEETROOT, 64));
        original.mobInteract(player, InteractionHand.MAIN_HAND);

        TestCreature loaded = ModEntities.TEST_CREATURE.get().create(helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));

        helper.assertTrue(loaded.isUnconscious(), "inconsciência não persistiu");
        helper.assertTrue(loaded.torpor() == original.torpor(), "torpor não persistiu");
        helper.assertTrue(loaded.tamingProgress() == original.tamingProgress() && loaded.tamingProgress() > 0,
                "progresso não persistiu: " + loaded.tamingProgress());
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
}
