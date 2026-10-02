package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.ChemistryBenchBlockEntity;
import dev.madebyfelipe.iceagesurvival.block.IncubatorBlockEntity;
import dev.madebyfelipe.iceagesurvival.core.genetics.Genome;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.CreatureEggItem;
import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Etapa 8: acasalamento, gestação, ovo, incubadora, mesa química e o genoma salvo. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BreedingTests {
    private static final String ARENA = "arena";
    /** Acasalar leva {@link PrehistoricCreature#MATING_SECONDS} s, conferidos de segundo em segundo. */
    private static final int MATING_TICKS = PrehistoricCreature.MATING_SECONDS * 20 + 40;

    private static LandCreature parent(GameTestHelper helper, EntityType<LandCreature> type, Player owner,
                                       boolean female, int x) {
        LandCreature creature = helper.spawnWithNoFreeWill(type, x, 1, 4);
        creature.tame(owner);
        creature.setFemale(female);
        creature.setMatingEnabled(true);
        return creature;
    }

    @GameTest(template = ARENA, batch = "breeding_1", timeoutTicks = MATING_TICKS + 40)
    public static void mammalPairConceivesALiveBirth(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature mother = parent(helper, ModEntities.SMILODON.get(), owner, true, 4);
        parent(helper, ModEntities.SMILODON.get(), owner, false, 6);
        helper.runAtTickTime(MATING_TICKS, () -> {
            helper.assertTrue(mother.isPregnant(), "a fêmea deveria estar prenhe");
            helper.assertTrue(mother.gestationProgress() >= 0.0F, "gestação sem progresso");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "breeding_2", timeoutTicks = MATING_TICKS + 40)
    public static void dinosaurPairLaysAnEgg(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature mother = parent(helper, ModEntities.VELOCIRAPTOR.get(), owner, true, 4);
        parent(helper, ModEntities.VELOCIRAPTOR.get(), owner, false, 6);
        helper.runAtTickTime(MATING_TICKS, () -> {
            List<ItemEntity> eggs = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    mother.getBoundingBox().inflate(4.0), item -> item.getItem().is(ModItems.CREATURE_EGG.get()));
            helper.assertTrue(eggs.size() == 1, "esperava um ovo, achei " + eggs.size());
            ItemStack egg = eggs.get(0).getItem();
            helper.assertTrue(CreatureEggItem.species(egg).orElse(null) == ModEntities.VELOCIRAPTOR.get(), "ovo sem espécie");
            helper.assertTrue(owner.getUUID().equals(CreatureEggItem.owner(egg)), "ovo sem dono");
            helper.assertFalse(mother.isPregnant(), "dinossauro não fica prenhe");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "breeding_3", timeoutTicks = MATING_TICKS + 40)
    public static void sameSexOrStrangersDoNotMate(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        LandCreature female = parent(helper, ModEntities.SMILODON.get(), owner, true, 4);
        parent(helper, ModEntities.SMILODON.get(), owner, true, 6);
        parent(helper, ModEntities.SMILODON.get(), stranger, false, 8);
        helper.runAtTickTime(MATING_TICKS, () -> {
            helper.assertFalse(female.isPregnant(), "cruzou com fêmea ou com criatura de outro dono");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "breeding_4")
    public static void offspringIsATamedBabyWithTheGivenGenome(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Genome genome = new Genome(StatPoints.NONE.with(Stat.ATTACK, 6), new int[]{0, 3, 2, 0, 0}, true);
        PrehistoricCreature baby = PrehistoricCreature.spawnOffspring(helper.getLevel(), ModEntities.SMILODON.get(),
                genome, owner.getUUID(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 1, 4.5)));
        helper.assertTrue(baby != null && baby.isBaby(), "não nasceu filhote");
        helper.assertTrue(baby.isTame() && owner.getUUID().equals(baby.getOwnerUUID()), "filhote sem dono");
        helper.assertTrue(baby.genome().equals(genome), "genoma diferente: " + baby.genome());
        helper.assertTrue(baby.maturationProgress() < 0.01F, "recém-nascido já crescido");
        helper.assertTrue(baby.getBbHeight() < ModEntities.SMILODON.get().getHeight() * 0.5F, "filhote grande demais");
        helper.assertFalse(baby.canBeSaddled(), "filhote aceitando sela");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "breeding_5")
    public static void speedMutationsSpeedTheCreatureUp(GameTestHelper helper) {
        LandCreature plain = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 2, 1, 2);
        LandCreature mutant = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 6, 1, 2);
        plain.setGenome(Genome.wild(StatPoints.NONE, false));
        mutant.setGenome(new Genome(StatPoints.NONE, new int[]{0, 0, 10, 0, 0}, false));
        double ratio = mutant.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                / plain.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(ratio - 1.3) < 1e-6, "10 mutações de velocidade deram ×" + ratio);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "breeding_6")
    public static void genomeSexAndGestationSurviveSaveAndLoad(GameTestHelper helper) {
        LandCreature original = helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 4, 1, 4);
        Genome genome = new Genome(StatPoints.NONE.with(Stat.HEALTH, 4), new int[]{2, 1, 0, 0, 0}, true);
        original.setGenome(genome);
        original.setFemale(true);
        original.setMatingEnabled(true);

        LandCreature loaded = ModEntities.MAMMOTH.get().create(helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));
        loaded.setUUID(UUID.randomUUID());
        helper.assertTrue(loaded.genome().equals(genome), "genoma não persistiu: " + loaded.genome());
        helper.assertTrue(loaded.isFemale() && loaded.isMatingEnabled(), "sexo ou acasalamento não persistiram");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "breeding_7")
    public static void incubatorHatchesAnEggWithFuel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, ModBlocks.INCUBATOR.get());
        IncubatorBlockEntity incubator = (IncubatorBlockEntity) helper.getBlockEntity(pos);
        Player owner = helper.makeMockSurvivalPlayer();
        Genome genome = Genome.wild(StatPoints.NONE.with(Stat.ARMOR, 3), false);
        incubator.setItem(IncubatorBlockEntity.EGG_SLOT, CreatureEggItem.create(ModEntities.VELOCIRAPTOR.get(), genome, owner.getUUID()));
        incubator.setItem(IncubatorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 8));

        int incubation = 300 * 20;
        for (int tick = 0; tick <= incubation + 1 && !incubator.getItem(IncubatorBlockEntity.EGG_SLOT).isEmpty(); tick++) {
            incubator.serverTick();
        }
        helper.assertTrue(incubator.getItem(IncubatorBlockEntity.EGG_SLOT).isEmpty(),
                "o ovo não chocou; heated=" + incubator.isHeated() + ", combustível="
                        + incubator.getItem(IncubatorBlockEntity.FUEL_SLOT).getCount()
                        + ", tempo de queima=" + incubator.getItem(IncubatorBlockEntity.FUEL_SLOT)
                                .getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING));
        List<LandCreature> babies = helper.getLevel().getEntitiesOfClass(LandCreature.class,
                new AABB(helper.absolutePos(pos)).inflate(2.0), LandCreature::isBaby);
        helper.assertTrue(babies.size() == 1, "esperava um filhote, achei " + babies.size());
        helper.assertTrue(babies.get(0).genome().equals(genome) && owner.getUUID().equals(babies.get(0).getOwnerUUID()),
                "filhote sem o genoma ou o dono do ovo");
        helper.assertTrue(incubator.getItem(IncubatorBlockEntity.FUEL_SLOT).getCount() < 8, "não gastou combustível");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "breeding_8")
    public static void coldIncubatorDoesNotHatch(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, ModBlocks.INCUBATOR.get());
        IncubatorBlockEntity incubator = (IncubatorBlockEntity) helper.getBlockEntity(pos);
        incubator.setItem(IncubatorBlockEntity.EGG_SLOT,
                CreatureEggItem.create(ModEntities.VELOCIRAPTOR.get(), Genome.wild(StatPoints.NONE, false), null));
        for (int tick = 0; tick <= 300 * 20 + 1; tick++) {
            incubator.serverTick();
        }
        helper.assertFalse(incubator.getItem(IncubatorBlockEntity.EGG_SLOT).isEmpty(), "chocou sem calor");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "breeding_9")
    public static void chemistryBenchDoublesTheNarcotic(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, ModBlocks.CHEMISTRY_BENCH.get());
        ChemistryBenchBlockEntity bench = (ChemistryBenchBlockEntity) helper.getBlockEntity(pos);
        bench.setItem(ChemistryBenchBlockEntity.INPUT_A, new ItemStack(Items.ROTTEN_FLESH, 1));
        bench.setItem(ChemistryBenchBlockEntity.INPUT_B, new ItemStack(ModItems.BLACK_FRUIT.get(), 1));
        for (int tick = 0; tick < 100; tick++) {
            bench.serverTick();
        }
        ItemStack output = bench.getItem(ChemistryBenchBlockEntity.OUTPUT);
        helper.assertTrue(output.is(ModItems.NARCOTIC.get()) && output.getCount() == 2, "saída: " + output);
        helper.assertTrue(bench.getItem(ChemistryBenchBlockEntity.INPUT_A).isEmpty()
                && bench.getItem(ChemistryBenchBlockEntity.INPUT_B).isEmpty(), "entradas não consumidas");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "breeding_10")
    public static void stimulantLowersTorpor(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 1, 4);
        smilodon.setTorpor(smilodon.maxTorpor() * 0.5);
        double before = smilodon.torpor();
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STIMULANT.get()));
        smilodon.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(smilodon.torpor() < before, "torpor não caiu: " + smilodon.torpor());
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "estimulante não foi gasto");
        helper.succeed();
    }
}
