package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.core.ecology.Activity;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.Swallow;
import dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.BreedingProfile;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.TamingProfile;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O Quetzalcoatlus, montaria de viagem longa: caçador a pé que engole a presa pequena inteira, decola num salto e só com
 * céu aberto, planador de térmica e diurno, e pescador de beira de lago como a garça.
 *
 * <p>Os testes que mexem na hora e no tempo do mundo ficam cada um no seu batch, com a hora devolvida no fim: a hora é
 * do mundo inteiro, e o {@link HuntTests#clearStrays} de um teste apagaria as criaturas do vizinho de batch.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class QuetzalTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final String SPECIES = "quetzal_species";
    private static final String DAY_THERMAL = "quetzal_day_thermal";
    private static final String DAY_WATER = "quetzal_day_water";
    private static final String DAY_HUNGRY = "quetzal_day_hungry";
    private static final String DAY_FISHING = "quetzal_day_fishing";
    private static final String NIGHT = "quetzal_night";
    private static final long NOON = 6_000L;
    private static final long MIDNIGHT = 18_000L;

    private static long savedDayTime;
    private static boolean savedRain;
    private static boolean savedThunder;

    private static EntityType<LandCreature> quetzal() {
        return ModEntities.QUETZALCOATLUS.get();
    }

    private static Species species(GameTestHelper helper, EntityType<?> type) {
        var species = Species.of(helper.getLevel().registryAccess(), type).orElse(null);
        helper.assertTrue(species != null, type + " não carregou");
        return species;
    }

    /** Hora certa e tempo limpo (sem chuva, a térmica existe); a hora e a chuva de antes voltam no fim do batch. */
    private static void setHour(ServerLevel level, long hour) {
        savedDayTime = level.getDayTime();
        savedRain = level.getLevelData().isRaining();
        savedThunder = level.getLevelData().isThundering();
        level.setDayTime(hour);
        level.setWeatherParameters(12_000, 0, false, false);
        level.setRainLevel(0.0F);
        level.setThunderLevel(0.0F);
    }

    private static void restoreHour(ServerLevel level) {
        level.setDayTime(savedDayTime);
        if (savedRain || savedThunder) {
            level.setWeatherParameters(0, 6_000, savedRain, savedThunder);
        }
    }

    @BeforeBatch(batch = DAY_THERMAL)
    public static void beforeDayThermal(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_THERMAL)
    public static void afterDayThermal(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_WATER)
    public static void beforeDayWater(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_WATER)
    public static void afterDayWater(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_HUNGRY)
    public static void beforeDayHungry(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_HUNGRY)
    public static void afterDayHungry(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_FISHING)
    public static void beforeDayFishing(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_FISHING)
    public static void afterDayFishing(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = NIGHT)
    public static void beforeNight(ServerLevel level) {
        setHour(level, MIDNIGHT);
    }

    @AfterBatch(batch = NIGHT)
    public static void afterNight(ServerLevel level) {
        restoreHour(level);
    }

    /** Presa que aguenta a bicada de 12: o que se vê é se ela foi engolida, não se morreu. */
    private static <T extends LivingEntity> T sturdy(T prey) {
        prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
        prey.setHealth(400.0F);
        return prey;
    }

    private static boolean droppedNear(GameTestHelper helper, Vec3 at) {
        return !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(8.0)).isEmpty();
    }

    /**
     * Uma laje de pedra (x0..x1, z0..z1) com o chão em y 0 e a terra firme em y 1; dentro dela, uma lagoa rasa (um bloco
     * de fundo) de px0..px1 em x, com ar por cima até a altura do Quetzalcoatlus. Quem fica na terra firme pisa em y 2.
     */
    private static void shore(GameTestHelper helper, int x0, int x1, int z0, int z1, int px0, int px1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                boolean pond = x >= px0 && x <= px1 && x > x0 && x < x1 && z > z0 && z < z1;
                helper.setBlock(x, 1, z, pond ? Blocks.WATER : Blocks.STONE);
                for (int y = 2; y <= 8; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
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

    // ---- Espécie e dados ----

    /**
     * Montaria de viagem longa: vida 110, ataque 12, 90 s de fôlego (o triplo do Pteranodonte), cruzeiro 1,1 com curva
     * de 50°/s e aceleração lenta, decolagem por salto, térmicas, 27 espaços de carga e ovo.
     */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void theQuetzalcoatlusIsALongRangeFlyingMount(GameTestHelper helper) {
        Species species = species(helper, quetzal());
        Species ptero = species(helper, ModEntities.PTERANODON.get());
        helper.assertTrue(species.stats().entry(Stat.HEALTH).base() == 110.0,
                "vida base: " + species.stats().entry(Stat.HEALTH).base());
        helper.assertTrue(species.stats().entry(Stat.ATTACK).base() == 12.0,
                "ataque base: " + species.stats().entry(Stat.ATTACK).base());
        double stamina = species.stats().value(Stat.FLIGHT_STAMINA, 0);
        helper.assertTrue(stamina == 90.0, "fôlego de voo: " + stamina);
        helper.assertTrue(stamina > ptero.stats().value(Stat.FLIGHT_STAMINA, 0) * 2.5,
                "deveria voar bem mais tempo que o Pteranodonte");
        MountProfile mount = species.mount().orElseThrow();
        MountProfile pteroMount = ptero.mount().orElseThrow();
        helper.assertTrue(mount.flying(), "deveria voar montado");
        helper.assertTrue(mount.flightSpeed() == 1.1 && mount.flightSpeed() > pteroMount.flightSpeed(),
                "cruzeiro: " + mount.flightSpeed());
        helper.assertTrue(mount.flightTurnRate() == 50.0 && mount.flightTurnRate() < pteroMount.flightTurnRate(),
                "curva: " + mount.flightTurnRate());
        MountProfile.FlightStyle flight = mount.flight();
        helper.assertTrue(flight.accelerationSeconds() > pteroMount.flight().accelerationSeconds(),
                "deveria embalar mais devagar que o Pteranodonte");
        helper.assertTrue(flight.leapHeight() >= 2.0 && flight.leapHeight() <= 3.0,
                "decola num salto de ~2,5 blocos: " + flight.leapHeight());
        helper.assertTrue(flight.thermalLift() > 0.0, "deveria subir nas térmicas");
        helper.assertTrue(species.storage().orElseThrow().slots() == 27, "carga: " + species.storage());
        helper.assertTrue(species.breeding().orElseThrow().offspring() == BreedingProfile.Offspring.EGG,
                "deveria botar ovo");
        helper.succeed();
    }

    /** O Pteranodonte continua voando como antes: sem salto, sem térmica e com os custos de fôlego de sempre. */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void thePteranodonKeepsItsFlight(GameTestHelper helper) {
        MountProfile.FlightStyle flight = species(helper, ModEntities.PTERANODON.get()).mount().orElseThrow().flight();
        helper.assertTrue(flight.equals(MountProfile.FlightStyle.DEFAULT), "o voo do Pteranodonte mudou: " + flight);
        helper.assertTrue(!flight.leaps() && flight.thermalLift() == 0.0, "o Pteranodonte não salta nem usa térmica");
        helper.assertTrue(flight.costs().equals(FlightStamina.Costs.DEFAULT), "custos do Pteranodonte: " + flight.costs());
        helper.succeed();
    }

    /**
     * Caçador a pé da presa pequena, que engole inteira; não caça gente, diurno, pescador e cauteloso. Grupos de 2 a 4,
     * a 400+ blocos do spawn, em qualquer bioma e três vezes mais nos campos abertos e na beira d'água.
     */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void theQuetzalcoatlusIsAGroundHunterOfOpenCountry(GameTestHelper helper) {
        Species species = species(helper, quetzal());
        BehaviorProfile behavior = species.behavior().orElseThrow();
        helper.assertTrue(behavior.huntSpecial() == BehaviorProfile.HuntSpecial.SWALLOW,
                "deveria engolir a presa: " + behavior.huntSpecial());
        TagKey<EntityType<?>> prey = TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("quetzalcoatlus_prey"));
        helper.assertTrue(behavior.prey().map(prey::equals).orElse(false), "presas: " + behavior.prey());
        for (EntityType<?> type : new EntityType<?>[] {EntityType.RABBIT, EntityType.CHICKEN, EntityType.FROG,
                EntityType.COD, EntityType.SALMON, ModEntities.DODO.get(), ModEntities.ORNITHOLESTES.get(),
                ModEntities.VELOCIRAPTOR.get()}) {
            helper.assertTrue(type.is(prey), type + " deveria ser presa do Quetzalcoatlus");
        }
        helper.assertFalse(EntityType.PLAYER.is(prey) || behavior.huntsPlayers(), "não caça gente");
        helper.assertFalse(ModEntities.GALLIMIMUS.get().is(prey), "o Galimimo é grande demais");
        helper.assertTrue(behavior.habits().activity() == Activity.Pattern.DIURNAL, "deveria ser diurno");
        helper.assertTrue(behavior.habits().fishing().isPresent(), "deveria pescar (behavior.habits.fishing)");
        helper.assertTrue(behavior.wariness().isPresent(), "deveria ser cauteloso (wariness)");

        SpawnProfile spawn = species.spawn().orElseThrow();
        helper.assertTrue(spawn.groupMin() == 2 && spawn.groupMax() == 4,
                "grupos: " + spawn.groupMin() + ".." + spawn.groupMax());
        helper.assertTrue(spawn.maxNearby() == 4, "máximo por perto: " + spawn.maxNearby());
        helper.assertTrue(spawn.minDistance() == 400, "distância do spawn: " + spawn.minDistance());
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> ideal = TagKey.create(Registries.BIOME, IceAgeSurvival.id("ideal_quetzalcoatlus"));
        helper.assertTrue(spawn.favored().map(favored -> favored.biomes().equals(ideal)).orElse(false),
                "deveria preferir #ideal_quetzalcoatlus: " + spawn.favored());
        var plains = biomes.getHolderOrThrow(Biomes.PLAINS);
        var savanna = biomes.getHolderOrThrow(Biomes.SAVANNA);
        var river = biomes.getHolderOrThrow(Biomes.RIVER);
        var snowy = biomes.getHolderOrThrow(Biomes.SNOWY_PLAINS);
        var forest = biomes.getHolderOrThrow(Biomes.FOREST);
        for (var biome : java.util.List.of(plains, savanna, river, snowy)) {
            helper.assertTrue(biome.is(ideal), biome.unwrapKey().orElseThrow().location() + " deveria ser ideal");
            helper.assertTrue(spawn.weightIn(biome) == 3 * spawn.weightIn(forest),
                    "no ideal deveria ser o triplo: " + spawn.weightIn(biome) + " × " + spawn.weightIn(forest));
        }
        helper.assertTrue(spawn.weightIn(forest) == 2, "peso fora do ideal: " + spawn.weightIn(forest));
        helper.succeed();
    }

    /** Peixe cru é a melhor comida; carne pequena, charque e peixe assado também domesticam. */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void theQuetzalcoatlusIsTamedWithFishAndSmallMeat(GameTestHelper helper) {
        TamingProfile taming = species(helper, quetzal()).taming().orElseThrow();
        for (Item item : new Item[] {Items.COD, Items.SALMON, ModItems.JERKY.get(), Items.RABBIT, Items.CHICKEN,
                Items.COOKED_COD}) {
            helper.assertTrue(taming.foodFor(new ItemStack(item)).isPresent(), "não aceita " + item + " na domesticação");
        }
        helper.assertTrue(taming.foodFor(new ItemStack(Items.WHEAT)).isEmpty(), "carnívoro aceitando trigo");
        var fish = taming.foodFor(new ItemStack(Items.COD)).orElseThrow();
        var cooked = taming.foodFor(new ItemStack(Items.COOKED_COD)).orElseThrow();
        var meat = taming.foodFor(new ItemStack(Items.RABBIT)).orElseThrow();
        helper.assertTrue(fish.value() * fish.quality() > meat.value() * meat.quality()
                        && fish.value() * fish.quality() > cooked.value() * cooked.quality(),
                "o peixe cru deveria ser a melhor comida");
        helper.succeed();
    }

    /**
     * Planar gasta muito menos fôlego que subir batendo as asas; e a subida que a térmica dá não conta como bater as
     * asas.
     */
    @GameTest(template = ARENA, batch = SPECIES)
    public static void glidingCostsLessStaminaThanFlapping(GameTestHelper helper) {
        LandCreature quetzal = helper.spawnWithNoFreeWill(quetzal(), 4, 0, 4);
        LandCreature ptero = helper.spawnWithNoFreeWill(ModEntities.PTERANODON.get(), 18, 0, 18);
        double flapping = quetzal.flightDrainRate(0.3);
        double gliding = quetzal.flightDrainRate(-0.1);
        helper.assertTrue(flapping >= gliding * 10.0, "bater as asas (" + flapping + ") deveria custar bem mais que planar ("
                + gliding + ")");
        helper.assertTrue(gliding < ptero.flightDrainRate(-0.1),
                "o Quetzalcoatlus deveria planar gastando menos que o Pteranodonte");
        helper.assertTrue(ptero.flightDrainRate(0.3) == FlightStamina.CLIMB_RATE
                        && ptero.flightDrainRate(0.0) == FlightStamina.CRUISE_RATE
                        && ptero.flightDrainRate(-0.3) == FlightStamina.GLIDE_RATE,
                "os custos do Pteranodonte mudaram");
        double lift = quetzal.thermalLift();
        if (lift > 0.0) {
            helper.assertTrue(quetzal.flightDrainRate(lift) == gliding,
                    "subir com a térmica deveria custar o planar, não o bater de asas");
        }
        helper.succeed();
    }

    // ---- Engolir inteira ----

    /** O golpe na presa pequena a engole: o dodô some sem cair nada, e o bando inteiro fica saciado. */
    @GameTest(template = ARENA, batch = "quetzal_swallow", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aHungryQuetzalcoatlusSwallowsADodoWhole(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature quetzal = helper.spawnWithNoFreeWill(quetzal(), 10, 0, 10);
        LandCreature mate = helper.spawnWithNoFreeWill(quetzal(), 14, 0, 10);
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 10, 0, 13);
        quetzal.starve();
        mate.starve();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(quetzal.sameGroup(mate), "os dois deveriam ser do mesmo bando");
            helper.assertTrue(Swallow.swallows(quetzal.sizeRatioOf(dodo), false),
                    "o dodô deveria caber no bico: " + quetzal.sizeRatioOf(dodo));
            Vec3 at = dodo.position();
            quetzal.doHurtTarget(dodo);
            helper.assertTrue(dodo.isRemoved() && !dodo.isAlive(), "o dodô deveria ter sido engolido");
            helper.assertFalse(droppedNear(helper, at), "engolido inteiro não deixa cair nada");
            helper.assertTrue(quetzal.isSated(), "engoliu e continua com fome: " + quetzal.hungerDrive());
            helper.assertTrue(mate.isSated(), "a refeição deveria saciar o bando: " + mate.hungerDrive());
            helper.succeed();
        });
    }

    /** A presa grande (o Galimimo), a domesticada e o filhote levam só a bicada. */
    @GameTest(template = ARENA, batch = "quetzal_peck", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void bigTameAndYoungPreyOnlyGetThePeck(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature quetzal = helper.spawnWithNoFreeWill(quetzal(), 10, 0, 10);
        LandCreature galli = sturdy(helper.spawnWithNoFreeWill(ModEntities.GALLIMIMUS.get(), 10, 0, 14));
        LandCreature tameDodo = sturdy(helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 13, 0, 10));
        LandCreature babyDodo = sturdy(helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 7, 0, 10));
        tameDodo.tame(helper.makeMockPlayer());
        babyDodo.setBaby(true);
        quetzal.starve();
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(Swallow.swallows(quetzal.sizeRatioOf(galli), false),
                    "o Galimimo não deveria caber no bico: " + quetzal.sizeRatioOf(galli));
            for (LivingEntity prey : new LivingEntity[] {galli, tameDodo, babyDodo}) {
                float before = prey.getHealth();
                prey.invulnerableTime = 0;
                helper.assertTrue(quetzal.doHurtTarget(prey), "a bicada deveria acertar " + prey);
                helper.assertTrue(!prey.isRemoved() && prey.isAlive(), prey + " foi engolido");
                helper.assertTrue(prey.getHealth() < before, prey + " deveria ter levado a bicada");
            }
            helper.assertFalse(quetzal.isSated(), "bicar não é comer");
            helper.succeed();
        });
    }

    // ---- Decolagem por salto ----

    /** Ferida em céu aberto, decola parada: agacha e salta cerca de 2,5 blocos quase sem sair do lugar, e sobe. */
    @GameTest(template = ARENA, batch = "quetzal_leap", timeoutTicks = 240, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aHurtWildQuetzalcoatlusLeapsStraightUp(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature quetzal = helper.spawn(quetzal(), 12, 0, 12);
        quetzal.setTicksSinceMeal(0);
        Vec3[] start = {null};
        boolean[] leapt = {false};
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(quetzal.hasOpenSkyForTakeoff(), "céu aberto na arena");
            start[0] = quetzal.position();
            quetzal.hurt(helper.getLevel().damageSources().playerAttack(helper.makeMockPlayer()), 1.0F);
        });
        helper.onEachTick(() -> {
            if (start[0] == null) {
                return;
            }
            Vec3 now = quetzal.position();
            double drift = Math.sqrt((now.x - start[0].x) * (now.x - start[0].x) + (now.z - start[0].z) * (now.z - start[0].z));
            if (now.y - start[0].y >= 2.0 && drift < 1.0) {
                leapt[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(start[0] != null, "ainda não foi ferida");
            helper.assertTrue(leapt[0], "deveria saltar ~2,5 blocos parada antes de sair voando (a "
                    + quetzal.position() + ", de " + start[0] + ")");
            helper.assertTrue(quetzal.isFlying(), "deveria estar voando");
            helper.assertTrue(quetzal.getY() > start[0].y + 4.0, "deveria ter subido: " + start[0].y + " → "
                    + quetzal.getY());
        });
    }

    /** Debaixo de uma copa, ferida, não decola: fica no chão, vulnerável. Em céu aberto, a decolagem é livre. */
    @GameTest(template = ARENA, batch = "quetzal_leaves", timeoutTicks = 200, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void underLeavesTheQuetzalcoatlusCannotTakeOff(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int x = 5; x <= 19; x++) {
            for (int z = 5; z <= 19; z++) {
                helper.setBlock(x, 6, z, leaves);
            }
        }
        LandCreature quetzal = helper.spawn(quetzal(), 12, 0, 12);
        LandCreature outside = helper.spawnWithNoFreeWill(quetzal(), 2, 0, 22);
        quetzal.starve(); // com fome não decola à toa: só a fuga a faria decolar
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(quetzal.hasOpenSkyForTakeoff(), "a copa logo acima deveria fechar o céu");
            helper.assertTrue(outside.hasOpenSkyForTakeoff(), "fora da copa o céu está aberto");
            quetzal.hurt(helper.getLevel().damageSources().playerAttack(helper.makeMockPlayer()), 1.0F);
        });
        helper.onEachTick(() -> helper.assertFalse(quetzal.isFlying(), "decolou debaixo da copa"));
        helper.runAtTickTime(120, helper::succeed);
    }

    // ---- Térmica e horário ----

    /** De dia, sem chuva, sobre terra: a selvagem saciada sobe em círculos na térmica gastando quase nada de fôlego. */
    @GameTest(template = ARENA, batch = DAY_THERMAL, timeoutTicks = 260, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void byDayTheQuetzalcoatlusSoarsUpAThermal(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature quetzal = helper.spawn(quetzal(), 12, 10, 12);
        quetzal.setTicksSinceMeal(0);
        quetzal.setWildFlying(true);
        double[] mark = new double[2];
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(quetzal.isFlying(), "deveria estar voando");
            helper.assertTrue(quetzal.thermalLift() > 0.0, "de dia, sobre terra, deveria haver térmica");
            mark[0] = quetzal.getY();
            mark[1] = quetzal.flightStaminaFraction();
        });
        helper.runAtTickTime(200, () -> {
            helper.assertTrue(quetzal.isFlying(), "deveria seguir voando");
            helper.assertTrue(quetzal.getY() > mark[0] + 1.0, "deveria ganhar altura na térmica: " + mark[0] + " → "
                    + quetzal.getY());
            float spent = (float) mark[1] - quetzal.flightStaminaFraction();
            helper.assertTrue(spent < 0.01F, "subindo na térmica deveria gastar quase nada de fôlego: " + spent);
            helper.succeed();
        });
    }

    /** Sobre a água não há térmica; sobre a terra, ao lado, há. */
    @GameTest(template = ARENA, batch = DAY_WATER, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void thereIsNoThermalOverWater(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        shore(helper, 1, 22, 2, 14, 2, 10);
        LandCreature overWater = helper.spawnWithNoFreeWill(quetzal(), 6, 7, 8);
        LandCreature overLand = helper.spawnWithNoFreeWill(quetzal(), 18, 7, 8);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(overWater.thermalLift() == 0.0, "sobre a água não deveria haver térmica: "
                    + overWater.thermalLift());
            helper.assertTrue(overLand.thermalLift() > 0.0, "sobre a terra deveria haver térmica");
            helper.succeed();
        });
    }

    /**
     * À noite não há térmica: a selvagem que voava pousa para dormir, sem ganhar altura, e a que está no chão não
     * decola à toa.
     */
    @GameTest(template = ARENA, batch = NIGHT, timeoutTicks = 400, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void atNightTheQuetzalcoatlusLandsAndStaysDown(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(MIDNIGHT);
        LandCreature flying = helper.spawn(quetzal(), 12, 10, 12);
        LandCreature grounded = helper.spawn(quetzal(), 4, 0, 20);
        flying.setTicksSinceMeal(0);
        grounded.setTicksSinceMeal(0);
        flying.setWildFlying(true);
        double startY = flying.getY();
        helper.assertTrue(flying.thermalLift() == 0.0, "à noite não deveria haver térmica");
        helper.onEachTick(() -> {
            helper.assertTrue(flying.getY() < startY + 5.0, "à noite ganhou altura: " + startY + " → " + flying.getY());
            helper.assertFalse(grounded.isFlying(), "decolou à toa à noite");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(flying.onGround() && !flying.isFlying(), "à noite deveria pousar (a " + flying.position()
                    + ", voando " + flying.isFlying() + ")");
            helper.assertTrue(helper.getTick() > 200, "esperando a que está no chão");
        });
    }

    /** Com fome, a selvagem no ar pousa logo para caçar a pé e não volta a decolar à toa. */
    @GameTest(template = ARENA, batch = DAY_HUNGRY, timeoutTicks = 500, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aHungryFlyingQuetzalcoatlusLandsToHunt(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature quetzal = helper.spawn(quetzal(), 12, 10, 12);
        quetzal.starve();
        quetzal.setWildFlying(true);
        long[] landedAt = {-1};
        helper.onEachTick(() -> {
            helper.assertTrue(quetzal.hungerDrive() != Hunger.Drive.SATED, "a fome passou");
            if (landedAt[0] < 0 && quetzal.onGround() && !quetzal.isFlying()) {
                landedAt[0] = helper.getTick();
            } else if (landedAt[0] >= 0) {
                helper.assertFalse(quetzal.isFlying(), "com fome, decolou de novo");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(landedAt[0] >= 0, "com fome, ainda não pousou (a " + quetzal.position() + ")");
            helper.assertTrue(helper.getTick() >= landedAt[0] + 100, "esperando ficar no chão");
        });
    }

    // ---- Pesca ----

    /** Selvagem com fome, de dia, perto da lagoa rasa: vai até a beira e pesca, como a garça. */
    @GameTest(template = ARENA, batch = DAY_FISHING, timeoutTicks = 800, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aHungryWildQuetzalcoatlusFishesInShallowWater(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        shore(helper, 1, 22, 2, 14, 2, 8);
        LandCreature quetzal = helper.spawn(quetzal(), 15, 2, 8);
        quetzal.starve();
        helper.succeedWhen(() -> {
            helper.assertTrue(quetzal.isAlive(), "o Quetzalcoatlus sumiu");
            helper.assertFalse(quetzal.isFlying(), "com fome, saiu voando em vez de pescar");
            helper.assertTrue(quetzal.isFishing(), "ainda não está pescando (a " + quetzal.position() + ", objetivos "
                    + quetzal.goalSelector.getRunningGoals().map(goal -> goal.getGoal().getClass().getSimpleName())
                    .toList() + ")");
        });
    }

    /** Domesticado com a ordem "Parar" na beira: pesca o peixe vivo ao lado e o guarda no inventário. */
    @GameTest(template = ARENA, batch = "quetzal_fish_tame", timeoutTicks = 900, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aTamedQuetzalcoatlusOrderedToStayFishesIntoItsInventory(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        shore(helper, 1, 16, 2, 12, 2, 7);
        LandCreature quetzal = helper.spawn(quetzal(), 10, 2, 7);
        Player owner = helper.makeMockSurvivalPlayer();
        quetzal.tame(owner);
        quetzal.setMovement(Movement.STAY);
        Cod cod = helper.spawnWithNoFreeWill(EntityType.COD, 7, 1, 7);
        helper.succeedWhen(() -> {
            helper.assertTrue(quetzal.isAlive(), "o Quetzalcoatlus sumiu");
            helper.assertTrue(count(quetzal, Items.COD) >= 1, "ainda não guardou peixe (pescando "
                    + quetzal.isFishing() + ", a " + quetzal.position() + ")");
            helper.assertFalse(cod.isAlive(), "guardou peixe e o bacalhau vivo continua na água");
            helper.assertTrue(quetzal.movement() == Movement.STAY, "saiu da ordem Parar");
        });
    }
}
