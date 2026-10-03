package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.Perception;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.ai.StalkGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Espreita (stalk): o carnívoro acompanha a manada sem ser notado e só depois dispara; e a regra de que
 * o carnívoro sempre percebe a presa de mais longe do que ela o percebe ({@link Perception}).
 *
 * <p>Cada cena tem lote próprio: os raios de caça e de alerta passam do tamanho da arena.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class StalkTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    /** Uma hora sem comer: com fome de verdade. */
    private static final long STARVING = 20L * 60 * 60;

    /** Nos dados de todas as espécies: o faro de cada carnívoro passa o alerta de cada presa da dieta + a folga. */
    @GameTest(template = EMPTY, batch = "stalk_perception")
    public static void everyCarnivoreSensesItsPreyFirst(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Species.REGISTRY_KEY);
        int checked = 0;
        for (var hunter : registry.entrySet()) {
            BehaviorProfile behavior = hunter.getValue().behavior().orElse(null);
            if (behavior == null || behavior.prey().isEmpty()) {
                continue;
            }
            double declared = behavior.ecology().huntRadius();
            for (var prey : registry.entrySet()) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(prey.getKey().location());
                var wariness = prey.getValue().behavior().flatMap(BehaviorProfile::wariness);
                if (prey == hunter || !type.is(behavior.prey().get()) || wariness.isEmpty()) {
                    continue;
                }
                double alert = Math.max(wariness.get().alertRadius(), wariness.get().calfRadius());
                helper.assertTrue(Perception.respects(declared, alert), hunter.getKey().location() + " fareja a "
                        + declared + ", mas " + prey.getKey().location() + " alerta a " + alert
                        + " (precisa de " + Perception.minimumHuntRadius(alert) + ")");
                checked++;
            }
        }
        helper.assertTrue(checked > 0, "nenhum par predador × presa conferido");
        helper.succeed();
    }

    private static LandCreature hungry(GameTestHelper helper, EntityType<LandCreature> type, int x, int z) {
        LandCreature hunter = helper.spawn(type, x, 0, z);
        hunter.setTicksSinceMeal(STARVING);
        hunter.setGroupId(UUID.randomUUID());
        return hunter;
    }

    private static List<LandCreature> herd(GameTestHelper helper, EntityType<LandCreature> type, int... xs) {
        UUID group = UUID.randomUUID();
        return java.util.Arrays.stream(xs).mapToObj(x -> {
            // Parada: aqui importa a ronda do predador; manada andando se desgarra e vira outro teste.
            LandCreature member = helper.spawnWithNoFreeWill(type, x, 0, 2);
            member.setGroupId(group);
            return member;
        }).toList();
    }

    /**
     * O Alossauro faminto escolhe um Galimimo da manada, mas não corre: espreita, rondando fora do raio em
     * que seria notado, e a manada ainda não se sabe caçada.
     */
    @GameTest(template = ARENA, batch = "stalk_herd", timeoutTicks = 200)
    public static void carnivoreStalksAHerdWithoutBeingNoticed(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        List<LandCreature> herd = herd(helper, ModEntities.GALLIMIMUS.get(), 2, 4, 6);
        LandCreature hunter = hungry(helper, ModEntities.ALLOSAURUS.get(), 4, 38);
        StringBuilder log = new StringBuilder();
        boolean[] was = {false};
        helper.onEachTick(() -> {
            if (!was[0] && hunter.isStalking()) {
                log.append("[espreita começou no tique ").append(helper.getTick()).append(" a ")
                        .append(hunter.getTarget() == null ? -1 : hunter.distanceTo(hunter.getTarget())).append("] ");
            }
            if (was[0] && !hunter.isStalking() && !log.toString().contains("parou")) {
                var t = hunter.getTarget();
                log.append("parou no tique ").append(helper.getTick()).append(": alvo=").append(t)
                        .append(" dist=").append(t == null ? -1 : hunter.distanceTo(t))
                        .append(" visto=").append(hunter.stalkBlown()).append(" ferido=").append(hunter.hurtTime)
                        .append(" caçando=").append(hunter.isHunting())
                        .append(" desgarrado=").append(t instanceof LandCreature c && c.isIsolated())
                        .append(" aviso=").append(t == null ? -1 : StalkGoal.noticeRadius(t))
                        .append(" atacado-por=").append(hunter.getLastHurtByMob());
            }
            was[0] = hunter.isStalking();
        });
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(herd.contains(hunter.getTarget()), "deveria ter escolhido um Galimimo: " + hunter.getTarget()
                    + " fome=" + hunter.hungerDrive() + " falhou=" + hunter.recentlyFailedHunt()
                    + " melhor=" + dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal.bestPrey(hunter)
                    + " aceita=" + herd.stream().map(m -> dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal.wouldHunt(
                            hunter, m, hunter.behavior().orElseThrow().prey().orElseThrow())).toList()
                    + " faro=" + hunter.huntRadius() + " dist=" + hunter.distanceTo(herd.get(0))
                    + " y=" + hunter.getY() + "/" + herd.get(0).getY() + " " + log);
            helper.assertTrue(hunter.isStalking(), "deveria estar espreitando, não correndo; " + log);
            for (LandCreature member : herd) {
                helper.assertFalse(member.isHunted(), "a manada já se sabe caçada");
                double gap = hunter.distanceTo(member) - (hunter.getBbWidth() + member.getBbWidth()) / 2.0;
                helper.assertTrue(gap > StalkGoal.noticeRadius(member),
                        "rondando dentro do raio de aviso: " + gap + " ≤ " + StalkGoal.noticeRadius(member));
            }
            helper.succeed();
        });
    }

    /** Presa de manada desgarrada: o bote vem na hora, e ela se sabe caçada. */
    @GameTest(template = ARENA, batch = "stalk_straggler", timeoutTicks = 200)
    public static void aStragglerGetsThePounce(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        // Parado: o Galimimo é arisco e fugiria do Alossauro que ronda antes de farejá-lo; aqui importa o bote.
        LandCreature straggler = helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 4, 0, 2);
        straggler.setGroupId(UUID.randomUUID());
        LandCreature hunter = hungry(helper, ModEntities.ALLOSAURUS.get(), 4, 38);
        helper.succeedWhen(() -> {
            helper.assertTrue(hunter.getTarget() == straggler, "deveria ter escolhido o desgarrado");
            helper.assertFalse(hunter.isStalking(), "ainda espreitando um desgarrado");
            helper.assertTrue(straggler.isHunted(), "o desgarrado não se sabe caçado depois do bote");
        });
    }

    /**
     * Notado pela manada, o bando que espreitava perde a surpresa e dispara junto; a manada se sabe caçada.
     * (Em bando, os Alossauros encaram a manada de Galimimos.)
     */
    @GameTest(template = ARENA, batch = "stalk_spotted", timeoutTicks = 260, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aSpottedPackPounces(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        List<LandCreature> herd = herd(helper, ModEntities.GALLIMIMUS.get(), 2, 4, 6);
        LandCreature hunter = hungry(helper, ModEntities.ALLOSAURUS.get(), 3, 38);
        LandCreature mate = hungry(helper, ModEntities.ALLOSAURUS.get(), 7, 38);
        mate.setGroupId(hunter.groupId());
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(hunter.isStalking() || mate.isStalking(), "o bando deveria estar espreitando antes de ser visto");
            (hunter.isStalking() ? hunter : mate).blowStalk();
        });
        helper.runAtTickTime(110, () -> helper.succeedWhen(() -> {
            helper.assertFalse(hunter.isStalking() || mate.isStalking(), "visto, o bando deveria ter disparado junto");
            helper.assertTrue(herd.stream().anyMatch(LandCreature::isHunted), "a manada não se sabe caçada");
        }));
    }

    /**
     * Sozinho, o Alossauro espreita a manada de Galimimos — que não se defende junto, então ele pode atacar — e,
     * notado, dá o bote em vez de desistir. Antes o solitário nunca atacava presa de manada e aqui desistia; a
     * desistência diante da manada que ele não encara segue nos testes de {@code HuntChoice.chooseToStalk}
     * (2026-10-03).
     */
    @GameTest(template = ARENA, batch = "stalk_alone_spotted", timeoutTicks = 200)
    public static void aLoneStalkerSpottedPouncesOnAHerdItCanTake(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        List<LandCreature> herd = herd(helper, ModEntities.GALLIMIMUS.get(), 2, 4, 6);
        LandCreature hunter = hungry(helper, ModEntities.ALLOSAURUS.get(), 4, 38);
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(hunter.isStalking(), "deveria estar espreitando antes de ser visto");
            hunter.blowStalk();
        });
        helper.runAtTickTime(105, () -> {
            helper.assertTrue(hunter.getTarget() instanceof LandCreature target && herd.contains(target),
                    "notado, deveria dar o bote num Galimimo: " + hunter.getTarget());
            helper.assertFalse(hunter.recentlyFailedHunt(), "não deveria contar como caçada frustrada");
            helper.assertTrue(herd.stream().anyMatch(LandCreature::isHunted), "a manada deveria se saber caçada");
            helper.succeed();
        });
    }

    /** Na perseguição, a presa que abre mais de 30 blocos escapa: o predador desiste e fica frustrado. */
    @GameTest(template = ARENA, batch = "stalk_chase_escape", timeoutTicks = 200)
    public static void aChasedPreyThatGetsThirtyBlocksAwayEscapes(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature prey = helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 4, 0, 2);
        prey.setGroupId(UUID.randomUUID());
        LandCreature hunter = hungry(helper, ModEntities.ALLOSAURUS.get(), 4, 24);
        boolean[] moved = {false};
        helper.onEachTick(() -> {
            if (!moved[0] && hunter.getTarget() == prey && !hunter.isStalking()) {
                // Depois do bote: a presa "corre" para longe de uma vez.
                prey.teleportTo(prey.getX(), prey.getY(), hunter.getZ() - 40.0);
                moved[0] = true;
            } else if (moved[0] && hunter.getTarget() == null) {
                helper.assertTrue(hunter.recentlyFailedHunt(), "desistiu sem contar como caçada frustrada");
                helper.succeed();
            }
        });
    }

    /** Espinossauro: nasce sobretudo na beira d'água (peso 3), mas também em qualquer outro bioma (peso 1). */
    @GameTest(template = EMPTY, batch = "stalk_spino_spawn")
    public static void spinosaurusFavorsWaterButNotOnly(GameTestHelper helper) {
        SpawnProfile spawn = Species.of(helper.getLevel().registryAccess(), ModEntities.SPINOSAURUS.get())
                .flatMap(Species::spawn).orElseThrow();
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var river = biomes.getHolderOrThrow(Biomes.RIVER);
        var plains = biomes.getHolderOrThrow(Biomes.PLAINS);
        var desert = biomes.getHolderOrThrow(Biomes.DESERT);
        helper.assertTrue(river.is(spawn.biomes()) && plains.is(spawn.biomes()) && desert.is(spawn.biomes()),
                "deveria nascer no rio, na planície e no deserto");
        helper.assertTrue(spawn.weightIn(river) == 3, "peso no rio " + spawn.weightIn(river));
        helper.assertTrue(spawn.weightIn(plains) == 1, "peso na planície " + spawn.weightIn(plains));
        helper.assertTrue(spawn.weightIn(desert) == 1, "peso no deserto " + spawn.weightIn(desert));
        helper.succeed();
    }
}
