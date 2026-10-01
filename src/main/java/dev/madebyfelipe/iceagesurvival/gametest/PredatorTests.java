package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Predadores selvagens de verdade: com IA, contra um jogador em sobrevivência. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class PredatorTests {
    private static final String ARENA = "arena";
    private static final long STARVING = 20L * 3600;

    /** Como {@code makeMockServerPlayerInLevel}, mas em sobrevivência: o do vanilla é criativo e nenhum mob o ataca. */
    static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-survivor")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void huntsThePlayer(GameTestHelper helper, EntityType<LandCreature> type) {
        huntsThePlayer(helper, type, false);
    }

    /** @param alreadyHunting o predador já tem o jogador como alvo (a mata tapa a visão para escolhê-lo) */
    private static void huntsThePlayer(GameTestHelper helper, EntityType<LandCreature> type, boolean alreadyHunting) {
        LandCreature predator = helper.spawn(type, 6, 0, 4);
        predator.setTicksSinceMeal(STARVING);
        Player player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 14.5)));
        if (alreadyHunting) {
            predator.setTarget(player);
        }
        float start = player.getHealth();
        helper.onEachTick(() -> {
            if (player.getHealth() < start) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(390, () -> helper.fail(type.getDescriptionId() + " não feriu o jogador; alvo "
                + predator.getTarget() + ", distância " + predator.distanceTo(player)
                + ", nav parada " + predator.getNavigation().isDone()
                + ", dificuldade " + helper.getLevel().getDifficulty()
                + ", metas " + predator.targetSelector.getAvailableGoals().stream()
                        .map(g -> g.getGoal().getClass().getSimpleName() + (g.isRunning() ? "*" : "")).toList()
                + ", pode atacar " + predator.canAttack(player) + ", inimigo " + player.canBeSeenAsEnemy()
                + ", follow " + predator.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
                + ", visto " + predator.getSensing().hasLineOfSight(player)));
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void tyrannosaurusHuntsThePlayer(GameTestHelper helper) {
        huntsThePlayer(helper, ModEntities.TYRANNOSAURUS.get());
    }

    /** Mata fechada entre o T-Rex e o jogador: troncos soltos, sem passagem de 3 blocos de largura. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void tyrannosaurusHuntsThroughTheForest(GameTestHelper helper) {
        for (int x = 0; x < 24; x += 2) {
            for (int y = 0; y < 4; y++) {
                helper.setBlock(x, y, 9, net.minecraft.world.level.block.Blocks.OAK_LOG);
            }
            helper.setBlock(x + 1, 0, 11, net.minecraft.world.level.block.Blocks.STONE);
        }
        huntsThePlayer(helper, ModEntities.TYRANNOSAURUS.get(), true);
    }

    private static final net.minecraft.tags.TagKey<EntityType<?>> REX_PREY = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE, IceAgeSurvival.id("tyrannosaurus_prey"));
    private static final net.minecraft.tags.TagKey<EntityType<?>> WOLF_PREY = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE, IceAgeSurvival.id("dire_wolf_prey"));

    @GameTest(template = ARENA, batch = "rex_prey")
    public static void rexOnlyHuntsABrontosaurusOutOfItsHerd(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 2, 0, 2);
        rex.setTicksSinceMeal(STARVING);
        LandCreature bronto = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 16, 0, 16);
        helper.assertTrue(HuntGoal.wouldHunt(rex, bronto, REX_PREY), "bronto desgarrado deveria ser presa");
        helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 18, 0, 18);
        helper.assertFalse(HuntGoal.wouldHunt(rex, bronto, REX_PREY), "bronto na manada não deveria ser presa do T-Rex");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wolfPackHuntsAMammothTogether(GameTestHelper helper) {
        LandCreature first = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 2, 0, 2);
        LandCreature second = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 4, 0, 2);
        LandCreature mammoth = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 14, 0, 14);
        helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 16, 0, 14);
        helper.assertTrue(HuntGoal.isPrey(first, mammoth, WOLF_PREY), "mamute é presa da matilha");
        first.setTarget(mammoth);
        first.rallyPack(mammoth);
        helper.assertTrue(second.getTarget() == mammoth, "o resto da matilha não partiu atrás da presa");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void predatorsLeaveTamedCreaturesAlone(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 2, 0, 2);
        LandCreature mammoth = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 14, 0, 14);
        mammoth.tame(helper.makeMockPlayer());
        helper.assertFalse(HuntGoal.isPrey(rex, mammoth, REX_PREY), "T-Rex caçando mamute domesticado");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void velociraptorHuntsThePlayer(GameTestHelper helper) {
        huntsThePlayer(helper, ModEntities.VELOCIRAPTOR.get());
    }

    /** Um carnívoro saciado não caça o jogador, mas um faminto o considera presa como qualquer animal. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void carnivoreOnlyHuntsPlayerWhenHungry(GameTestHelper helper) {
        LandCreature predator = helper.spawn(ModEntities.VELOCIRAPTOR.get(), 6, 0, 4);
        var prey = predator.behavior().orElseThrow().prey().orElseThrow();
        Player player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 12.5)));

        predator.setTicksSinceMeal(0);
        helper.assertFalse(HuntGoal.wouldHunt(predator, player, prey), "Velociraptor saciado escolheu o jogador");
        helper.runAtTickTime(30, () -> helper.assertTrue(predator.getTarget() != player,
                "Velociraptor saciado perseguiu o jogador"));

        helper.runAtTickTime(35, () -> {
            predator.setTicksSinceMeal(STARVING);
            helper.assertTrue(HuntGoal.wouldHunt(predator, player, prey),
                    "Velociraptor faminto não considerou o jogador como presa");
        });
        helper.onEachTick(() -> {
            if (predator.getTarget() == player) {
                helper.succeed();
            }
        });
    }

    /** Sem fome não começa a caçada, mas continua se defendendo quando o jogador o fere. */
    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void carnivoreRetaliatesAgainstPlayerWithoutHuntingThem(GameTestHelper helper) {
        LandCreature predator = helper.spawn(ModEntities.VELOCIRAPTOR.get(), 6, 0, 4);
        predator.setTicksSinceMeal(0);
        ServerPlayer player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 8.0)));
        predator.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(predator.getTarget() == player, "não revidou ao jogador que o feriu");
            helper.assertFalse(predator.isHunting(), "revide foi tratado como caçada por fome");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void allosaurusHuntsThePlayer(GameTestHelper helper) {
        huntsThePlayer(helper, ModEntities.ALLOSAURUS.get());
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void direbearHuntsThePlayer(GameTestHelper helper) {
        huntsThePlayer(helper, ModEntities.DIREBEAR.get());
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void spinosaurusHuntsThePlayer(GameTestHelper helper) {
        huntsThePlayer(helper, ModEntities.SPINOSAURUS.get());
    }

    /**
     * O Brontossauro é pacífico: não vai atrás do jogador. Lote próprio: solto na arena comum, ele
     * encarava os predadores das cenas vizinhas e os tirava do jogador.
     */
    @GameTest(template = ARENA, batch = "bronto_player", timeoutTicks = 200)
    public static void brontosaurusLeavesThePlayerAlone(GameTestHelper helper) {
        LandCreature bronto = helper.spawn(ModEntities.BRONTOSAURUS.get(), 6, 0, 4);
        Player player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 14.5)));
        helper.runAtTickTime(150, () -> {
            helper.assertTrue(bronto.getTarget() == null, "brontossauro escolheu o jogador como alvo");
            helper.succeed();
        });
    }

    /** Velocidade de corrida do jogador, em blocos por segundo. */
    private static final double PLAYER_SPRINT = 5.612;

    // Batch próprio: na arena compartilhada, bichos e jogadores dos testes vizinhos viravam alvo
    // do T-Rex no meio da medida.
    @GameTest(template = ARENA, timeoutTicks = 100, batch = "rex_speed")
    public static void tyrannosaurusOutrunsASprintingPlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = helper.spawn(ModEntities.TYRANNOSAURUS.get(), 6, 0, 2);
        Player player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 22.5)));
        rex.setTarget(player);
        Vec3[] start = new Vec3[1];
        helper.runAtTickTime(15, () -> start[0] = rex.position());
        helper.runAtTickTime(35, () -> {
            double speed = rex.position().subtract(start[0]).horizontalDistance();
            helper.assertTrue(speed > PLAYER_SPRINT, String.format("T-Rex corre %.2f blocos/s; o jogador corre %.2f",
                    speed, PLAYER_SPRINT));
            helper.succeed();
        });
    }

    /** Jogador de costas: o Smilodon espreita, devagar, e só dá o bote quando chega perto. */
    @GameTest(template = ARENA, timeoutTicks = 400, batch = "smilodon_stalk")
    public static void smilodonStalksAPlayerWhoIsNotLooking(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), 6, 0, 2);
        Player player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 20.5)));
        // Olha sempre para o lado oposto ao Smilodon.
        helper.onEachTick(() -> player.lookAt(EntityAnchorArgument.Anchor.EYES,
                player.getEyePosition().scale(2.0).subtract(smilodon.getEyePosition())));
        smilodon.setTarget(player);
        helper.runAtTickTime(20, () -> helper.assertTrue(smilodon.isStalking(), "deveria estar espreitando"));
        helper.onEachTick(() -> {
            if (player.getHealth() < player.getMaxHealth()) {
                helper.assertFalse(smilodon.isStalking(), "mordeu ainda espreitando");
                helper.succeed();
            }
        });
    }

    /** Jogador olhando para ele: acaba a espreita e vem o bote. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void smilodonPouncesWhenSeen(GameTestHelper helper) {
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), 6, 0, 2);
        Player player = survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(6.5, 0, 20.5)));
        helper.onEachTick(() -> player.lookAt(EntityAnchorArgument.Anchor.EYES, smilodon.getEyePosition()));
        smilodon.setTarget(player);
        helper.runAtTickTime(10, () -> {
            helper.assertFalse(smilodon.isStalking(), "visto, deveria ter partido para o ataque");
            helper.assertTrue(smilodon.getTarget() == player, "perdeu o alvo");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void smilodonHuntsThePlayer(GameTestHelper helper) {
        huntsThePlayer(helper, ModEntities.SMILODON.get());
    }
}
