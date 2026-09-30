package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.SoundProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Só são registrados quando o jogo sobe com {@code neoforge.enabledGameTestNamespaces}. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class CreatureFrameworkTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void speciesIsLoadedFromDatapack(GameTestHelper helper) {
        helper.assertTrue(
                Species.of(helper.getLevel().registryAccess(), ModEntities.TEST_CREATURE.get()).isPresent(),
                "espécie test_creature não carregada");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void speciesSoundsAreLoaded(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        SoundProfile rex = Species.of(registries, ModEntities.TYRANNOSAURUS.get()).orElseThrow().sounds().orElseThrow();
        helper.assertTrue(rex.ambient().isPresent() && rex.hurt().isPresent() && rex.death().isPresent(),
                "T-Rex sem som de ambiente, dano ou morte");
        helper.assertTrue(rex.alert().isPresent(), "T-Rex sem rugido");
        helper.assertTrue(rex.volume() > 1.0F, "T-Rex devia ser ouvido de longe");
        helper.assertTrue(Species.of(registries, ModEntities.DIRE_WOLF.get()).orElseThrow().sounds().isEmpty(),
                "lobo-terrível não tem sons no Revival");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void wildCreatureRollsLevelAndAppliesStats(GameTestHelper helper) {
        TestCreature creature = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
        Species species = creature.species().orElseThrow();

        int level = creature.creatureLevel();
        helper.assertTrue(level >= 10 && level <= 100 && level % 10 == 0, "nível fora da escala: " + level);
        helper.assertTrue(creature.statPoints().level() == level, "nível sincronizado difere dos pontos");

        assertClose(helper, "vida máxima", species.stats().value(Stat.HEALTH, creature.statPoints()), creature.getMaxHealth());
        assertClose(helper, "vida atual", creature.getMaxHealth(), creature.getHealth());
        assertClose(helper, "ataque", species.stats().value(Stat.ATTACK, creature.statPoints()),
                creature.getAttributeValue(Attributes.ATTACK_DAMAGE));
        assertClose(helper, "torpor máximo", species.stats().value(Stat.TORPOR, creature.statPoints()), creature.maxTorpor());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void statsAndOwnerSurviveSaveAndLoad(GameTestHelper helper) {
        TestCreature original = helper.spawnWithNoFreeWill(ModEntities.TEST_CREATURE.get(), 1, 2, 1);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        original.tame(owner);
        original.setHealth(original.getMaxHealth() / 2);

        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        TestCreature loaded = ModEntities.TEST_CREATURE.get().create(helper.getLevel());
        loaded.load(saved);

        helper.assertTrue(loaded.statPoints().equals(original.statPoints()), "pontos de atributo não persistiram");
        helper.assertTrue(loaded.creatureLevel() == original.creatureLevel(), "nível não persistiu");
        assertClose(helper, "vida máxima", original.getMaxHealth(), loaded.getMaxHealth());
        assertClose(helper, "vida atual", original.getHealth(), loaded.getHealth());
        helper.assertTrue(loaded.isTame(), "estado domesticado não persistiu");
        helper.assertTrue(owner.getUUID().equals(loaded.getOwnerUUID()), "dono não persistiu");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void smilodonIsTerritorialUntilTamed(GameTestHelper helper) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        BehaviorProfile behavior = smilodon.behavior().orElseThrow();
        helper.assertTrue(smilodon.hasRestriction(), "selvagem sem território");
        helper.assertTrue(smilodon.getRestrictCenter().equals(smilodon.blockPosition()), "território fora do ponto de origem");
        assertClose(helper, "raio de percepção", behavior.aggroRadius(), smilodon.getAttributeValue(Attributes.FOLLOW_RANGE));

        smilodon.tame(helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(!smilodon.hasRestriction(), "domesticado continua preso ao território");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void territorySurvivesSaveAndLoad(GameTestHelper helper) {
        LandCreature original = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        LandCreature loaded = ModEntities.SMILODON.get().create(helper.getLevel());
        loaded.load(original.saveWithoutId(new CompoundTag()));
        // O NBT carrega o UUID da original; o mundo recusaria duas entidades com o mesmo.
        loaded.setUUID(UUID.randomUUID());
        loaded.moveTo(original.getX() + 1, original.getY(), original.getZ());
        helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "entidade recarregada não entrou no mundo");
        helper.assertTrue(loaded.getRestrictCenter().equals(original.getRestrictCenter()), "centro do território mudou ao recarregar");
        helper.succeed();
    }

    private static void assertClose(GameTestHelper helper, String what, double expected, double actual) {
        helper.assertTrue(Math.abs(expected - actual) < 1e-3, what + ": esperado " + expected + ", obtido " + actual);
    }
}
