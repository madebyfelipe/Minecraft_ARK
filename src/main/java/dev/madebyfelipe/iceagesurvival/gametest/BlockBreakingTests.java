package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.defense.DefenseBlocks;
import dev.madebyfelipe.iceagesurvival.defense.DefenseDamage;
import dev.madebyfelipe.iceagesurvival.entity.BlockBreaking;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BodyProfile;
import dev.madebyfelipe.iceagesurvival.species.BodyProfile.Breaks;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Quebra de blocos pela fauna (D44): quem quebra o quê ({@code body.breaks}), os golpes na madeira (menos nos
 * gigantes, {@code body.giant}), o tronco que cai ao esbarrar, o filhote que não quebra, o {@code mobGriefing} e a
 * pedra que nunca cede.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BlockBreakingTests {
    /** Molde 3 × 3 × 3 com o chão de pedra em y = 1: tudo vai em y = 2. */
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    private static BodyProfile body(GameTestHelper helper, EntityType<?> type) {
        return Species.of(helper.getLevel().registryAccess(), type).flatMap(Species::body).orElse(BodyProfile.DEFAULT);
    }

    /** {@code n} golpes seguidos (um por tick, como golpes de verdade em ticks diferentes). */
    private static void strike(GameTestHelper helper, PrehistoricCreature creature, BlockPos relative, int times,
                               Runnable after) {
        for (int tick = 1; tick <= times; tick++) {
            helper.runAtTickTime(tick, () -> DefenseDamage.hit(creature, helper.absolutePos(relative),
                    helper.getBlockState(relative)));
        }
        helper.runAtTickTime(times + 1, after);
    }

    // ---- Os dados ----

    /** A tabela do Felipe: madeira, só plantas ou nada, e os seis gigantes. */
    @GameTest(template = EMPTY)
    public static void everySpeciesBreaksWhatTheTableSays(GameTestHelper helper) {
        Map<Supplier<EntityType<LandCreature>>, Breaks> expected = new LinkedHashMap<>();
        for (var wood : java.util.List.of(ModEntities.TYRANNOSAURUS, ModEntities.SPINOSAURUS, ModEntities.BRONTOSAURUS,
                ModEntities.ALLOSAURUS, ModEntities.TRICERATOPS, ModEntities.MAMMOTH, ModEntities.STEGOSAURUS,
                ModEntities.ELASMOTHERIUM, ModEntities.BARYONYX)) {
            expected.put(wood, Breaks.WOOD);
        }
        for (var plants : java.util.List.of(ModEntities.SMILODON, ModEntities.DIREBEAR, ModEntities.GALLIMIMUS,
                ModEntities.KELENKEN)) {
            expected.put(plants, Breaks.PLANTS);
        }
        for (var none : java.util.List.of(ModEntities.VELOCIRAPTOR, ModEntities.UTAHRAPTOR, ModEntities.ORNITHOLESTES,
                ModEntities.DODO, ModEntities.PTERANODON)) {
            expected.put(none, Breaks.NONE);
        }
        expected.forEach((type, breaks) -> helper.assertTrue(body(helper, type.get()).breakLevel() == breaks,
                type.get().getDescriptionId() + " quebra " + body(helper, type.get()).breakLevel() + ", deveria " + breaks));
        helper.assertTrue(body(helper, ModEntities.QUETZALCOATLUS.get()).breakLevel() == Breaks.NONE,
                "o Quetzalcoatlus não deveria quebrar nada");

        var giants = java.util.Set.of(ModEntities.TYRANNOSAURUS.get(), ModEntities.SPINOSAURUS.get(),
                ModEntities.BRONTOSAURUS.get(), ModEntities.TRICERATOPS.get(), ModEntities.MAMMOTH.get(),
                ModEntities.STEGOSAURUS.get());
        for (var type : expected.keySet()) {
            helper.assertTrue(body(helper, type.get()).giant() == giants.contains(type.get()),
                    type.get().getDescriptionId() + (giants.contains(type.get()) ? " deveria" : " não deveria")
                            + " ser gigante");
            if (giants.contains(type.get())) {
                helper.assertTrue(body(helper, type.get()).plowHardness() >= 2.0F,
                        type.get().getDescriptionId() + " é gigante e não derruba tronco esbarrando");
            }
        }
        helper.succeed();
    }

    // ---- Golpes ----

    /** T-Rex (gigante): a cerca de madeira cai em 2 golpes, o muro de madeira em 3. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void tyrannosaurusBreaksAFenceInTwoHitsAndAWallInThree(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        BlockPos fence = new BlockPos(0, 2, 0);
        BlockPos wall = new BlockPos(2, 2, 2);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        helper.setBlock(wall, DefenseBlocks.WOOD_WALL.get());
        helper.runAtTickTime(1, () -> {
            hitBoth(helper, rex, fence, wall);
            helper.assertBlockPresent(Blocks.OAK_FENCE, fence);
        });
        helper.runAtTickTime(2, () -> {
            hitBoth(helper, rex, fence, wall);
            helper.assertBlockNotPresent(Blocks.OAK_FENCE, fence);
            helper.assertBlockPresent(DefenseBlocks.WOOD_WALL.get(), wall);
        });
        helper.runAtTickTime(3, () -> {
            DefenseDamage.hit(rex, helper.absolutePos(wall), helper.getBlockState(wall));
            helper.assertBlockNotPresent(DefenseBlocks.WOOD_WALL.get(), wall);
            helper.succeed();
        });
    }

    private static void hitBoth(GameTestHelper helper, PrehistoricCreature creature, BlockPos first, BlockPos second) {
        helper.assertTrue(DefenseDamage.hit(creature, helper.absolutePos(first), helper.getBlockState(first)),
                "o golpe em " + helper.getBlockState(first) + " deveria contar");
        helper.assertTrue(DefenseDamage.hit(creature, helper.absolutePos(second), helper.getBlockState(second)),
                "o golpe em " + helper.getBlockState(second) + " deveria contar");
    }

    /** Alossauro (madeira, não gigante): a tábua cai no terceiro golpe, não antes. */
    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void allosaurusBreaksPlanksInThreeHits(GameTestHelper helper) {
        LandCreature allosaurus = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 1, 2, 1);
        BlockPos planks = new BlockPos(0, 2, 0);
        helper.setBlock(planks, Blocks.OAK_PLANKS);
        strike(helper, allosaurus, planks, BlockBreaking.WOOD_HITS - 1, () -> {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, planks);
            DefenseDamage.hit(allosaurus, helper.absolutePos(planks), helper.getBlockState(planks));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, planks);
            helper.succeed();
        });
    }

    /** O T-Rex selvagem atrás do jogador, cercado por cerca tripla de madeira (o degrau dele passa por cima de uma), golpeia a cerca até abrir. */
    @GameTest(template = ARENA, batch = "breaking_wild_rex", timeoutTicks = 600,
            setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void wildTyrannosaurusBreaksTheFenceToReachThePlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ring(helper, 14, 18, 3, Blocks.OAK_FENCE.defaultBlockState());
        Player player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(16.5, 0, 16.5)));
        LandCreature rex = helper.spawn(ModEntities.TYRANNOSAURUS.get(), 8, 0, 16);
        rex.setTarget(player);
        helper.onEachTick(() -> {
            if (rex.getTarget() == null && player.isAlive()) {
                rex.setTarget(player);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(ringOpened(helper, 14, 18, 3, Blocks.OAK_FENCE),
                "o T-Rex ainda não abriu a cerca"));
    }

    /** O Smilodon (só plantas) atrás do jogador abre a copa no caminho; tábua ele não golpeia. */
    @GameTest(template = ARENA, batch = "breaking_wild_smilodon", timeoutTicks = 600,
            setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void wildSmilodonOpensTheLeavesToReachThePlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ring(helper, 14, 18, 3, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        Player player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(16.5, 0, 16.5)));
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), 8, 0, 16);
        smilodon.setTarget(player);
        helper.onEachTick(() -> {
            if (smilodon.getTarget() == null && player.isAlive()) {
                smilodon.setTarget(player);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(ringOpened(helper, 14, 18, 3, Blocks.OAK_LEAVES),
                "o Smilodon ainda não abriu a copa"));
    }

    @GameTest(template = EMPTY)
    public static void smilodonDoesNotStrikePlanks(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        BlockPos planks = new BlockPos(0, 2, 0);
        helper.setBlock(planks, Blocks.OAK_PLANKS);
        helper.assertFalse(DefenseDamage.hit(smilodon, helper.absolutePos(planks), helper.getBlockState(planks)),
                "o golpe do Smilodon na tábua não deveria contar");
        helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(planks)) == 0, "a tábua levou golpe");
        helper.assertTrue(body(helper, ModEntities.SMILODON.get()).breaksPlants(), "o Smilodon deveria abrir folhas");
        helper.succeed();
    }

    /** O raptor não quebra nada: nem madeira a golpes, nem folha ao esbarrar. */
    @GameTest(template = EMPTY)
    public static void raptorBreaksNothing(GameTestHelper helper) {
        LandCreature raptor = helper.spawnWithNoFreeWill(ModEntities.VELOCIRAPTOR.get(), 1, 2, 1);
        BlockPos fence = new BlockPos(0, 2, 0);
        BlockPos leaves = new BlockPos(1, 2, 2);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        helper.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        helper.assertFalse(DefenseDamage.hit(raptor, helper.absolutePos(fence), helper.getBlockState(fence)),
                "o golpe do raptor na cerca não deveria contar");
        BodyProfile body = body(helper, ModEntities.VELOCIRAPTOR.get());
        raptor.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)).add(0, 0, 0.6));
        BlockBreaking.bumpThrough(raptor, body.plowHardness(), body.breaksPlants());
        helper.assertBlockPresent(Blocks.OAK_FENCE, fence);
        helper.assertBlockPresent(Blocks.OAK_LEAVES, leaves);
        helper.assertFalse(BlockBreaking.breaksWood(raptor), "raptor não derruba madeira");
        helper.succeed();
    }

    /** Filhote não quebra: nem golpe na madeira, nem tronco ao esbarrar; o gigante adulto derruba o tronco. */
    @GameTest(template = EMPTY)
    public static void babyBreaksNothingButTheAdultGiantKnocksTheLog(GameTestHelper helper) {
        LandCreature baby = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        baby.setBaby(true);
        BlockPos fence = new BlockPos(0, 2, 0);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        helper.assertFalse(DefenseDamage.hit(baby, helper.absolutePos(fence), helper.getBlockState(fence)),
                "o golpe do filhote não deveria contar");
        helper.assertFalse(BlockBreaking.breaksWood(baby), "filhote não derruba madeira");

        BodyProfile body = body(helper, ModEntities.TYRANNOSAURUS.get());
        BlockPos log = new BlockPos(1, 2, 2);
        helper.setBlock(log, Blocks.OAK_LOG);
        baby.setYRot(0.0F);
        baby.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
        BlockBreaking.bumpThrough(baby, body.plowHardness(), body.breaksPlants());
        helper.assertBlockPresent(Blocks.OAK_LOG, log);

        LandCreature allosaurus = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 1, 2, 1);
        allosaurus.setYRot(0.0F);
        BodyProfile allosaurusBody = body(helper, ModEntities.ALLOSAURUS.get());
        BlockBreaking.bumpThrough(allosaurus, allosaurusBody.plowHardness(), allosaurusBody.breaksPlants());
        helper.assertBlockPresent(Blocks.OAK_LOG, log);

        LandCreature adult = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        adult.setYRot(0.0F);
        BlockBreaking.bumpThrough(adult, body.plowHardness(), body.breaksPlants());
        helper.assertBlockNotPresent(Blocks.OAK_LOG, log);
        helper.assertBlockPresent(Blocks.OAK_FENCE, fence);
        helper.succeed();
    }

    /** Sem {@code mobGriefing} nada quebra: nem golpe, nem esbarrão, nem mordida montada. */
    @GameTest(template = EMPTY, batch = "breaking_no_griefing", timeoutTicks = 20)
    public static void nothingBreaksWithoutMobGriefing(GameTestHelper helper) {
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean before = rule.get();
        rule.set(false, helper.getLevel().getServer());
        try {
            LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
            rex.setYRot(0.0F);
            BlockPos fence = new BlockPos(0, 2, 0);
            BlockPos log = new BlockPos(1, 2, 2);
            helper.setBlock(fence, Blocks.OAK_FENCE);
            helper.setBlock(log, Blocks.OAK_LOG);
            for (int i = 0; i < 4; i++) {
                helper.assertFalse(DefenseDamage.hit(rex, helper.absolutePos(fence), helper.getBlockState(fence)),
                        "sem mobGriefing o golpe não deveria contar");
            }
            BodyProfile body = body(helper, ModEntities.TYRANNOSAURUS.get());
            BlockBreaking.bumpThrough(rex, body.plowHardness(), body.breaksPlants());
            helper.assertBlockPresent(Blocks.OAK_FENCE, fence);
            helper.assertBlockPresent(Blocks.OAK_LOG, log);
            helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(fence)) == 0, "a cerca levou golpe");
        } finally {
            rule.set(before, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /** Pedra nunca cede: nem ao golpe do gigante, nem ao esbarrão. */
    @GameTest(template = EMPTY)
    public static void stoneNeverBreaks(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        rex.setYRot(0.0F);
        for (Block stone : java.util.List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.STONE_BRICK_WALL,
                DefenseBlocks.STONE_WALL.get())) {
            BlockPos pos = new BlockPos(1, 2, 2);
            helper.setBlock(pos, stone);
            BlockState state = helper.getBlockState(pos);
            helper.assertFalse(BlockBreaking.isWood(state), stone + " não é madeira");
            for (int i = 0; i < 4; i++) {
                helper.assertFalse(DefenseDamage.hit(rex, helper.absolutePos(pos), state), "o golpe em " + stone + " contou");
            }
            BodyProfile body = body(helper, ModEntities.TYRANNOSAURUS.get());
            BlockBreaking.bumpThrough(rex, body.plowHardness(), body.breaksPlants());
            helper.assertBlockPresent(stone, pos);
        }
        helper.succeed();
    }

    /** As nossas defesas de muro e portão estão na tag {@code fossil:unbreakable} do Revival. */
    @GameTest(template = EMPTY)
    public static void ourWallsAreUnbreakableForTheRevival(GameTestHelper helper) {
        var unbreakable = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                new net.minecraft.resources.ResourceLocation("fossil", "unbreakable"));
        for (Block block : java.util.List.of(DefenseBlocks.WOOD_WALL.get(), DefenseBlocks.STONE_WALL.get(),
                DefenseBlocks.WOOD_GATE.get(), DefenseBlocks.STONE_GATE.get(), DefenseBlocks.LARGE_WOOD_GATE.get(),
                DefenseBlocks.LARGE_STONE_GATE.get())) {
            helper.assertTrue(block.defaultBlockState().is(unbreakable), block + " fora de #fossil:unbreakable");
        }
        helper.succeed();
    }

    /** Anel de {@code height} blocos de altura de {@code from} a {@code to} (x e z), no chão da arena. */
    private static void ring(GameTestHelper helper, int from, int to, int height, BlockState state) {
        for (int x = from; x <= to; x++) {
            for (int z = from; z <= to; z++) {
                if (x == from || x == to || z == from || z == to) {
                    for (int y = 0; y < height; y++) {
                        helper.setBlock(x, y, z, state);
                    }
                }
            }
        }
    }

    /** Algum bloco do anel, até {@code height} de altura, saiu. */
    private static boolean ringOpened(GameTestHelper helper, int from, int to, int height, Block block) {
        for (int x = from; x <= to; x++) {
            for (int z = from; z <= to; z++) {
                if (x == from || x == to || z == from || z == to) {
                    for (int y = 0; y < height; y++) {
                        if (!helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
