package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
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

    /** Velociraptores em bando caçam Elasmotérios solitários, além dos dodôs. */
    @GameTest(template = ARENA, batch = "hunt_raptor", timeoutTicks = 300)
    public static void velociraptorsHuntAnElasmotherium(GameTestHelper helper) {
        clearStrays(helper);
        List<LandCreature> pack = List.of(hungry(helper, ModEntities.VELOCIRAPTOR.get(), 4, 3),
                hungry(helper, ModEntities.VELOCIRAPTOR.get(), 6, 3),
                hungry(helper, ModEntities.VELOCIRAPTOR.get(), 8, 3),
                hungry(helper, ModEntities.VELOCIRAPTOR.get(), 10, 3));
        LandCreature raptor = pack.get(0);
        LandCreature elasmotherium = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 12, 0, 16);
        boolean wouldHunt = HuntGoal.wouldHunt(raptor, elasmotherium,
                raptor.behavior().orElseThrow().prey().orElseThrow());
        helper.assertTrue(wouldHunt, "um bando faminto de velociraptores deveria escolher o Elasmotério; prey="
                + HuntGoal.isPrey(raptor, elasmotherium, raptor.behavior().orElseThrow().prey().orElseThrow())
                + ", isolado=" + elasmotherium.isIsolated() + ", distância=" + raptor.distanceTo(elasmotherium)
                + ", bando=" + (1 + helper.getLevel().getEntitiesOfClass(LandCreature.class,
                        raptor.getBoundingBox().inflate(raptor.behavior().orElseThrow().herdRadius()),
                        other -> other != raptor && other.getType() == ModEntities.VELOCIRAPTOR.get()
                                && !other.isTame() && !other.isBaby() && !other.isUnconscious()).size())
                + ", razão tamanho=" + Math.pow((elasmotherium.getBbWidth() * elasmotherium.getBbWidth()
                        * elasmotherium.getBbHeight())
                        / (raptor.getBbWidth() * raptor.getBbWidth() * raptor.getBbHeight()), 2.0 / 3.0));
        helper.onEachTick(() -> {
            long pursuing = pack.stream().filter(member -> member.getTarget() == elasmotherium).count();
            if (pursuing >= 2 && elasmotherium.isHunted() && elasmotherium.huntingPack() >= 2) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(290, () -> helper.fail("matilha não perseguiu: alvos "
                + pack.stream().map(member -> String.valueOf(member.getTarget())).toList()
                + ", fome " + raptor.hungerDrive() + ", intervalo " + raptor.ecology().hungerSeconds()
                + ", caçado " + elasmotherium.isHunted() + ", matilha " + elasmotherium.huntingPack()));
    }

    /** Mesmo sem fome plena, um bando oportunista caça um Elasmotério isolado que está perto. */
    @GameTest(template = ARENA, batch = "hunt_raptor_opportunistic", timeoutTicks = 240)
    public static void opportunisticVelociraptorsHuntANearbyElasmotherium(GameTestHelper helper) {
        clearStrays(helper);
        List<LandCreature> pack = List.of(
                helper.spawn(ModEntities.VELOCIRAPTOR.get(), 4, 0, 3),
                helper.spawn(ModEntities.VELOCIRAPTOR.get(), 6, 0, 3),
                helper.spawn(ModEntities.VELOCIRAPTOR.get(), 8, 0, 3),
                helper.spawn(ModEntities.VELOCIRAPTOR.get(), 10, 0, 3));
        pack.forEach(raptor -> raptor.setTicksSinceMeal(40 * 20L));
        LandCreature elasmotherium = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 12, 0, 10);
        helper.assertTrue(pack.get(0).hungerDrive() == dev.madebyfelipe.iceagesurvival.core.ecology.Hunger.Drive.OPPORTUNISTIC,
                "teste precisa começar no estado oportunista");
        helper.onEachTick(() -> {
            if (pack.stream().anyMatch(raptor -> raptor.getTarget() == elasmotherium)) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(230, () -> helper.fail("o bando oportunista ignorou o Elasmotério próximo"));
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

    /** O mais fraco, encarado pelo rival, vai embora; numa briga, quem cai à metade da vida desiste. */
    @GameTest(template = EMPTY)
    public static void weakerRivalYieldsAndFightsAreNotToTheDeath(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        LandCreature allo = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 5, 2, 5);
        helper.assertTrue(rex.isRival(allo) && allo.isRival(rex), "T-Rex e Alossauro deveriam ser rivais");
        // Os níveis são sorteados; ferido, o Alossauro é sem dúvida o mais fraco.
        allo.setHealth(allo.getMaxHealth() * 0.4F);
        helper.assertTrue(rex.dominance() > allo.dominance(), "o T-Rex deveria ser o dominante");
        allo.challengedBy(rex);
        helper.assertTrue(allo.yieldingFrom() == rex, "o Alossauro calmo deveria ter cedido");

        LandCreature other = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 1, 2, 5);
        rex.setTarget(other);
        other.hurt(helper.getLevel().damageSources().mobAttack(rex), other.getMaxHealth() * 0.6F);
        helper.assertTrue(other.isAlive() && other.yieldingFrom() == rex, "quem perdeu a briga não desistiu");
        helper.assertTrue(rex.getTarget() != other, "o vencedor seguiu atrás de quem desistiu");
        helper.succeed();
    }
}
