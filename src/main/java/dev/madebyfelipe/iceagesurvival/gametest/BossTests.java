package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.effect.BleedingEffect;
import dev.madebyfelipe.iceagesurvival.endgame.ArenaAltarBlock;
import dev.madebyfelipe.iceagesurvival.endgame.BossPhase;
import dev.madebyfelipe.iceagesurvival.endgame.ReturnToAltarGoal;
import dev.madebyfelipe.iceagesurvival.entity.GiganotosaurusBoss;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O boss da arena (D47): o tributo no altar, as fases e a resistência, a mordida que faz sangrar, a imunidade ao
 * torpor, o troféu e os dentes, a espada serrilhada e a volta ao altar. Cada teste na arena tem o próprio lote: um
 * boss solto atacaria as criaturas dos testes vizinhos.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BossTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final BlockPos ALTAR = new BlockPos(12, 0, 12);

    private static GiganotosaurusBoss boss(GameTestHelper helper, int x, int y, int z) {
        return (GiganotosaurusBoss) helper.spawnWithNoFreeWill(ModEntities.GIGANOTOSAURUS.get(), x, y, z);
    }

    private static BlockPos placeAltar(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBlocks.ARENA_ALTAR.get());
        return helper.absolutePos(relative);
    }

    private static void offer(GameTestHelper helper, Player player, BlockPos altar) {
        BlockState state = helper.getLevel().getBlockState(altar);
        state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(altar), Direction.UP, altar, false));
    }

    private static List<GiganotosaurusBoss> bossesOf(GameTestHelper helper, BlockPos altar) {
        return helper.getLevel().getEntitiesOfClass(GiganotosaurusBoss.class, new AABB(altar).inflate(64),
                boss -> boss.isAlive() && altar.equals(boss.altar()));
    }

    /** Some com os bosses que sobraram: persistentes, ficariam atacando os testes seguintes. */
    private static void dismiss(GameTestHelper helper, BlockPos around) {
        helper.getLevel().getEntitiesOfClass(GiganotosaurusBoss.class, new AABB(around).inflate(96), boss -> true)
                .forEach(GiganotosaurusBoss::discard);
    }

    /** A cabeça de um apex no altar chama um boss, um só: com ele vivo (ou ainda rugindo), outro tributo é recusado. */
    @GameTest(template = ARENA, batch = "boss_tribute", timeoutTicks = ArenaAltarBlock.SUMMON_DELAY_TICKS + 60,
            setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aTributeOnTheAltarCallsOneBoss(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        BlockPos altar = placeAltar(helper, ALTAR);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
        offer(helper, player, altar);
        helper.assertFalse(helper.getLevel().getBlockState(altar).getValue(ArenaAltarBlock.SUMMONING),
                "um diamante não é tributo");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TYRANNOSAURUS_HEAD.get()));
        offer(helper, player, altar);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "a cabeça de T-Rex não foi consumida");
        helper.assertTrue(helper.getLevel().getBlockState(altar).getValue(ArenaAltarBlock.SUMMONING),
                "o altar não começou a chamar");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPINOSAURUS_HEAD.get()));
        offer(helper, player, altar);
        helper.assertFalse(player.getMainHandItem().isEmpty(), "durante o rugido, o altar aceitou outro tributo");

        helper.runAfterDelay(ArenaAltarBlock.SUMMON_DELAY_TICKS + 5, () -> {
            List<GiganotosaurusBoss> bosses = bossesOf(helper, altar);
            helper.assertTrue(bosses.size() == 1, "deveria haver um boss ligado ao altar: " + bosses.size());
            helper.assertFalse(helper.getLevel().getBlockState(altar).getValue(ArenaAltarBlock.SUMMONING),
                    "o altar ficou chamando");
            offer(helper, player, altar);
            helper.assertFalse(player.getMainHandItem().isEmpty(), "com o boss vivo, o altar aceitou outro tributo");
            helper.assertTrue(bossesOf(helper, altar).size() == 1, "o segundo tributo chamou outro boss");
            dismiss(helper, altar);
            helper.succeed();
        });
    }

    /** 1024 de vida; o golpe de 100 tira 100 na fase 1, 70 na 2 e 50 na 3. Ao passar para a fase 2, ruge. */
    @GameTest(template = EMPTY, batch = "boss_phases")
    public static void phasesFollowHealthAndResistanceGrows(GameTestHelper helper) {
        GiganotosaurusBoss boss = boss(helper, 1, 2, 1);
        var damage = helper.getLevel().damageSources().magic();
        helper.assertTrue(boss.getMaxHealth() == 1024.0F, "vida máxima: " + boss.getMaxHealth());
        helper.assertTrue(boss.phase() == BossPhase.ONE, "começa na fase 1");

        boss.hurt(damage, 100.0F);
        helper.assertTrue(Math.abs(boss.getHealth() - 924.0F) < 0.01F, "fase 1 deveria tomar 100: " + boss.getHealth());

        boss.setHealth(500.0F);
        helper.assertTrue(boss.phase() == BossPhase.TWO, "abaixo de 50% deveria estar na fase 2");
        boss.invulnerableTime = 0;
        boss.hurt(damage, 100.0F);
        helper.assertTrue(Math.abs(boss.getHealth() - 430.0F) < 0.01F, "fase 2 deveria tomar 70: " + boss.getHealth());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(boss.isRoaring(), "na passagem para a fase 2, o boss deveria rugir");
            boss.setHealth(250.0F);
            helper.assertTrue(boss.phase() == BossPhase.THREE, "abaixo de 25% deveria estar na fase 3");
            boss.invulnerableTime = 0;
            boss.hurt(damage, 100.0F);
            helper.assertTrue(Math.abs(boss.getHealth() - 200.0F) < 0.01F,
                    "fase 3 deveria tomar 50: " + boss.getHealth());
            boss.discard();
            helper.succeed();
        });
    }

    /** A mordida corta: sangramento que empilha, mais fundo na fase 3, e que tira vida sozinho. */
    @GameTest(template = EMPTY, batch = "boss_bite")
    public static void theBiteMakesTheVictimBleed(GameTestHelper helper) {
        GiganotosaurusBoss boss = boss(helper, 1, 2, 1);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 1, 2, 1);
        var bleeding = ModEffects.BLEEDING.get();

        helper.assertTrue(boss.doHurtTarget(golem), "a mordida não acertou");
        MobEffectInstance first = golem.getEffect(bleeding);
        helper.assertTrue(first != null && first.getAmplifier() == 0, "a primeira mordida deveria abrir 1 nível");

        golem.invulnerableTime = 0;
        boss.doHurtTarget(golem);
        helper.assertTrue(golem.getEffect(bleeding).getAmplifier() == 1, "a segunda mordida deveria empilhar");

        boss.setHealth(200.0F); // fase 3: corta dois níveis de uma vez
        golem.invulnerableTime = 0;
        boss.doHurtTarget(golem);
        helper.assertTrue(golem.getEffect(bleeding).getAmplifier() == 3,
                "na fase 3 a mordida deveria somar dois níveis: " + golem.getEffect(bleeding).getAmplifier());
        helper.assertFalse(boss.hasEffect(bleeding), "o boss não sangra");

        float health = golem.getHealth();
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(golem.getHealth() < health, "o sangramento não tirou vida");
            boss.discard();
            golem.discard();
            helper.succeed();
        });
    }

    /** Nem o dardo nem o torpor direto derrubam o boss. */
    @GameTest(template = ARENA, batch = "boss_tranq", timeoutTicks = 80, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void torporDoesNotKnockTheBossOut(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        GiganotosaurusBoss boss = boss(helper, 8, 0, 8);
        boss.addTorpor(1.0E6, helper.makeMockSurvivalPlayer());
        boss.setTorpor(1.0E6);
        helper.assertFalse(boss.isUnconscious(), "o torpor direto derrubou o boss");
        Vec3 from = boss.position().add(6, 2, 0);
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

    /** Vencido, deixa a cabeça e os dentes serrilhados; o altar aceita outro tributo e chama um boss novo. */
    @GameTest(template = ARENA, batch = "boss_defeat", timeoutTicks = ArenaAltarBlock.SUMMON_DELAY_TICKS + 80,
            setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aDefeatedBossDropsItsHeadAndTheAltarCallsAgain(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        BlockPos altar = placeAltar(helper, ALTAR);
        GiganotosaurusBoss boss = ArenaAltarBlock.summon(helper.getLevel(), altar);
        helper.assertTrue(boss != null && altar.equals(boss.altar()), "o altar não chamou o boss");
        ServerPlayer killer = PredatorTests.survivalPlayer(helper);
        boss.hurt(helper.getLevel().damageSources().playerAttack(killer), 100_000.0F);
        helper.assertTrue(boss.isDeadOrDying(), "o boss deveria ter caído");

        helper.runAfterDelay(5, () -> {
            AABB around = boss.getBoundingBox().inflate(8);
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around,
                    item -> item.getItem().is(ModItems.GIGANOTOSAURUS_HEAD.get())).isEmpty(), "sem a cabeça no chão");
            int teeth = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around,
                    item -> item.getItem().is(ModItems.SERRATED_TOOTH.get())).stream()
                    .mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(teeth >= 3, "dentes serrilhados no chão: " + teeth);

            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPINOSAURUS_HEAD.get()));
            offer(helper, player, altar);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "com o boss vencido, o altar recusou o tributo");
            helper.runAfterDelay(ArenaAltarBlock.SUMMON_DELAY_TICKS + 5, () -> {
                helper.assertTrue(bossesOf(helper, altar).size() == 1, "o altar não chamou o boss de novo");
                dismiss(helper, altar);
                helper.succeed();
            });
        });
    }

    /** O golpe da espada serrilhada abre um sangramento curto; o da espada de diamante comum, não. */
    @GameTest(template = EMPTY, batch = "boss_sword")
    public static void theSerratedSwordMakesTheVictimBleed(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(IceAgeSurvival.id("serrated_sword")).isPresent(),
                "sem receita da espada serrilhada");
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        Pig plain = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        Pig cut = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        var bleeding = ModEffects.BLEEDING.get();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        plain.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertFalse(plain.hasEffect(bleeding), "a espada de diamante comum fez sangrar");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SERRATED_SWORD.get()));
        cut.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        MobEffectInstance effect = cut.getEffect(bleeding);
        helper.assertTrue(effect != null && effect.getAmplifier() == 0
                && effect.getDuration() <= BleedingEffect.SWORD_TICKS, "a espada serrilhada não fez sangrar");
        plain.discard();
        cut.discard();
        player.discard();
        helper.succeed();
    }

    /** Puxado para fora da arena, larga tudo e volta andando para perto do altar. */
    @GameTest(template = ARENA, batch = "boss_leash", timeoutTicks = 260, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aBossPulledAwayWalksBackToTheAltar(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        BlockPos altar = placeAltar(helper, ALTAR);
        GiganotosaurusBoss boss = ArenaAltarBlock.summon(helper.getLevel(), altar);
        helper.assertTrue(boss != null, "o altar não chamou o boss");
        boss.setArenaRadius(8);
        Vec3 away = helper.absoluteVec(new Vec3(23.0, 0.0, 12.5));
        boss.teleportTo(away.x, away.y, away.z);
        helper.assertTrue(boss.distanceToAltarSqr() > 8 * 8, "o boss não ficou fora da arena");
        double home = ReturnToAltarGoal.homeDistance(8) + 0.5;
        helper.succeedWhen(() -> {
            helper.assertTrue(boss.isAlive(), "o boss sumiu");
            helper.assertTrue(boss.distanceToAltarSqr() <= home * home,
                    "o boss não voltou ao altar: " + Math.sqrt(boss.distanceToAltarSqr()));
            dismiss(helper, altar);
        });
    }

    /** Arrastado para longe demais (duas vezes o raio), reaparece ao lado do altar. */
    @GameTest(template = ARENA, batch = "boss_leash_far", timeoutTicks = 60, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aBossDraggedFarReappearsAtTheAltar(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        BlockPos altar = placeAltar(helper, new BlockPos(4, 0, 4));
        GiganotosaurusBoss boss = ArenaAltarBlock.summon(helper.getLevel(), altar);
        helper.assertTrue(boss != null, "o altar não chamou o boss");
        boss.setArenaRadius(8);
        Vec3 away = helper.absoluteVec(new Vec3(21.5, 0.0, 21.5));
        boss.teleportTo(away.x, away.y, away.z);
        helper.assertTrue(boss.distanceToAltarSqr() > 16 * 16, "o boss não ficou longe o bastante");
        double home = ReturnToAltarGoal.homeDistance(8) + 0.5;
        helper.succeedWhen(() -> {
            helper.assertTrue(boss.distanceToAltarSqr() <= home * home,
                    "o boss não reapareceu no altar: " + Math.sqrt(boss.distanceToAltarSqr()));
            dismiss(helper, altar);
        });
    }
}
