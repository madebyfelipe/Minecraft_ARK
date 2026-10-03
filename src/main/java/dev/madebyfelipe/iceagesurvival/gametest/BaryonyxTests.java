package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.ai.FishingGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.FleeWhenWeakGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.WaterEdgeStrollGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.FishingProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.TamingProfile;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O Baryonyx: espinossaurídeo pescador da beira d'água. Pesca (selvagem come, domesticado com "Parar" guarda o peixe),
 * fisga com a garra-gancho, não se afoga, foge ferido para a água e, montado, nada sem derrubar quem monta.
 *
 * <p>As cenas com água constroem o próprio chão de pedra: não dependem do relevo do mundo do gametest.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class BaryonyxTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    private static Species species(GameTestHelper helper) {
        var species = Species.of(helper.getLevel().registryAccess(), ModEntities.BARYONYX.get()).orElse(null);
        helper.assertTrue(species != null, "o Baryonyx não carregou");
        return species;
    }

    /** Presa que aguenta o golpe de 18: o gancho é visto no puxão e na lentidão, não na morte. */
    private static <T extends LivingEntity> T sturdy(T prey) {
        prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
        prey.setHealth(400.0F);
        return prey;
    }

    /**
     * Uma laje de pedra (x0..x1, z0..z1) com o chão em y 0 e a terra firme em y 1; dentro dela, uma lagoa rasa (um
     * bloco de fundo) de px0..px1 em x, cercada pela borda da laje, com ar por cima. Quem fica na terra firme pisa em y 2.
     */
    private static void shore(GameTestHelper helper, int x0, int x1, int z0, int z1, int px0, int px1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                boolean pond = x >= px0 && x <= px1 && x > x0 && x < x1 && z > z0 && z < z1;
                helper.setBlock(x, 1, z, pond ? Blocks.WATER : Blocks.STONE);
                helper.setBlock(x, 2, z, Blocks.AIR);
                helper.setBlock(x, 3, z, Blocks.AIR);
            }
        }
    }

    /** Um tanque fundo: paredes de pedra em volta de x0..x1 × z0..z1, água de y 1 a {@code depth}, chão em y 0. */
    private static void tank(GameTestHelper helper, int x0, int x1, int z0, int z1, int depth) {
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                boolean inside = x >= x0 && x <= x1 && z >= z0 && z <= z1;
                for (int y = 1; y <= depth + 1; y++) {
                    helper.setBlock(x, y, z, !inside ? Blocks.STONE : y <= depth ? Blocks.WATER : Blocks.AIR);
                }
            }
        }
    }

    private static int count(PrehistoricCreature creature, Item item) {
        int total = 0;
        for (int slot = 0; slot < creature.inventory().getContainerSize(); slot++) {
            ItemStack stack = creature.inventory().getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int fishInInventory(PrehistoricCreature creature) {
        return count(creature, Items.COD) + count(creature, Items.SALMON) + count(creature, Items.TROPICAL_FISH)
                + count(creature, Items.PUFFERFISH);
    }

    // ---- Espécie e dados ----

    /** Predador médio da beira d'água: vida 150, ataque 18, garra-gancho, pescador, anfíbio e montaria que nada. */
    @GameTest(template = EMPTY, batch = "baryonyx_species")
    public static void theBaryonyxIsAFishingSpinosaurid(GameTestHelper helper) {
        Species species = species(helper);
        helper.assertTrue(species.stats().entry(Stat.HEALTH).base() == 150.0,
                "vida base: " + species.stats().entry(Stat.HEALTH).base());
        helper.assertTrue(species.stats().entry(Stat.ATTACK).base() == 18.0,
                "ataque base: " + species.stats().entry(Stat.ATTACK).base());
        BehaviorProfile behavior = species.behavior().orElseThrow();
        helper.assertTrue(behavior.huntSpecial() == BehaviorProfile.HuntSpecial.GAFF,
                "deveria caçar com a garra-gancho: " + behavior.huntSpecial());
        helper.assertTrue(behavior.prey().map(tag -> tag.location().getPath().equals("baryonyx_prey")).orElse(false),
                "deveria caçar #baryonyx_prey: " + behavior.prey());
        FishingProfile fishing = behavior.habits().fishing().orElse(null);
        helper.assertTrue(fishing != null, "deveria ser pescador (behavior.habits.fishing)");
        helper.assertTrue(fishing.chance() > 0.0 && fishing.chance() < 1.0, "chance de pesca: " + fishing.chance());
        helper.assertTrue(fishing.radius() > 0.0 && fishing.tameRadius() > 0.0,
                "raios da pesca: " + fishing.radius() + " / " + fishing.tameRadius());
        var body = species.body().orElseThrow();
        helper.assertTrue(body.amphibious(), "deveria ser anfíbio (body.amphibious)");
        helper.assertTrue(species.mount().orElseThrow().swims(), "deveria ser montaria que nada (mount.swims)");
        EntityType<LandCreature> type = ModEntities.BARYONYX.get();
        helper.assertTrue(type.is(ModTags.PREDATORS), "deveria estar em #predators");
        helper.assertTrue(WildSpawner.isCarnivore(helper.getLevel(), type), "deveria contar como carnívoro no spawn");
        helper.assertFalse(type.is(ModTags.FISH), "o Baryonyx não é peixe");
        helper.assertTrue(EntityType.COD.is(TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("baryonyx_prey"))),
                "o peixe deveria estar nas presas do Baryonyx");
        helper.succeed();
    }

    /**
     * Solitário ou em par, a 300+ blocos do spawn do mundo, na planície nevada (o mundo Era do Gelo) e mais comum nos
     * biomas de água.
     */
    @GameTest(template = EMPTY, batch = "baryonyx_species")
    public static void theBaryonyxSpawnsFarAwayAndNearWater(GameTestHelper helper) {
        SpawnProfile spawn = species(helper).spawn().orElseThrow();
        helper.assertTrue(spawn.minDistance() == 300, "distância mínima do spawn: " + spawn.minDistance());
        helper.assertTrue(spawn.groupMin() == 1 && spawn.groupMax() == 1,
                "deveria nascer sozinho: " + spawn.groupMin() + ".." + spawn.groupMax());
        double pair = spawn.family().map(family -> family.pairChance()).orElse(0.0);
        helper.assertTrue(pair > 0.0 && pair < 1.0, "chance de nascer em par: " + pair);
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var snowyPlains = biomes.getHolderOrThrow(Biomes.SNOWY_PLAINS);
        var river = biomes.getHolderOrThrow(Biomes.RIVER);
        var frozenRiver = biomes.getHolderOrThrow(Biomes.FROZEN_RIVER);
        helper.assertTrue(snowyPlains.is(spawn.biomes()), "deveria nascer na planície nevada");
        helper.assertTrue(river.is(spawn.biomes()) && frozenRiver.is(spawn.biomes()), "deveria nascer nos rios");
        TagKey<Biome> ideal = TagKey.create(Registries.BIOME, IceAgeSurvival.id("ideal_baryonyx"));
        helper.assertTrue(spawn.favored().map(favored -> favored.biomes().equals(ideal)).orElse(false),
                "deveria preferir #ideal_baryonyx: " + spawn.favored());
        var plains = biomes.getHolderOrThrow(Biomes.PLAINS);
        var swamp = biomes.getHolderOrThrow(Biomes.SWAMP);
        helper.assertTrue(river.is(ideal) && swamp.is(ideal), "rio e pântano são o habitat dele");
        helper.assertTrue(spawn.weightIn(river) > spawn.weightIn(plains),
                "deveria ser mais comum no rio (" + spawn.weightIn(river) + ") que na planície ("
                        + spawn.weightIn(plains) + ")");
        helper.succeed();
    }

    /** Peixe cru, charque, carne vermelha crua e carne de dodô crua e assada domesticam o Baryonyx. */
    @GameTest(template = EMPTY, batch = "baryonyx_species")
    public static void theBaryonyxIsTamedWithFishAndMeat(GameTestHelper helper) {
        TamingProfile taming = species(helper).taming().orElseThrow();
        for (Item item : new Item[] {Items.COD, Items.SALMON, ModItems.JERKY.get(), Items.BEEF,
                ModItems.DODO_MEAT.get(), ModItems.COOKED_DODO_MEAT.get()}) {
            helper.assertTrue(taming.foodFor(new ItemStack(item)).isPresent(), "não aceita " + item + " na domesticação");
        }
        helper.assertTrue(taming.foodFor(new ItemStack(Items.WHEAT)).isEmpty(), "carnívoro aceitando trigo");
        var fish = taming.foodFor(new ItemStack(Items.COD)).orElseThrow();
        var cooked = taming.foodFor(new ItemStack(ModItems.COOKED_DODO_MEAT.get())).orElseThrow();
        helper.assertTrue(fish.value() * fish.quality() > cooked.value() * cooked.quality(),
                "o peixe cru deveria ser a melhor comida, acima da carne assada");
        helper.succeed();
    }

    /** O Espinossauro o domina: está nas presas dele, e o Espinossauro com fome o caça. */
    @GameTest(template = ARENA, batch = "baryonyx_spinosaurus", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theSpinosaurusHuntsTheBaryonyx(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        TagKey<EntityType<?>> spinoPrey = TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("spinosaurus_prey"));
        helper.assertTrue(ModEntities.BARYONYX.get().is(spinoPrey), "deveria estar em #spinosaurus_prey");
        LandCreature spino = helper.spawnWithNoFreeWill(ModEntities.SPINOSAURUS.get(), 4, 0, 4);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 16, 0, 16);
        spino.starve();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(spino.sizeRatioOf(baryonyx) < 1.0, "o Baryonyx deveria ser menor que o Espinossauro");
            helper.assertTrue(HuntGoal.wouldHunt(spino, baryonyx, spinoPrey),
                    "o Espinossauro com fome deveria caçar o Baryonyx");
            helper.succeed();
        });
    }

    // ---- Pesca: o bote ----

    /** Só se pesca em água rasa (1 a 2 blocos) com ar por cima: gelo tampa a água. */
    @GameTest(template = ARENA, batch = "baryonyx_fishable")
    public static void onlyShallowOpenWaterIsFishable(GameTestHelper helper) {
        tank(helper, 2, 2, 2, 2, 1);
        tank(helper, 6, 6, 2, 2, 2);
        tank(helper, 10, 10, 2, 2, 4);
        tank(helper, 14, 14, 2, 2, 1);
        helper.setBlock(14, 2, 2, Blocks.ICE);
        var level = helper.getLevel();
        helper.assertTrue(FishingGoal.fishable(level, helper.absolutePos(new BlockPos(2, 1, 2))),
                "água de um bloco aberta deveria dar pesca");
        helper.assertTrue(FishingGoal.fishable(level, helper.absolutePos(new BlockPos(6, 2, 2))),
                "água de dois blocos aberta deveria dar pesca");
        helper.assertFalse(FishingGoal.fishable(level, helper.absolutePos(new BlockPos(10, 4, 2))),
                "água de quatro blocos é funda demais para vadear");
        helper.assertFalse(FishingGoal.fishable(level, helper.absolutePos(new BlockPos(14, 1, 2))),
                "água coberta de gelo não deveria dar pesca");
        helper.assertFalse(FishingGoal.fishable(level, helper.absolutePos(new BlockPos(2, 2, 2))),
                "ar não é água");
        helper.succeed();
    }

    /** Selvagem: o peixe vivo ao alcance do bote é captura certa; o peixe some e o Baryonyx come (a fome cai). */
    @GameTest(template = ARENA, batch = "baryonyx_strike_wild", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aWildBaryonyxEatsTheLiveFishItStrikes(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 14, 2, 10, 2, 7);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 9, 2, 6);
        Cod cod = helper.spawnWithNoFreeWill(EntityType.COD, 7, 1, 6);
        baryonyx.starve();
        long hungerBefore = baryonyx.ticksSinceMeal();
        BlockPos water = helper.absolutePos(new BlockPos(7, 1, 6));
        helper.assertTrue(baryonyx.nearestLiveFish() == cod, "o bacalhau deveria estar ao alcance do bote");
        helper.assertTrue(baryonyx.resolveFishingStrike(water), "peixe vivo ao alcance deveria ser captura certa");
        helper.assertFalse(cod.isAlive(), "o peixe apanhado deveria sumir");
        helper.assertTrue(baryonyx.fishCaught() == 1, "peixes apanhados: " + baryonyx.fishCaught());
        helper.assertTrue(baryonyx.ticksSinceMeal() < hungerBefore, "comeu o peixe e a fome não caiu");
        helper.assertTrue(fishInInventory(baryonyx) == 0, "o selvagem guardou o peixe em vez de comer");
        helper.succeed();
    }

    /** Selvagem e sem peixe à vista: algum bote acaba apanhando (a chance da espécie), e ele come. */
    @GameTest(template = ARENA, batch = "baryonyx_strike_chance", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aWildBaryonyxSometimesCatchesWithoutSeeingAFish(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 14, 2, 10, 2, 7);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 9, 2, 6);
        baryonyx.starve();
        long hungerBefore = baryonyx.ticksSinceMeal();
        BlockPos water = helper.absolutePos(new BlockPos(7, 1, 6));
        helper.assertTrue(baryonyx.nearestLiveFish() == null, "não deveria haver peixe vivo na cena");
        int strikes = 0;
        boolean caught = false;
        while (!caught && strikes < 200) {
            strikes++;
            caught = baryonyx.resolveFishingStrike(water);
        }
        helper.assertTrue(caught, "200 botes vazios seguidos");
        helper.assertTrue(strikes > 0 && baryonyx.fishCaught() == 1, "peixes apanhados: " + baryonyx.fishCaught());
        helper.assertTrue(baryonyx.ticksSinceMeal() < hungerBefore, "comeu o peixe e a fome não caiu");
        helper.assertTrue(fishInInventory(baryonyx) == 0, "o selvagem guardou o peixe");
        helper.succeed();
    }

    /** Domesticado: o peixe vivo apanhado vai para o inventário como o peixe cru dele (o salmão vira salmão). */
    @GameTest(template = ARENA, batch = "baryonyx_strike_tame", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aTamedBaryonyxStoresTheLiveFishItCatches(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 14, 2, 10, 2, 7);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 9, 2, 6);
        baryonyx.tame(helper.makeMockSurvivalPlayer());
        Salmon salmon = helper.spawnWithNoFreeWill(EntityType.SALMON, 7, 1, 5);
        BlockPos water = helper.absolutePos(new BlockPos(7, 1, 5));
        helper.assertTrue(baryonyx.resolveFishingStrike(water), "peixe vivo ao alcance deveria ser captura certa");
        helper.assertFalse(salmon.isAlive(), "o salmão apanhado deveria sumir");
        helper.assertTrue(count(baryonyx, Items.SALMON) == 1, "deveria guardar um salmão: " + count(baryonyx, Items.SALMON));
        helper.assertTrue(count(baryonyx, Items.COD) == 0, "guardou bacalhau de um salmão");
        helper.succeed();
    }

    /** Domesticado, sem peixe à vista, na água temperada (nem rio nem fria): guarda bacalhau. */
    @GameTest(template = ARENA, batch = "baryonyx_strike_tame_cod", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aTamedBaryonyxStoresCodFromTemperateWater(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 14, 2, 10, 2, 7);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 9, 2, 6);
        baryonyx.tame(helper.makeMockSurvivalPlayer());
        BlockPos water = helper.absolutePos(new BlockPos(7, 1, 6));
        var biome = helper.getLevel().getBiome(water);
        helper.assertFalse(biome.is(BiomeTags.IS_RIVER) || biome.value().coldEnoughToSnow(water),
                "a cena deveria estar em água temperada fora de rio: " + biome.unwrapKey());
        boolean caught = false;
        for (int strike = 0; strike < 200 && !caught; strike++) {
            caught = baryonyx.resolveFishingStrike(water);
        }
        helper.assertTrue(caught, "200 botes vazios seguidos");
        helper.assertTrue(count(baryonyx, Items.COD) == 1, "deveria guardar um bacalhau: " + count(baryonyx, Items.COD));
        helper.assertTrue(count(baryonyx, Items.SALMON) == 0, "salmão fora do rio e da água fria");
        helper.succeed();
    }

    /** O bote da pesca deixa a garra-gancho pronta. */
    @GameTest(template = EMPTY, batch = "baryonyx_strike_gaff")
    public static void theFishingStrikeArmsTheGaff(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 1, 2, 1);
        helper.assertFalse(baryonyx.isGaffReady(), "gancho pronto antes do bote");
        baryonyx.beginFishingStrike();
        helper.assertTrue(baryonyx.isGaffReady(), "o bote da pesca não deixou o gancho pronto");
        helper.succeed();
    }

    // ---- Pesca: o comportamento ----

    /** Selvagem com fome, perto da lagoa rasa: vai até a água e fica pescando. */
    @GameTest(template = ARENA, batch = "baryonyx_fish_wild", timeoutTicks = 800, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aHungryWildBaryonyxGoesFishing(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 22, 2, 12, 2, 7);
        LandCreature baryonyx = helper.spawn(ModEntities.BARYONYX.get(), 14, 2, 7);
        baryonyx.starve();
        helper.succeedWhen(() -> {
            helper.assertTrue(baryonyx.isAlive(), "o Baryonyx sumiu");
            helper.assertTrue(baryonyx.hungerDrive() != Hunger.Drive.SATED, "a fome passou sem pescar");
            helper.assertTrue(baryonyx.isFishing(), "ainda não está pescando (a " + baryonyx.position() + ", objetivos "
                    + baryonyx.goalSelector.getRunningGoals().map(goal -> goal.getGoal().getClass().getSimpleName())
                    .toList() + ")");
        });
    }

    /** Selvagem saciado não pesca. */
    @GameTest(template = ARENA, batch = "baryonyx_fish_sated", timeoutTicks = 500, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aSatedWildBaryonyxDoesNotFish(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 22, 2, 12, 2, 7);
        LandCreature baryonyx = helper.spawn(ModEntities.BARYONYX.get(), 14, 2, 7);
        baryonyx.setTicksSinceMeal(0);
        helper.assertTrue(baryonyx.hungerDrive() == Hunger.Drive.SATED, "deveria começar saciado");
        helper.onEachTick(() -> {
            helper.assertTrue(baryonyx.isAlive(), "o Baryonyx sumiu");
            helper.assertFalse(baryonyx.isFishing(), "saciado e pescando");
        });
        helper.runAtTickTime(400, helper::succeed);
    }

    /**
     * Domesticado com a ordem "Parar" na beira: pesca o peixe vivo que está ao lado e o guarda no inventário. O peixe
     * vivo faz do primeiro bote uma captura certa.
     */
    @GameTest(template = ARENA, batch = "baryonyx_fish_tame", timeoutTicks = 900, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aTamedBaryonyxOrderedToStayFishesIntoItsInventory(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 14, 2, 10, 2, 7);
        LandCreature baryonyx = helper.spawn(ModEntities.BARYONYX.get(), 9, 2, 6);
        baryonyx.tame(helper.makeMockSurvivalPlayer());
        baryonyx.setMovement(Movement.STAY);
        Cod cod = helper.spawnWithNoFreeWill(EntityType.COD, 7, 1, 6);
        helper.succeedWhen(() -> {
            helper.assertTrue(baryonyx.isAlive(), "o Baryonyx sumiu");
            helper.assertTrue(count(baryonyx, Items.COD) >= 1, "ainda não guardou peixe (pescando "
                    + baryonyx.isFishing() + ", a " + baryonyx.position() + ")");
            helper.assertFalse(cod.isAlive(), "guardou peixe e o bacalhau vivo continua na água");
            helper.assertTrue(baryonyx.movement() == Movement.STAY, "saiu da ordem Parar");
        });
    }

    /** Domesticado seguindo o dono não para para pescar. */
    @GameTest(template = ARENA, batch = "baryonyx_fish_follow", timeoutTicks = 500, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aTamedBaryonyxFollowingItsOwnerDoesNotFish(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 14, 2, 10, 2, 7);
        LandCreature baryonyx = helper.spawn(ModEntities.BARYONYX.get(), 9, 2, 6);
        Player owner = helper.makeMockSurvivalPlayer();
        owner.setPos(helper.absoluteVec(new Vec3(11.5, 2, 6.5)));
        baryonyx.tame(owner);
        baryonyx.setMovement(Movement.FOLLOW);
        Cod cod = helper.spawnWithNoFreeWill(EntityType.COD, 7, 1, 6);
        helper.onEachTick(() -> {
            helper.assertFalse(baryonyx.isFishing(), "seguindo o dono e pescando");
            helper.assertTrue(cod.isAlive() && fishInInventory(baryonyx) == 0, "seguindo o dono e pescou");
        });
        helper.runAtTickTime(400, helper::succeed);
    }

    // ---- Garra-gancho ----

    /** O bote deixa o golpe pronto; o golpe fisga o porco: puxa-o para perto e o prende com lentidão forte. */
    @GameTest(template = ARENA, batch = "baryonyx_gaff", timeoutTicks = 60, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theGaffPullsThePreyCloseAndHoldsIt(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 4, 0, 6);
        Pig pig = sturdy(helper.spawn(EntityType.PIG, 9, 0, 6));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(hunter.sizeRatioOf(pig) <= 1.0, "o porco deveria ser menor que o Baryonyx");
            double before = hunter.distanceTo(pig);
            helper.assertFalse(hunter.isGaffReady(), "gancho pronto sem bote");
            hunter.onPounce(pig);
            helper.assertTrue(hunter.isGaffReady(), "o bote não deixou o gancho pronto");
            hunter.doHurtTarget(pig);
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "o golpe não acertou");
            helper.assertFalse(hunter.isGaffReady(), "o gancho continuou pronto depois de fisgar");
            helper.assertTrue(hunter.lastGaffed() == pig, "o porco não foi fisgado");
            var slow = pig.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null, "o porco fisgado deveria ficar preso");
            helper.assertTrue(slow.getAmplifier() >= 2, "lentidão fraca demais: nível " + (slow.getAmplifier() + 1));
            Vec3 toHunter = hunter.position().subtract(pig.position());
            Vec3 pull = pig.getDeltaMovement();
            helper.assertTrue(pull.x * toHunter.x + pull.z * toHunter.z > 0.0,
                    "o puxão deveria ser na direção do Baryonyx: " + pull);
            helper.runAfterDelay(8, () -> {
                helper.assertTrue(hunter.distanceTo(pig) < before - 0.5,
                        "o porco não veio para perto: " + before + " → " + hunter.distanceTo(pig));
                helper.succeed();
            });
        });
    }

    /** Sem bote antes, o golpe é só uma mordida. */
    @GameTest(template = ARENA, batch = "baryonyx_gaff_nopounce", timeoutTicks = 40, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void withoutThePounceTheBiteDoesNotGaff(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 4, 0, 6);
        Pig pig = sturdy(helper.spawnWithNoFreeWill(EntityType.PIG, 7, 0, 6));
        hunter.doHurtTarget(pig);
        helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "o golpe não acertou");
        helper.assertFalse(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "fisgou sem bote");
        helper.assertTrue(hunter.lastGaffed() == null, "fisgou sem bote");
        helper.succeed();
    }

    /** Em terra, a presa maior que ele (o mamute) não é fisgada nem puxada. */
    @GameTest(template = ARENA, batch = "baryonyx_gaff_big", timeoutTicks = 40, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theGaffDoesNotPullBiggerPreyOnLand(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 4, 0, 6);
        LandCreature mammoth = sturdy(helper.spawnWithNoFreeWill(ModEntities.MAMMOTH.get(), 10, 0, 6));
        helper.assertTrue(hunter.sizeRatioOf(mammoth) > 1.0, "o mamute deveria ser maior: " + hunter.sizeRatioOf(mammoth));
        helper.assertFalse(mammoth.isInWater(), "o mamute deveria estar em terra");
        hunter.onPounce(mammoth);
        hunter.doHurtTarget(mammoth);
        helper.assertTrue(mammoth.getHealth() < mammoth.getMaxHealth(), "o golpe não acertou");
        helper.assertFalse(mammoth.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "fisgou um mamute em terra");
        helper.assertTrue(hunter.lastGaffed() == null, "fisgou um mamute em terra");
        Vec3 toHunter = hunter.position().subtract(mammoth.position());
        Vec3 motion = mammoth.getDeltaMovement();
        helper.assertFalse(motion.x * toHunter.x + motion.z * toHunter.z > 0.01, "puxou o mamute: " + motion);
        helper.succeed();
    }

    /** O bote só deixa o gancho pronto por um tempo. */
    @GameTest(template = ARENA, batch = "baryonyx_gaff_window", timeoutTicks = HuntSpecials.GAFF_WINDOW_TICKS + 40,
            setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theGaffWindowCloses(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 4, 0, 6);
        Pig pig = sturdy(helper.spawnWithNoFreeWill(EntityType.PIG, 7, 0, 6));
        hunter.onPounce(pig);
        helper.assertTrue(hunter.isGaffReady(), "o bote não deixou o gancho pronto");
        helper.runAtTickTime(HuntSpecials.GAFF_WINDOW_TICKS + 10, () -> {
            helper.assertFalse(hunter.isGaffReady(), "o gancho continua pronto muito depois do bote");
            hunter.doHurtTarget(pig);
            helper.assertFalse(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o bote antigo ainda fisgou");
            helper.succeed();
        });
    }

    // ---- Anfíbio ----

    /** Debaixo d'água não perde o fôlego; a vaca, na mesma situação, perde (prova de que a cena afoga). */
    @GameTest(template = ARENA, batch = "baryonyx_breath", timeoutTicks = 200, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theBaryonyxDoesNotDrown(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        tank(helper, 3, 5, 3, 5, 5);
        tank(helper, 12, 12, 4, 4, 4);
        LandCreature baryonyx = helper.spawnWithNoFreeWill(ModEntities.BARYONYX.get(), 4, 1, 4);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 12, 1, 4);
        helper.assertTrue(baryonyx.canBreatheUnderwater(), "o anfíbio deveria respirar debaixo d'água");
        float health = baryonyx.getHealth();
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(baryonyx.isEyeInFluid(FluidTags.WATER), "a cena não pôs a cabeça do Baryonyx na água");
            helper.assertTrue(cow.isEyeInFluid(FluidTags.WATER), "a cena não pôs a cabeça da vaca na água");
            helper.assertTrue(cow.getAirSupply() < cow.getMaxAirSupply(), "a vaca não perdeu fôlego: a cena não afoga");
            helper.assertTrue(baryonyx.getAirSupply() == baryonyx.getMaxAirSupply(),
                    "o Baryonyx perdeu fôlego: " + baryonyx.getAirSupply());
            helper.assertTrue(baryonyx.getHealth() >= health, "o Baryonyx se feriu debaixo d'água");
            helper.succeed();
        });
    }

    /** Anfíbio: entra na água sem receio no caminho e passeia pela beira em vez de evitá-la. */
    @GameTest(template = EMPTY, batch = "baryonyx_shore")
    public static void theBaryonyxDoesNotAvoidWater(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature baryonyx = helper.spawn(ModEntities.BARYONYX.get(), 1, 2, 1);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(baryonyx.getPathfindingMalus(BlockPathTypes.WATER) >= 0.0F
                            && baryonyx.getPathfindingMalus(BlockPathTypes.WATER) < BlockPathTypes.WATER.getMalus(),
                    "a água deveria ser caminho livre: " + baryonyx.getPathfindingMalus(BlockPathTypes.WATER));
            helper.assertTrue(baryonyx.goalSelector.getAvailableGoals().stream()
                            .anyMatch(goal -> goal.getGoal() instanceof WaterEdgeStrollGoal),
                    "deveria passear pela beira d'água");
            helper.succeed();
        });
    }

    /**
     * Ferido e fraco, foge para a água — a lagoa do lado oposto a quem o feriu — e entra nela: o destino da fuga fica
     * por dentro d'água, não na margem (antes a navegação parava com o corpo ainda em terra).
     */
    @GameTest(template = ARENA, batch = "baryonyx_flee", timeoutTicks = 300, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aWoundedBaryonyxFleesIntoTheWater(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 22, 2, 12, 2, 7);
        LandCreature baryonyx = helper.spawn(ModEntities.BARYONYX.get(), 11, 2, 7);
        Pig attacker = helper.spawnWithNoFreeWill(EntityType.PIG, 19, 2, 7);
        baryonyx.setTicksSinceMeal(0);
        baryonyx.setHealth(baryonyx.getMaxHealth() * 0.1F);
        boolean hurt = baryonyx.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);
        helper.assertTrue(hurt && baryonyx.getLastHurtByMob() == attacker, "o golpe do porco não contou");
        Vec3 refuge = FleeWhenWeakGoal.waterRefuge(baryonyx, attacker);
        helper.assertTrue(refuge != null, "não achou água para fugir");
        helper.assertTrue(helper.getLevel().getFluidState(BlockPos.containing(refuge)).is(FluidTags.WATER)
                        || helper.getLevel().getFluidState(BlockPos.containing(refuge).below()).is(FluidTags.WATER),
                "o refúgio não é água: " + refuge);
        helper.assertTrue(attacker.position().distanceTo(refuge) > baryonyx.position().distanceTo(refuge),
                "o refúgio fica do lado de quem o feriu");
        // Para o relatório de falha: quão perto da água chegou, e se a fuga chegou a rodar.
        double[] closest = {Double.MAX_VALUE};
        int[] fleeTicks = {0};
        StringBuilder trail = new StringBuilder();
        helper.onEachTick(() -> {
            Vec3 at = baryonyx.position();
            closest[0] = Math.min(closest[0], Math.hypot(at.x - refuge.x, at.z - refuge.z));
            if (baryonyx.goalSelector.getRunningGoals().anyMatch(goal -> goal.getGoal() instanceof FleeWhenWeakGoal)) {
                fleeTicks[0]++;
            }
            if (helper.getTick() % 20 == 0) {
                Vec3 rel = helper.relativeVec(at);
                trail.append(String.format(" t%d(%.1f,%.1f,%.1f)%s", helper.getTick(), rel.x, rel.y, rel.z,
                        baryonyx.getTarget() != null ? "*" : ""));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(baryonyx.isAlive(), "o Baryonyx morreu");
            helper.assertTrue(baryonyx.isInWater(), "ferido, ainda não fugiu para a água (mais perto do refúgio: "
                    + String.format("%.2f", closest[0]) + " blocos, ticks fugindo " + fleeTicks[0] + ", trajeto"
                    + trail + "; a "
                    + helper.relativeVec(baryonyx.position()) + ", refúgio " + helper.relativeVec(refuge) + ", vida "
                    + baryonyx.getHealth() + "/" + baryonyx.getMaxHealth() + ", objetivos "
                    + baryonyx.goalSelector.getRunningGoals().map(goal -> goal.getGoal().getClass().getSimpleName())
                    .toList() + ", ferido por " + baryonyx.getLastHurtByMob() + ")");
        });
    }

    // ---- Montaria que nada ----

    private static LandCreature mountIn(GameTestHelper helper, EntityType<LandCreature> type, int x, int z, Player rider) {
        LandCreature mount = helper.spawnWithNoFreeWill(type, x, 1, z);
        mount.tame(rider);
        mount.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        mount.setSaddled(true);
        rider.setPos(mount.position());
        helper.assertTrue(mount.ride(rider), "deveria montar o " + type);
        return mount;
    }

    /** Montado, mergulha até a cabeça de quem monta ficar debaixo d'água, e quem monta continua em cima. */
    @GameTest(template = ARENA, batch = "baryonyx_swim_mount", timeoutTicks = 120, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theRiderStaysOnTheSwimmingBaryonyx(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        tank(helper, 2, 5, 2, 5, 8);
        helper.assertTrue(species(helper).mount().orElseThrow().swims(), "o Baryonyx deveria nadar montado");
        Player rider = helper.makeMockSurvivalPlayer();
        LandCreature baryonyx = mountIn(helper, ModEntities.BARYONYX.get(), 4, 4, rider);
        helper.assertTrue(baryonyx.canBeRiddenUnderFluidType(ForgeMod.WATER_TYPE.get(), rider),
                "a montaria que nada deveria aceitar quem monta debaixo d'água");
        boolean[] submerged = {false};
        helper.onEachTick(() -> {
            if (rider.isEyeInFluid(FluidTags.WATER)) {
                submerged[0] = true;
            }
            helper.assertTrue(rider.getVehicle() == baryonyx, "quem montava o Baryonyx foi derrubado na água");
        });
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(submerged[0], "a cena não pôs a cabeça de quem monta o Baryonyx na água");
            helper.succeed();
        });
    }
}
