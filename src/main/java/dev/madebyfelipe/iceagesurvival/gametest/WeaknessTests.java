package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.authlib.GameProfile;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.Species;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** As fraquezas dos três caçadores e o porte pelo status. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WeaknessTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final long STARVING = 20L * 3600;

    /** Uma piscina de um bloco de fundo, de x/z 2 a 10, cercada de pedra; fora dela, chão seco. */
    private static void pool(GameTestHelper helper) {
        for (int x = 1; x <= 11; x++) {
            for (int z = 1; z <= 11; z++) {
                boolean rim = x == 1 || x == 11 || z == 1 || z == 11;
                helper.setBlock(x, 1, z, rim ? Blocks.STONE : Blocks.WATER);
            }
        }
    }

    /** O Smilodon selvagem que cai na água sai dela; o domesticado não tem pavor. */
    @GameTest(template = ARENA, batch = "weak_water", timeoutTicks = 300, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aWildSmilodonGetsOutOfTheWater(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        pool(helper);
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), 6, 1, 6);
        helper.assertTrue(smilodon.fearsWater(), "o Smilodon selvagem deveria ter pavor de água");
        helper.runAtTickTime(10, () -> helper.assertTrue(smilodon.isInWater(), "a cena não pôs o Smilodon na água"));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 10, "cedo");
            helper.assertFalse(smilodon.isInWater(), "o Smilodon continua na água");
        });
    }

    /** Presa na água é refúgio contra o Smilodon selvagem: nem a caça nem a ataca; o domesticado ataca. */
    @GameTest(template = ARENA, batch = "weak_water_prey", timeoutTicks = 60, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void preyInTheWaterIsSafeFromTheSmilodon(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        pool(helper);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 14, 1, 6);
        smilodon.setTicksSinceMeal(STARVING);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 6, 1, 6);
        var prey = smilodon.behavior().orElseThrow().prey().orElseThrow();
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(pig.isInWater(), "a cena não pôs o porco na água");
            helper.assertFalse(HuntGoal.isPrey(smilodon, pig, prey), "presa na água não deveria ser caçada");
            helper.assertFalse(smilodon.canAttack(pig), "o Smilodon selvagem não deveria atacar quem está na água");
            smilodon.tame(helper.makeMockSurvivalPlayer());
            helper.assertFalse(smilodon.fearsWater(), "o domesticado não tem pavor de água");
            helper.assertTrue(smilodon.canAttack(pig), "o domesticado ataca na água");
            helper.succeed();
        });
    }

    /** O Utahraptor não quebra folhas: o caminho contorna a copa, e quem sobe nas árvores o despista. */
    @GameTest(template = EMPTY, batch = "weak_leaves")
    public static void theUtahraptorCannotBreakLeaves(GameTestHelper helper) {
        var species = Species.of(helper.getLevel().registryAccess(), ModEntities.UTAHRAPTOR.get()).orElseThrow();
        helper.assertFalse(species.body().orElseThrow().breaksLeaves(), "o Utahraptor não deveria quebrar folhas");
        LandCreature utah = helper.spawnWithNoFreeWill(ModEntities.UTAHRAPTOR.get(), 1, 1, 1);
        helper.assertTrue(utah.getPathfindingMalus(BlockPathTypes.LEAVES) < 0.0F,
                "as folhas deveriam bloquear o caminho do Utahraptor");
        helper.succeed();
    }

    /** Um jogador que defende com o escudo, voltado para quem bate. */
    private static ServerPlayer shieldBearer(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-shield")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public boolean isBlocking() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** A bicada num escudo erguido trava o bico da Kelenken por 2 s; sem escudo, ela acerta normalmente. */
    @GameTest(template = ARENA, batch = "weak_beak", timeoutTicks = HuntSpecials.BEAK_STUCK_TICKS + 40, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aShieldJamsTheKelenkenBeak(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature kelenken = helper.spawnWithNoFreeWill(ModEntities.KELENKEN.get(), 6, 1, 6);
        ServerPlayer player = shieldBearer(helper);
        var spot = helper.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 1, 6.5));
        player.moveTo(spot.x, spot.y, spot.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, kelenken.getEyePosition());
        player.setYHeadRot(player.getYRot());
        float health = player.getHealth();
        helper.assertFalse(kelenken.doHurtTarget(player), "a bicada deveria parar no escudo");
        helper.assertTrue(player.getHealth() == health, "o escudo deixou passar dano");
        helper.assertTrue(kelenken.isBeakStuck(), "o bico deveria travar no escudo");
        helper.assertFalse(kelenken.doHurtTarget(player), "com o bico travado não ataca");
        helper.runAtTickTime(HuntSpecials.BEAK_STUCK_TICKS + 5, () -> {
            helper.assertFalse(kelenken.isBeakStuck(), "o bico deveria destravar depois de 2 s");
            helper.succeed();
        });
    }

    /** Porte pelo status: Kelenken um pouco acima do Utahraptor; Tricerátopo e Estegossauro bem acima. */
    @GameTest(template = EMPTY, batch = "weak_size")
    public static void sizeFollowsStrength(GameTestHelper helper) {
        float utah = ModEntities.UTAHRAPTOR.get().getHeight();
        float kelenken = ModEntities.KELENKEN.get().getHeight();
        helper.assertTrue(kelenken > utah && kelenken < utah * 2.0F, "Kelenken " + kelenken + " contra Utah " + utah);
        for (EntityType<?> big : java.util.List.of(ModEntities.STEGOSAURUS.get(), ModEntities.TRICERATOPS.get())) {
            helper.assertTrue(big.getHeight() > utah * 2.0F, big + " deveria ser bem mais alto que o Utahraptor");
        }
        helper.assertTrue(ModEntities.TRICERATOPS.get().getHeight() > ModEntities.STEGOSAURUS.get().getHeight()
                        || ModEntities.TRICERATOPS.get().getWidth() > ModEntities.STEGOSAURUS.get().getWidth(),
                "o Tricerátopo, mais forte, deveria ser o maior dos dois");
        helper.succeed();
    }
}
