package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import dev.madebyfelipe.iceagesurvival.item.TranqGunItem;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Rifle tranquilizante, besta de dardos e o dardo sedativo. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WeaponTests {
    private static final String EMPTY = "empty";

    private static TranqArrow fire(GameTestHelper helper, TranqGunItem gun, ItemStack ammo, Player[] shooter) {
        Player player = helper.makeMockSurvivalPlayer();
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gun));
        player.getInventory().add(ammo);
        gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        shooter[0] = player;
        List<TranqArrow> darts = helper.getLevel().getEntitiesOfClass(TranqArrow.class,
                new AABB(player.blockPosition()).inflate(8), dart -> dart.getOwner() == player);
        helper.assertTrue(darts.size() == 1, "deveria ter saído um dardo: " + darts.size());
        return darts.get(0);
    }

    /** O rifle dispara o dardo com 3× o torpor da flecha, gasta um dardo e entra em recarga. */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void theRifleFiresASedativeDart(GameTestHelper helper) {
        Player[] shooter = new Player[1];
        TranqArrow dart = fire(helper, ModItems.TRANQ_RIFLE.get(), new ItemStack(ModItems.TRANQ_DART.get(), 4), shooter);
        double expected = ServerConfig.TRANQ_ARROW_TORPOR.get() * 3.0;
        helper.assertTrue(Math.abs(dart.fixedTorpor() - expected) < 1e-6, "torpor do rifle " + dart.fixedTorpor());
        helper.assertTrue(shooter[0].getInventory().countItem(ModItems.TRANQ_DART.get()) == 3, "o dardo não foi gasto");
        helper.assertTrue(shooter[0].getCooldowns().isOnCooldown(ModItems.TRANQ_RIFLE.get()), "o rifle não entrou em recarga");
        helper.assertTrue(dart.getDeltaMovement().length() > 4.0, "o dardo do rifle deveria sair rápido");
        helper.succeed();
    }

    /** A besta de dardos fica no meio: 2× o torpor, e aceita também a flecha tranquilizante. */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void theDartCrossbowSitsBetweenBowAndRifle(GameTestHelper helper) {
        Player[] shooter = new Player[1];
        TranqArrow dart = fire(helper, ModItems.TRANQ_CROSSBOW.get(), new ItemStack(ModItems.TRANQ_ARROW.get(), 2), shooter);
        double expected = ServerConfig.TRANQ_ARROW_TORPOR.get() * 2.0;
        helper.assertTrue(Math.abs(dart.fixedTorpor() - expected) < 1e-6, "torpor da besta " + dart.fixedTorpor());
        helper.assertTrue(ModItems.TRANQ_CROSSBOW.get().reloadTicks() < ModItems.TRANQ_RIFLE.get().reloadTicks(),
                "a besta deveria recarregar mais rápido que o rifle");
        helper.succeed();
    }

    /** Dardo = pepita de ferro + narcótico; rifle e besta têm receita. */
    @GameTest(template = EMPTY, batch = "weapons")
    public static void weaponRecipesExist(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("tranq_dart", "tranq_rifle", "tranq_crossbow")) {
            helper.assertTrue(recipes.byKey(new ResourceLocation(IceAgeSurvival.MODID, name)).isPresent(),
                    "sem receita: " + name);
        }
        helper.succeed();
    }
}
