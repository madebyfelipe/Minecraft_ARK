package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Carcaça (D45): a presa grande morta selvagem sem jogador fica caída com porções de carne; quem tem fome vem comer;
 * o jogador carneia a sobra com machado ou espada.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CarcassTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final long STARVING = 20L * 60 * 60;

    /** Galimimo morto por um Alossauro: vira carcaça com 6 porções, caída e intocável. */
    @GameTest(template = EMPTY, batch = "carcass_left")
    public static void aBigPreyKilledByAPredatorLeavesACarcass(GameTestHelper helper) {
        LandCreature prey = helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 2, 0, 2);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 2, 0, 5);
        prey.hurt(helper.getLevel().damageSources().mobAttack(hunter), 10_000.0F);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(prey.isAlive(), "o Galimimo sumiu em vez de virar carcaça");
            helper.assertTrue(prey.isCarcass(), "deveria ser carcaça");
            helper.assertTrue(prey.carcassPortionsLeft() == 6, "porções: " + prey.carcassPortionsLeft());
            prey.hurt(helper.getLevel().damageSources().mobAttack(hunter), 10_000.0F);
            helper.assertTrue(prey.isAlive() && prey.isCarcass(), "a carcaça não se fere");
            helper.succeed();
        });
    }

    /** Morto pelo jogador: o loot é dele, sem carcaça. Presa pequena (dodô) também não deixa carcaça. */
    @GameTest(template = EMPTY, batch = "carcass_player")
    public static void killedByThePlayerOrSmallLeavesNoCarcass(GameTestHelper helper) {
        LandCreature prey = helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 2, 0, 2);
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 5, 0, 2);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 2, 0, 5);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        prey.hurt(helper.getLevel().damageSources().playerAttack(player), 10_000.0F);
        dodo.hurt(helper.getLevel().damageSources().mobAttack(hunter), 10_000.0F);
        helper.runAtTickTime(5, () -> {
            helper.assertFalse(prey.isCarcass(), "morto pelo jogador virou carcaça");
            helper.assertFalse(dodo.isCarcass(), "o dodô virou carcaça");
            helper.succeed();
        });
    }

    /** O Alossauro faminto fareja a carcaça, vai até ela e come: a carne diminui e a fome passa. */
    @GameTest(template = ARENA, batch = "carcass_eat", timeoutTicks = 600)
    public static void aHungryPredatorEatsFromTheCarcass(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature prey = helper.spawnWithNoFreeWill(ModEntities.TRICERATOPS.get(), 6, 0, 6);
        LandCreature killer = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 0, 1);
        prey.hurt(helper.getLevel().damageSources().mobAttack(killer), 10_000.0F);
        killer.discard();
        LandCreature hunter = helper.spawn(ModEntities.ALLOSAURUS.get(), 20, 0, 20);
        hunter.setTicksSinceMeal(STARVING);
        hunter.setGroupId(UUID.randomUUID());
        helper.succeedWhen(() -> {
            helper.assertTrue(prey.isCarcass(), "o Tricerátopo não virou carcaça");
            helper.assertTrue(prey.carcassPortionsLeft() < 30, "ninguém comeu: alvo " + hunter.getTarget()
                    + ", comendo " + hunter.feedingOn());
            helper.assertTrue(hunter.hungerDrive() == Hunger.Drive.SATED, "comeu e continua com fome: "
                    + hunter.hungerDrive() + ", porções " + prey.carcassPortionsLeft());
        });
    }

    /**
     * A carne segue o porte da criatura (tools/gen_meat_loot.py): a vaca larga 1–3, o T-Rex morto pelo jogador bem
     * mais (o mínimo da regra é 14), e o Urso-terrível, que antes não largava nada, agora larga carne.
     */
    @GameTest(template = EMPTY, batch = "carcass_meat")
    public static void aBigKillDropsFarMoreMeatThanACow(GameTestHelper helper) {
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 4, 2, 4);
        rex.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000.0F);
        helper.runAfterDelay(3, () -> {
            int beef = meatAround(helper, rex, Items.BEEF);
            helper.assertTrue(beef >= 10, "o T-Rex deveria largar bem mais que a vaca (1–3): " + beef);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "carcass_meat")
    public static void aDirebearKillDropsMeat(GameTestHelper helper) {
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        LandCreature bear = helper.spawnWithNoFreeWill(ModEntities.DIREBEAR.get(), 4, 2, 4);
        bear.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000.0F);
        helper.runAfterDelay(3, () -> {
            int beef = meatAround(helper, bear, Items.BEEF);
            helper.assertTrue(beef >= 4, "o Urso-terrível deveria largar carne, mais que a vaca: " + beef);
            helper.succeed();
        });
    }

    private static int meatAround(GameTestHelper helper, LandCreature creature, net.minecraft.world.item.Item meat) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, creature.getBoundingBox().inflate(6.0)).stream()
                .filter(drop -> drop.getItem().is(meat)).mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    /** Com machado, o jogador carneia a sobra: sai loot e a carcaça acaba. */
    @GameTest(template = EMPTY, batch = "carcass_butcher")
    public static void theAxeButchersTheCarcass(GameTestHelper helper) {
        LandCreature prey = helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 2, 0, 2);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 2, 0, 6);
        prey.hurt(helper.getLevel().damageSources().mobAttack(hunter), 10_000.0F);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(prey.isCarcass(), "não virou carcaça");
            prey.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(prey.isAlive(), "a carcaça carneada continua lá");
            boolean dropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    prey.getBoundingBox().inflate(4.0)).isEmpty();
            helper.assertTrue(dropped, "carneou e não saiu nada");
            helper.succeed();
        });
    }
}
