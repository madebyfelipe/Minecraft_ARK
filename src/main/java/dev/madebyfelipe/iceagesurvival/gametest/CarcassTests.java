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
