package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.CreatureOrder;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.Smilodon;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CommandTests {
    private static final String EMPTY = "empty";

    /** Jogador posicionado na área de teste, para as checagens de distância valerem. */
    private static Player playerAt(GameTestHelper helper, Smilodon near) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(near.position());
        return player;
    }

    /** Smilodon domesticado que sempre obedece. */
    private static Smilodon tamedSmilodon(GameTestHelper helper, Player owner) {
        Smilodon smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        return smilodon;
    }

    @GameTest(template = EMPTY)
    public static void ownerCanChangeOrder(GameTestHelper helper) {
        Smilodon wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, wild);
        wild.tame(owner);
        wild.setAffinity(PrehistoricCreature.MAX_AFFINITY);

        helper.assertTrue(wild.order() == CreatureOrder.DEFEND, "ordem inicial: " + wild.order());
        helper.assertTrue(CreatureCommands.setOrder(owner, wild, CreatureOrder.STAY), "dono não conseguiu comandar");
        helper.assertTrue(wild.order() == CreatureOrder.STAY && wild.isOrderedToSit(), "ordem de ficar não aplicada");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void strangerCannotCommand(GameTestHelper helper) {
        Smilodon smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        Player stranger = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);

        helper.assertTrue(!CreatureCommands.setOrder(stranger, smilodon, CreatureOrder.STAY), "estranho comandou a criatura");
        helper.assertTrue(smilodon.order() == CreatureOrder.DEFEND, "ordem mudou por um estranho");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wildCreatureCannotBeCommanded(GameTestHelper helper) {
        Smilodon wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player player = playerAt(helper, wild);
        helper.assertTrue(!CreatureCommands.setOrder(player, wild, CreatureOrder.FOLLOW), "criatura selvagem aceitou ordem");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ownerTooFarCannotCommand(GameTestHelper helper) {
        Smilodon smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        owner.setPos(smilodon.position().add(CreatureCommands.COMMAND_RANGE + 5, 0, 0));
        helper.assertTrue(!CreatureCommands.setOrder(owner, smilodon, CreatureOrder.STAY), "comando aceito de longe demais");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void attackOrderTargetsOnlyForCreaturesAllowedToFight(GameTestHelper helper) {
        Smilodon smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);

        CreatureCommands.setOrder(owner, smilodon, CreatureOrder.FLEE);
        helper.assertTrue(CreatureCommands.orderAttack(owner, pig) == 0 && smilodon.getTarget() == null,
                "criatura em fuga aceitou ordem de ataque");

        CreatureCommands.setOrder(owner, smilodon, CreatureOrder.FOLLOW);
        helper.assertTrue(CreatureCommands.orderAttack(owner, pig) == 1, "criatura seguindo não atacou");
        helper.assertTrue(smilodon.getTarget() == pig, "alvo não definido");

        CreatureCommands.setOrder(owner, smilodon, CreatureOrder.STAY);
        helper.assertTrue(smilodon.getTarget() == null, "ordem de ficar não largou o alvo");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void attackOrderRejectsOwnCreaturesAndTheOwner(GameTestHelper helper) {
        Smilodon first = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, first);
        first.tame(owner);
        first.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        Smilodon second = tamedSmilodon(helper, owner);

        helper.assertTrue(CreatureCommands.orderAttack(owner, second) == 0, "mandou atacar a própria criatura");
        helper.assertTrue(CreatureCommands.orderAttack(owner, owner) == 0, "mandou atacar o próprio dono");
        helper.assertTrue(first.getTarget() == null && second.getTarget() == null, "alvo indevido definido");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void orderAndAffinitySurviveSaveAndLoad(GameTestHelper helper) {
        Smilodon original = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, original);
        original.tame(owner);
        original.setAffinity(73.0F);
        original.setOrder(CreatureOrder.FLEE);

        Smilodon loaded = ModEntities.SMILODON.get().create(helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));
        loaded.setUUID(UUID.randomUUID());

        helper.assertTrue(loaded.order() == CreatureOrder.FLEE, "ordem não persistiu: " + loaded.order());
        helper.assertTrue(loaded.affinity() == 73.0F, "afinidade não persistiu: " + loaded.affinity());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void feedingTamedCreatureHealsAndRaisesAffinityOncePerInterval(GameTestHelper helper) {
        Smilodon smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(10.0F);
        smilodon.setHealth(1.0F);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 8));

        smilodon.mobInteract(owner, InteractionHand.MAIN_HAND);
        smilodon.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(owner.getMainHandItem().getCount() == 7, "comeu " + (8 - owner.getMainHandItem().getCount()) + " vezes");
        helper.assertTrue(smilodon.affinity() == 15.0F, "afinidade " + smilodon.affinity());
        helper.assertTrue(smilodon.getHealth() > 1.0F, "não curou");
        helper.succeed();
    }
}
