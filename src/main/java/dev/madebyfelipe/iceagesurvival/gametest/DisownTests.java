package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.command.Stance;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Deserdar (MC-18): a criatura fica sem dono e mansa, larga sela e carga, e qualquer um a reivindica. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class DisownTests {
    private static final String EMPTY = "empty";

    private static LandCreature tamedSmilodon(GameTestHelper helper, Player owner) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        return smilodon;
    }

    @GameTest(template = EMPTY, batch = "disown")
    public static void onlyTheOwnerDisownsAndTheGearDrops(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamedSmilodon(helper, owner);
        smilodon.setSaddled(true);
        smilodon.inventory().setItem(0, new ItemStack(Items.BONE, 7));

        helper.assertFalse(smilodon.disown(stranger), "um estranho deserdou a criatura");
        helper.assertTrue(smilodon.isOwner(owner), "perdeu o dono pela mão de um estranho");
        helper.assertTrue(smilodon.disown(owner), "o dono não conseguiu deserdar");

        helper.assertTrue(smilodon.isAbandoned() && smilodon.getOwnerUUID() == null && smilodon.isTame(),
                "não ficou mansa e sem dono");
        helper.assertTrue(smilodon.stance() == Stance.PASSIVE, "postura: " + smilodon.stance());
        helper.assertFalse(smilodon.isSaddled(), "a sela continua nela");
        helper.assertTrue(smilodon.inventory().isEmpty(), "o inventário continua nela");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(smilodon.blockPosition()).inflate(4));
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.SADDLE)), "a sela não caiu");
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.BONE)
                && drop.getItem().getCount() == 7), "o inventário não caiu");
        helper.assertFalse(smilodon.canCommand(owner), "o antigo dono ainda comanda");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "disown")
    public static void theAbandonedStateSurvivesReload(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamedSmilodon(helper, owner);
        smilodon.disown(owner);

        LandCreature loaded = ModEntities.SMILODON.get().create(helper.getLevel());
        loaded.load(smilodon.saveWithoutId(new CompoundTag()));
        loaded.setUUID(UUID.randomUUID());
        helper.assertTrue(loaded.isAbandoned(), "voltou selvagem ao recarregar: tame=" + loaded.isTame()
                + " dono=" + loaded.getOwnerUUID());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = "disown")
    public static void anyoneClaimsTheAbandonedCreatureWithAClick(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player finder = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamedSmilodon(helper, owner);
        smilodon.disown(owner);
        finder.setPos(smilodon.position());

        smilodon.mobInteract(finder, InteractionHand.MAIN_HAND);
        helper.assertTrue(smilodon.isOwner(finder) && !smilodon.isAbandoned(), "o clique não deu a criatura");
        helper.assertTrue(smilodon.stance() != Stance.PASSIVE, "ficou passiva com o dono novo");
        helper.succeed();
    }
}
