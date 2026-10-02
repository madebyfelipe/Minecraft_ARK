package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Bandos com identidade, território contra qualquer criatura, fome do bando e prioridade de alvo.
 * Cada cena num lote próprio: as regras novas fazem as criaturas de cenas vizinhas brigarem.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class PackTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    /** Quem nasce junto na reposição é um bando; outro grupo da mesma espécie, não. */
    @GameTest(template = EMPTY, batch = "pack_spawn")
    public static void creaturesSpawnedTogetherShareAPack(GameTestHelper helper) {
        var level = helper.getLevel();
        var type = ModEntities.DODO.get();
        var pack = new PrehistoricCreature.Pack(type, UUID.randomUUID());
        var other = new PrehistoricCreature.Pack(type, UUID.randomUUID());
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        helper.setBlock(3, 1, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        helper.setBlock(5, 1, 1, net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        helper.assertTrue(WildSpawner.spawnAt(level, type, helper.absolutePos(new BlockPos(1, 2, 1)), pack), "1º membro");
        helper.assertTrue(WildSpawner.spawnAt(level, type, helper.absolutePos(new BlockPos(3, 2, 1)), pack), "2º membro");
        helper.assertTrue(WildSpawner.spawnAt(level, type, helper.absolutePos(new BlockPos(5, 2, 1)), other), "outro bando");
        List<LandCreature> dodos = level.getEntitiesOfClass(LandCreature.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8), c -> c.getType() == type);
        long inPack = dodos.stream().filter(dodo -> pack.id().equals(dodo.groupId())).count();
        long inOther = dodos.stream().filter(dodo -> other.id().equals(dodo.groupId())).count();
        helper.assertTrue(inPack == 2 && inOther == 1, "bandos: " + inPack + " e " + inOther);
        dodos.forEach(dodo -> dodo.discard());
        helper.succeed();
    }

    /** Sem bando (mundo antigo, ovo, comando), é adotada pelo bando mais perto com vaga; cheio, funda outro. */
    @GameTest(template = EMPTY, batch = "pack_adoption")
    public static void groupAdoptionRespectsTheMaximum(GameTestHelper helper) {
        EntityType<LandCreature> type = ModEntities.GALLIMIMUS.get();
        List<LandCreature> all = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            all.add(helper.spawnWithNoFreeWill(type, 1 + i % 4, 2, 1 + i / 4 * 2));
        }
        int max = all.get(0).groupMax();
        helper.runAfterDelay(5, () -> {
            var bySize = all.stream().collect(java.util.stream.Collectors.groupingBy(LandCreature::groupId,
                    java.util.stream.Collectors.counting()));
            helper.assertTrue(all.stream().allMatch(c -> c.groupId() != null), "alguém ficou sem bando");
            helper.assertTrue(bySize.values().stream().allMatch(size -> size <= max), "bando acima de " + max + ": " + bySize);
            helper.assertTrue(bySize.size() >= 2, "sete galimimos num bando só: " + bySize);
            helper.succeed();
        });
    }

    /** Um membro come: o bando inteiro fica saciado e larga a caça. */
    @GameTest(template = EMPTY, batch = "pack_meal")
    public static void satietyIsSharedByThePack(GameTestHelper helper) {
        UUID group = UUID.randomUUID();
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.UTAHRAPTOR.get(), 1, 2, 1);
        LandCreature mate = helper.spawnWithNoFreeWill(ModEntities.UTAHRAPTOR.get(), 4, 2, 1);
        hunter.setGroupId(group);
        mate.setGroupId(group);
        hunter.setTicksSinceMeal(100_000);
        mate.setTicksSinceMeal(100_000);
        var cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 2, 3);
        hunter.killedEntity(helper.getLevel(), cow);
        // A vaca repartida por dois não sacia por inteiro, mas a fome cai igual para o bando todo.
        helper.assertTrue(hunter.ticksSinceMeal() < 100_000 && mate.ticksSinceMeal() == hunter.ticksSinceMeal(),
                "a saciedade deveria ser do bando: " + hunter.ticksSinceMeal() + " / " + mate.ticksSinceMeal());
        helper.succeed();
    }

    /** Outro bando da mesma espécie no território: guerra, os dois lados se atacam. */
    @GameTest(template = ARENA, batch = "pack_war", timeoutTicks = 200)
    public static void rivalPacksOfTheSameSpeciesFight(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature a = helper.spawn(ModEntities.TRICERATOPS.get(), 4, 0, 4);
        LandCreature b = helper.spawn(ModEntities.TRICERATOPS.get(), 14, 0, 12);
        a.setGroupId(UUID.randomUUID());
        b.setGroupId(UUID.randomUUID());
        helper.onEachTick(() -> {
            if (a.getTarget() == b && b.getTarget() == a) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(190, () -> helper.fail("os bandos não brigaram: alvos " + a.getTarget() + " / " + b.getTarget()));
    }

    /** Outra espécie, mais fraca, no território de um bando: cede e vai embora. */
    @GameTest(template = ARENA, batch = "pack_intruder", timeoutTicks = 200)
    public static void weakerIntruderLeavesTheTerritory(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature trike = helper.spawn(ModEntities.TRICERATOPS.get(), 6, 0, 6);
        LandCreature galli = helper.spawn(ModEntities.GALLIMIMUS.get(), 12, 0, 8);
        trike.setGroupId(UUID.randomUUID());
        galli.setGroupId(UUID.randomUUID());
        helper.onEachTick(() -> {
            if (galli.yieldingFrom() == trike) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(190, () -> helper.fail("o Galimimo não cedeu o território: alvo do Tricerátopo "
                + trike.getTarget() + ", cedendo a " + galli.yieldingFrom()));
    }

    /** Provocado pelo jogador, mas com um rival da espécie investindo: o rival vem primeiro. */
    @GameTest(template = ARENA, batch = "pack_priority", timeoutTicks = 120)
    public static void biggerThreatOutranksThePlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = helper.spawn(ModEntities.TYRANNOSAURUS.get(), 6, 0, 6);
        LandCreature rival = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 14, 0, 6);
        rex.setGroupId(UUID.randomUUID());
        rival.setGroupId(UUID.randomUUID());
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 12.5)));
        rex.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        rex.setTarget(player);
        rival.setTarget(rex);
        helper.onEachTick(() -> {
            if (rex.getTarget() == rival) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(110, () -> helper.fail("o T-Rex seguiu no jogador com um rival investindo: " + rex.getTarget()));
    }

    /** O jogador é presa como as outras, pela tabela de dieta: com a favorita por perto, ele fica para depois. */
    @GameTest(template = ARENA, batch = "pack_prey_first", timeoutTicks = 200)
    public static void hungryPredatorPrefersAnimalPreyOverThePlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = helper.spawn(ModEntities.UTAHRAPTOR.get(), 6, 0, 6);
        hunter.setGroupId(UUID.randomUUID());
        hunter.setTicksSinceMeal(100_000);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 10.5)));
        // Na tabela do Utahraptor o jogador vale 2 e o Galimimo, a favorita, 3.
        LandCreature cow = helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 10, 0, 10);
        cow.setGroupId(UUID.randomUUID());
        helper.assertTrue(dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal.bestPrey(hunter) == cow,
                "o Galimimo deveria valer mais que o jogador: escolheu " + dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal.bestPrey(hunter));
        helper.onEachTick(() -> {
            if (hunter.getTarget() == cow) {
                helper.succeed();
            } else if (hunter.getTarget() == player) {
                helper.fail("foi no jogador com um Galimimo do lado");
            }
        });
    }

    /** {@code /ias hunger}: o predador saciado passa a caçar na hora. */
    @GameTest(template = EMPTY, batch = "pack_starve")
    public static void starveMakesAPredatorHuntNow(GameTestHelper helper) {
        LandCreature allosaurus = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 2, 2, 2);
        allosaurus.setTicksSinceMeal(0);
        helper.assertTrue(allosaurus.isSated(), "começa saciado");
        allosaurus.starve();
        helper.assertTrue(allosaurus.hungerDrive() == dev.madebyfelipe.iceagesurvival.core.ecology.Hunger.Drive.HUNTING,
                "deveria estar caçando: " + allosaurus.hungerDrive());
        helper.succeed();
    }
}
