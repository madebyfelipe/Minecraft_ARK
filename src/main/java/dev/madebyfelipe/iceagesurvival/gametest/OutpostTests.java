package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.authlib.GameProfile;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.AnalyzerItem;
import dev.madebyfelipe.iceagesurvival.network.TerminalReadPayload;
import dev.madebyfelipe.iceagesurvival.outpost.MilitaryTerminalBlockEntity;
import dev.madebyfelipe.iceagesurvival.outpost.OutpostPiece;
import dev.madebyfelipe.iceagesurvival.outpost.Outposts;
import dev.madebyfelipe.iceagesurvival.world.DinoFileData;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Os postos militares (D51): o terminal destrava o próximo registro uma vez por pessoa e por terminal, o Analisador
 * mira o terminal, a peça do posto monta a sala com o terminal virado para a porta em qualquer orientação, e o posto
 * está no worldgen espalhado pelo mundo. Escritos a partir da especificação.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class OutpostTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    @GameTest(template = EMPTY)
    public static void eachTerminalGivesOneRecordPerPerson(GameTestHelper helper) {
        ServerPlayer first = serverPlayer(helper, "sentinela-a");
        ServerPlayer second = serverPlayer(helper, "sentinela-b");
        BlockPos one = new BlockPos(0, 1, 0);
        BlockPos two = new BlockPos(2, 1, 0);
        try {
            helper.setBlock(one, Outposts.MILITARY_TERMINAL.get());
            helper.setBlock(two, Outposts.MILITARY_TERMINAL.get());
            helper.assertTrue(helper.getBlockEntity(one) instanceof MilitaryTerminalBlockEntity,
                    "o terminal não tem o bloco-entidade de leitura");
            helper.assertTrue(DinoFileData.records(first) == 0, "pessoa nova já tem registros: "
                    + DinoFileData.records(first));

            helper.assertTrue(AnalyzerItem.completeTerminal(first, helper.absolutePos(one)),
                    "a primeira leitura do terminal não destravou registro");
            helper.assertTrue(DinoFileData.records(first) == 1, "depois de um terminal devia ter 1 registro, tem "
                    + DinoFileData.records(first));
            helper.assertFalse(AnalyzerItem.completeTerminal(first, helper.absolutePos(one)),
                    "ler de novo o mesmo terminal destravou outro registro");
            helper.assertTrue(DinoFileData.records(first) == 1, "a releitura mudou a contagem: "
                    + DinoFileData.records(first));

            helper.assertTrue(AnalyzerItem.completeTerminal(first, helper.absolutePos(two)),
                    "um terminal novo não destravou o próximo registro");
            helper.assertTrue(DinoFileData.records(first) == 2, "depois de dois terminais devia ter 2, tem "
                    + DinoFileData.records(first));

            helper.assertTrue(AnalyzerItem.completeTerminal(second, helper.absolutePos(one)),
                    "o terminal já lido por outra pessoa não deu registro à segunda");
            helper.assertTrue(DinoFileData.records(second) == 1, "a segunda pessoa devia ter 1 registro, tem "
                    + DinoFileData.records(second));
            helper.assertTrue(DinoFileData.records(first) == 2, "a leitura de outra pessoa mexeu na contagem da primeira");

            helper.assertFalse(AnalyzerItem.completeTerminal(first, helper.absolutePos(new BlockPos(4, 1, 0))),
                    "um bloco que não é terminal destravou registro");
        } finally {
            logOut(helper, first);
            logOut(helper, second);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void analyzerAimsAtATerminalButNotThroughAWall(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "sentinela");
        BlockPos terminal = new BlockPos(12, 1, 12);
        BlockPos wall = new BlockPos(8, 1, 12);
        try {
            helper.setBlock(terminal, Outposts.MILITARY_TERMINAL.get());
            player.moveTo(helper.absoluteVec(new Vec3(3.5, 0, 12.5)));
            player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(helper.absolutePos(terminal)));
            helper.assertTrue(helper.absolutePos(terminal).equals(AnalyzerItem.aimedTerminal(player)),
                    "o terminal na mira não foi achado: " + AnalyzerItem.aimedTerminal(player));
            helper.assertTrue(AnalyzerItem.aimed(player) == null, "o terminal foi lido como criatura");

            helper.setBlock(wall, Blocks.STONE);
            helper.setBlock(wall.above(), Blocks.STONE);
            helper.assertTrue(AnalyzerItem.aimedTerminal(player) == null,
                    "achou o terminal através de uma parede de pedra");

            helper.setBlock(wall, Blocks.AIR);
            helper.setBlock(wall.above(), Blocks.AIR);
            helper.setBlock(terminal, Blocks.STONE);
            helper.assertTrue(AnalyzerItem.aimedTerminal(player) == null, "pedra comum foi lida como terminal");
        } finally {
            logOut(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void outpostPieceBuildsTheTerminalFacingTheDoorInEveryOrientation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        BlockPos[] origins = {new BlockPos(1, 1, 1), new BlockPos(13, 1, 1), new BlockPos(1, 1, 13),
                new BlockPos(13, 1, 13)};
        for (int i = 0; i < facings.length; i++) {
            OutpostPiece piece = new OutpostPiece(helper.absolutePos(origins[i]), facings[i]);
            piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                    level.getRandom(), piece.getBoundingBox(), new ChunkPos(helper.absolutePos(origins[i])),
                    helper.absolutePos(origins[i]));
            BlockPos terminal = piece.terminalPos();
            BlockState state = level.getBlockState(terminal);
            helper.assertTrue(state.is(Outposts.MILITARY_TERMINAL.get()),
                    "posto virado para " + facings[i] + " sem terminal em " + terminal + ": " + state);
            helper.assertTrue(level.getBlockEntity(terminal) instanceof MilitaryTerminalBlockEntity,
                    "o terminal do posto (" + facings[i] + ") não tem bloco-entidade");

            BlockPos door = piece.doorPos();
            helper.assertTrue(level.getBlockState(door).isAir() && level.getBlockState(door.above()).isAir(),
                    "a porta do posto (" + facings[i] + ") não está aberta");
            Direction screen = state.getValue(HorizontalDirectionalBlock.FACING);
            BlockPos ahead = terminal.relative(screen, OutpostPiece.SIZE - 2);
            helper.assertTrue(ahead.getX() == door.getX() && ahead.getZ() == door.getZ(),
                    "a tela do terminal (" + facings[i] + ") aponta para " + screen + ", não para a porta em " + door);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void outpostsAreSpreadAcrossTheWorld(GameTestHelper helper) {
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet set = sets.get(IceAgeSurvival.id("military_outposts"));
        helper.assertTrue(set != null, "falta o structure_set military_outposts");
        helper.assertTrue(set.structures().stream().anyMatch(entry -> entry.structure().is(Outposts.OUTPOST_STRUCTURE)),
                "o conjunto não espalha o posto militar");
        helper.assertTrue(set.placement() instanceof RandomSpreadStructurePlacement spread && spread.spacing() == 40
                        && spread.separation() == 20,
                "o posto devia se espalhar a cada 40 chunks (separação 20): " + set.placement());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void terminalReadPayloadSurvivesEncodeAndDecode(GameTestHelper helper) {
        for (TerminalReadPayload message : new TerminalReadPayload[] {new TerminalReadPayload(3, true),
                new TerminalReadPayload(0, false), new TerminalReadPayload(300, false)}) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                TerminalReadPayload.encode(message, buf);
                TerminalReadPayload back = TerminalReadPayload.decode(buf);
                helper.assertTrue(message.equals(back), "ida e volta mudou o pacote: " + message + " → " + back);
                helper.assertTrue(buf.readableBytes() == 0, "sobraram bytes depois de ler " + message);
            } finally {
                buf.release();
            }
        }
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

    private static void logOut(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}
