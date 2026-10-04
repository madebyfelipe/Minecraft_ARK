package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.authlib.GameProfile;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import dev.madebyfelipe.iceagesurvival.defense.DefenseBlocks;
import dev.madebyfelipe.iceagesurvival.item.AnalyzerItem;
import dev.madebyfelipe.iceagesurvival.network.TerminalReadPayload;
import dev.madebyfelipe.iceagesurvival.outpost.MilitaryTerminalBlockEntity;
import dev.madebyfelipe.iceagesurvival.outpost.OutpostPiece;
import dev.madebyfelipe.iceagesurvival.outpost.Outposts;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.world.DinoFileData;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Os postos militares (D52): o terminal destrava o próximo registro uma vez por pessoa e por terminal, o Analisador
 * mira o terminal, o template da torre traz terminal, baú com saque, muro e portão grande, o terminal fica de frente
 * para o portão em qualquer rotação, o baú às vezes tem rifle e dardos, e o posto está no worldgen espalhado pelo
 * mundo. Escritos a partir da especificação.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class OutpostTests {
    private static final ResourceLocation LOOT = IceAgeSurvival.id("chests/military_outpost");
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
            helper.assertTrue(DinoFileData.records(first, Manual.POSTS) == 0, "pessoa nova já tem registros: "
                    + DinoFileData.records(first, Manual.POSTS));

            helper.assertTrue(AnalyzerItem.completeTerminal(first, helper.absolutePos(one)),
                    "a primeira leitura do terminal não destravou registro");
            helper.assertTrue(DinoFileData.records(first, Manual.POSTS) == 1, "depois de um terminal devia ter 1 registro, tem "
                    + DinoFileData.records(first, Manual.POSTS));
            helper.assertFalse(AnalyzerItem.completeTerminal(first, helper.absolutePos(one)),
                    "ler de novo o mesmo terminal destravou outro registro");
            helper.assertTrue(DinoFileData.records(first, Manual.POSTS) == 1, "a releitura mudou a contagem: "
                    + DinoFileData.records(first, Manual.POSTS));

            helper.assertTrue(AnalyzerItem.completeTerminal(first, helper.absolutePos(two)),
                    "um terminal novo não destravou o próximo registro");
            helper.assertTrue(DinoFileData.records(first, Manual.POSTS) == 2, "depois de dois terminais devia ter 2, tem "
                    + DinoFileData.records(first, Manual.POSTS));

            helper.assertTrue(AnalyzerItem.completeTerminal(second, helper.absolutePos(one)),
                    "o terminal já lido por outra pessoa não deu registro à segunda");
            helper.assertTrue(DinoFileData.records(second, Manual.POSTS) == 1, "a segunda pessoa devia ter 1 registro, tem "
                    + DinoFileData.records(second, Manual.POSTS));
            helper.assertTrue(DinoFileData.records(first, Manual.POSTS) == 2, "a leitura de outra pessoa mexeu na contagem da primeira");

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

    @GameTest(template = EMPTY)
    public static void towerTemplateHasTheTerminalChestWallAndGate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager().get(OutpostPiece.TOWER).orElse(null);
        helper.assertTrue(template != null, "falta o template do posto: " + OutpostPiece.TOWER);
        helper.assertTrue(template.getSize().getX() == OutpostPiece.TOWER_SIZE && template.getSize().getZ() == OutpostPiece.TOWER_SIZE,
                "o posto devia ter " + OutpostPiece.TOWER_SIZE + " de lado: " + template.getSize());
        StructurePlaceSettings plain = new StructurePlaceSettings();
        List<StructureTemplate.StructureBlockInfo> terminals = template.filterBlocks(BlockPos.ZERO, plain,
                Outposts.MILITARY_TERMINAL.get());
        helper.assertTrue(terminals.size() == 1, "o posto devia ter 1 terminal, tem " + terminals.size());
        List<StructureTemplate.StructureBlockInfo> chests = template.filterBlocks(BlockPos.ZERO, plain, Blocks.CHEST);
        helper.assertTrue(chests.size() == 1, "o posto devia ter 1 baú, tem " + chests.size());
        CompoundTag chest = chests.get(0).nbt();
        helper.assertTrue(chest != null && LOOT.toString().equals(chest.getString("LootTable")),
                "o baú do posto não usa a tabela de saque " + LOOT + ": " + chest);
        int gate = template.filterBlocks(BlockPos.ZERO, plain, DefenseBlocks.LARGE_STONE_GATE.get()).size();
        helper.assertTrue(gate == 25, "o portão grande devia ter 25 partes, tem " + gate);
        List<StructureTemplate.StructureBlockInfo> walls = template.filterBlocks(BlockPos.ZERO, plain,
                DefenseBlocks.STONE_WALL.get());
        helper.assertFalse(walls.isEmpty(), "o posto não tem muro de pedra");
        for (StructureTemplate.StructureBlockInfo wall : walls) {
            int x = wall.pos().getX();
            int z = wall.pos().getZ();
            int last = OutpostPiece.TOWER_SIZE - 1;
            helper.assertTrue(x == 0 || z == 0 || x == last || z == last, "muro fora do perímetro em " + wall.pos());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void towerTerminalFacesTheGateInEveryRotation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplateManager templates = level.getStructureManager();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (Rotation rotation : Rotation.values()) {
            OutpostPiece piece = new OutpostPiece(templates, OutpostPiece.TOWER, origin, rotation);
            StructureTemplate template = templates.getOrCreate(OutpostPiece.TOWER);
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation)
                    .setRotationPivot(new BlockPos(OutpostPiece.TOWER_SIZE / 2, 0, OutpostPiece.TOWER_SIZE / 2));
            StructureTemplate.StructureBlockInfo terminal = template.filterBlocks(origin, settings,
                    Outposts.MILITARY_TERMINAL.get()).get(0);
            BlockPos expected = piece.worldPos(new BlockPos(9, 1, 10));
            helper.assertTrue(terminal.pos().equals(expected),
                    rotation + ": o terminal está em " + terminal.pos() + ", a peça diz " + expected);
            Direction facing = terminal.state().getValue(HorizontalDirectionalBlock.FACING);
            BlockPos gate = piece.worldPos(new BlockPos(7, 1, 0));
            int along = (gate.getX() - terminal.pos().getX()) * facing.getStepX()
                    + (gate.getZ() - terminal.pos().getZ()) * facing.getStepZ();
            helper.assertTrue(along > 0, rotation + ": a tela do terminal aponta para " + facing
                    + ", de costas para o portão em " + gate);
            helper.assertTrue(piece.getBoundingBox().isInside(gate) && piece.getBoundingBox().isInside(terminal.pos()),
                    rotation + ": terminal ou portão fora da caixa da peça " + piece.getBoundingBox());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void complexTemplateHasATerminalLootChestsAndOnlyOurBlocks(GameTestHelper helper) {
        StructureTemplate template = helper.getLevel().getStructureManager().get(OutpostPiece.COMPLEX).orElse(null);
        helper.assertTrue(template != null, "falta o template do complexo: " + OutpostPiece.COMPLEX);
        StructurePlaceSettings plain = new StructurePlaceSettings();
        int terminals = template.filterBlocks(BlockPos.ZERO, plain, Outposts.MILITARY_TERMINAL.get()).size();
        helper.assertTrue(terminals == 1, "o complexo devia ter 1 terminal, tem " + terminals);
        List<StructureTemplate.StructureBlockInfo> chests = template.filterBlocks(BlockPos.ZERO, plain, Blocks.CHEST);
        helper.assertFalse(chests.isEmpty(), "o complexo não tem baú");
        // O baú do térreo, junto do terminal, é o de comando: o saque dos postos com o Receptor de Sinal garantido.
        for (StructureTemplate.StructureBlockInfo chest : chests) {
            String table = chest.nbt() == null ? "" : chest.nbt().getString("LootTable");
            helper.assertTrue(LOOT.toString().equals(table) || "iceagesurvival:chests/military_outpost_command".equals(table),
                    "baú do complexo sem o saque dos postos em " + chest.pos() + ": " + chest.nbt());
        }
        CompoundTag saved = template.save(new CompoundTag());
        for (Tag entry : saved.getList("palette", Tag.TAG_COMPOUND)) {
            String name = ((CompoundTag) entry).getString("Name");
            helper.assertTrue(name.startsWith("minecraft:") || name.startsWith(IceAgeSurvival.MODID + ":"),
                    "bloco de outro mod no complexo: " + name);
        }
        String text = saved.toString();
        helper.assertFalse(text.contains("UNSC") || text.contains("Hive") || text.contains("lootr"),
                "sobrou texto ou dado da build original no complexo");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void bothOutpostVariantsAreInTheWorldgenSet(GameTestHelper helper) {
        StructureSet set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(IceAgeSurvival.id("military_outposts"));
        helper.assertTrue(set != null, "falta o structure_set military_outposts");
        for (var key : List.of(Outposts.OUTPOST_STRUCTURE, Outposts.OUTPOST_COMPLEX_STRUCTURE)) {
            helper.assertTrue(set.structures().stream().anyMatch(entry -> entry.structure().is(key)),
                    "o conjunto não espalha " + key.location());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void outpostChestSometimesHasARifleAndDarts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().getLootData().getLootTable(LOOT);
        helper.assertTrue(table != LootTable.EMPTY, "falta a tabela de saque " + LOOT);
        int rifles = 0;
        int darts = 0;
        for (int roll = 0; roll < 400; roll++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
                    Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO))).create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.is(ModItems.TRANQ_RIFLE.get())) {
                    rifles++;
                    helper.assertTrue(stack.isDamaged(), "o rifle do posto devia vir gasto");
                } else if (stack.is(ModItems.TRANQ_DART.get())) {
                    darts++;
                    helper.assertTrue(stack.getCount() >= 4 && stack.getCount() <= 12,
                            "dardos fora de 4 a 12: " + stack.getCount());
                }
            }
        }
        // 25% e 60% de 400: longe de zero e de tudo.
        helper.assertTrue(rifles > 50 && rifles < 150, "rifles em 400 baús: " + rifles + " (esperado ~100)");
        helper.assertTrue(darts > 180 && darts < 300, "dardos em 400 baús: " + darts + " (esperado ~240)");
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
        for (TerminalReadPayload message : new TerminalReadPayload[] {new TerminalReadPayload(Manual.POSTS, 3, true),
                new TerminalReadPayload(Manual.BASE, 0, false), new TerminalReadPayload(Manual.DOSSIER, 300, false)}) {
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
