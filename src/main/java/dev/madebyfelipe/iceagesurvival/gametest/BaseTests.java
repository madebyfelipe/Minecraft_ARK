package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.authlib.GameProfile;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.world.phys.AABB;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.outpost.ContainmentCoreBlockEntity;
import dev.madebyfelipe.iceagesurvival.outpost.Outposts;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.world.DinoFileData;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * A base militar (D52): o template traz o núcleo, os quatro geradores, oito terminais da série da base e os baús com
 * o saque da base; a base é uma por mundo; o campo de êxtase segura uma criatura (sem IA, invulnerável, imune a
 * tranquilizante) enquanto há gerador de pé e solta quando cai o último; a morte do que foi preso destrava o dossiê
 * para quem está perto. Escritos a partir da especificação.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BaseTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    @GameTest(template = EMPTY)
    public static void baseTemplateHasCoreGeneratorsTerminalsAndLoot(GameTestHelper helper) {
        StructureTemplate template = helper.getLevel().getStructureManager()
                .get(IceAgeSurvival.id("military_base")).orElse(null);
        helper.assertTrue(template != null, "falta o template da base");
        StructurePlaceSettings plain = new StructurePlaceSettings();
        int cores = template.filterBlocks(BlockPos.ZERO, plain, Outposts.CONTAINMENT_CORE.get()).size();
        helper.assertTrue(cores == 1, "a base devia ter 1 núcleo, tem " + cores);
        int generators = template.filterBlocks(BlockPos.ZERO, plain, Outposts.STASIS_GENERATOR.get()).size();
        helper.assertTrue(generators == 4, "a base devia ter 4 geradores, tem " + generators);
        List<StructureTemplate.StructureBlockInfo> terminals = template.filterBlocks(BlockPos.ZERO, plain,
                Outposts.MILITARY_TERMINAL.get());
        helper.assertTrue(terminals.size() == 8, "a base devia ter 8 terminais, tem " + terminals.size());
        for (StructureTemplate.StructureBlockInfo terminal : terminals) {
            helper.assertTrue(terminal.nbt() != null && Manual.BASE.equals(terminal.nbt().getString("Series")),
                    "terminal da base fora da série da base em " + terminal.pos() + ": " + terminal.nbt());
        }
        List<StructureTemplate.StructureBlockInfo> chests = template.filterBlocks(BlockPos.ZERO, plain, Blocks.CHEST);
        helper.assertFalse(chests.isEmpty(), "a base não tem baú");
        for (StructureTemplate.StructureBlockInfo chest : chests) {
            helper.assertTrue(chest.nbt() != null
                            && "iceagesurvival:chests/military_base".equals(chest.nbt().getString("LootTable")),
                    "baú da base sem o saque da base em " + chest.pos());
        }
        helper.assertTrue(helper.getLevel().getServer().getLootData()
                .getLootTable(IceAgeSurvival.id("chests/military_base")) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                "falta a tabela de saque da base");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void thereIsOneBasePerWorld(GameTestHelper helper) {
        StructureSet set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(IceAgeSurvival.id("military_base"));
        helper.assertTrue(set != null, "falta o structure_set military_base");
        helper.assertTrue(set.structures().stream().anyMatch(entry -> entry.structure().is(Outposts.BASE_STRUCTURE)),
                "o conjunto não espalha a base");
        helper.assertTrue(set.placement() instanceof ConcentricRingsStructurePlacement rings && rings.count() == 1,
                "devia haver uma base por mundo: " + set.placement());
        helper.succeed();
    }

    // Batch próprio: o núcleo faz nascer o Titanovenator, grande demais para dividir a arena com os vizinhos.
    @GameTest(template = ARENA, batch = "base_containment", timeoutTicks = 200)
    public static void stasisFieldHoldsACreatureUntilTheLastGeneratorFalls(GameTestHelper helper) {
        BlockPos core = new BlockPos(12, 1, 12);
        BlockPos first = new BlockPos(4, 1, 4);
        BlockPos second = new BlockPos(20, 1, 20);
        helper.setBlock(core, Outposts.CONTAINMENT_CORE.get());
        helper.setBlock(first, Outposts.STASIS_GENERATOR.get());
        helper.setBlock(second, Outposts.STASIS_GENERATOR.get());
        LandCreature held = helper.spawn(ModEntities.SMILODON.get(), new Vec3(14.5, 1, 12.5));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(held.isNoAi() && held.isInvulnerable(), "a criatura no campo devia estar parada e"
                    + " invulnerável: noAi=" + held.isNoAi() + " invulnerável=" + held.isInvulnerable());
            helper.assertTrue(held.getPersistentData().getBoolean(ContainmentCoreBlockEntity.CONTAINED_TAG),
                    "a criatura presa não ficou marcada");
            double before = held.torpor();
            held.addTorpor(1000.0);
            helper.assertTrue(held.torpor() == before, "o tranquilizante agiu dentro do campo");
            ContainmentCoreBlockEntity entity = (ContainmentCoreBlockEntity) helper.getBlockEntity(core);
            helper.assertTrue(entity.generatorCount() == 2, "o núcleo achou " + entity.generatorCount() + " geradores");
            List<TitanovenatorBoss> bosses = helper.getLevel().getEntitiesOfClass(TitanovenatorBoss.class,
                    new AABB(helper.absolutePos(core)).inflate(8));
            helper.assertTrue(bosses.size() == 1, "o núcleo devia fazer nascer um Titanovenator, achei " + bosses.size());
            TitanovenatorBoss boss = bosses.get(0);
            helper.assertTrue(helper.absolutePos(core.above()).equals(boss.lair()), "o covil devia ser o núcleo: "
                    + boss.lair());
            helper.assertTrue(boss.isNoAi() && boss.isInvulnerable(), "o Titanovenator devia estar preso no campo");
            helper.setBlock(first, Blocks.AIR);
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(held.isNoAi() && held.isInvulnerable(), "com um gerador de pé o campo devia segurar");
            helper.setBlock(second, Blocks.AIR);
        });
        helper.runAfterDelay(75, () -> {
            ContainmentCoreBlockEntity entity = (ContainmentCoreBlockEntity) helper.getBlockEntity(core);
            helper.assertTrue(entity.released(), "sem geradores o campo devia cair");
            helper.assertFalse(held.isNoAi() || held.isInvulnerable(), "caído o campo, a criatura devia acordar");
            held.discard();
            helper.getLevel().getEntitiesOfClass(TitanovenatorBoss.class, new AABB(helper.absolutePos(core)).inflate(32))
                    .forEach(boss -> {
                        helper.assertFalse(boss.isNoAi() || boss.isInvulnerable(), "caído o campo, o boss devia acordar");
                        boss.discard();
                    });
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void deathOfTheContainedUnlocksTheDossierForWhoIsNear(GameTestHelper helper) {
        ServerPlayer near = serverPlayer(helper, "biologo");
        LandCreature specimen = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        try {
            near.moveTo(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
            helper.assertTrue(DinoFileData.records(near, Manual.DOSSIER) == 0, "o dossiê já estava destravado");
            specimen.getPersistentData().putBoolean(ContainmentCoreBlockEntity.CONTAINED_TAG, true);
            specimen.kill();
            helper.assertTrue(DinoFileData.records(near, Manual.DOSSIER) == 1,
                    "a morte do espécime não destravou o dossiê: " + DinoFileData.records(near, Manual.DOSSIER));
            LandCreature common = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
            common.kill();
            helper.assertTrue(DinoFileData.records(near, Manual.DOSSIER) == 1, "o dossiê subiu de novo");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(near);
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
}
