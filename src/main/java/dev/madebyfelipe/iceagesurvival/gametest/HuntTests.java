package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.RivalryGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Ecologia dinâmica num servidor de verdade: a fome leva às caçadas, a presa e a manada disparam,
 * o estresse sobe e desce, rivais disputam e quem perde vai embora.
 *
 * <p>Cada cena tem lote próprio: os raios de caça e de alerta passam do tamanho da arena, e os
 * bichos de uma cena veriam os da vizinha.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class HuntTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    /** Uma hora sem comer: com fome de verdade (20 ticks por segundo). */
    private static final long STARVING = 20L * 60 * 60;

    /** Raio da limpeza: o maior raio de caça, com folga. */
    private static final double STRAY_RADIUS = 128.0;

    /**
     * Tira do mundo do gametest os bichos e jogadores de teste que sobraram de cenas anteriores
     * (o gametest só limpa a própria estrutura). Sem isto, a caçada de uma cena pode escolher um
     * alvo de outra. Só para cenas com lote próprio: num lote compartilhado apagaria a cena vizinha.
     */
    static void clearStrays(GameTestHelper helper) {
        var center = helper.absoluteVec(new Vec3(12, 0, 12));
        List.copyOf(helper.getLevel().players()).forEach(player -> player.discard());
        for (var stray : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(STRAY_RADIUS, 64, STRAY_RADIUS),
                mob -> true)) {
            stray.discard();
        }
    }

    private static LandCreature hungry(GameTestHelper helper, EntityType<LandCreature> type, int x, int z) {
        LandCreature hunter = helper.spawn(type, x, 0, z);
        hunter.setTicksSinceMeal(STARVING);
        return hunter;
    }

    /** O bando de alossauros parte para a manada de mamutes, e a manada dispara. */
    @GameTest(template = ARENA, batch = "hunt_allosaurus", timeoutTicks = 300)
    public static void allosaurusPackChasesAMammothHerdThatRuns(GameTestHelper helper) {
        clearStrays(helper);
        List<LandCreature> pack = List.of(hungry(helper, ModEntities.ALLOSAURUS.get(), 4, 3),
                hungry(helper, ModEntities.ALLOSAURUS.get(), 7, 3), hungry(helper, ModEntities.ALLOSAURUS.get(), 10, 3));
        LandCreature first = helper.spawn(ModEntities.MAMMOTH.get(), 9, 0, 17);
        LandCreature second = helper.spawn(ModEntities.MAMMOTH.get(), 13, 0, 17);
        Vec3 start = first.position();
        helper.onEachTick(() -> {
            boolean chasing = pack.stream().anyMatch(allo -> allo.getTarget() == first || allo.getTarget() == second);
            if (chasing && first.isHunted() && second.isHunted() && first.huntingPack() >= 2
                    && first.position().distanceTo(start) > 3.0) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(290, () -> helper.fail("alvos " + pack.stream().map(a -> String.valueOf(a.getTarget())).toList()
                + " fome " + pack.get(0).hungerDrive() + " caçado " + first.isHunted() + "/" + second.isHunted()
                + " bando " + first.huntingPack() + " andou " + first.position().distanceTo(start)
                + " estresse " + first.stress()));
    }

    /** O T-Rex faminto vai atrás do Elasmotério, que foge. */
    @GameTest(template = ARENA, batch = "hunt_rex", timeoutTicks = 300)
    public static void tyrannosaurusHuntsAnElasmotherium(GameTestHelper helper) {
        clearStrays(helper);
        LandCreature rex = hungry(helper, ModEntities.TYRANNOSAURUS.get(), 4, 3);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 14, 0, 18);
        helper.assertTrue(HuntGoal.wouldHunt(rex, elasmo,
                        rex.behavior().orElseThrow().prey().orElseThrow()),
                "o T-Rex faminto não considerou o Elasmotério isolado uma presa");
        helper.onEachTick(() -> {
            if (rex.getTarget() == elasmo && elasmo.isHunted() && rex.isHunting()) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(290, () -> helper.fail("T-Rex não iniciou caçada: alvo=" + rex.getTarget()
                + ", fome=" + rex.hungerDrive() + ", ticks desde refeição=" + rex.ticksSinceMeal()
                + ", caça ativa=" + rex.isHunting() + ", presa avisada=" + elasmo.isHunted()
                + ", navegação parada=" + rex.getNavigation().isDone()
                + ", goals=" + rex.targetSelector.getAvailableGoals().stream()
                        .map(goal -> goal.getGoal().getClass().getSimpleName() + (goal.isRunning() ? "*" : ""))
                        .toList()));
    }

    /**
     * Velociraptor do tamanho real (um peru grande): faminto, ignora o Elasmotério e o jogador e caça
     * bicho pequeno — o dodô.
     */
    @GameTest(template = ARENA, batch = "hunt_raptor", timeoutTicks = 300)
    public static void hungryVelociraptorHuntsSmallPreyOnly(GameTestHelper helper) {
        clearStrays(helper);
        LandCreature raptor = hungry(helper, ModEntities.VELOCIRAPTOR.get(), 4, 3);
        // Só para a regra: posto no mundo, o Elasmotério investiria contra o raptor e roubaria a cena.
        LandCreature elasmotherium = ModEntities.ELASMOTHERIUM.get().create(helper.getLevel());
        var prey = raptor.behavior().orElseThrow().prey().orElseThrow();
        helper.assertTrue(!HuntGoal.isPrey(raptor, elasmotherium, prey), "o Elasmotério não é presa do Velociraptor");
        helper.assertTrue(!HuntGoal.wouldHunt(raptor, PredatorTests.survivalPlayer(helper), prey),
                "o Velociraptor não caça gente");
        LandCreature dodo = helper.spawn(ModEntities.DODO.get(), 8, 0, 9);
        helper.onEachTick(() -> {
            if (raptor.getTarget() == dodo) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(290, () -> helper.fail("o Velociraptor faminto não caçou o dodô: alvo " + raptor.getTarget()
                + ", fome " + raptor.hungerDrive()));
    }


    /** Barriga cheia: o predador ignora a presa que passa ao lado. */
    @GameTest(template = ARENA, batch = "hunt_sated", timeoutTicks = 120)
    public static void satedPredatorLeavesPreyAlone(GameTestHelper helper) {
        clearStrays(helper);
        LandCreature wolf = helper.spawn(ModEntities.DIRE_WOLF.get(), 4, 0, 4);
        wolf.setTicksSinceMeal(0);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 8, 0, 4);
        helper.onEachTick(() -> helper.assertTrue(wolf.getTarget() != pig, "saciado e caçando mesmo assim"));
        helper.runAtTickTime(110, helper::succeed);
    }

    /** Ferir um mamute estressa ele e a manada; com o tempo, passa. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void hurtingAMammothStressesTheHerdAndItFades(GameTestHelper helper) {
        LandCreature hurt = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 1, 2, 1);
        LandCreature herdmate = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 4, 2, 1);
        hurt.hurt(helper.getLevel().damageSources().generic(), 2.0F);
        hurt.invulnerableTime = 0;
        hurt.hurt(helper.getLevel().damageSources().generic(), 2.0F);
        double peak = hurt.stress();
        helper.assertTrue(peak >= 15.0, "ferido e calmo: " + peak);
        helper.assertTrue(hurt.mood() != Stress.Mood.CALM, "humor não mudou");
        helper.assertTrue(herdmate.stress() > 0.0, "a manada não sentiu");
        helper.runAtTickTime(160, () -> {
            helper.assertTrue(hurt.stress() < peak, "o estresse não baixou: " + hurt.stress());
            helper.succeed();
        });
    }

    /** Machos da mesma espécie disputam território mesmo sem fêmea próxima; o mais fraco cede. */
    @GameTest(template = EMPTY)
    public static void sameSpeciesMalesYieldAndFightsAreNotToTheDeath(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        LandCreature rival = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 5, 2, 5);
        rex.setFemale(false);
        rival.setFemale(false);
        LandCreature allo = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 10, 2, 1);
        helper.assertTrue(rex.isRival(rival) && rival.isRival(rex), "machos T-Rex da mesma espécie deveriam rivalizar");
        helper.assertFalse(rex.isRival(allo), "T-Rex e Alossauro não são rivais entre espécies");
        // Os níveis são sorteados; ferido, o outro macho é sem dúvida o mais fraco.
        rival.setHealth(rival.getMaxHealth() * 0.4F);
        helper.assertTrue(rex.dominance() > rival.dominance(), "o T-Rex deveria ser o dominante");
        RivalryGoal goal = new RivalryGoal(rex);
        boolean challenged = false;
        for (int attempt = 0; attempt < 2400 && !challenged; attempt++) {
            challenged = goal.canUse();
        }
        helper.assertTrue(challenged, "não houve disputa territorial entre machos sem fêmea próxima");
        goal.start();
        helper.assertTrue(rival.yieldingFrom() == rex, "o macho mais fraco deveria ter cedido");

        LandCreature other = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 5);
        other.setFemale(false);
        rex.setTarget(other);
        other.hurt(helper.getLevel().damageSources().mobAttack(rex), other.getMaxHealth() * 0.6F);
        helper.assertTrue(other.isAlive() && other.yieldingFrom() == rex, "quem perdeu a briga não desistiu");
        helper.assertTrue(rex.getTarget() != other, "o vencedor seguiu atrás de quem desistiu");
        helper.succeed();
    }
}
