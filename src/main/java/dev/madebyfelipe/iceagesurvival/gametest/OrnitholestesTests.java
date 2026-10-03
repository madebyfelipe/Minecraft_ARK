package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.Activity;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.HabitsProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O Ornitholestes: agarrão na presa pequena, camuflagem no sub-bosque, sono de dia, carniça e o sentinela
 * domesticado.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class OrnitholestesTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    /*
     * Os testes que mexem na hora do mundo ficam cada um no seu batch: a hora é do mundo inteiro, e o
     * HuntTests.clearStrays de um teste apagaria as criaturas do vizinho de batch.
     */
    private static final String DAY_SLEEP = "ornitholestes_day_sleep";
    private static final String DAY_WAKE = "ornitholestes_day_wake";
    private static final String DAY_TAMED = "ornitholestes_day_tamed";
    private static final String DAY_DEEP = "ornitholestes_day_deep";
    private static final String NIGHT_AWAKE = "ornitholestes_night_awake";
    private static final String NIGHT_EAT = "ornitholestes_night_eat";
    private static final String NIGHT_GUARDED = "ornitholestes_night_guarded";
    private static final long NOON = 6_000L;
    private static final long MIDNIGHT = 18_000L;

    private static long savedDayTime;

    private static void setHour(ServerLevel level, long hour) {
        savedDayTime = level.getDayTime();
        level.setDayTime(hour);
    }

    private static void restoreHour(ServerLevel level) {
        level.setDayTime(savedDayTime);
    }

    @BeforeBatch(batch = DAY_SLEEP)
    public static void beforeDaySleep(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_SLEEP)
    public static void afterDaySleep(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_WAKE)
    public static void beforeDayWake(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_WAKE)
    public static void afterDayWake(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_DEEP)
    public static void beforeDayDeep(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_DEEP)
    public static void afterDayDeep(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_TAMED)
    public static void beforeDayTamed(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_TAMED)
    public static void afterDayTamed(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = NIGHT_AWAKE)
    public static void beforeNightAwake(ServerLevel level) {
        setHour(level, MIDNIGHT);
    }

    @AfterBatch(batch = NIGHT_AWAKE)
    public static void afterNightAwake(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = NIGHT_EAT)
    public static void beforeNightEat(ServerLevel level) {
        setHour(level, MIDNIGHT);
    }

    @AfterBatch(batch = NIGHT_EAT)
    public static void afterNightEat(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = NIGHT_GUARDED)
    public static void beforeNightGuarded(ServerLevel level) {
        setHour(level, MIDNIGHT);
    }

    @AfterBatch(batch = NIGHT_GUARDED)
    public static void afterNightGuarded(ServerLevel level) {
        restoreHour(level);
    }

    private static LandCreature ornitholestes(GameTestHelper helper, int x, int y, int z) {
        return helper.spawnWithNoFreeWill(ModEntities.ORNITHOLESTES.get(), x, y, z);
    }

    /** Presa que aguenta o golpe: o agarrão é visto na lentidão, não na morte. */
    private static <T extends net.minecraft.world.entity.LivingEntity> T sturdy(T prey) {
        prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        prey.setHealth(200.0F);
        return prey;
    }

    // ---- Espécie ----

    @GameTest(template = EMPTY, batch = "ornitholestes_species")
    public static void theOrnitholestesIsASpecies(GameTestHelper helper) {
        var species = Species.of(helper.getLevel().registryAccess(), ModEntities.ORNITHOLESTES.get()).orElse(null);
        helper.assertTrue(species != null, "o Ornitholestes não carregou");
        BehaviorProfile behavior = species.behavior().orElseThrow();
        helper.assertTrue(behavior.huntSpecial() == BehaviorProfile.HuntSpecial.GRAB,
                "deveria caçar com o agarrão: " + behavior.huntSpecial());
        helper.assertTrue(behavior.prey().map(tag -> tag.location().getPath().equals("ornitholestes_prey")).orElse(false),
                "deveria caçar #ornitholestes_prey: " + behavior.prey());
        HabitsProfile habits = behavior.habits();
        helper.assertTrue(habits.activity() == Activity.Pattern.NOCTURNAL, "deveria ser noturno: " + habits.activity());
        helper.assertTrue(Math.abs(habits.camouflage() - 0.5) < 1e-9, "camuflagem: " + habits.camouflage());
        helper.assertTrue(habits.scavenges(), "deveria comer carniça");
        helper.assertTrue(Math.abs(habits.sentinelRadius() - 32.0) < 1e-9, "sentinela: " + habits.sentinelRadius());
        var spawn = species.spawn().orElseThrow();
        helper.assertTrue(spawn.minDistance() == 0, "deveria nascer perto do spawn: " + spawn.minDistance());
        TagKey<Biome> biomes = TagKey.create(Registries.BIOME,
                new ResourceLocation(IceAgeSurvival.MODID, "spawns_ornitholestes"));
        helper.assertTrue(spawn.biomes().equals(biomes), "bioma de spawn: " + spawn.biomes());
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.SNOWY_PLAINS).is(biomes), "deveria nascer na planície nevada");
        helper.succeed();
    }

    /** No ecossistema: predador pequeno, presa do Velociraptor e da Kelenken. */
    @GameTest(template = EMPTY, batch = "ornitholestes_species")
    public static void theOrnitholestesHasItsPlaceInTheFoodChain(GameTestHelper helper) {
        EntityType<LandCreature> type = ModEntities.ORNITHOLESTES.get();
        helper.assertTrue(type.is(ModTags.PREDATORS), "deveria estar em #predators");
        for (String tag : new String[] {"small_prey", "velociraptor_prey", "kelenken_prey"}) {
            TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(IceAgeSurvival.MODID, tag));
            helper.assertTrue(type.is(key), "deveria estar em #" + tag);
        }
        TagKey<EntityType<?>> prey = TagKey.create(Registries.ENTITY_TYPE,
                new ResourceLocation(IceAgeSurvival.MODID, "ornitholestes_prey"));
        helper.assertTrue(EntityType.RABBIT.is(prey) && EntityType.CHICKEN.is(prey) && EntityType.FROG.is(prey)
                && EntityType.FOX.is(prey) && ModEntities.DODO.get().is(prey), "presas do Ornitholestes incompletas");
        helper.succeed();
    }

    // ---- Agarrão ----

    /** O bote não arranca; o golpe seguinte prende a galinha. */
    @GameTest(template = ARENA, batch = "ornitholestes_grab", timeoutTicks = 40)
    public static void theGrabHoldsAChickenAfterThePounce(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = ornitholestes(helper, 4, 0, 4);
        Chicken chicken = sturdy(helper.spawnWithNoFreeWill(EntityType.CHICKEN, 5, 0, 4));
        double before = hunter.getAttributeValue(Attributes.MOVEMENT_SPEED);
        hunter.onPounce(chicken);
        helper.assertTrue(Math.abs(hunter.getAttributeValue(Attributes.MOVEMENT_SPEED) - before) < 1e-9,
                "o agarrão não tem arrancada");
        hunter.doHurtTarget(chicken);
        var slow = chicken.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(slow != null, "a galinha não foi agarrada");
        helper.assertTrue(slow.getAmplifier() == HuntSpecials.GRAB_AMPLIFIER, "lentidão nível " + slow.getAmplifier());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "ornitholestes_grab_rabbit", timeoutTicks = 40)
    public static void theGrabHoldsARabbitAfterThePounce(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = ornitholestes(helper, 4, 0, 10);
        Rabbit rabbit = sturdy(helper.spawnWithNoFreeWill(EntityType.RABBIT, 5, 0, 10));
        hunter.onPounce(rabbit);
        hunter.doHurtTarget(rabbit);
        helper.assertTrue(rabbit.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o coelho não foi agarrado");
        helper.succeed();
    }

    /** Sem bote antes, o golpe é só uma mordida. */
    @GameTest(template = ARENA, batch = "ornitholestes_grab_nopounce", timeoutTicks = 40)
    public static void withoutThePounceTheBiteDoesNotGrab(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = ornitholestes(helper, 4, 0, 4);
        Chicken chicken = sturdy(helper.spawnWithNoFreeWill(EntityType.CHICKEN, 5, 0, 4));
        hunter.doHurtTarget(chicken);
        helper.assertTrue(chicken.getHealth() < chicken.getMaxHealth(), "o golpe não acertou");
        helper.assertFalse(chicken.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "agarrou sem bote");
        helper.succeed();
    }

    /** Presa maior que ele (a vaca) não fica presa nas mãos do Ornitholestes. */
    @GameTest(template = ARENA, batch = "ornitholestes_grab_big", timeoutTicks = 40)
    public static void theGrabDoesNotHoldBiggerPrey(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = ornitholestes(helper, 4, 0, 4);
        Cow cow = sturdy(helper.spawnWithNoFreeWill(EntityType.COW, 6, 0, 4));
        helper.assertTrue(hunter.sizeRatioOf(cow) > 1.0, "a vaca deveria ser maior: " + hunter.sizeRatioOf(cow));
        hunter.onPounce(cow);
        hunter.doHurtTarget(cow);
        helper.assertTrue(cow.getHealth() < cow.getMaxHealth(), "o golpe não acertou");
        helper.assertFalse(cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "agarrou uma vaca");
        helper.succeed();
    }

    /** O bote só deixa o golpe pronto por um tempo. */
    @GameTest(template = ARENA, batch = "ornitholestes_grab_window", timeoutTicks = HuntSpecials.GRAB_WINDOW_TICKS + 40)
    public static void theGrabWindowCloses(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature hunter = ornitholestes(helper, 4, 0, 4);
        Chicken chicken = sturdy(helper.spawnWithNoFreeWill(EntityType.CHICKEN, 5, 0, 4));
        hunter.onPounce(chicken);
        helper.runAtTickTime(HuntSpecials.GRAB_WINDOW_TICKS + 10, () -> {
            hunter.doHurtTarget(chicken);
            helper.assertFalse(chicken.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o bote antigo ainda agarrou");
            helper.succeed();
        });
    }

    // ---- Camuflagem ----

    /** No chão limpo fica à vista; com folhas em volta, escondido — e notado a metade do raio. */
    @GameTest(template = EMPTY, batch = "ornitholestes_cover", timeoutTicks = 80)
    public static void leavesAroundHideTheOrnitholestes(GameTestHelper helper) {
        LandCreature hunter = ornitholestes(helper, 2, 2, 2);
        helper.runAtTickTime(5, () -> {
            BlockPos feet = hunter.blockPosition();
            helper.assertTrue(PrehistoricCreature.coverBlocksAt(helper.getLevel(), feet) == 0,
                    "chão limpo com esconderijo: " + PrehistoricCreature.coverBlocksAt(helper.getLevel(), feet));
            helper.assertFalse(hunter.isConcealed(), "escondido no chão limpo");
            helper.assertTrue(Math.abs(PrehistoricCreature.perceivedRadius(hunter, 20.0) - 20.0) < 1e-9,
                    "à vista deveria ser notado no raio inteiro");
            var leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            helper.getLevel().setBlockAndUpdate(feet.north(), leaves);
            helper.getLevel().setBlockAndUpdate(feet.above().north(), leaves);
            helper.getLevel().setBlockAndUpdate(feet.east(), leaves);
            helper.assertTrue(PrehistoricCreature.coverBlocksAt(helper.getLevel(), feet) == 3,
                    "três folhas em volta: " + PrehistoricCreature.coverBlocksAt(helper.getLevel(), feet));
        });
        // A camuflagem é conferida a cada 20 ticks.
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(hunter.isConcealed(), "não ficou escondido entre as folhas");
            helper.assertTrue(Math.abs(PrehistoricCreature.perceivedRadius(hunter, 20.0) - 10.0) < 1e-9,
                    "escondido deveria ser notado a metade do raio: " + PrehistoricCreature.perceivedRadius(hunter, 20.0));
            helper.succeed();
        });
    }

    /** Mato e neve alta escondem; neve rasa, não. */
    @GameTest(template = EMPTY, batch = "ornitholestes_cover")
    public static void grassAndDeepSnowCountAsCover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spot = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(spot.north(), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2));
        helper.assertTrue(PrehistoricCreature.coverBlocksAt(level, spot) == 0, "neve de duas camadas não esconde");
        level.setBlockAndUpdate(spot.north(), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3));
        helper.assertTrue(PrehistoricCreature.coverBlocksAt(level, spot) == 1, "neve de três camadas esconde");
        level.setBlockAndUpdate(spot.south(), Blocks.GRASS.defaultBlockState());
        level.setBlockAndUpdate(spot.west(), Blocks.FERN.defaultBlockState());
        helper.assertTrue(PrehistoricCreature.coverBlocksAt(level, spot) == 3,
                "grama, samambaia e neve alta: " + PrehistoricCreature.coverBlocksAt(level, spot));
        level.setBlockAndUpdate(spot.east(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(PrehistoricCreature.coverBlocksAt(level, spot) == 3, "pedra não é esconderijo");
        helper.succeed();
    }

    // ---- Horário ----

    /**
     * De dia o selvagem procura onde se deitar e dorme. Dois indivíduos nascidos no mesmo tick têm ids de paridade
     * diferente: o GoalSelector só avalia objetivos novos em ticks alternados conforme (tick do servidor + id), e os
     * dois precisam dormir.
     */
    @GameTest(template = ARENA, batch = DAY_SLEEP, timeoutTicks = 500)
    public static void theWildOrnitholestesSleepsByDay(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature first = helper.spawn(ModEntities.ORNITHOLESTES.get(), 6, 0, 6);
        LandCreature second = helper.spawn(ModEntities.ORNITHOLESTES.get(), 16, 0, 16);
        helper.succeedWhen(() -> {
            for (LandCreature hunter : new LandCreature[] {first, second}) {
                helper.assertTrue(hunter.restsNow(), "de dia deveria ser hora de dormir");
                helper.assertTrue(hunter.isResting(), "o de id " + hunter.getId() + " ainda não dormiu");
            }
        });
    }

    /** Ferido, acorda. */
    @GameTest(template = ARENA, batch = DAY_WAKE, timeoutTicks = 500)
    public static void theSleepingOrnitholestesWakesWhenHurt(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature hunter = helper.spawn(ModEntities.ORNITHOLESTES.get(), 16, 0, 16);
        Chicken attacker = helper.spawnWithNoFreeWill(EntityType.CHICKEN, 20, 0, 20);
        boolean[] hurt = {false};
        helper.onEachTick(() -> {
            if (!hurt[0] && hunter.isResting()) {
                hurt[0] = true;
                hunter.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1.0F);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(hurt[0], "ainda não dormiu (alvo " + hunter.getTarget() + ", hora de dormir "
                    + hunter.restsNow() + ", objetivos " + hunter.goalSelector.getRunningGoals()
                    .map(goal -> goal.getGoal().getClass().getSimpleName()).toList() + ")");
            helper.assertFalse(hunter.isResting(), "ferido, continuou dormindo");
        });
    }

    /**
     * Sono pesado: o jogador ao lado não o acorda — só o golpe (pedido do Felipe, 2026-10-03; antes notava a ameaça
     * a metade do raio e fugia).
     */
    @GameTest(template = ARENA, batch = DAY_DEEP, timeoutTicks = 700)
    public static void theSleepingOrnitholestesIgnoresAPlayerNearby(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature hunter = helper.spawn(ModEntities.ORNITHOLESTES.get(), 16, 0, 16);
        long[] asleepAt = {-1};
        Player[] player = {null};
        helper.onEachTick(() -> {
            if (asleepAt[0] < 0 && hunter.isResting()) {
                asleepAt[0] = helper.getTick();
                player[0] = helper.makeMockSurvivalPlayer();
                Vec3 beside = hunter.position().add(2.0, 0.0, 0.0);
                player[0].moveTo(beside.x, beside.y, beside.z);
            } else if (asleepAt[0] >= 0) {
                helper.assertTrue(hunter.isResting(), "acordou com o jogador ao lado, sem ser atacado");
                if (helper.getTick() - asleepAt[0] > 200) {
                    helper.succeed();
                }
            }
        });
    }

    /** Domesticado segue o dono a qualquer hora: não dorme de dia. */
    @GameTest(template = ARENA, batch = DAY_TAMED, timeoutTicks = 400)
    public static void theTamedOrnitholestesDoesNotSleepByDay(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature tamed = helper.spawn(ModEntities.ORNITHOLESTES.get(), 6, 0, 16);
        Player owner = helper.makeMockSurvivalPlayer();
        tamed.tame(owner);
        helper.onEachTick(() -> {
            helper.assertTrue(tamed.isAlive(), "o Ornitholestes sumiu");
            helper.assertFalse(tamed.restsNow(), "domesticado com hora de dormir");
            helper.assertFalse(tamed.isResting(), "domesticado dormiu de dia");
        });
        helper.runAtTickTime(320, helper::succeed);
    }

    /** À noite, a hora dele: não dorme. */
    @GameTest(template = ARENA, batch = NIGHT_AWAKE, timeoutTicks = 400)
    public static void theWildOrnitholestesIsAwakeAtNight(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(MIDNIGHT);
        LandCreature hunter = helper.spawn(ModEntities.ORNITHOLESTES.get(), 4, 0, 4);
        helper.onEachTick(() -> {
            helper.assertTrue(hunter.isAlive(), "o Ornitholestes sumiu");
            helper.assertFalse(hunter.restsNow(), "à noite com hora de dormir");
            helper.assertFalse(hunter.isResting(), "dormiu à noite");
        });
        helper.runAtTickTime(320, helper::succeed);
    }

    // ---- Carniça ----

    private static ItemEntity meat(GameTestHelper helper, Vec3 at, int count) {
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.BEEF, count));
        item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(32767);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    /** Com fome, vai até a carne crua no chão e come um pedaço — os dois, de ids de paridade diferente. */
    @GameTest(template = ARENA, batch = NIGHT_EAT, timeoutTicks = 400)
    public static void theHungryOrnitholestesEatsMeatOnTheGround(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(MIDNIGHT);
        LandCreature first = helper.spawn(ModEntities.ORNITHOLESTES.get(), 4, 0, 4);
        LandCreature second = helper.spawn(ModEntities.ORNITHOLESTES.get(), 4, 0, 18);
        LandCreature[] hunters = {first, second};
        ItemEntity[] meats = new ItemEntity[2];
        long[] hungerBefore = new long[2];
        for (int i = 0; i < 2; i++) {
            hunters[i].starve();
            meats[i] = meat(helper, hunters[i].position().add(8, 0.2, 0), 3);
            hungerBefore[i] = hunters[i].ticksSinceMeal();
        }
        helper.succeedWhen(() -> {
            for (int i = 0; i < 2; i++) {
                ItemEntity meat = meats[i];
                helper.assertTrue(!meat.isAlive() || meat.getItem().getCount() < 3, "o de id " + hunters[i].getId()
                        + " não comeu a carne (" + meat.getItem().getCount() + ", a " + hunters[i].distanceTo(meat)
                        + " blocos)");
                helper.assertTrue(hunters[i].ticksSinceMeal() < hungerBefore[i], "comeu e a fome não diminuiu");
            }
        });
    }

    /** Com um predador maior perto da carne, o necrófago não chega perto. */
    @GameTest(template = ARENA, batch = NIGHT_GUARDED, timeoutTicks = 400)
    public static void theOrnitholestesLeavesMeatGuardedByABiggerPredator(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(MIDNIGHT);
        LandCreature hunter = helper.spawn(ModEntities.ORNITHOLESTES.get(), 4, 0, 4);
        hunter.starve();
        ItemEntity meat = meat(helper, hunter.position().add(8, 0.2, 0), 3);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 14, 0, 6);
        long hungerBefore = hunter.ticksSinceMeal();
        helper.assertTrue(hunter.sizeRatioOf(smilodon) > 1.0, "o Smilodon deveria ser maior");
        helper.onEachTick(() -> {
            helper.assertTrue(hunter.isAlive() && smilodon.isAlive(), "uma das criaturas sumiu");
            helper.assertTrue(hunter.ticksSinceMeal() >= hungerBefore, "comeu alguma coisa");
            helper.assertTrue(meat.isAlive() && meat.getItem().getCount() == 3, "comeu a carne guardada pelo Smilodon");
        });
        helper.runAtTickTime(300, helper::succeed);
    }

    // ---- Sentinela ----

    /** Domesticado, avisa o dono do Smilodon selvagem que chega perto — uma vez só por minuto. */
    @GameTest(template = ARENA, batch = "ornitholestes_sentinel", timeoutTicks = 200)
    public static void theSentinelWarnsItsOwnerOfAPredator(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature sentinel = helper.spawnWithNoFreeWill(ModEntities.ORNITHOLESTES.get(), 4, 0, 4);
        ServerPlayer owner = PredatorTests.survivalPlayer(helper);
        owner.setPos(sentinel.position().add(1, 0, 1));
        sentinel.tame(owner);
        helper.assertTrue(sentinel.lastSentinelWarning() == Long.MIN_VALUE, "avisou antes de ter predador");
        helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 14, 0, 4);
        long[] firstWarning = {Long.MIN_VALUE};
        helper.onEachTick(() -> {
            long warned = sentinel.lastSentinelWarning();
            if (firstWarning[0] == Long.MIN_VALUE && warned != Long.MIN_VALUE) {
                firstWarning[0] = warned;
            }
            if (firstWarning[0] != Long.MIN_VALUE) {
                helper.assertTrue(warned == firstWarning[0], "avisou de novo do mesmo predador antes de um minuto");
            }
        });
        helper.runAtTickTime(150, () -> {
            helper.assertTrue(firstWarning[0] != Long.MIN_VALUE, "o sentinela não avisou do Smilodon");
            owner.discard();
            helper.succeed();
        });
    }

    /** Com o dono longe, o sentinela não tem a quem avisar. */
    @GameTest(template = ARENA, batch = "ornitholestes_sentinel_far", timeoutTicks = 200)
    public static void theSentinelDoesNotWarnAFarAwayOwner(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature sentinel = helper.spawnWithNoFreeWill(ModEntities.ORNITHOLESTES.get(), 4, 0, 4);
        ServerPlayer owner = PredatorTests.survivalPlayer(helper);
        owner.setPos(sentinel.position().add(100, 0, 0));
        sentinel.tame(owner);
        helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 14, 0, 4);
        helper.onEachTick(() -> {
            helper.assertTrue(sentinel.isAlive(), "o sentinela sumiu");
            helper.assertTrue(sentinel.lastSentinelWarning() == Long.MIN_VALUE, "avisou um dono a mais de 64 blocos");
        });
        helper.runAtTickTime(100, () -> {
            owner.discard();
            helper.succeed();
        });
    }
}
