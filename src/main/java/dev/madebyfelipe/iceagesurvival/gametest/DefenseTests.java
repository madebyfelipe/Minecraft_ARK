package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.defense.BearTrapBlock;
import dev.madebyfelipe.iceagesurvival.defense.BearTrapBlockEntity;
import dev.madebyfelipe.iceagesurvival.defense.DefenseBlocks;
import dev.madebyfelipe.iceagesurvival.defense.DefenseDamage;
import dev.madebyfelipe.iceagesurvival.defense.DefenseGateBlock;
import dev.madebyfelipe.iceagesurvival.entity.BlockBreaking;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Blocos de defesa: muro alto, portões, armadilhas e a cobertura de folhagem, e a regra de que só quem tem
 * {@code body.breaks: wood} derruba madeira, por golpes contados (D44; o resto em {@link BlockBreakingTests}).
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class DefenseTests {
    /** Molde 3 × 3 × 3 com o chão de pedra em y = 1 (o molde começa um acima do bloco de estrutura): tudo vai em y = 2. */
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    /** Coloca a defesa com o item, como o jogador faz: olhando para o sul, clicando na posição (vazia). */
    private static void place(GameTestHelper helper, Player player, Block block, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        ItemStack stack = new ItemStack(block.asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setYRot(0.0F);
        InteractionResult result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        helper.assertTrue(result.consumesAction(), "não colocou " + block + ": " + result);
    }

    private static InteractionResult use(GameTestHelper helper, Player player, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return helper.getBlockState(relative).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    private static void touch(GameTestHelper helper, BlockPos relative, LivingEntity entity) {
        helper.getBlockState(relative).entityInside(helper.getLevel(), helper.absolutePos(relative), entity);
    }

    private static boolean hurt(LivingEntity entity) {
        return entity.getHealth() < entity.getMaxHealth();
    }

    private static LandCreature tamedDodo(GameTestHelper helper, Player owner, int x, int z) {
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), x, 2, z);
        dodo.tame(owner);
        return dodo;
    }

    private static double collisionTop(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockState(relative)
                .getCollisionShape(helper.getLevel(), helper.absolutePos(relative), CollisionContext.empty())
                .max(Direction.Axis.Y);
    }

    // ---- Muro alto ----

    /** Colisão de 1,5 no topo, como a cerca, e empilhado também; para os caminhos, intransponível. */
    @GameTest(template = EMPTY)
    public static void wallsCollideOneAndAHalfHigh(GameTestHelper helper) {
        helper.setBlock(1, 2, 1, DefenseBlocks.WOOD_WALL.get());
        helper.setBlock(1, 3, 1, DefenseBlocks.WOOD_WALL.get());
        helper.setBlock(0, 2, 0, DefenseBlocks.STONE_WALL.get());
        helper.assertTrue(Math.abs(collisionTop(helper, new BlockPos(1, 3, 1)) - 1.5) < 1e-6,
                "topo da pilha de madeira: " + collisionTop(helper, new BlockPos(1, 3, 1)));
        helper.assertTrue(Math.abs(collisionTop(helper, new BlockPos(0, 2, 0)) - 1.5) < 1e-6,
                "muro de pedra: " + collisionTop(helper, new BlockPos(0, 2, 0)));
        BlockState wall = helper.getBlockState(new BlockPos(1, 2, 1));
        helper.assertFalse(wall.isPathfindable(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)),
                PathComputationType.LAND), "o muro não deveria ser caminho");
        helper.succeed();
    }

    /** Nenhum bloco de defesa é vegetação que o gigante atravessa quebrando. */
    @GameTest(template = EMPTY)
    public static void noDefenseIsPlowable(GameTestHelper helper) {
        for (Block block : List.of(DefenseBlocks.WOOD_WALL.get(), DefenseBlocks.STONE_WALL.get(),
                DefenseBlocks.WOOD_GATE.get(), DefenseBlocks.STONE_GATE.get(), DefenseBlocks.LARGE_WOOD_GATE.get(),
                DefenseBlocks.LARGE_STONE_GATE.get(), DefenseBlocks.SPIKE_TRAP.get(), DefenseBlocks.THORN_PALISADE.get(),
                DefenseBlocks.BEAR_TRAP.get(), DefenseBlocks.FOLIAGE_COVER.get())) {
            helper.assertFalse(block.defaultBlockState().is(ModTags.PLOWABLE), block + " está em #plowable");
            helper.assertFalse(block instanceof LeavesBlock, block + " é folha (o gigante quebraria esbarrando)");
            helper.assertTrue(DefenseBlocks.isDefense(block.defaultBlockState()), block + " não é defesa");
        }
        helper.succeed();
    }

    // ---- Golpes dos gigantes ----

    /** Três golpes de T-Rex (gigante) derrubam o muro de madeira; o de pedra não cede nunca. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void giantBreaksWoodInThreeHitsButNotStone(GameTestHelper helper) {
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        BlockPos wood = new BlockPos(0, 2, 0);
        BlockPos stone = new BlockPos(2, 2, 2);
        helper.setBlock(wood, DefenseBlocks.WOOD_WALL.get());
        helper.setBlock(stone, DefenseBlocks.STONE_WALL.get());
        for (int tick = 1; tick < BlockBreaking.DEFENSE_HITS_GIANT; tick++) {
            helper.runAtTickTime(tick, () -> {
                helper.assertTrue(DefenseDamage.hit(rex, helper.absolutePos(wood), helper.getBlockState(wood)),
                        "o golpe do T-Rex na madeira deveria contar");
                DefenseDamage.hit(rex, helper.absolutePos(stone), helper.getBlockState(stone));
            });
        }
        helper.runAtTickTime(BlockBreaking.DEFENSE_HITS_GIANT + 1, () -> {
            helper.assertBlockPresent(DefenseBlocks.WOOD_WALL.get(), wood);
            helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)) == BlockBreaking.DEFENSE_HITS_GIANT - 1,
                    "golpes contados: " + DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)));
            DefenseDamage.hit(rex, helper.absolutePos(wood), helper.getBlockState(wood));
            helper.assertBlockNotPresent(DefenseBlocks.WOOD_WALL.get(), wood);
            for (int i = 0; i < 4; i++) {
                DefenseDamage.hit(rex, helper.absolutePos(stone), helper.getBlockState(stone));
            }
            helper.assertBlockPresent(DefenseBlocks.STONE_WALL.get(), stone);
            helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(stone)) == 0, "pedra contou golpe");
            helper.succeed();
        });
    }

    /** Criatura sem {@code body.breaks: wood} não conta golpe nem na madeira. */
    @GameTest(template = EMPTY)
    public static void commonCreatureDoesNotBreakWood(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        BlockPos wood = new BlockPos(0, 2, 0);
        helper.setBlock(wood, DefenseBlocks.WOOD_WALL.get());
        helper.assertFalse(DefenseDamage.hit(smilodon, helper.absolutePos(wood), helper.getBlockState(wood)),
                "o golpe do Smilodon não deveria contar");
        helper.assertBlockPresent(DefenseBlocks.WOOD_WALL.get(), wood);
        helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)) == 0, "Smilodon contou golpe");
        helper.succeed();
    }

    /** Golpes em partes diferentes do mesmo portão somam no portão; no último, ele cai inteiro. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void hitsOnAGateAddUpOnTheWholeGate(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 2, 2, 2);
        BlockPos lower = new BlockPos(0, 2, 0);
        BlockPos upper = lower.above();
        place(helper, owner, DefenseBlocks.WOOD_GATE.get(), lower);
        helper.assertBlockPresent(DefenseBlocks.WOOD_GATE.get(), upper);
        for (int tick = 1; tick <= BlockBreaking.DEFENSE_HITS_GIANT; tick++) {
            BlockPos part = tick % 2 == 0 ? upper : lower;
            int count = tick;
            helper.runAtTickTime(tick, () -> {
                if (count < BlockBreaking.DEFENSE_HITS_GIANT) {
                    // As duas partes na mesma mordida contam um golpe só.
                    DefenseDamage.hit(rex, helper.absolutePos(lower), helper.getBlockState(lower));
                    DefenseDamage.hit(rex, helper.absolutePos(upper), helper.getBlockState(upper));
                    helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(upper)) == count,
                            "golpes no portão: " + DefenseDamage.hits(helper.getLevel(), helper.absolutePos(upper)));
                } else {
                    DefenseDamage.hit(rex, helper.absolutePos(part), helper.getBlockState(part));
                }
            });
        }
        helper.runAtTickTime(BlockBreaking.DEFENSE_HITS_GIANT + 2, () -> {
            helper.assertBlockNotPresent(DefenseBlocks.WOOD_GATE.get(), lower);
            helper.assertBlockNotPresent(DefenseBlocks.WOOD_GATE.get(), upper);
            helper.succeed();
        });
    }

    /** Montaria de cada lado da regra, virada para +z e montada pelo dono. */
    private static LandCreature mounted(GameTestHelper helper, Player owner, EntityType<LandCreature> type) {
        var center = helper.absoluteVec(new Vec3(4, 0, 4));
        helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(center, center).inflate(20.0),
                mob -> true).forEach(net.minecraft.world.entity.Entity::discard);
        LandCreature mount = helper.spawnWithNoFreeWill(type, 4, 0, 4);
        mount.tame(owner);
        mount.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        mount.setSaddled(true);
        mount.setYRot(0.0F);
        mount.yBodyRot = 0.0F;
        owner.setPos(mount.position());
        helper.assertTrue(mount.ride(owner), "deveria montar");
        return mount;
    }

    /** A mordida do T-Rex montado conta um golpe na madeira em vez de quebrar; pedra fica sem golpe. */
    @GameTest(template = EMPTY, batch = "defense_bite_rex", timeoutTicks = 60)
    public static void mountedGiantBiteCountsAHit(GameTestHelper helper) {
        Player owner = PredatorTests.survivalPlayer(helper);
        LandCreature rex = mounted(helper, owner, ModEntities.TYRANNOSAURUS.get());
        BlockPos wood = new BlockPos(4, 2, 6);
        BlockPos stone = new BlockPos(5, 2, 6);
        helper.setBlock(wood, DefenseBlocks.WOOD_WALL.get());
        helper.setBlock(stone, DefenseBlocks.STONE_WALL.get());
        helper.assertTrue(rex.attackAsMount(owner, null), "a mordida deveria sair sem alvo");
        helper.assertBlockPresent(DefenseBlocks.WOOD_WALL.get(), wood);
        helper.assertBlockPresent(DefenseBlocks.STONE_WALL.get(), stone);
        helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)) == 1,
                "a mordida deveria contar um golpe: " + DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)));
        helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(stone)) == 0, "pedra contou golpe");
        helper.runAfterDelay(21, () -> {
            helper.assertTrue(rex.attackAsMount(owner, null), "a recarga deveria ter passado");
            helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)) == 2,
                    "a segunda mordida deveria somar: " + DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)));
            helper.succeed();
        });
    }

    /**
     * O Alossauro montado ({@code break_hardness} sem restrição de tag, {@code body.breaks: wood}, não gigante) quebra
     * a terra à frente mordendo e conta um golpe no muro de madeira, sem derrubá-lo (são seis).
     */
    @GameTest(template = EMPTY, batch = "defense_bite_common")
    public static void mountedAllosaurusBiteDigsAndStrikesTheWall(GameTestHelper helper) {
        Player owner = PredatorTests.survivalPlayer(helper);
        LandCreature allosaurus = mounted(helper, owner, ModEntities.ALLOSAURUS.get());
        BlockPos wood = new BlockPos(4, 1, 6);
        BlockPos dirt = new BlockPos(4, 2, 7);
        helper.setBlock(wood, DefenseBlocks.WOOD_WALL.get());
        helper.setBlock(dirt, net.minecraft.world.level.block.Blocks.DIRT);
        helper.assertTrue(allosaurus.attackAsMount(owner, null), "a mordida deveria sair sem alvo");
        helper.assertBlockNotPresent(net.minecraft.world.level.block.Blocks.DIRT, dirt);
        helper.assertBlockPresent(DefenseBlocks.WOOD_WALL.get(), wood);
        helper.assertTrue(DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)) == 1,
                "o Alossauro deveria contar um golpe: " + DefenseDamage.hits(helper.getLevel(), helper.absolutePos(wood)));
        helper.succeed();
    }

    /** O T-Rex selvagem com o alvo cercado por muro de madeira golpeia o muro até abrir. */
    /**
     * Um muro de um bloco segura também os gigantes: o degrau deles (T-Rex 2,1, Bronto 2,6) passaria por cima da
     * colisão de 1,5, então junto de uma defesa ele cai para 1. Empurrado contra o muro, o T-Rex não sobe.
     */
    @GameTest(template = ARENA, batch = "defense_step")
    public static void giantsDoNotStepOverAOneBlockWall(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        for (int z = 10; z <= 22; z++) {
            helper.setBlock(14, 0, z, DefenseBlocks.STONE_WALL.get());
        }
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 8, 0, 16);
        LandCreature bronto = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 20, 0, 4);
        helper.assertTrue(rex.maxUpStep() > 1.5F, "longe do muro o T-Rex perdeu o degrau: " + rex.maxUpStep());
        helper.runAfterDelay(2, () -> {
            double ground = rex.getY();
            for (int i = 0; i < 40; i++) {
                rex.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.25, 0.0, 0.0));
            }
            helper.assertTrue(rex.maxUpStep() <= 1.0F, "junto do muro o degrau é " + rex.maxUpStep());
            helper.assertTrue(rex.getY() < ground + 0.5, "o T-Rex subiu no muro: y " + rex.getY() + " (chão " + ground + ")");
            helper.assertTrue(rex.getBoundingBox().maxX <= helper.absoluteVec(new Vec3(14, 0, 0)).x + 1.0E-3,
                    "o T-Rex atravessou o muro");
            helper.assertTrue(bronto.maxUpStep() > 2.0F, "o Bronto longe do muro perdeu o degrau: " + bronto.maxUpStep());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "defense_wild_giant", timeoutTicks = 600)
    public static void wildGiantBreaksTheWallToItsTarget(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        for (int x = 13; x <= 19; x++) {
            for (int z = 13; z <= 19; z++) {
                if (x == 13 || x == 19 || z == 13 || z == 19) {
                    helper.setBlock(x, 0, z, DefenseBlocks.WOOD_WALL.get());
                    helper.setBlock(x, 1, z, DefenseBlocks.WOOD_WALL.get());
                }
            }
        }
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 16, 0, 16);
        LandCreature rex = helper.spawn(ModEntities.TYRANNOSAURUS.get(), 10, 0, 16);
        rex.setTarget(pig);
        helper.onEachTick(() -> {
            if (rex.getTarget() == null && pig.isAlive()) {
                rex.setTarget(pig);
            }
        });
        helper.succeedWhen(() -> {
            boolean opened = false;
            for (int y = 0; y <= 1 && !opened; y++) {
                for (int z = 13; z <= 19; z++) {
                    if (!helper.getBlockState(new BlockPos(13, y, z)).is(DefenseBlocks.WOOD_WALL.get())) {
                        opened = true;
                    }
                }
            }
            helper.assertTrue(opened, "o T-Rex ainda não abriu o muro");
        });
    }

    // ---- Portões ----

    /** O portão comum é 1 × 2; só o dono abre, as duas partes juntas, e aberto não colide. */
    @GameTest(template = EMPTY)
    public static void gateOpensOnlyForTheOwner(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        BlockPos lower = new BlockPos(1, 2, 1);
        BlockPos upper = lower.above();
        place(helper, owner, DefenseBlocks.WOOD_GATE.get(), lower);
        helper.assertBlockPresent(DefenseBlocks.WOOD_GATE.get(), upper);
        helper.assertTrue(Math.abs(collisionTop(helper, upper) - 1.5) < 1e-6, "topo do portão: " + collisionTop(helper, upper));

        use(helper, stranger, lower);
        helper.assertFalse(helper.getBlockState(lower).getValue(DefenseGateBlock.OPEN), "o estranho abriu o portão");

        use(helper, owner, upper);
        helper.assertTrue(helper.getBlockState(lower).getValue(DefenseGateBlock.OPEN)
                && helper.getBlockState(upper).getValue(DefenseGateBlock.OPEN), "o dono deveria abrir as duas partes");
        helper.assertTrue(helper.getBlockState(lower).getCollisionShape(helper.getLevel(), helper.absolutePos(lower),
                CollisionContext.empty()).isEmpty(), "aberto não deveria colidir");

        use(helper, stranger, upper);
        helper.assertTrue(helper.getBlockState(lower).getValue(DefenseGateBlock.OPEN), "o estranho fechou o portão");
        use(helper, owner, lower);
        helper.assertFalse(helper.getBlockState(upper).getValue(DefenseGateBlock.OPEN), "o dono deveria fechar");
        helper.succeed();
    }

    /** O portão grande é 5 × 5; o dono abre tudo de qualquer parte; quebrar uma parte desfaz o portão e dá um item. */
    @GameTest(template = ARENA)
    public static void largeGateIsFiveByFive(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        BlockPos master = new BlockPos(12, 0, 12);
        place(helper, owner, DefenseBlocks.LARGE_STONE_GATE.get(), master);
        // Olhando para o sul, a direita é o oeste: as colunas vão de x = 14 (coluna 0) a x = 10 (coluna 4).
        for (int column = 0; column < 5; column++) {
            for (int row = 0; row < 5; row++) {
                BlockPos part = new BlockPos(14 - column, row, 12);
                BlockState state = helper.getBlockState(part);
                helper.assertTrue(state.is(DefenseBlocks.LARGE_STONE_GATE.get()) && state.getValue(DefenseGateBlock.COLUMN) == column
                        && state.getValue(DefenseGateBlock.ROW) == row, "parte " + column + "," + row + " errada: " + state);
            }
        }
        BlockPos corner = new BlockPos(14, 4, 12);
        use(helper, stranger, corner);
        helper.assertFalse(helper.getBlockState(master).getValue(DefenseGateBlock.OPEN), "o estranho abriu o portão");
        use(helper, owner, corner);
        for (int column = 0; column < 5; column++) {
            for (int row = 0; row < 5; row++) {
                helper.assertTrue(helper.getBlockState(new BlockPos(14 - column, row, 12)).getValue(DefenseGateBlock.OPEN),
                        "parte " + column + "," + row + " não abriu");
            }
        }

        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(11, 3, 12)), true);
        for (int column = 0; column < 5; column++) {
            for (int row = 0; row < 5; row++) {
                helper.assertBlockNotPresent(DefenseBlocks.LARGE_STONE_GATE.get(), new BlockPos(14 - column, row, 12));
            }
        }
        var area = new AABB(helper.absolutePos(new BlockPos(8, -1, 9)), helper.absolutePos(new BlockPos(17, 7, 16)));
        int items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                item -> item.getItem().is(DefenseBlocks.LARGE_STONE_GATE.get().asItem()))
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertTrue(items == 1, "o portão desfeito deveria dar um item, deu " + items);
        helper.succeed();
    }

    // ---- Armadilhas ----

    /** Espetos: ferem o selvagem, o estranho e a domesticada dele (com lentidão); poupam o dono e a domesticada dele. */
    @GameTest(template = EMPTY)
    public static void spikesHurtStrangersAndSpareTheOwner(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        BlockPos trap = new BlockPos(1, 2, 1);
        place(helper, owner, DefenseBlocks.SPIKE_TRAP.get(), trap);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 0, 2, 0);
        LandCreature ownersDodo = tamedDodo(helper, owner, 2, 0);
        LandCreature strangersDodo = tamedDodo(helper, stranger, 0, 2);
        for (LivingEntity entity : List.of(pig, owner, stranger, ownersDodo, strangersDodo)) {
            touch(helper, trap, entity);
        }
        helper.assertTrue(Math.abs(pig.getMaxHealth() - pig.getHealth() - 3.0F) < 1e-3, "o porco deveria levar 3: " + pig.getHealth());
        helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o porco deveria ficar lento");
        helper.assertTrue(hurt(stranger), "o estranho deveria se ferir");
        helper.assertTrue(hurt(strangersDodo), "a domesticada do estranho deveria se ferir");
        helper.assertFalse(hurt(owner), "o dono se feriu");
        helper.assertFalse(hurt(ownersDodo), "a domesticada do dono se feriu");
        helper.succeed();
    }

    /** Espetos: um golpe por segundo, não um por tick. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void spikesHitOncePerSecond(GameTestHelper helper) {
        BlockPos trap = new BlockPos(1, 2, 1);
        helper.setBlock(trap, DefenseBlocks.SPIKE_TRAP.get());
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 0, 2, 0);
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
        pig.setHealth(100.0F);
        touch(helper, trap, pig);
        helper.runAtTickTime(10, () -> {
            touch(helper, trap, pig);
            helper.assertTrue(Math.abs(pig.getHealth() - 97.0F) < 1e-3, "no mesmo segundo, um golpe só: " + pig.getHealth());
        });
        helper.runAtTickTime(25, () -> {
            touch(helper, trap, pig);
            helper.assertTrue(Math.abs(pig.getHealth() - 94.0F) < 1e-3, "um segundo depois, outro golpe: " + pig.getHealth());
            helper.succeed();
        });
    }

    /** Espinhos: 2 de dano ao encostar e o empurrão para longe; o dono passa ileso. */
    @GameTest(template = EMPTY)
    public static void thornsHurtAndPushBack(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        BlockPos thorns = new BlockPos(1, 2, 1);
        place(helper, owner, DefenseBlocks.THORN_PALISADE.get(), thorns);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 2, 1);
        pig.setDeltaMovement(Vec3.ZERO);
        touch(helper, thorns, pig);
        touch(helper, thorns, owner);
        helper.assertTrue(Math.abs(pig.getMaxHealth() - pig.getHealth() - 2.0F) < 1e-3, "o porco deveria levar 2: " + pig.getHealth());
        helper.assertTrue(pig.getDeltaMovement().x > 0.1, "o porco deveria ser empurrado para +x: " + pig.getDeltaMovement());
        helper.assertFalse(hurt(owner), "o dono se feriu nos espinhos");
        helper.succeed();
    }

    /**
     * Urso: 6 de dano, prende 5 s sem andar, solta e fica desarmada; o estranho não rearma, o dono rearma. O dono
     * pisa nela armada sem disparar.
     */
    @GameTest(template = EMPTY, timeoutTicks = 140)
    public static void bearTrapHoldsFiveSecondsAndNeedsRearming(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        BlockPos trap = new BlockPos(1, 2, 1);
        place(helper, owner, DefenseBlocks.BEAR_TRAP.get(), trap);
        touch(helper, trap, owner);
        helper.assertTrue(helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.ARMED,
                "o dono disparou a própria armadilha");

        Pig pig = helper.spawn(EntityType.PIG, 1, 2, 1);
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
        pig.setHealth(100.0F);
        long start = helper.getLevel().getGameTime();
        touch(helper, trap, pig);
        helper.assertTrue(Math.abs(pig.getHealth() - 94.0F) < 1e-3, "o porco deveria levar 6: " + pig.getHealth());
        helper.assertTrue(helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.HOLDING,
                "a armadilha deveria estar prendendo");
        BearTrapBlockEntity entity = (BearTrapBlockEntity) helper.getBlockEntity(trap);
        helper.assertTrue(entity.releaseAt() - start == BearTrapBlock.HOLD_TICKS, "deveria prender 5 s: " + (entity.releaseAt() - start));
        Vec3 center = Vec3.atBottomCenterOf(helper.absolutePos(trap));
        // Empurrado o tempo todo: solto, sairia vários blocos; preso, não sai do lugar.
        helper.onEachTick(() -> pig.setDeltaMovement(0.4, pig.getDeltaMovement().y, 0.0));
        helper.runAtTickTime(60, () -> helper.assertTrue(Math.abs(pig.getX() - center.x) < 0.5,
                "o porco preso andou: " + (pig.getX() - center.x)));
        helper.runAtTickTime(BearTrapBlock.HOLD_TICKS - 5, () -> helper.assertTrue(
                helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.HOLDING, "soltou antes dos 5 s"));
        helper.runAtTickTime(BearTrapBlock.HOLD_TICKS + 5, () -> {
            helper.assertTrue(helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.SPRUNG,
                    "depois dos 5 s deveria soltar e ficar desarmada: " + helper.getBlockState(trap));
            use(helper, stranger, trap);
            helper.assertTrue(helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.SPRUNG,
                    "o estranho rearmou");
            use(helper, owner, trap);
            helper.assertTrue(helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.ARMED,
                    "o dono deveria rearmar");
            helper.succeed();
        });
    }

    /** O gigante fica preso só 2 s. */
    @GameTest(template = EMPTY)
    public static void bearTrapHoldsAGiantTwoSeconds(GameTestHelper helper) {
        BlockPos trap = new BlockPos(1, 2, 1);
        helper.setBlock(trap, DefenseBlocks.BEAR_TRAP.get());
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        long start = helper.getLevel().getGameTime();
        touch(helper, trap, rex);
        BearTrapBlockEntity entity = (BearTrapBlockEntity) helper.getBlockEntity(trap);
        helper.assertTrue(helper.getBlockState(trap).getValue(BearTrapBlock.STAGE) == BearTrapBlock.Stage.HOLDING,
                "a armadilha deveria prender o T-Rex");
        helper.assertTrue(entity.releaseAt() - start == BearTrapBlock.GIANT_HOLD_TICKS,
                "o gigante deveria ficar 2 s: " + (entity.releaseAt() - start));
        helper.succeed();
    }

    // ---- Cobertura de folhagem ----

    /** A cobertura aguenta jogador (qualquer um) e a domesticada do dono; o resto atravessa. */
    @GameTest(template = EMPTY)
    public static void foliageCoverHoldsPlayersAndTheOwnersTames(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        BlockPos cover = new BlockPos(1, 3, 1);
        place(helper, owner, DefenseBlocks.FOLIAGE_COVER.get(), cover);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 0, 2, 0);
        LandCreature ownersDodo = tamedDodo(helper, owner, 2, 0);
        LandCreature strangersDodo = tamedDodo(helper, stranger, 0, 2);
        BlockState state = helper.getBlockState(cover);
        BlockPos pos = helper.absolutePos(cover);
        for (LivingEntity walker : List.of(owner, stranger, ownersDodo)) {
            helper.assertFalse(state.getCollisionShape(helper.getLevel(), pos, CollisionContext.of(walker)).isEmpty(),
                    walker + " deveria andar por cima");
        }
        for (LivingEntity faller : List.of(pig, strangersDodo)) {
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), pos, CollisionContext.of(faller)).isEmpty(),
                    faller + " deveria atravessar");
        }
        helper.succeed();
    }

    /** O porco que pisa na cobertura a quebra e cai no fosso. */
    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void foliageCoverBreaksUnderAWildAnimal(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        BlockPos cover = new BlockPos(1, 3, 1);
        place(helper, owner, DefenseBlocks.FOLIAGE_COVER.get(), cover);
        helper.assertBlockState(new BlockPos(1, 2, 1), BlockState::isAir, () -> "o fosso deveria estar vazio");
        Pig pig = helper.spawn(EntityType.PIG, 1, 4, 1);
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(DefenseBlocks.FOLIAGE_COVER.get(), cover);
            helper.assertTrue(pig.getY() < helper.absolutePos(cover).getY() + 1.0, "o porco não caiu");
        });
    }
}
