package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.authlib.GameProfile;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.world.CreatureCall;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Ordem de locomover (tecla B) e o botão Chamar do menu da tecla O (MC-21). */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class OrderTests {
    private static final String ARENA = "arena";

    private static LandCreature tamed(LandCreature creature, Player owner) {
        creature.tame(owner);
        creature.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        return creature;
    }

    /** Locomover: a criatura anda até o bloco mirado e fica lá, em Ficar. */
    @GameTest(template = ARENA, batch = "order_move", timeoutTicks = 300)
    public static void moveOrderWalksToTheSpotAndStays(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), 4, 0, 4);
        Player owner = helper.makeMockSurvivalPlayer();
        owner.setPos(smilodon.position());
        tamed(smilodon, owner);
        BlockPos destination = helper.absolutePos(new BlockPos(14, 0, 4));

        helper.assertTrue(CreatureCommands.orderMove(owner, destination) == 1, "a criatura não aceitou a ordem");
        helper.assertTrue(smilodon.movement() == Movement.STAY && destination.equals(smilodon.moveOrder()),
                "ordem não aplicada: " + smilodon.movement() + " " + smilodon.moveOrder());
        Vec3 target = Vec3.atBottomCenterOf(destination);
        helper.succeedWhen(() -> {
            helper.assertTrue(smilodon.position().distanceTo(target) < 3.0,
                    "ainda longe do destino: " + smilodon.position().distanceTo(target));
            helper.assertTrue(smilodon.moveOrder() == null, "chegou mas a ordem continua");
            helper.assertTrue(smilodon.movement() == Movement.STAY, "não ficou no lugar ao chegar");
        });
    }

    /** Destino além do alcance da mira, estranho e assobio que cancela. */
    @GameTest(template = ARENA, batch = "order_move_rules")
    public static void moveOrderChecksRangeAndOwner(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 0, 4);
        Player owner = helper.makeMockSurvivalPlayer();
        owner.setPos(smilodon.position());
        Player stranger = helper.makeMockSurvivalPlayer();
        stranger.setPos(smilodon.position());
        tamed(smilodon, owner);

        BlockPos far = BlockPos.containing(owner.position().add(CreatureCommands.TARGET_RANGE + 10, 0, 0));
        helper.assertTrue(CreatureCommands.orderMove(owner, far) == 0, "aceitou destino longe demais");
        BlockPos near = helper.absolutePos(new BlockPos(10, 0, 4));
        helper.assertTrue(CreatureCommands.orderMove(stranger, near) == 0, "estranho mandou a criatura andar");
        helper.assertTrue(smilodon.moveOrder() == null, "ordem de estranho aplicada");

        helper.assertTrue(CreatureCommands.orderMove(owner, near) == 1, "dono não conseguiu mandar");
        smilodon.setMovement(Movement.FOLLOW);
        helper.assertTrue(smilodon.moveOrder() == null, "outra ordem de movimento não cancelou o destino");
        helper.succeed();
    }

    /** Chamar: a criatura do dono aparece ao lado dele; a de outro não vem. */
    @GameTest(template = ARENA, batch = "order_call")
    public static void callBringsTheCreatureNextToItsOwner(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ServerPlayer owner = serverPlayer(helper, "chamador");
        ServerPlayer stranger = serverPlayer(helper, "estranho");
        Vec3 here = helper.absoluteVec(new Vec3(4.5, 0, 4.5));
        owner.setPos(here);
        stranger.setPos(here);
        LandCreature smilodon = tamed(helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 19, 0, 19), owner);
        Vec3 before = smilodon.position();

        helper.assertFalse(CreatureCall.bring(stranger, smilodon), "a criatura atendeu um estranho");
        helper.assertTrue(smilodon.position().equals(before), "a criatura se mexeu para o estranho");
        helper.assertTrue(CreatureCall.bring(owner, smilodon), "a criatura não veio");
        helper.assertTrue(smilodon.position().distanceTo(owner.position()) < 4.0,
                "longe do dono: " + smilodon.position().distanceTo(owner.position()));
        helper.assertTrue(helper.getLevel().noCollision(smilodon), "apareceu dentro de um bloco");
        helper.succeed();
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper, String name) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name)) {
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
}
