package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.item.TranqGunItem;
import dev.madebyfelipe.iceagesurvival.item.TranqRifleItem;
import dev.madebyfelipe.iceagesurvival.network.RifleTracerPayload;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Rifle tranquilizante (hitscan), besta de dardos (projétil) e o dardo sedativo. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WeaponTests {
    private static final String EMPTY = "empty";

    private static final String ARENA = "arena";

    /** Um jogador de sobrevivência com a arma na mão e munição no inventário, num ponto da área (relativo). */
    private static Player armed(GameTestHelper helper, Vec3 at, TranqGunItem gun, ItemStack ammo) {
        Player player = helper.makeMockSurvivalPlayer();
        player.moveTo(helper.absoluteVec(at));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gun));
        player.getInventory().add(ammo);
        return player;
    }

    /** Dispara uma arma de dardo (a besta) e devolve o dardo que saiu. */
    private static TranqArrow fireDart(GameTestHelper helper, TranqGunItem gun, ItemStack ammo, Player[] shooter) {
        Player player = armed(helper, new Vec3(2.5, 2, 2.5), gun, ammo);
        gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        shooter[0] = player;
        List<TranqArrow> darts = helper.getLevel().getEntitiesOfClass(TranqArrow.class,
                new AABB(player.blockPosition()).inflate(8), dart -> dart.getOwner() == player);
        helper.assertTrue(darts.size() == 1, "deveria ter saído um dardo: " + darts.size());
        return darts.get(0);
    }

    /** Dardos voando na arena inteira (o rifle não deve criar nenhum). */
    private static List<TranqArrow> dartsInArena(GameTestHelper helper) {
        Vec3 corner = helper.absoluteVec(Vec3.ZERO);
        return helper.getLevel().getEntitiesOfClass(TranqArrow.class,
                new AABB(corner, corner.add(24, 12, 24)).inflate(4));
    }

    /**
     * O rifle é hitscan: no mesmo tick do clique a criatura à frente já tem o torpor e o dano baixo do dardo, sem
     * entidade de projétil. Gasta um dardo e entra em recarga.
     */
    @GameTest(template = ARENA, batch = "weapons", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theRifleHitsInstantlyWithoutAProjectile(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        TestCreature target = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 12, 1, 12);
        TranqGunItem rifle = ModItems.TRANQ_RIFLE.get();
        Player player = armed(helper, new Vec3(3.5, 1, 12.5), rifle, new ItemStack(ModItems.TRANQ_DART.get(), 4));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
        float health = target.getHealth();

        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(target.torpor() > 0, "o tiro não aplicou torpor na hora");
        helper.assertTrue(target.getHealth() < health, "o tiro não feriu: " + target.getHealth());
        helper.assertTrue(dartsInArena(helper).isEmpty(), "o rifle não deveria criar dardo voando");
        helper.assertTrue(player.getInventory().countItem(ModItems.TRANQ_DART.get()) == 3, "o dardo não foi gasto");
        helper.assertTrue(player.getCooldowns().isOnCooldown(rifle), "o rifle não entrou em recarga");
        helper.succeed();
    }

    /** O tiro do rifle fere o apex mas não dá torpor: o apex só cai vencendo o desafio. */
    @GameTest(template = ARENA, batch = "weapons_apex", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theRifleDoesNotSedateTheApex(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 8, 1, 12);
        TranqGunItem rifle = ModItems.TRANQ_RIFLE.get();
        Player player = armed(helper, new Vec3(21.5, 1, 12.5), rifle, new ItemStack(ModItems.TRANQ_DART.get(), 4));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, rex.getBoundingBox().getCenter());
        float health = rex.getHealth();

        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(rex.getHealth() < health, "o tiro deveria ter acertado o T-Rex");
        helper.assertTrue(rex.torpor() == 0, "o apex recebeu torpor: " + rex.torpor());
        helper.succeed();
    }

    /** Uma parede segura o tiro: a criatura atrás dela não sente nada, e o traçante para na parede. */
    @GameTest(template = ARENA, batch = "weapons", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theRifleShotStopsAtAWall(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        for (int y = 1; y <= 5; y++) {
            for (int z = 9; z <= 15; z++) {
                helper.setBlock(8, y, z, Blocks.STONE);
            }
        }
        TestCreature target = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 14, 1, 12);
        TranqGunItem rifle = ModItems.TRANQ_RIFLE.get();
        Player player = armed(helper, new Vec3(3.5, 1, 12.5), rifle, new ItemStack(ModItems.TRANQ_DART.get(), 4));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
        float health = target.getHealth();

        TranqRifleItem.Shot shot = TranqRifleItem.trace(helper.getLevel(), player, TranqRifleItem.RANGE);
        helper.assertTrue(shot.target() == null, "o raio atravessou a parede: " + shot.target());
        helper.assertTrue(shot.impact() == RifleTracerPayload.Impact.BLOCK, "o raio deveria parar num bloco");
        double wall = helper.absoluteVec(new Vec3(8, 0, 0)).x;
        helper.assertTrue(Math.abs(shot.end().x - wall) < 0.01, "o raio parou fora da parede: " + shot.end());

        rifle.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(target.torpor() == 0, "torpor atrás da parede: " + target.torpor());
        helper.assertTrue(target.getHealth() == health, "dano atrás da parede");
        helper.succeed();
    }

    /** A besta continua lançando o dardo (projétil que voa e se recolhe), com 2× o torpor da flecha. */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void theDartCrossbowStillFiresADart(GameTestHelper helper) {
        Player[] shooter = new Player[1];
        TranqArrow dart = fireDart(helper, ModItems.TRANQ_CROSSBOW.get(), new ItemStack(ModItems.TRANQ_DART.get(), 4),
                shooter);
        double expected = ServerConfig.TRANQ_ARROW_TORPOR.get() * 2.0;
        helper.assertTrue(Math.abs(dart.fixedTorpor() - expected) < 1e-6, "torpor da besta " + dart.fixedTorpor());
        helper.assertTrue(shooter[0].getInventory().countItem(ModItems.TRANQ_DART.get()) == 3, "o dardo não foi gasto");
        helper.assertTrue(shooter[0].getCooldowns().isOnCooldown(ModItems.TRANQ_CROSSBOW.get()),
                "a besta não entrou em recarga");
        helper.succeed();
    }

    /** A besta de dardos fica no meio: 2× o torpor, e aceita também a flecha tranquilizante. */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void theDartCrossbowSitsBetweenBowAndRifle(GameTestHelper helper) {
        Player[] shooter = new Player[1];
        TranqArrow dart = fireDart(helper, ModItems.TRANQ_CROSSBOW.get(), new ItemStack(ModItems.TRANQ_ARROW.get(), 2),
                shooter);
        double expected = ServerConfig.TRANQ_ARROW_TORPOR.get() * 2.0;
        helper.assertTrue(Math.abs(dart.fixedTorpor() - expected) < 1e-6, "torpor da besta " + dart.fixedTorpor());
        helper.assertTrue(ModItems.TRANQ_CROSSBOW.get().reloadTicks() < ModItems.TRANQ_RIFLE.get().reloadTicks(),
                "a besta deveria recarregar mais rápido que o rifle");
        helper.succeed();
    }

    /**
     * A escala por torpor/s: o arco puxado (1 tiro/s) e a besta empatam, o arco Força V vem depois e o rifle é o
     * maior de todos. A besta nunca passa do arco Força V, nem por tiro.
     */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void theRifleSedatesFastestAndTheCrossbowStaysUnderAPowerFiveBow(GameTestHelper helper) {
        double arrow = ServerConfig.TRANQ_ARROW_TORPOR.get();
        double bowDrawSeconds = 1.0;
        double powerFive = arrow * (1.0 + 0.25 * (5 + 1));
        TranqGunItem crossbow = ModItems.TRANQ_CROSSBOW.get();
        TranqGunItem rifle = ModItems.TRANQ_RIFLE.get();
        double crossbowRate = crossbow.torpor() / (crossbow.reloadTicks() / 20.0);
        double rifleRate = rifle.torpor() / (rifle.reloadTicks() / 20.0);
        helper.assertTrue(crossbow.torpor() <= powerFive, "besta por tiro " + crossbow.torpor() + " > Força V " + powerFive);
        helper.assertTrue(crossbowRate <= powerFive / bowDrawSeconds, "besta por segundo " + crossbowRate);
        helper.assertTrue(crossbowRate <= arrow / bowDrawSeconds, "a besta não deveria passar do arco comum: " + crossbowRate);
        helper.assertTrue(rifleRate > powerFive / bowDrawSeconds, "rifle por segundo " + rifleRate + " ≤ Força V");
        helper.assertTrue(rifleRate > crossbowRate, "rifle " + rifleRate + " ≤ besta " + crossbowRate);
        helper.succeed();
    }

    /** Dardo = pepita de ferro + narcótico; rifle e besta têm receita, todas na Bancada de Armeiro. */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void weaponRecipesExist(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("tranq_dart", "tranq_rifle", "tranq_crossbow")) {
            helper.assertTrue(recipes.byKey(new ResourceLocation(IceAgeSurvival.MODID, "bench/armory/" + name)).isPresent(),
                    "sem receita: " + name);
        }
        helper.succeed();
    }
}
