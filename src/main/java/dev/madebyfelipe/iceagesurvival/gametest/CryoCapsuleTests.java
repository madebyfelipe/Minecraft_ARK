package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.genetics.Genome;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.CryoCapsuleItem;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** MC-19: cápsula criogênica guarda a criatura do dono inteira e a solta em outro lugar, sem duplicar nem perder. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CryoCapsuleTests {
    private static final String ARENA = "arena";

    private static Player ownerNear(GameTestHelper helper, PrehistoricCreature creature) {
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(creature.position().add(2, 0, 0));
        creature.tame(player);
        return player;
    }

    @GameTest(template = ARENA, batch = "cryo_1")
    public static void capsuleFreezesAndReleasesTheWholeCreature(GameTestHelper helper) {
        LandCreature original = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, original);
        Genome genome = new Genome(StatPoints.NONE.with(Stat.HEALTH, 5), new int[]{1, 0, 0, 0, 0, 0}, true);
        original.setGenome(genome);
        original.setCustomName(Component.literal("Dente"));
        original.setSaddled(true);
        original.inventory().setItem(0, new ItemStack(Items.BONE, 7));
        UUID id = original.getUUID();

        ItemStack capsule = new ItemStack(ModItems.CRYO_CAPSULE.get());
        helper.assertTrue(CryoCapsuleItem.freeze(owner, capsule, original), "não congelou");
        helper.assertTrue(original.isRemoved() && CryoCapsuleItem.holdsCreature(capsule), "criatura ficou no mundo");
        LandCreature other = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 8, 1, 8);
        other.tame(owner);
        helper.assertFalse(CryoCapsuleItem.freeze(owner, capsule, other), "cápsula cheia aceitou outra criatura");

        Vec3 spot = helper.absoluteVec(new Vec3(10.5, 1, 4.5));
        helper.assertTrue(CryoCapsuleItem.release(helper.getLevel(), owner, capsule, spot), "não soltou");
        helper.assertFalse(CryoCapsuleItem.holdsCreature(capsule), "cápsula não esvaziou");
        helper.assertFalse(CryoCapsuleItem.release(helper.getLevel(), owner, capsule, spot), "soltou duas vezes");
        PrehistoricCreature thawed = (PrehistoricCreature) helper.getLevel().getEntity(id);
        helper.assertTrue(thawed != null && thawed.position().distanceTo(spot) < 0.01, "não apareceu no lugar");
        helper.assertTrue(thawed.genome().equals(genome) && owner.getUUID().equals(thawed.getOwnerUUID()),
                "perdeu genoma ou dono");
        helper.assertTrue("Dente".equals(thawed.getName().getString()) && thawed.isSaddled()
                && thawed.inventory().getItem(0).getCount() == 7, "perdeu nome, sela ou inventário");
        thawed.discard();
        other.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "cryo_2")
    public static void onlyTheOwnerFreezesAndBlockedSpotKeepsTheCreature(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, creature);
        Player stranger = helper.makeMockSurvivalPlayer();
        stranger.setPos(creature.position());
        ItemStack capsule = new ItemStack(ModItems.CRYO_CAPSULE.get());
        helper.assertFalse(CryoCapsuleItem.freeze(stranger, capsule, creature), "outro jogador congelou");
        helper.assertTrue(CryoCapsuleItem.freeze(owner, capsule, creature), "dono não congelou");

        BlockPos wall = new BlockPos(10, 1, 10);
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(wall.above(), Blocks.STONE);
        helper.assertFalse(CryoCapsuleItem.release(helper.getLevel(), owner, capsule,
                Vec3.atBottomCenterOf(helper.absolutePos(wall))), "soltou dentro da pedra");
        helper.assertTrue(CryoCapsuleItem.holdsCreature(capsule), "perdeu a criatura ao falhar");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "cryo_3")
    public static void frozenClocksAreStoredAsTimeLeft(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        ownerNear(helper, creature);
        var tag = creature.freezeData();
        helper.assertFalse(tag.contains("NextMating") || tag.contains("NextFeedTime") || tag.contains("Pos"),
                "relógios absolutos ou posição ficaram na cápsula");
        helper.assertTrue(tag.contains("FrozenMatingLeft") && tag.contains("FrozenFeedLeft"), "sem o tempo restante");
        creature.discard();
        helper.succeed();
    }
}
