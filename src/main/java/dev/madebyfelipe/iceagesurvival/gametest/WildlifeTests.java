package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Ecologia: como a fauna reage a jogadores, predadores e aos próprios filhotes. O Elasmotério é o
 * caso completo (rinoceronte: solitário, cauteloso, investe quando surpreendido ou com filhote).
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WildlifeTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    /** Um lote por cena: animais de testes vizinhos se enxergariam (os raios de alerta passam de 28 blocos). */
    private static final String BATCH = "wildlife";

    @GameTest(template = EMPTY)
    public static void elasmotheriumIsSolitaryWithRareFamilies(GameTestHelper helper) {
        Species elasmo = Species.of(helper.getLevel().registryAccess(), ModEntities.ELASMOTHERIUM.get()).orElseThrow();
        BehaviorProfile behavior = elasmo.behavior().orElseThrow();
        SpawnProfile spawn = elasmo.spawn().orElseThrow();
        helper.assertTrue(behavior.herdRadius() == 0 && !behavior.groupDefense(), "Elasmotério não anda em manada");
        helper.assertTrue(!behavior.aggressive(), "não caça o jogador: só reage");
        helper.assertTrue(spawn.groupMin() == 1 && spawn.groupMax() == 1, "nasce sozinho");
        var family = spawn.family().orElseThrow();
        helper.assertTrue(family.calfChance() > 0 && family.calfChance() <= 0.3, "mãe com filhote deveria ser rara");
        helper.assertTrue(family.mateChance() > 0 && family.mateChance() < 0.5, "casal com filhote, mais raro ainda");
        helper.assertTrue(behavior.wariness().orElseThrow().chargeRadius() > 0, "investe quando surpreendido");
        helper.succeed();
    }

    /** Chegar colado num Elasmotério sem ele ter visto: investida, com o golpe que joga para o alto. */
    @GameTest(template = ARENA, batch = BATCH + "_1", timeoutTicks = 200)
    public static void surprisedElasmotheriumCharges(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 4, 0, 4);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 0, 8.0)));
        float start = player.getHealth();
        helper.onEachTick(() -> {
            if (player.getHealth() < start) {
                helper.succeed();
            }
        });
    }

    /** Agachado e a uma distância educada, o jogador passa: o faro conta mais que a vista. */
    @GameTest(template = ARENA, batch = BATCH + "_2", timeoutTicks = 120)
    public static void sneakingPastAtADistanceDoesNotProvoke(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 2, 0, 1);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.setShiftKeyDown(true);
        // Alerta de 28 blocos; agachado, metade: a 21 blocos passa sem ser notado.
        player.moveTo(helper.absoluteVec(new Vec3(2.5, 0, 22.5)));
        float start = player.getHealth();
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(player.getHealth() == start, "o jogador agachado foi atacado");
            helper.assertFalse(elasmo.isAggressive(), "o Elasmotério investiu contra quem passou agachado longe");
            helper.succeed();
        });
    }

    /** Ferir o filhote põe a mãe atrás de quem feriu. */
    @GameTest(template = EMPTY)
    public static void hurtingACalfBringsTheMother(GameTestHelper helper) {
        LandCreature mother = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 2, 2, 2);
        LandCreature calf = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 6, 2, 2);
        calf.setAge(-24000);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(8.5, 2, 2.5)));
        calf.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(mother.getTarget() == player, "a mãe deveria ir atrás de quem feriu o filhote");
        helper.assertTrue(mother.hasCalfNearby(14), "a mãe deveria perceber o filhote por perto");
        helper.succeed();
    }

    /** Diante de algo muito maior, foge — mesmo um animal que investe contra o jogador. */
    @GameTest(template = ARENA, batch = BATCH + "_3", timeoutTicks = 160)
    public static void elasmotheriumFleesFromTyrannosaurus(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 8, 0, 4);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 8, 0, 12);
        double start = elasmo.distanceTo(rex);
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(elasmo.distanceTo(rex) > start + 3.0,
                    "o Elasmotério deveria ter fugido do T-Rex: " + start + " → " + elasmo.distanceTo(rex));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH + "_4", timeoutTicks = 160)
    public static void dodoRunsFromAWolf(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        var wolf = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 8, 0, 8);
        LandCreature dodo = helper.spawn(ModEntities.DODO.get(), 8, 0, 12);
        double start = dodo.distanceTo(wolf);
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(dodo.distanceTo(wolf) > start + 3.0,
                    "o dodô deveria ter fugido do lobo: " + start + " → " + dodo.distanceTo(wolf));
            helper.succeed();
        });
    }

    /** Um Brontossauro em investida vira uma ameaça visível; o Velociraptor foge pelo porte. */
    @GameTest(template = ARENA, batch = BATCH + "_5", timeoutTicks = 160)
    public static void velociraptorFleesFromThreateningBrontosaurus(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature bronto = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 8, 0, 4);
        bronto.setAggressive(true);
        LandCreature raptor = helper.spawn(ModEntities.VELOCIRAPTOR.get(), 8, 0, 10);
        Vec3 start = raptor.position();
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(raptor.position().distanceTo(start) > 3.0,
                    "o Velociraptor não fugiu do Brontossauro agressivo: " + start + " → " + raptor.position());
            helper.succeed();
        });
    }

    private static void largeHerbivoreDrivesOffSatedVelociraptor(GameTestHelper helper,
                                                                  EntityType<LandCreature> herbivoreType) {
        HuntTests.clearStrays(helper);
        LandCreature herbivore = helper.spawn(herbivoreType, 8, 0, 4);
        LandCreature raptor = helper.spawn(ModEntities.VELOCIRAPTOR.get(), 8, 0, 12);
        raptor.setTicksSinceMeal(0);
        double start = raptor.distanceTo(herbivore);
        helper.onEachTick(() -> {
            if (herbivore.isAggressive() && raptor.distanceTo(herbivore) > start + 3.0) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(140, () -> helper.fail(herbivoreType.getDescriptionId()
                + " não afastou o Velociraptor saciado; distância " + start + " → "
                + raptor.distanceTo(herbivore) + ", agressivo=" + herbivore.isAggressive()));
    }

    /** Mesmo saciado e sem caçar mamute, o pequeno predador é expulso pela manada. */
    @GameTest(template = ARENA, batch = BATCH + "_mammoth", timeoutTicks = 160)
    public static void mammothHerdDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.MAMMOTH.get());
    }

    @GameTest(template = ARENA, batch = BATCH + "_brontosaurus", timeoutTicks = 160)
    public static void brontosaurusDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.BRONTOSAURUS.get());
    }

    @GameTest(template = ARENA, batch = BATCH + "_stegosaurus", timeoutTicks = 160)
    public static void stegosaurusDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.STEGOSAURUS.get());
    }

    @GameTest(template = ARENA, batch = BATCH + "_elasmotherium", timeoutTicks = 160)
    public static void elasmotheriumDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.ELASMOTHERIUM.get());
    }

    /** Depois de abater a presa, o predador come e para de caçar por um tempo. */
    @GameTest(template = EMPTY)
    public static void predatorIsSatedAfterAKill(GameTestHelper helper) {
        LandCreature wolf = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 3, 2, 3);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 5, 2, 3);
        wolf.setTicksSinceMeal(20L * 3600);
        helper.assertFalse(wolf.isSated(), "começa com fome");
        wolf.killedEntity(helper.getLevel(), pig);
        helper.assertTrue(wolf.isSated(), "deveria estar saciado depois de abater a presa");
        helper.succeed();
    }
}
