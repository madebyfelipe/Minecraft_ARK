package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Troféu de apex: a cabeça, o tributo, o desafio e a vitória que derruba o apex para domar. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class ApexTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final long STARVING = 20L * 60 * 60;

    private static LandCreature rex(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 4, 0, 4);
    }

    /** Morto por um jogador, o T-Rex deixa a cabeça. */
    @GameTest(template = ARENA, batch = "apex_head")
    public static void aRexKilledByAPlayerDropsItsHead(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = rex(helper);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        rex.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000.0F);
        helper.succeedWhen(() -> helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                rex.getBoundingBox().inflate(6), item -> item.getItem().is(ModItems.TYRANNOSAURUS_HEAD.get())).isEmpty(),
                "sem cabeça de T-Rex no chão"));
    }

    /** O tranquilizante não derruba o apex. */
    @GameTest(template = ARENA, batch = "apex_tranq", timeoutTicks = 80)
    public static void tranquilizersDoNotWorkOnTheApex(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = rex(helper);
        Vec3 from = rex.position().add(4, 2, 0);
        TranqArrow dart = new TranqArrow(helper.getLevel(), from.x, from.y, from.z,
                new ItemStack(ModItems.TRANQ_DART.get()), null);
        dart.shoot(-1, 0, 0, 2.0F, 0);
        helper.getLevel().addFreshEntity(dart);
        helper.succeedWhen(() -> {
            helper.assertTrue(rex.getHealth() < rex.getMaxHealth(), "o dardo ainda não acertou");
            helper.assertTrue(rex.torpor() == 0, "o apex recebeu torpor: " + rex.torpor());
        });
    }

    /** Com a cabeça de outro T-Rex na mão, o jogador não é presa; sem ela, é. */
    @GameTest(template = EMPTY, batch = "apex_tribute")
    public static void theTrophyMakesTheRexRespectThePlayer(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 2, 2, 2);
        rex.setTicksSinceMeal(STARVING);
        Player player = helper.makeMockSurvivalPlayer();
        var prey = rex.behavior().orElseThrow().prey().orElseThrow();
        helper.assertTrue(HuntGoal.isPrey(rex, player, prey), "sem a cabeça, o jogador deveria ser presa");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TYRANNOSAURUS_HEAD.get()));
        helper.assertTrue(rex.respectsTribute(player), "o T-Rex não reconheceu o tributo");
        helper.assertFalse(HuntGoal.isPrey(rex, player, prey), "com a cabeça, o jogador não deveria ser presa");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPINOSAURUS_HEAD.get()));
        helper.assertFalse(rex.respectsTribute(player), "a cabeça de outra espécie não vale para o T-Rex");
        helper.succeed();
    }

    /** Desafiado com a cabeça e vencido, o T-Rex não morre: cai desmaiado no nome do desafiante. */
    @GameTest(template = EMPTY, batch = "apex_duel")
    public static void aDefeatedRexFallsForTheChallenger(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 2, 2, 2);
        Player player = helper.makeMockSurvivalPlayer();
        player.moveTo(rex.position().add(3, 0, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TYRANNOSAURUS_HEAD.get()));
        rex.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(rex.isDueling(), "o desafio não começou");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "a cabeça não foi consumida");
        rex.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000.0F);
        helper.assertTrue(rex.isAlive() && !rex.isDeadOrDying(), "vencido, o T-Rex não deveria morrer");
        helper.assertTrue(rex.isUnconscious() && !rex.isTame(), "deveria cair desmaiado, ainda selvagem");
        helper.assertTrue(rex.canAccessInventory(player), "deveria estar no nome do desafiante");
        helper.assertFalse(rex.isDueling(), "o desafio deveria ter acabado");
        helper.succeed();
    }

    /** Até três criaturas do desafiante lutam junto; a quarta cancela o ritual. */
    @GameTest(template = EMPTY, batch = "apex_duel_crowd")
    public static void aFourthCreatureCancelsTheRitual(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 2, 2, 2);
        Player player = helper.makeMockSurvivalPlayer();
        player.moveTo(rex.position().add(3, 0, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TYRANNOSAURUS_HEAD.get()));
        rex.mobInteract(player, InteractionHand.MAIN_HAND);
        for (int i = 0; i <= ApexDuel.MAX_CREATURES; i++) {
            LandCreature ally = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 6 + i, 2, 6);
            ally.tame(player);
            helper.assertTrue(rex.isDueling(), "cancelou antes da criatura " + (i + 1));
            rex.invulnerableTime = 0;
            rex.hurt(helper.getLevel().damageSources().mobAttack(ally), 1.0F);
        }
        helper.assertFalse(rex.isDueling(), "a quarta criatura deveria cancelar o ritual");
        helper.succeed();
    }
}
