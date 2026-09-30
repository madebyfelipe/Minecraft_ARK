package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.command.CreatureOrder;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Etapa 7: sela, controle da montaria e o que a impede. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class MountTests {
    private static final String EMPTY = "empty";

    private static Player playerAt(GameTestHelper helper, LandCreature near) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(near.position());
        return player;
    }

    /** Smilodon domesticado, selado e com afinidade cheia: pronto para montar. */
    private static LandCreature readyToRide(GameTestHelper helper, Player owner) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        smilodon.setSaddled(true);
        return smilodon;
    }

    // ---- Dados ----

    @GameTest(template = EMPTY)
    public static void mountDataMatchesTheDesign(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        MountProfile smilodon = Species.of(registries, ModEntities.SMILODON.get()).orElseThrow().mount().orElseThrow();
        MountProfile mammoth = Species.of(registries, ModEntities.MAMMOTH.get()).orElseThrow().mount().orElseThrow();
        helper.assertTrue(Species.of(registries, ModEntities.DIRE_WOLF.get()).orElseThrow().mount().isEmpty(),
                "lobo-terrível não deveria ser montável");
        helper.assertTrue(smilodon.seatHeight(2.3F) > 2.0, "assento do Smilodon baixo: " + smilodon.seatHeight(2.3F));
        helper.assertTrue(mammoth.seatHeight(3.1F) > smilodon.seatHeight(2.3F), "mamute com assento mais baixo que o Smilodon");
        helper.assertTrue(smilodon.speedMultiplier() > mammoth.speedMultiplier() * 0.5, "Smilodon devia ser o mais rápido");
        helper.assertTrue(smilodon.jumpStrength() > mammoth.jumpStrength(), "mamute pula mais que o Smilodon");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tyrannosaurusCanBeSaddled(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 1, 2, 1);
        rex.tame(owner);
        owner.setPos(rex.position());
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        helper.assertTrue(rex.canBeSaddled(), "T-Rex deveria aceitar sela");
        rex.interact(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(rex.isSaddled(), "T-Rex deveria ter sido selado");
        helper.succeed();
    }

    /** T-Rex domesticado, selado, virado para +z e montado pelo dono. */
    private static LandCreature mountedRex(GameTestHelper helper, Player owner) {
        return mounted(helper, owner, ModEntities.TYRANNOSAURUS.get());
    }

    private static LandCreature mounted(GameTestHelper helper, Player owner,
                                        net.minecraft.world.entity.EntityType<LandCreature> type) {
        LandCreature rex = helper.spawnWithNoFreeWill(type, 4, 0, 4);
        rex.tame(owner);
        rex.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        rex.setSaddled(true);
        rex.setYRot(0.0F);
        rex.yBodyRot = 0.0F;
        owner.setPos(rex.position());
        helper.assertTrue(rex.ride(owner), "deveria montar");
        return rex;
    }

    @GameTest(template = EMPTY)
    public static void riderCommandsTheMountToBite(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature rex = mountedRex(helper, owner);
        net.minecraft.world.entity.animal.Pig far = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, 1, 2, 14);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(rex.attackAsMount(stranger, far), "quem não conduz não manda morder");
        helper.assertTrue(rex.attackAsMount(owner, far), "a mordida sai mesmo com o alvo longe");
        helper.assertTrue(far.getHealth() == far.getMaxHealth(), "alvo longe demais não deveria ser mordido");
        helper.assertFalse(rex.attackAsMount(owner, null), "segunda mordida no mesmo tick deveria esperar a recarga");

        // Sem ninguém na mira, a mordida pega quem estiver na frente.
        net.minecraft.world.entity.animal.Pig prey = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, 4, 2, 7);
        helper.runAfterDelay(21, () -> {
            helper.assertTrue(rex.attackAsMount(owner, null), "a recarga deveria ter passado");
            helper.assertTrue(prey.getHealth() < prey.getMaxHealth(), "quem está na frente deveria ser mordido");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void bigMountBiteBreaksTheTerrainInFront(GameTestHelper helper) {
        Player owner = helper.makeMockServerPlayerInLevel();
        LandCreature rex = mountedRex(helper, owner);
        helper.setBlock(4, 2, 6, Blocks.DIRT);
        helper.setBlock(4, 3, 7, Blocks.STONE);
        helper.setBlock(3, 4, 6, Blocks.OAK_LOG);
        helper.setBlock(5, 2, 7, Blocks.OBSIDIAN);
        helper.setBlock(5, 3, 6, Blocks.CHEST);
        helper.setBlock(4, 2, 11, Blocks.DIRT);

        helper.assertTrue(rex.attackAsMount(owner, null), "a mordida deveria sair sem alvo");

        helper.assertBlockNotPresent(Blocks.DIRT, 4, 2, 6);
        helper.assertBlockNotPresent(Blocks.STONE, 4, 3, 7);
        helper.assertBlockPresent(Blocks.OAK_LOG, 3, 4, 6);
        helper.assertBlockPresent(Blocks.OBSIDIAN, 5, 2, 7);
        helper.assertBlockPresent(Blocks.CHEST, 5, 3, 6);
        helper.assertBlockPresent(Blocks.DIRT, 4, 2, 11);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mammothBiteHarvestsWoodButNotStone(GameTestHelper helper) {
        Player owner = helper.makeMockServerPlayerInLevel();
        LandCreature mammoth = mounted(helper, owner, ModEntities.MAMMOTH.get());
        helper.setBlock(4, 1, 6, Blocks.OAK_LOG);
        helper.setBlock(3, 2, 7, Blocks.OAK_LEAVES);
        helper.setBlock(5, 0, 6, Blocks.STONE);
        helper.setBlock(4, 0, 7, Blocks.DIRT);

        helper.assertTrue(mammoth.attackAsMount(owner, null), "a mordida deveria sair sem alvo");

        helper.assertBlockNotPresent(Blocks.OAK_LOG, 4, 1, 6);
        helper.assertBlockNotPresent(Blocks.OAK_LEAVES, 3, 2, 7);
        helper.assertBlockPresent(Blocks.STONE, 5, 0, 6);
        helper.assertBlockPresent(Blocks.DIRT, 4, 0, 7);
        helper.assertItemEntityPresent(Items.OAK_LOG, new BlockPos(4, 1, 6), 2.0);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void smallMountBiteLeavesTheTerrain(GameTestHelper helper) {
        Player owner = helper.makeMockServerPlayerInLevel();
        LandCreature smilodon = readyToRide(helper, owner);
        smilodon.setYRot(0.0F);
        owner.setPos(smilodon.position());
        helper.assertTrue(smilodon.ride(owner), "deveria montar");
        helper.setBlock(1, 2, 3, Blocks.DIRT);
        helper.assertTrue(smilodon.attackAsMount(owner, null), "a mordida deveria sair sem alvo");
        helper.assertBlockPresent(Blocks.DIRT, 1, 2, 3);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void seatComesFromTheSpeciesData(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        MountProfile mount = smilodon.mountProfile().orElseThrow();

        double seat = smilodon.getPassengerRidingPosition(owner).y - smilodon.getY();
        helper.assertTrue(Math.abs(seat - mount.seatHeight(smilodon.getBbHeight())) < 0.01,
                "assento em " + seat + ", esperado " + mount.seatHeight(smilodon.getBbHeight()));
        helper.succeed();
    }

    // ---- Selar ----

    @GameTest(template = EMPTY)
    public static void ownerSaddlesWithASaddleInHand(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.tame(owner);
        owner.setPos(smilodon.position());
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));

        smilodon.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(smilodon.isSaddled(), "não selou");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "a sela não foi consumida");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wildCreatureRefusesTheSaddle(GameTestHelper helper) {
        LandCreature wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        Player player = playerAt(helper, wild);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));

        wild.mobInteract(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(!wild.isSaddled(), "criatura selvagem aceitou sela");
        helper.assertTrue(!wild.canBeRiddenBy(player), "criatura selvagem aceitou ser montada");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void unrideableSpeciesNeverAcceptsARider(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature wolf = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 1, 2, 1);
        wolf.tame(owner);
        wolf.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        owner.setPos(wolf.position());

        helper.assertTrue(!wolf.canBeSaddled(), "lobo aceita sela");
        wolf.setSaddled(true);
        helper.assertTrue(!wolf.canBeRiddenBy(owner), "lobo aceitou ser montado com a sela forçada");
        helper.assertTrue(!wolf.ride(owner), "lobo deixou montar");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void saddleSurvivesSaveAndLoad(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature original = readyToRide(helper, owner);

        LandCreature loaded = ModEntities.SMILODON.get().create(helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));

        helper.assertTrue(loaded.isSaddled(), "a sela não persistiu");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void saddleDropsOnDeath(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        smilodon.kill();
        helper.assertItemEntityPresent(Items.SADDLE, new BlockPos(1, 2, 1), 3.0);
        helper.succeed();
    }

    // ---- Montar ----

    @GameTest(template = EMPTY)
    public static void ownerRidesASaddledCreature(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        owner.setPos(smilodon.position());

        helper.assertTrue(smilodon.canBeRiddenBy(owner), "dono não pode montar");
        helper.assertTrue(smilodon.ride(owner), "montaria recusada");
        helper.assertTrue(owner.getVehicle() == smilodon, "o jogador não subiu");
        helper.assertTrue(smilodon.getControllingPassenger() == owner, "quem monta não está no controle");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ridingReleasesACreatureToldToStay(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        owner.setPos(smilodon.position());
        smilodon.setOrder(CreatureOrder.STAY);

        helper.assertTrue(smilodon.ride(owner), "montaria recusada");
        helper.assertTrue(smilodon.order().followsOwner() && !smilodon.isOrderedToSit(),
                "continuou mandada ficar: " + smilodon.order());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void withoutSaddleTheCreatureRefuses(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        owner.setPos(smilodon.position());

        helper.assertTrue(!smilodon.ride(owner), "montou sem sela");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lowAffinityRefusesTheRider(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        owner.setPos(smilodon.position());
        smilodon.setAffinity(smilodon.mountProfile().orElseThrow().minAffinity() - 1.0F);

        helper.assertTrue(!smilodon.canBeRiddenBy(owner), "montou com afinidade abaixo do mínimo");
        helper.assertTrue(!smilodon.ride(owner), "montaria aceita com afinidade baixa");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void strangerCannotRide(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        Player stranger = playerAt(helper, smilodon);

        helper.assertTrue(!smilodon.canBeRiddenBy(stranger), "estranho pode montar");
        helper.assertTrue(!smilodon.ride(stranger), "estranho montou");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void onlyOneRiderAtATime(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        owner.setPos(smilodon.position());
        helper.assertTrue(smilodon.ride(owner), "montaria recusada");

        // Já ocupada: nem o próprio dono sobe duas vezes, nem um estranho entra na garupa.
        helper.assertTrue(!smilodon.ride(owner), "o dono montou duas vezes");
        helper.assertTrue(!smilodon.ride(playerAt(helper, smilodon)), "aceitou um segundo passageiro");
        helper.assertTrue(smilodon.getPassengers().size() == 1, "passageiros: " + smilodon.getPassengers().size());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void knockoutThrowsTheRiderOff(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        owner.setPos(smilodon.position());
        helper.assertTrue(smilodon.ride(owner), "montaria recusada");

        smilodon.setTorpor(smilodon.maxTorpor());

        helper.assertTrue(smilodon.isUnconscious(), "não caiu");
        helper.assertTrue(!owner.isPassenger(), "continuou montado numa criatura inconsciente");
        helper.assertTrue(smilodon.getControllingPassenger() == null, "criatura inconsciente ainda tem controlador");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void removingTheSaddleThrowsTheRiderOff(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = readyToRide(helper, owner);
        owner.setPos(smilodon.position());
        helper.assertTrue(smilodon.ride(owner), "montaria recusada");

        smilodon.setSaddled(false);

        helper.assertTrue(!owner.isPassenger(), "continuou montado sem sela");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void debugTameGivesAUsableCreature(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        owner.setPos(smilodon.position());
        int wildLevel = smilodon.creatureLevel();

        smilodon.debugTame(owner);

        helper.assertTrue(smilodon.isTame() && smilodon.isOwner(owner), "não domesticou para quem chamou");
        helper.assertTrue(smilodon.affinity() == PrehistoricCreature.MAX_AFFINITY,
                "afinidade " + smilodon.affinity());
        helper.assertTrue(smilodon.creatureLevel() > wildLevel, "sem os níveis bônus da domesticação perfeita");
        helper.assertTrue(smilodon.torpor() == 0 && !smilodon.isUnconscious(), "ficou inconsciente");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void setCreatureLevelRerollsTheStats(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        smilodon.setCreatureLevel(150);
        helper.assertTrue(smilodon.creatureLevel() == 150, "nível " + smilodon.creatureLevel());
        helper.assertTrue(smilodon.getHealth() == smilodon.getMaxHealth(), "não veio com a vida cheia");
        helper.assertTrue(smilodon.torpor() <= smilodon.maxTorpor(), "torpor acima do novo máximo");
        helper.succeed();
    }
}
