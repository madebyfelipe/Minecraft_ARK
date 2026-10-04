package dev.madebyfelipe.iceagesurvival.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.titan.TitanPhase;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.entity.ai.ReturnToLairGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O Titanovenator, boss do endgame (dossiê nº 017): bem maior que o Rex, três fases com resistência crescente, a
 * mordida que ancora e faz sangrar, o rugido de troca de fase, a pancada de corpo, a imunidade ao torpor, o covil a que
 * a base militar o prende e as animações que ele pede ao arquivo próprio. Cada teste tem o próprio lote: um boss solto
 * atacaria as criaturas dos testes vizinhos.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class TitanovenatorTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final BlockPos LAIR = new BlockPos(12, 0, 12);

    private static TitanovenatorBoss boss(GameTestHelper helper, int x, int y, int z) {
        return (TitanovenatorBoss) helper.spawnWithNoFreeWill(ModEntities.TITANOVENATOR.get(), x, y, z);
    }

    private static JsonObject resource(String path) {
        try (InputStream stream = TitanovenatorTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("recurso não encontrado: " + path);
            }
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError("não leu " + path, e);
        }
    }

    /** O boss é consideravelmente maior que o Rex: a caixa e o modelo são 1,6× os dele. */
    @GameTest(template = EMPTY)
    public static void theTitanovenatorIsMuchBiggerThanTheRex(GameTestHelper helper) {
        var rex = ModEntities.TYRANNOSAURUS.get().getDimensions();
        var titan = ModEntities.TITANOVENATOR.get().getDimensions();
        helper.assertTrue(titan.height / rex.height >= 1.5F, "altura " + titan.height + " contra " + rex.height);
        helper.assertTrue(titan.width / rex.width >= 1.5F, "largura " + titan.width + " contra " + rex.width);
        double rexScale = resource("/assets/iceagesurvival/creature_models/tyrannosaurus.json").get("scale").getAsDouble();
        double titanScale = resource("/assets/iceagesurvival/creature_models/titanovenator.json").get("scale").getAsDouble();
        helper.assertTrue(Math.abs(titanScale / rexScale - 1.6) < 0.01, "escala do modelo: " + titanScale);
        // O corpo, a cabeça e a cauda crescem junto: as partes do More Hitboxes seguem a mesma proporção.
        JsonObject rexBoxes = resource("/data/iceagesurvival/hitboxes/tyrannosaurus.json");
        JsonObject titanBoxes = resource("/data/iceagesurvival/hitboxes/titanovenator.json");
        for (int i = 0; i < 3; i++) {
            double rexBody = rexBoxes.getAsJsonArray("elements").get(i).getAsJsonObject().get("height").getAsDouble();
            double titanBody = titanBoxes.getAsJsonArray("elements").get(i).getAsJsonObject().get("height").getAsDouble();
            helper.assertTrue(Math.abs(titanBody / rexBody - 1.6) < 0.01, "parte " + i + " fora de proporção");
        }
        helper.succeed();
    }

    /** Espécie sem domesticação, montaria nem nascimento selvagem: só a base militar o chama. */
    @GameTest(template = EMPTY)
    public static void theBossIsNotFaunaNorATameableMount(GameTestHelper helper) {
        Species species = Species.of(helper.getLevel().registryAccess(), ModEntities.TITANOVENATOR.get()).orElseThrow();
        helper.assertTrue(species.spawn().isEmpty(), "o boss nasce sozinho");
        helper.assertTrue(species.taming().isEmpty(), "o boss tem domesticação");
        helper.assertTrue(species.mount().isEmpty(), "o boss tem montaria");
        helper.assertTrue(ModItems.TITANOVENATOR_SPAWN_EGG.isPresent(), "sem ovo gerador para testar");
        helper.succeed();
    }

    /** 1024 de vida (o teto do vanilla); o golpe de 100 tira 100 na fase 1, 70 na 2 e 50 na 3. */
    @GameTest(template = EMPTY, batch = "titan_phases")
    public static void phasesFollowHealthAndResistanceGrows(GameTestHelper helper) {
        TitanovenatorBoss boss = boss(helper, 3, 2, 3);
        var damage = helper.getLevel().damageSources().magic();
        helper.assertTrue(boss.getMaxHealth() == 1024.0F, "vida máxima: " + boss.getMaxHealth());
        helper.assertTrue(boss.phase() == TitanPhase.ONE, "começa na fase 1");

        boss.hurt(damage, 100.0F);
        helper.assertTrue(Math.abs(boss.getHealth() - 924.0F) < 0.01F, "fase 1 deveria tomar 100: " + boss.getHealth());

        boss.setHealth(600.0F);
        helper.assertTrue(boss.phase() == TitanPhase.TWO, "abaixo de 66% deveria estar na fase 2");
        boss.invulnerableTime = 0;
        boss.hurt(damage, 100.0F);
        helper.assertTrue(Math.abs(boss.getHealth() - 530.0F) < 0.01F, "fase 2 deveria tomar 70: " + boss.getHealth());

        boss.setHealth(300.0F);
        helper.assertTrue(boss.phase() == TitanPhase.THREE, "abaixo de 33% deveria estar na fase 3");
        boss.invulnerableTime = 0;
        boss.hurt(damage, 100.0F);
        helper.assertTrue(Math.abs(boss.getHealth() - 250.0F) < 0.01F, "fase 3 deveria tomar 50: " + boss.getHealth());
        boss.discard();
        helper.succeed();
    }

    /** Cada troca de fase ruge uma vez, parado, e a onda do peito empurra quem está perto sem ferir. */
    @GameTest(template = EMPTY, batch = "titan_roar", timeoutTicks = 140)
    public static void eachPhaseChangeRoarsOnceAndShovesAwayWhoIsClose(GameTestHelper helper) {
        TitanovenatorBoss boss = boss(helper, 8, 2, 8);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 12, 2, 8);
        helper.assertFalse(boss.isRoaring(), "rugindo antes da hora");
        boss.setHealth(600.0F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(boss.isRoaring(), "na passagem para a fase 2, o boss deveria rugir");
            helper.assertFalse(boss.doHurtTarget(pig), "rugindo, não morde");
        });
        helper.runAfterDelay(TitanovenatorBoss.ROAR_TICKS - 10, () -> {
            helper.assertTrue(pig.getDeltaMovement().horizontalDistance() > 0.2 || pig.getX() > helper.absoluteVec(
                    new Vec3(12.5, 0, 8.5)).x + 0.5, "a onda do rugido não empurrou o porco");
            helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "a onda do rugido não fere");
        });
        helper.runAfterDelay(TitanovenatorBoss.ROAR_TICKS + 8, () -> {
            helper.assertFalse(boss.isRoaring(), "o rugido não acabou");
            boss.setHealth(300.0F);
        });
        helper.runAfterDelay(TitanovenatorBoss.ROAR_TICKS + 12, () -> {
            helper.assertTrue(boss.isRoaring(), "na passagem para a fase 3, o boss deveria rugir de novo");
            boss.discard();
            pig.discard();
            helper.succeed();
        });
    }

    /** A mordida ancora: sangramento que empilha (mais fundo na fase 3) e puxa a vítima para junto da boca. */
    @GameTest(template = EMPTY, batch = "titan_bite")
    public static void theBiteBleedsAndPullsTheVictimBack(GameTestHelper helper) {
        TitanovenatorBoss boss = boss(helper, 3, 2, 3);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 3, 2, 3);
        // Vida de sobra: a mordida de 42 mataria o golem antes da terceira e o sangramento não empilharia em morto.
        golem.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0);
        golem.setHealth(1000.0F);
        var bleeding = ModEffects.BLEEDING.get();

        helper.assertTrue(boss.doHurtTarget(golem), "a mordida não acertou");
        MobEffectInstance first = golem.getEffect(bleeding);
        helper.assertTrue(first != null && first.getAmplifier() == 0, "a primeira mordida deveria abrir 1 nível");

        golem.invulnerableTime = 0;
        boss.doHurtTarget(golem);
        helper.assertTrue(golem.getEffect(bleeding).getAmplifier() == 1, "a segunda mordida deveria empilhar");

        boss.setHealth(300.0F); // fase 3: corta dois níveis de uma vez
        golem.invulnerableTime = 0;
        boss.doHurtTarget(golem);
        helper.assertTrue(golem.getEffect(bleeding).getAmplifier() == 3,
                "na fase 3 a mordida deveria somar dois níveis: " + golem.getEffect(bleeding).getAmplifier());
        helper.assertFalse(boss.hasEffect(bleeding), "o boss não sangra");

        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 8, 2, 3);
        pig.setPos(boss.getX() + 6.0, boss.getY(), boss.getZ());
        boss.invulnerableTime = 0;
        boss.doHurtTarget(pig);
        helper.assertTrue(pig.getDeltaMovement().x < -0.2, "a mordida não puxou a vítima para a boca: "
                + pig.getDeltaMovement());
        boss.discard();
        golem.discard();
        pig.discard();
        helper.succeed();
    }

    /** Nem o dardo nem o torpor direto derrubam o boss. */
    @GameTest(template = ARENA, batch = "titan_tranq", timeoutTicks = 80, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void torporDoesNotKnockTheBossOut(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        TitanovenatorBoss boss = boss(helper, 8, 0, 8);
        boss.addTorpor(1.0E6, helper.makeMockSurvivalPlayer());
        boss.setTorpor(1.0E6);
        helper.assertFalse(boss.isUnconscious(), "o torpor direto derrubou o boss");
        Vec3 from = boss.position().add(8, 3, 0);
        TranqArrow dart = new TranqArrow(helper.getLevel(), from.x, from.y, from.z,
                new ItemStack(ModItems.TRANQ_DART.get()), null);
        dart.shoot(-1, 0, 0, 2.0F, 0);
        helper.getLevel().addFreshEntity(dart);
        helper.succeedWhen(() -> {
            helper.assertTrue(boss.getHealth() < boss.getMaxHealth(), "o dardo ainda não acertou");
            helper.assertTrue(boss.torpor() == 0 && !boss.isUnconscious(), "o boss recebeu torpor: " + boss.torpor());
            boss.discard();
        });
    }

    /** Cercado por dois inimigos, o boss gira o quadril e varre os dois. */
    @GameTest(template = EMPTY, batch = "titan_slam", timeoutTicks = 120)
    public static void surroundedByTwoFoesTheBossSlamsThemBoth(GameTestHelper helper) {
        TitanovenatorBoss boss = boss(helper, 8, 2, 8);
        // Criaturas do mod: são inimigos do covil e não têm a invulnerabilidade de 3 s do jogador que acabou de entrar.
        var first = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 8, 2, 8);
        var second = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 8, 2, 8);
        Vec3 center = boss.position();
        first.setPos(center.x + 4.0, center.y, center.z);
        second.setPos(center.x - 4.0, center.y, center.z);
        boss.setTarget(first);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(first.getHealth() < first.getMaxHealth() || !first.isAlive(), "o primeiro não foi varrido");
            helper.assertTrue(second.getHealth() < second.getMaxHealth() || !second.isAlive(),
                    "o segundo não foi varrido");
            boss.discard();
            helper.succeed();
        });
    }

    /** A investida derruba: dano de parte do ataque e empurrão para a frente e para cima. */
    @GameTest(template = EMPTY, batch = "titan_charge")
    public static void aChargeHitDamagesAndKnocksTheVictimDown(GameTestHelper helper) {
        TitanovenatorBoss boss = boss(helper, 3, 2, 3);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 5, 2, 3);
        helper.assertFalse(boss.chargeReady(), "a fase 1 não investe");
        boss.setHealth(600.0F);
        boss.chargeHit(pig, new Vec3(1.0, 0.0, 0.0));
        helper.assertTrue(pig.getHealth() < pig.getMaxHealth() || !pig.isAlive(), "a investida não feriu");
        helper.assertTrue(pig.getDeltaMovement().x > 1.0 && pig.getDeltaMovement().y > 0.3,
                "a investida não derrubou: " + pig.getDeltaMovement());
        boss.discard();
        pig.discard();
        helper.succeed();
    }

    /** A base chama o boss no covil; o covil vai no save e a fase já alcançada também. */
    @GameTest(template = EMPTY, batch = "titan_save")
    public static void aSummonedBossIsTiedToItsLairAndRemembersIt(GameTestHelper helper) {
        BlockPos lair = helper.absolutePos(new BlockPos(6, 1, 6));
        TitanovenatorBoss boss = TitanovenatorBoss.summon(helper.getLevel(), lair, 40);
        helper.assertTrue(boss != null, "summon não criou o boss");
        helper.assertTrue(lair.equals(boss.lair()) && boss.lairRadius() == 40, "covil: " + boss.lair());
        boss.setHealth(600.0F);
        helper.runAfterDelay(2, () -> {
            CompoundTag tag = new CompoundTag();
            boss.addAdditionalSaveData(tag);
            TitanovenatorBoss loaded = (TitanovenatorBoss) ModEntities.TITANOVENATOR.get().create(helper.getLevel());
            loaded.readAdditionalSaveData(tag);
            helper.assertTrue(lair.equals(loaded.lair()) && loaded.lairRadius() == 40, "o covil não foi para o save");
            boss.discard();
            helper.succeed();
        });
    }

    /** Puxado para fora do covil, larga tudo e volta andando para perto dele. */
    @GameTest(template = ARENA, batch = "titan_leash", timeoutTicks = 300, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aBossPulledAwayWalksBackToItsLair(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        BlockPos lair = helper.absolutePos(LAIR);
        TitanovenatorBoss boss = TitanovenatorBoss.summon(helper.getLevel(), lair, 10);
        helper.assertTrue(boss != null, "summon não criou o boss");
        Vec3 away = helper.absoluteVec(new Vec3(25.0, 0.0, 12.5));
        boss.teleportTo(away.x, away.y, away.z);
        helper.assertTrue(boss.distanceToLairSqr() > 10 * 10, "o boss não ficou fora do covil");
        double home = ReturnToLairGoal.homeDistance(10) + 0.5;
        helper.succeedWhen(() -> {
            helper.assertTrue(boss.isAlive(), "o boss sumiu");
            helper.assertTrue(boss.distanceToLairSqr() <= home * home,
                    "o boss não voltou ao covil: " + Math.sqrt(boss.distanceToLairSqr()));
            boss.discard();
        });
    }

    /** Vencido, deixa carne, ossos e dentes serrilhados. */
    @GameTest(template = ARENA, batch = "titan_defeat", timeoutTicks = 80, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aDefeatedBossDropsMeatBonesAndTeeth(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        TitanovenatorBoss boss = boss(helper, 8, 0, 8);
        ServerPlayer killer = PredatorTests.survivalPlayer(helper);
        boss.hurt(helper.getLevel().damageSources().playerAttack(killer), 100_000.0F);
        helper.assertTrue(boss.isDeadOrDying(), "o boss deveria ter caído");
        helper.runAfterDelay(5, () -> {
            AABB around = boss.getBoundingBox().inflate(10);
            List<ItemStack> drops = new ArrayList<>();
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(item -> drops.add(item.getItem()));
            int teeth = drops.stream().filter(s -> s.is(ModItems.SERRATED_TOOTH.get())).mapToInt(ItemStack::getCount).sum();
            int bones = drops.stream().filter(s -> s.is(net.minecraft.world.item.Items.BONE)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(teeth >= 4, "dentes serrilhados no chão: " + teeth);
            helper.assertTrue(bones >= 4, "ossos no chão: " + bones);
            helper.succeed();
        });
    }

    /** A barra de boss aparece para quem está perto do covil e some para quem está longe. */
    @GameTest(template = EMPTY, batch = "titan_bar", timeoutTicks = 80)
    public static void theBossBarShowsOnlyNearTheLair(GameTestHelper helper) {
        BlockPos lair = helper.absolutePos(new BlockPos(4, 1, 4));
        TitanovenatorBoss boss = TitanovenatorBoss.summon(helper.getLevel(), lair, 20);
        helper.assertTrue(boss != null, "summon não criou o boss");
        ServerPlayer near = PredatorTests.survivalPlayer(helper);
        ServerPlayer far = PredatorTests.survivalPlayer(helper);
        near.setPos(lair.getX() + 3.0, lair.getY(), lair.getZ());
        far.setPos(lair.getX() + 200.0, lair.getY(), lair.getZ());
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(boss.showsBossBarTo(near), "quem está no covil não vê a barra");
            helper.assertFalse(boss.showsBossBarTo(far), "quem está longe vê a barra");
            boss.discard();
            helper.succeed();
        });
    }

    /** Toda animação que o boss pede (as de base, as três versões por fase e os gestos) existe no arquivo próprio. */
    @GameTest(template = EMPTY)
    public static void everyAnimationTheBossAsksForExists(GameTestHelper helper) {
        JsonObject animations = resource("/assets/iceagesurvival/animations/entity/titanovenator.animation.json")
                .getAsJsonObject("animations");
        String prefix = "animation.titanovenator.";
        List<String> missing = new ArrayList<>();
        for (String phase : List.of("", "_f2", "_f3")) {
            for (String name : List.of("idle", "walk", "run", "attack", "attack_2", "speak")) {
                if (!animations.has(prefix + name + phase)) {
                    missing.add(name + phase);
                }
            }
        }
        for (String name : List.of("unconscious", "eat", "call", "roar_phase")) {
            if (!animations.has(prefix + name)) {
                missing.add(name);
            }
        }
        helper.assertTrue(missing.isEmpty(), "animações que faltam: " + missing);
        // Os gestos de uma vez não podem repetir para sempre.
        for (String name : List.of("attack", "attack_2_f2", "speak_f3", "roar_phase", "eat", "call")) {
            helper.assertFalse(animations.getAsJsonObject(prefix + name).get("loop").getAsBoolean(),
                    name + " está marcada como loop");
        }
        helper.succeed();
    }
}
