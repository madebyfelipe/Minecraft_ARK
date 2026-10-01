package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.core.command.Stance;
import dev.madebyfelipe.iceagesurvival.core.command.Whistle;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CommandTests {
    private static final String EMPTY = "empty";

    /** Jogador posicionado na área de teste, para as checagens de distância valerem. */
    private static Player playerAt(GameTestHelper helper, LandCreature near) {
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(near.position());
        return player;
    }

    /** Smilodon domesticado que sempre obedece. */
    private static LandCreature tamedSmilodon(GameTestHelper helper, Player owner) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        return smilodon;
    }

    @GameTest(template = EMPTY)
    public static void ownerCanChangeOrder(GameTestHelper helper) {
        LandCreature wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, wild);
        wild.tame(owner);
        wild.setAffinity(PrehistoricCreature.MAX_AFFINITY);

        helper.assertTrue(wild.movement() == Movement.FOLLOW && wild.stance() == Stance.DEFEND,
                "ordens iniciais: " + wild.movement() + " " + wild.stance());
        helper.assertTrue(CreatureCommands.whistle(owner, Whistle.STAY, wild) == 1, "dono não conseguiu comandar");
        helper.assertTrue(wild.movement() == Movement.STAY && wild.isOrderedToSit(), "ordem de parar não aplicada");
        helper.assertTrue(wild.stance() == Stance.DEFEND, "parar mexeu na postura");
        helper.assertTrue(CreatureCommands.whistle(owner, Whistle.PASSIVE, wild) == 1, "assobio de passivo recusado");
        helper.assertTrue(wild.stance() == Stance.PASSIVE && wild.movement() == Movement.STAY,
                "passivo não aplicado ou mexeu no movimento");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void whistleWithoutAimReachesAllOwnCreaturesInRange(GameTestHelper helper) {
        LandCreature first = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, first);
        first.tame(owner);
        first.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        LandCreature second = tamedSmilodon(helper, owner);
        LandCreature stranger = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);

        helper.assertTrue(CreatureCommands.whistle(owner, Whistle.STAY, null) == 2, "não chegou às duas criaturas");
        helper.assertTrue(first.movement() == Movement.STAY && second.movement() == Movement.STAY, "alguma não parou");
        helper.assertTrue(stranger.movement() == Movement.FOLLOW, "criatura selvagem ouviu o assobio");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void strangerCannotCommand(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        Player stranger = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);

        helper.assertTrue(CreatureCommands.whistle(stranger, Whistle.STAY, smilodon) == 0, "estranho comandou a criatura");
        helper.assertTrue(smilodon.movement() == Movement.FOLLOW, "ordem mudou por um estranho");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wildCreatureCannotBeCommanded(GameTestHelper helper) {
        LandCreature wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player player = playerAt(helper, wild);
        helper.assertTrue(CreatureCommands.whistle(player, Whistle.STAY, wild) == 0, "criatura selvagem aceitou ordem");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ownerTooFarCannotCommand(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        owner.setPos(smilodon.position().add(CreatureCommands.COMMAND_RANGE + 5, 0, 0));
        helper.assertTrue(CreatureCommands.whistle(owner, Whistle.STAY, smilodon) == 0, "comando aceito de longe demais");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void attackOrderTargetsOnlyForCreaturesAllowedToFight(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, smilodon);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);

        CreatureCommands.whistle(owner, Whistle.FLEE, smilodon);
        helper.assertTrue(CreatureCommands.orderAttack(owner, pig) == 0 && smilodon.getTarget() == null,
                "criatura em fuga aceitou ordem de ataque");

        CreatureCommands.whistle(owner, Whistle.NEUTRAL, smilodon);
        helper.assertTrue(CreatureCommands.orderAttack(owner, pig) == 1, "criatura neutra não atacou");
        helper.assertTrue(smilodon.getTarget() == pig, "alvo não definido");

        CreatureCommands.whistle(owner, Whistle.PASSIVE, smilodon);
        helper.assertTrue(smilodon.getTarget() == null, "passivo não largou o alvo");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void attackOrderRejectsOwnCreaturesAndTheOwner(GameTestHelper helper) {
        LandCreature first = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, first);
        first.tame(owner);
        first.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        LandCreature second = tamedSmilodon(helper, owner);

        helper.assertTrue(CreatureCommands.orderAttack(owner, second) == 0, "mandou atacar a própria criatura");
        helper.assertTrue(CreatureCommands.orderAttack(owner, owner) == 0, "mandou atacar o próprio dono");
        helper.assertTrue(first.getTarget() == null && second.getTarget() == null, "alvo indevido definido");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void legacyOrderIsConvertedOnLoad(GameTestHelper helper) {
        LandCreature original = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        original.tame(playerAt(helper, original));
        CompoundTag tag = original.saveWithoutId(new CompoundTag());
        tag.remove("Movement");
        tag.remove("Stance");
        tag.putString("Order", "stay");

        LandCreature loaded = ModEntities.SMILODON.get().create(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(loaded.movement() == Movement.STAY && loaded.stance() == Stance.PASSIVE,
                "ordem antiga \"stay\" virou " + loaded.movement() + " " + loaded.stance());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void orderAndAffinitySurviveSaveAndLoad(GameTestHelper helper) {
        LandCreature original = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player owner = playerAt(helper, original);
        original.tame(owner);
        original.setAffinity(73.0F);
        original.setMovement(Movement.STAY);
        original.setStance(Stance.FLEE);

        LandCreature loaded = ModEntities.SMILODON.get().create(helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));
        loaded.setUUID(UUID.randomUUID());

        helper.assertTrue(loaded.movement() == Movement.STAY && loaded.stance() == Stance.FLEE,
                "ordens não persistiram: " + loaded.movement() + " " + loaded.stance());
        helper.assertTrue(loaded.affinity() == 73.0F, "afinidade não persistiu: " + loaded.affinity());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void feedingTamedCreatureHealsAndRaisesAffinityOncePerInterval(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
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
