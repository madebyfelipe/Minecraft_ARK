package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.menu.CreatureStorageMenu;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.PackBonusProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Força e porte pela biologia real, bônus de bando dos predadores de bando, sexo à vista e o slot de sela.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BalanceTests {
    private static final String EMPTY = "empty";

    private static double base(GameTestHelper helper, EntityType<?> type, Stat stat) {
        return Species.of(helper.getLevel().registryAccess(), type).orElseThrow().stats().entry(stat).base();
    }

    /** O Smilodon (160–280 kg) não é mais um terço do Utahraptor, e os herbívoros de toneladas são maiores e mais duros. */
    @GameTest(template = EMPTY, batch = "balance")
    public static void strengthFollowsTheRealAnimal(GameTestHelper helper) {
        double smilodon = base(helper, ModEntities.SMILODON.get(), Stat.HEALTH);
        double utah = base(helper, ModEntities.UTAHRAPTOR.get(), Stat.HEALTH);
        helper.assertTrue(smilodon >= utah * 0.8, "Smilodon com " + smilodon + " de vida contra " + utah + " do Utahraptor");
        for (EntityType<?> big : java.util.List.of(ModEntities.STEGOSAURUS.get(), ModEntities.TRICERATOPS.get())) {
            helper.assertTrue(base(helper, big, Stat.HEALTH) > 2 * utah, big + " deveria aguentar mais que dois Utahraptors");
            helper.assertTrue(big.getWidth() > ModEntities.UTAHRAPTOR.get().getWidth() * 2.5,
                    big + " deveria ser bem maior que o Utahraptor: " + big.getWidth());
        }
        helper.succeed();
    }

    /** Todo predador de bando tem o bônus; o do Utahraptor é o maior (a marca dele). */
    @GameTest(template = EMPTY, batch = "balance")
    public static void packPredatorsGetThePackBonus(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        double best = 0;
        for (EntityType<?> type : java.util.List.of(ModEntities.ALLOSAURUS.get(), ModEntities.UTAHRAPTOR.get(),
                ModEntities.VELOCIRAPTOR.get())) {
            PackBonusProfile bonus = Species.of(access, type).flatMap(Species::packBonus).orElse(null);
            helper.assertTrue(bonus != null, type + " é predador de bando e não tem bônus de bando");
            best = Math.max(best, bonus.attackMultiplier());
        }
        double utah = Species.of(access, ModEntities.UTAHRAPTOR.get()).flatMap(Species::packBonus).orElseThrow()
                .attackMultiplier();
        helper.assertTrue(utah == best, "o bônus do Utahraptor deveria ser o maior");
        helper.assertTrue(Species.of(access, ModEntities.SMILODON.get()).flatMap(Species::packBonus).isEmpty(),
                "o Smilodon é solitário: sem bônus de bando");
        helper.succeed();
    }

    /** Com nome, o sexo aparece ao lado dele. */
    @GameTest(template = EMPTY, batch = "balance")
    public static void aNamedCreatureShowsItsSex(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        creature.setFemale(true);
        creature.setCustomName(Component.literal("Dida"));
        helper.assertTrue(creature.getDisplayName().getString().equals("Dida ♀"),
                "nome exibido: " + creature.getDisplayName().getString());
        creature.setFemale(false);
        helper.assertTrue(creature.getDisplayName().getString().endsWith("♂"), "macho sem ♂");
        helper.succeed();
    }

    /** O slot de sela do inventário sela e desela a criatura. */
    @GameTest(template = EMPTY, batch = "balance")
    public static void theSaddleSlotSaddlesAndUnsaddles(GameTestHelper helper) {
        LandCreature trike = helper.spawnWithNoFreeWill(ModEntities.TRICERATOPS.get(), 2, 2, 2);
        Player owner = helper.makeMockSurvivalPlayer();
        owner.setPos(trike.position());
        trike.tame(owner);
        CreatureStorageMenu menu = new CreatureStorageMenu(0, owner.getInventory(), trike);
        helper.assertTrue(menu.hasSaddleSlot(), "o Tricerátopo usa sela: deveria ter o slot");
        var saddle = menu.slots.get(menu.slots.size() - 1);
        helper.assertTrue(saddle.mayPlace(new ItemStack(PrehistoricCreature.SADDLE_ITEM)), "o slot não aceita sela");
        helper.assertFalse(saddle.mayPlace(new ItemStack(net.minecraft.world.item.Items.DIRT)), "o slot aceita qualquer coisa");
        saddle.set(new ItemStack(PrehistoricCreature.SADDLE_ITEM));
        helper.assertTrue(trike.isSaddled(), "pôr a sela no slot deveria selar");
        ItemStack taken = saddle.remove(1);
        helper.assertTrue(taken.is(PrehistoricCreature.SADDLE_ITEM) && !trike.isSaddled(), "tirar a sela deveria deselar");
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 5, 2, 5);
        dodo.tame(owner);
        helper.assertFalse(new CreatureStorageMenu(1, owner.getInventory(), dodo).hasSaddleSlot(),
                "o dodô não usa sela");
        helper.succeed();
    }
}
