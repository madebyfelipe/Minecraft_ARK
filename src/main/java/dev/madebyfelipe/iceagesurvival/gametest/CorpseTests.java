package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.PrepStationBlockEntity;
import dev.madebyfelipe.iceagesurvival.block.ReviveTableBlockEntity;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.ImplantItem;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** A domesticada que morre vira corpo com o implante; a mesa de reviver a traz de volta; a estação de preparação. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CorpseTests {
    private static final String EMPTY = "empty";

    private static LandCreature tamed(GameTestHelper helper, Player owner) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 2, 2, 2);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        return smilodon;
    }

    private static ItemStack implantIn(PrehistoricCreature creature) {
        for (int slot = 0; slot < creature.inventory().getContainerSize(); slot++) {
            if (creature.inventory().getItem(slot).is(ModItems.IMPLANT.get())) {
                return creature.inventory().getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Morta, a domesticada fica no chão como corpo: invulnerável, com o implante no inventário. */
    @GameTest(template = EMPTY, batch = "corpse")
    public static void aTamedCreatureLeavesACorpseWithItsImplant(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamed(helper, owner);
        smilodon.hurt(helper.getLevel().damageSources().generic(), 10_000.0F);
        helper.assertTrue(smilodon.isAlive() && smilodon.isCorpse(), "deveria ter virado corpo, não sumido");
        helper.assertTrue(smilodon.isUnconscious(), "o corpo fica caído");
        ItemStack implant = implantIn(smilodon);
        boolean onGround = !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                smilodon.getBoundingBox().inflate(3), item -> item.getItem().is(ModItems.IMPLANT.get())).isEmpty();
        helper.assertTrue(!implant.isEmpty() || onGround, "sem implante no corpo nem no chão");
        helper.assertTrue(ImplantItem.holdsCreature(implant.isEmpty() ? ItemStack.EMPTY : implant) || onGround,
                "implante sem a criatura");
        float health = smilodon.getHealth();
        smilodon.hurt(helper.getLevel().damageSources().generic(), 50.0F);
        helper.assertTrue(smilodon.getHealth() == health && smilodon.isAlive(), "o corpo levou dano");
        helper.succeed();
    }

    /** Tirado tudo (o implante inclusive), o corpo some. */
    @GameTest(template = EMPTY, batch = "corpse_empty", timeoutTicks = 120)
    public static void anEmptiedCorpseDisappears(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamed(helper, owner);
        smilodon.hurt(helper.getLevel().damageSources().generic(), 10_000.0F);
        smilodon.inventory().clearContent();
        helper.succeedWhen(() -> helper.assertTrue(smilodon.isRemoved(), "o corpo vazio continua no chão"));
    }

    /** Implante + diamante na mesa de reviver: a criatura volta, do mesmo dono e nível, com um quarto da vida. */
    @GameTest(template = EMPTY, batch = "corpse_revive")
    public static void theRevivalTableBringsItBack(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamed(helper, owner);
        int level = smilodon.creatureLevel();
        ItemStack implant = ImplantItem.of(smilodon, ModItems.IMPLANT.get());
        smilodon.discard();
        BlockPos tablePos = new BlockPos(6, 2, 6);
        helper.setBlock(tablePos, ModBlocks.REVIVE_TABLE.get());
        ReviveTableBlockEntity table = (ReviveTableBlockEntity) helper.getBlockEntity(tablePos);
        table.setItem(ReviveTableBlockEntity.IMPLANT, implant);
        table.setItem(ReviveTableBlockEntity.DIAMOND, new ItemStack(Items.DIAMOND, 2));
        for (int tick = 0; tick < ReviveTableBlockEntity.REVIVE_TICKS; tick++) {
            table.serverTick();
        }
        var revived = helper.getLevel().getEntitiesOfClass(LandCreature.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(tablePos)).inflate(3),
                creature -> creature.getType() == ModEntities.SMILODON.get());
        helper.assertTrue(revived.size() == 1, "deveria ter voltado um Smilodon: " + revived.size());
        LandCreature back = revived.get(0);
        helper.assertTrue(back.isTame() && owner.getUUID().equals(back.getOwnerUUID()), "voltou sem o dono");
        helper.assertTrue(back.creatureLevel() == level, "voltou com outro nível: " + back.creatureLevel());
        helper.assertTrue(Math.abs(back.getHealth() - back.getMaxHealth() * ImplantItem.REVIVE_HEALTH) < 0.5F,
                "vida ao voltar: " + back.getHealth());
        helper.assertFalse(back.isCorpse(), "voltou como corpo");
        helper.assertTrue(table.getItem(ReviveTableBlockEntity.IMPLANT).isEmpty()
                        && table.getItem(ReviveTableBlockEntity.DIAMOND).getCount() == 1,
                "deveria gastar o implante e um diamante");
        helper.succeed();
    }

    /** Carne crua apodrece; carne com açúcar vira charque. */
    @GameTest(template = EMPTY, batch = "prep_station")
    public static void thePrepStationRotsMeatAndMakesJerky(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.PREP_STATION.get());
        PrepStationBlockEntity station = (PrepStationBlockEntity) helper.getBlockEntity(pos);
        station.setItem(0, new ItemStack(Items.BEEF, 1));
        for (int tick = 0; tick < 1200; tick++) {
            station.serverTick();
        }
        helper.assertTrue(station.getItem(2).is(Items.ROTTEN_FLESH), "a carne não apodreceu: " + station.getItem(2));
        station.setItem(2, ItemStack.EMPTY);
        station.setItem(0, new ItemStack(Items.BEEF, 2));
        station.setItem(1, new ItemStack(Items.SUGAR, 1));
        for (int tick = 0; tick < 600; tick++) {
            station.serverTick();
        }
        helper.assertTrue(station.getItem(2).is(ModItems.JERKY.get()) && station.getItem(2).getCount() == 2,
                "não fez charque: " + station.getItem(2));
        helper.succeed();
    }
}
