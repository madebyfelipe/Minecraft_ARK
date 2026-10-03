package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.block.ChemistryRecipe;
import dev.madebyfelipe.iceagesurvival.core.ecology.Activity;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.effect.VenomEffect;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.VenomBite;
import dev.madebyfelipe.iceagesurvival.entity.ai.VenomTrackGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * A Megalania, o varanídeo gigante: morde, solta e segue o rastro da presa envenenada (Fry et al. 2009; Bull et al.
 * 2010). A peçonha é choque (torpor) nas criaturas do mod e sangramento sem regeneração no jogador e nos mobs vanilla;
 * o antídoto da mesa química cura. Domesticada, derruba a presa no nome do dono e não ataca o apex. Diurna.
 *
 * <p>Os testes que mexem na hora ficam no seu batch, com a hora devolvida no fim: ela é do mundo inteiro.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class MegalaniaTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final String SPECIES = "megalania_species";
    private static final String NIGHT = "megalania_night";
    /* Diurna: a mordida e o rastro são de dia, senão ela vai dormir no meio do teste. */
    private static final String DAY_RETREAT = "megalania_day_retreat";
    private static final String DAY_TRAIL = "megalania_day_trail";
    private static final String DAY_FINISH = "megalania_day_finish";
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

    @BeforeBatch(batch = NIGHT)
    public static void beforeNight(ServerLevel level) {
        setHour(level, MIDNIGHT);
    }

    @AfterBatch(batch = NIGHT)
    public static void afterNight(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_RETREAT)
    public static void beforeDayRetreat(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_RETREAT)
    public static void afterDayRetreat(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_TRAIL)
    public static void beforeDayTrail(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_TRAIL)
    public static void afterDayTrail(ServerLevel level) {
        restoreHour(level);
    }

    @BeforeBatch(batch = DAY_FINISH)
    public static void beforeDayFinish(ServerLevel level) {
        setHour(level, NOON);
    }

    @AfterBatch(batch = DAY_FINISH)
    public static void afterDayFinish(ServerLevel level) {
        restoreHour(level);
    }

    private static EntityType<LandCreature> megalania() {
        return ModEntities.MEGALANIA.get();
    }

    private static <T extends LivingEntity> T withHealth(T entity, double health) {
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        entity.setHealth((float) health);
        return entity;
    }

    // ---- Espécie e dados ----

    /** Vida 120, ataque 14, armadura 4, speed 0,3; peçonha; diurna; solitária a 300+ blocos; ideal savana e badlands. */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void theMegalaniaIsASolitaryVenomousHunter(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Species species = Species.of(registries, megalania()).orElse(null);
        helper.assertTrue(species != null, "a Megalania não carregou");
        helper.assertTrue(species.stats().entry(Stat.HEALTH).base() == 120.0, "vida");
        helper.assertTrue(species.stats().entry(Stat.ATTACK).base() == 14.0, "ataque");
        helper.assertTrue(species.stats().entry(Stat.ARMOR).base() == 4.0, "armadura");
        helper.assertTrue(species.stats().entry(Stat.SPEED).base() == 0.3, "velocidade");
        helper.assertTrue(megalania().getWidth() == 1.5F && megalania().getHeight() == 1.1F, "caixa de colisão");

        BehaviorProfile behavior = species.behavior().orElseThrow();
        helper.assertTrue(behavior.huntSpecial() == BehaviorProfile.HuntSpecial.VENOM, "deveria morder com peçonha");
        helper.assertTrue(behavior.herdRadius() == 0, "solitária");
        helper.assertTrue(behavior.habits().activity() == Activity.Pattern.DIURNAL, "deveria ser diurna");
        TagKey<EntityType<?>> prey = TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("megalania_prey"));
        helper.assertTrue(behavior.prey().map(prey::equals).orElse(false), "presas: " + behavior.prey());

        SpawnProfile spawn = species.spawn().orElseThrow();
        helper.assertTrue(spawn.groupMin() == 1 && spawn.groupMax() == 1 && spawn.maxNearby() == 1, "solitária no spawn");
        helper.assertTrue(spawn.minDistance() == 300, "distância do spawn: " + spawn.minDistance());
        var biomes = registries.registryOrThrow(Registries.BIOME);
        TagKey<Biome> ideal = TagKey.create(Registries.BIOME, IceAgeSurvival.id("ideal_megalania"));
        var forest = biomes.getHolderOrThrow(Biomes.FOREST);
        for (var key : java.util.List.of(Biomes.SAVANNA, Biomes.BADLANDS)) {
            var biome = biomes.getHolderOrThrow(key);
            helper.assertTrue(biome.is(ideal), key.location() + " deveria ser ideal");
            helper.assertTrue(spawn.weightIn(biome) == 3 * spawn.weightIn(forest),
                    "no ideal deveria ser o triplo: " + spawn.weightIn(biome) + " × " + spawn.weightIn(forest));
        }
        helper.assertTrue(spawn.weightIn(forest) > 0, "nasce em qualquer bioma");
        helper.succeed();
    }

    /** A Megalania e o Quetzalcoatlus são predadores (as presas os temem); o Quetzalcoatlus é presa do T-Rex. */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void theNewHuntersHaveTheirPlaceInTheFoodChain(GameTestHelper helper) {
        TagKey<EntityType<?>> predators = TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("predators"));
        TagKey<EntityType<?>> rexPrey = TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("tyrannosaurus_prey"));
        helper.assertTrue(megalania().is(predators), "a Megalania deveria estar em #predators");
        helper.assertTrue(ModEntities.QUETZALCOATLUS.get().is(predators), "o Quetzalcoatlus deveria estar em #predators");
        helper.assertTrue(ModEntities.QUETZALCOATLUS.get().is(rexPrey), "o Quetzalcoatlus deveria ser presa do T-Rex");
        helper.succeed();
    }

    /** Antídoto na mesa química: carvão (vegetal ou mineral) e um frasco de vidro. */
    @GameTest(template = EMPTY, batch = SPECIES)
    public static void theAntidoteIsMadeAtTheChemistryBench(GameTestHelper helper) {
        for (var coal : new net.minecraft.world.item.Item[] {Items.CHARCOAL, Items.COAL}) {
            var recipe = ChemistryRecipe.find(new ItemStack(coal), new ItemStack(Items.GLASS_BOTTLE)).orElse(null);
            helper.assertTrue(recipe != null && recipe.output().is(ModItems.ANTIDOTE.get()),
                    "carvão + frasco deveria dar antídoto: " + coal);
        }
        helper.succeed();
    }

    // ---- Peçonha ----

    /** Numa criatura do mod a peçonha é choque: soma torpor a cada segundo, sem sangrar. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void venomShocksAModCreatureIntoTorpor(GameTestHelper helper) {
        LandCreature hunter = helper.spawnWithNoFreeWill(megalania(), 1, 2, 1);
        LandCreature dodo = withHealth(helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 2, 2, 1), 200.0);
        hunter.doHurtTarget(dodo);
        helper.assertTrue(VenomBite.isEnvenomed(dodo), "a mordida deveria envenenar");
        double torpor = dodo.torpor();
        float health = dodo.getHealth();
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(dodo.torpor() > torpor + dodo.maxTorpor() * VenomEffect.TORPOR_PER_SECOND,
                    "o choque deveria somar torpor: " + torpor + " → " + dodo.torpor());
            helper.assertTrue(dodo.getHealth() >= health, "na criatura do mod a peçonha não sangra");
            helper.succeed();
        });
    }

    /** No jogador a peçonha sangra e não deixa regenerar, nem com a fome cheia. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void venomBleedsThePlayerWithoutRegeneration(GameTestHelper helper) {
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        LandCreature hunter = helper.spawnWithNoFreeWill(megalania(), 1, 2, 1);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.setHealth(10.0F);
        VenomBite.envenom(hunter, player);
        player.heal(4.0F);
        helper.assertTrue(player.getHealth() == 10.0F, "envenenado não deveria curar: " + player.getHealth());
        // O jogador falso não tem conexão que o faça andar: o tick de vida (efeitos, fome) roda aqui.
        helper.onEachTick(player::doTick);
        helper.runAfterDelay(140, () -> {
            float health = player.getHealth();
            player.discard();
            helper.assertTrue(health < 10.0F, "deveria ter sangrado, com a fome cheia: " + health);
            helper.succeed();
        });
    }

    /** O antídoto bebido cura a peçonha e devolve o frasco; dado a uma criatura, cura na hora. */
    @GameTest(template = EMPTY)
    public static void theAntidoteCuresTheVenom(GameTestHelper helper) {
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        LandCreature hunter = helper.spawnWithNoFreeWill(megalania(), 1, 2, 1);
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 2, 2, 1);
        VenomBite.envenom(hunter, player);
        VenomBite.envenom(hunter, dodo);

        ItemStack drink = new ItemStack(ModItems.ANTIDOTE.get());
        ItemStack left = drink.getItem().finishUsingItem(drink, helper.getLevel(), player);
        helper.assertFalse(player.hasEffect(ModEffects.VENOM.get()), "bebido, o antídoto deveria curar");
        helper.assertTrue(left.is(Items.GLASS_BOTTLE), "deveria sobrar o frasco: " + left);

        ItemStack dose = new ItemStack(ModItems.ANTIDOTE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, dose);
        dose.getItem().interactLivingEntity(dose, player, dodo, InteractionHand.MAIN_HAND);
        helper.assertFalse(VenomBite.isEnvenomed(dodo), "dado ao dodô, deveria curar");
        helper.assertTrue(dose.isEmpty(), "a dose deveria ser gasta");
        player.discard();
        helper.succeed();
    }

    // ---- Mordida, recuo e rastro ----

    /** Morde e recua: logo depois da mordida a Megalania se afasta da presa, em vez de agarrar e lutar. */
    @GameTest(template = ARENA, batch = DAY_RETREAT, timeoutTicks = 60, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theMegalaniaBitesAndBacksOff(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature hunter = helper.spawn(megalania(), 8, 0, 12);
        hunter.setTicksSinceMeal(0);
        Pig pig = withHealth(helper.spawnWithNoFreeWill(EntityType.PIG, 10, 0, 12), 200.0);
        hunter.doHurtTarget(pig);
        helper.assertTrue(VenomBite.isEnvenomed(pig), "a mordida deveria envenenar");
        helper.assertTrue(VenomTrackGoal.retreating(hunter), "logo depois da mordida deveria recuar");
        double start = hunter.distanceTo(pig);
        Vec3 from = hunter.position();
        // Ela mesma anda para trás: o empurrão da mordida na presa não conta.
        helper.succeedWhen(() -> helper.assertTrue(hunter.distanceTo(pig) > start + 2.0
                        && hunter.position().distanceTo(from) > 2.0,
                "deveria ter recuado: " + start + " → " + hunter.distanceTo(pig) + ", andou "
                        + hunter.position().distanceTo(from)));
    }

    /**
     * Depois do recuo, segue o rastro: vai atrás da presa que se afastou, prova o ar com a língua ({@code tongueflick})
     * e não morde de novo enquanto a peçonha age.
     */
    @GameTest(template = ARENA, batch = DAY_TRAIL, timeoutTicks = 400, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theMegalaniaFollowsTheTrailFlickingItsTongue(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature hunter = helper.spawn(megalania(), 10, 0, 6);
        hunter.setTicksSinceMeal(0);
        Pig pig = withHealth(helper.spawnWithNoFreeWill(EntityType.PIG, 12, 0, 6), 200.0);
        hunter.doHurtTarget(pig);
        float afterBite = pig.getHealth();
        helper.runAfterDelay(VenomTrackGoal.RETREAT_TICKS + 10,
                () -> pig.moveTo(helper.absoluteVec(new Vec3(12.5, 0, 21.5))));
        helper.onEachTick(() -> helper.assertTrue(pig.getHealth() > afterBite - 10.0F,
                "mordeu de novo durante o rastro: " + afterBite + " → " + pig.getHealth()));
        helper.succeedWhen(() -> {
            var trail = VenomBite.trail(hunter).orElse(null);
            helper.assertTrue(trail != null && trail.victim() == pig, "perdeu o rastro");
            helper.assertTrue(trail.tongueFlicks() >= 2, "deveria provar o ar com a língua: " + trail.tongueFlicks());
            helper.assertTrue(helper.getTick() > VenomTrackGoal.RETREAT_TICKS + 20, "esperando a presa se afastar");
            helper.assertTrue(hunter.distanceTo(pig) < VenomTrackGoal.TRAIL_DISTANCE + 3.0,
                    "deveria ter ido atrás da presa: " + hunter.distanceTo(pig) + " (objetivos "
                            + hunter.goalSelector.getRunningGoals().map(goal -> goal.getGoal().getClass().getSimpleName())
                            .toList() + ", dormindo " + hunter.isResting() + ", caminho "
                            + hunter.getNavigation().getPath() + ", em " + hunter.position() + ", presa em " + pig.position() + ")");
        });
    }

    /** A selvagem termina a presa que o choque derrubou. */
    @GameTest(template = ARENA, batch = DAY_FINISH, timeoutTicks = 400, setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theWildMegalaniaFinishesTheDownedPrey(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(NOON);
        LandCreature hunter = helper.spawn(megalania(), 10, 0, 6);
        hunter.setTicksSinceMeal(0);
        LandCreature dodo = withHealth(helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 12, 0, 6), 40.0);
        dodo.setTorpor(dodo.maxTorpor() * 0.9);
        hunter.doHurtTarget(dodo);
        helper.succeedWhen(() -> helper.assertFalse(dodo.isAlive(),
                "deveria ter terminado o dodô caído (desmaiado " + dodo.isUnconscious() + ")"));
    }

    // ---- Domesticada ----

    /** Domesticada, o choque da mordida derruba a criatura no nome do dono; e ela não ataca o apex. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void theTamedMegalaniaKnocksOutPreyForItsOwner(GameTestHelper helper) {
        ServerPlayer owner = PredatorTests.survivalPlayer(helper);
        LandCreature hunter = helper.spawnWithNoFreeWill(megalania(), 1, 2, 1);
        hunter.tame(owner);
        LandCreature dodo = withHealth(helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 2, 2, 1), 200.0);
        dodo.setTorpor(dodo.maxTorpor() * 0.9);
        hunter.doHurtTarget(dodo);

        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 3, 2, 3);
        hunter.setTarget(rex);
        helper.assertTrue(hunter.getTarget() == null, "domesticada, não deveria atacar o apex");
        LandCreature wild = helper.spawnWithNoFreeWill(megalania(), 1, 2, 3);
        wild.doHurtTarget(rex);
        helper.assertFalse(VenomBite.isEnvenomed(rex), "o apex não leva peçonha (só cai no desafio)");

        helper.succeedWhen(() -> {
            helper.assertTrue(dodo.isUnconscious(), "o choque deveria derrubar o dodô");
            helper.assertTrue(owner.getUUID().equals(dodo.tamerUUID()), "deveria cair no nome do dono");
            owner.discard();
        });
    }

    // ---- Horário ----

    /** Diurna: à noite a selvagem dorme. */
    @GameTest(template = ARENA, batch = NIGHT, timeoutTicks = 500)
    public static void theWildMegalaniaSleepsAtNight(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        helper.getLevel().setDayTime(MIDNIGHT);
        LandCreature hunter = helper.spawn(megalania(), 12, 0, 12);
        helper.succeedWhen(() -> {
            helper.assertTrue(hunter.restsNow(), "à noite deveria ser hora de dormir");
            helper.assertTrue(hunter.isResting(), "ainda não dormiu");
        });
    }
}
