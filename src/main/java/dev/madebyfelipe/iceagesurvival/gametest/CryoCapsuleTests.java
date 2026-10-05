package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.genetics.Genome;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.cryo.CryoStorage;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.CryoCapsuleItem;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * MC-19/MC-21: a criogenia do menu guarda a criatura do dono inteira, por jogador, e a solta em outro lugar, sem
 * duplicar nem perder; as cápsulas antigas viram entradas.
 */
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
    public static void storageFreezesAndReleasesTheWholeCreature(GameTestHelper helper) {
        LandCreature original = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, original);
        Genome genome = new Genome(StatPoints.NONE.with(Stat.HEALTH, 5), new int[]{1, 0, 0, 0, 0, 0}, true);
        original.setGenome(genome);
        original.setCustomName(Component.literal("Dente"));
        original.setSaddled(true);
        original.inventory().setItem(0, new ItemStack(Items.BONE, 7));
        UUID id = original.getUUID();

        helper.assertTrue(CryoStorage.freeze(owner, original), "não congelou");
        helper.assertTrue(original.isRemoved(), "criatura ficou no mundo");
        helper.assertFalse(CryoStorage.freeze(owner, original), "congelou duas vezes");
        List<CryoStorage.Entry> entries = CryoStorage.entries(owner);
        helper.assertTrue(entries.size() == 1 && id.equals(entries.get(0).creature())
                && "Dente".equals(entries.get(0).name()), "entrada errada: " + entries);
        UUID entry = entries.get(0).id();

        Vec3 spot = helper.absoluteVec(new Vec3(10.5, 1, 4.5));
        helper.assertTrue(CryoStorage.release(helper.getLevel(), owner, entry, spot), "não soltou");
        helper.assertTrue(CryoStorage.entries(owner).isEmpty(), "entrada ficou depois de soltar");
        helper.assertFalse(CryoStorage.release(helper.getLevel(), owner, entry, spot.add(3, 0, 0)), "soltou duas vezes");
        PrehistoricCreature thawed = (PrehistoricCreature) helper.getLevel().getEntity(id);
        helper.assertTrue(thawed != null && thawed.position().distanceTo(spot) < 0.01, "não apareceu no lugar");
        helper.assertTrue(thawed.genome().equals(genome) && owner.getUUID().equals(thawed.getOwnerUUID()),
                "perdeu genoma ou dono");
        helper.assertTrue("Dente".equals(thawed.getName().getString()) && thawed.isSaddled()
                && thawed.inventory().getItem(0).getCount() == 7, "perdeu nome, sela ou inventário");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrehistoricCreature.class,
                thawed.getBoundingBox().inflate(16), creature -> id.equals(creature.getUUID())).size() == 1,
                "duplicou a criatura");
        thawed.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "cryo_2")
    public static void onlyTheOwnerFreezesAndBlockedSpotKeepsTheEntry(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, creature);
        Player stranger = helper.makeMockSurvivalPlayer();
        stranger.setPos(creature.position());
        helper.assertFalse(CryoStorage.freeze(stranger, creature), "outro jogador congelou");
        helper.assertTrue(CryoStorage.entries(stranger).isEmpty() && !creature.isRemoved(), "estranho guardou");
        helper.assertTrue(CryoStorage.freeze(owner, creature), "dono não congelou");
        UUID entry = CryoStorage.entries(owner).get(0).id();
        helper.assertFalse(CryoStorage.release(helper.getLevel(), stranger, entry,
                helper.absoluteVec(new Vec3(10.5, 1, 4.5))), "estranho soltou a criatura do outro");

        BlockPos wall = new BlockPos(10, 1, 10);
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(wall.above(), Blocks.STONE);
        helper.assertFalse(CryoStorage.release(helper.getLevel(), owner, entry,
                Vec3.atBottomCenterOf(helper.absolutePos(wall))), "soltou dentro da pedra");
        List<CryoStorage.Entry> kept = CryoStorage.entries(owner);
        helper.assertTrue(kept.size() == 1 && kept.get(0).id().equals(entry)
                && kept.get(0).data().contains("UUID"), "perdeu ou esvaziou a entrada ao falhar");

        helper.assertTrue(CryoStorage.release(helper.getLevel(), owner, entry, helper.absoluteVec(new Vec3(4.5, 1, 4.5))),
                "não soltou depois de falhar");
        helper.getLevel().getEntity(creature.getUUID()).discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "cryo_3")
    public static void frozenClocksAreStoredAsTimeLeft(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, creature);
        helper.assertTrue(CryoStorage.freeze(owner, creature), "não congelou");
        CompoundTag tag = CryoStorage.entries(owner).get(0).data();
        helper.assertFalse(tag.contains("NextMating") || tag.contains("NextFeedTime") || tag.contains("Pos"),
                "relógios absolutos ou posição ficaram na criogenia");
        helper.assertTrue(tag.contains("FrozenMatingLeft") && tag.contains("FrozenFeedLeft"), "sem o tempo restante");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "cryo_4")
    public static void legacyCapsulesBecomeEntriesOnce(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, creature);
        creature.setCustomName(Component.literal("Antigo"));
        UUID id = creature.getUUID();
        ItemStack full = CryoCapsuleItem.legacyCapsule(BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()),
                creature.freezeData(), creature.creatureLevel(), "Antigo");
        ItemStack copy = full.copy(); // como uma cópia de criativo da mesma cápsula
        creature.discard();

        owner.getInventory().setItem(3, full);
        full.inventoryTick(helper.getLevel(), owner, 3, false);
        helper.assertTrue(owner.getInventory().getItem(3).isEmpty(), "cápsula cheia ficou no inventário");
        helper.assertTrue(CryoStorage.entries(owner).size() == 1, "não virou entrada");
        helper.assertFalse(CryoCapsuleItem.convert(owner, full), "converteu duas vezes");
        owner.getInventory().setItem(5, copy);
        CryoCapsuleItem.convertAll(owner, owner.getInventory());
        helper.assertTrue(owner.getInventory().getItem(5).isEmpty() && CryoStorage.entries(owner).size() == 1,
                "a cópia da mesma criatura entrou de novo");

        ItemStack empty = new ItemStack(ModItems.CRYO_CAPSULE.get());
        owner.getInventory().setItem(6, empty);
        empty.inventoryTick(helper.getLevel(), owner, 6, false);
        helper.assertTrue(owner.getInventory().getItem(6).isEmpty() && CryoStorage.entries(owner).size() == 1,
                "cápsula vazia ficou ou virou entrada");

        UUID entry = CryoStorage.entries(owner).get(0).id();
        helper.assertTrue(CryoStorage.release(helper.getLevel(), owner, entry, helper.absoluteVec(new Vec3(10.5, 1, 4.5))),
                "não soltou a criatura convertida");
        PrehistoricCreature thawed = (PrehistoricCreature) helper.getLevel().getEntity(id);
        helper.assertTrue(thawed != null && "Antigo".equals(thawed.getName().getString())
                && owner.getUUID().equals(thawed.getOwnerUUID()), "a convertida perdeu identidade ou dono");
        thawed.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "cryo_5")
    public static void storageSurvivesSaveAndLoad(GameTestHelper helper) {
        LandCreature creature = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        Player owner = ownerNear(helper, creature);
        creature.setCustomName(Component.literal("Salvo"));
        helper.assertTrue(CryoStorage.freeze(owner, creature), "não congelou");
        CryoStorage data = CryoStorage.get(helper.getLevel().getServer());
        helper.assertTrue(data != null && data.isDirty(), "não marcou para salvar");

        CompoundTag saved = data.save(new CompoundTag());
        CryoStorage loaded = CryoStorage.load(saved);
        List<CryoStorage.Entry> before = data.list(owner.getUUID());
        List<CryoStorage.Entry> after = loaded.list(owner.getUUID());
        helper.assertTrue(before.size() == 1 && before.equals(after), "perdeu a entrada ao salvar: " + after);
        helper.assertTrue(creature.getUUID().equals(after.get(0).creature()) && "Salvo".equals(after.get(0).name()),
                "entrada carregada errada");
        helper.succeed();
    }
}
