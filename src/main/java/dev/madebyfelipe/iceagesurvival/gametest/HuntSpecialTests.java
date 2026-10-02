package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** A marca de cada caçador: emboscada do Smilodon, salto do Utahraptor, bicada da Kelenken. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class HuntSpecialTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    /** O bote do Smilodon arranca (+50% de velocidade) e o primeiro golpe agarra; a arrancada é curta. */
    @GameTest(template = ARENA, batch = "special_ambush", timeoutTicks = 140)
    public static void theSmilodonAmbushBurstsAndGrabs(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 4, 0, 4);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 6, 0, 4);
        double before = smilodon.getAttributeValue(Attributes.MOVEMENT_SPEED);
        smilodon.onPounce(pig);
        double burst = smilodon.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(burst > before * 1.4, "sem arrancada: " + before + " → " + burst);
        smilodon.doHurtTarget(pig);
        helper.assertTrue(pig.isDeadOrDying() || pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "o golpe não agarrou");
        helper.runAtTickTime(HuntSpecials.AMBUSH_TICKS + 10, () -> {
            helper.assertTrue(Math.abs(smilodon.getAttributeValue(Attributes.MOVEMENT_SPEED) - before) < 1e-6,
                    "a arrancada deveria ter acabado");
            helper.succeed();
        });
    }

    /** O bote do Utahraptor a média distância é um salto na direção da presa. */
    @GameTest(template = ARENA, batch = "special_leap", timeoutTicks = 40)
    public static void theUtahraptorLeapsOnItsPrey(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature utah = helper.spawnWithNoFreeWill(ModEntities.UTAHRAPTOR.get(), 4, 0, 4);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 10, 0, 4);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(utah.onGround(), "o Utahraptor deveria estar no chão");
            utah.onPounce(pig);
            var v = utah.getDeltaMovement();
            helper.assertTrue(v.y > 0.3, "não saltou: " + v);
            helper.assertTrue(v.x * (pig.getX() - utah.getX()) > 0, "saltou para o lado errado: " + v);
            helper.succeed();
        });
    }

    /** A bicada da Kelenken passa da armadura (dano a mais) e depois ela recua. */
    @GameTest(template = EMPTY, batch = "special_beak")
    public static void theKelenkenBeakPiercesAndRetreats(GameTestHelper helper) {
        LandCreature kelenken = helper.spawnWithNoFreeWill(ModEntities.KELENKEN.get(), 2, 2, 2);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 4, 2, 2);
        double attack = kelenken.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float before = golem.getHealth();
        kelenken.doHurtTarget(golem);
        float dealt = before - golem.getHealth();
        helper.assertTrue(dealt >= attack * (1.0 + HuntSpecials.BEAK_PIERCE) - 0.5,
                "bicada de " + dealt + " com ataque " + attack);
        helper.assertTrue(kelenken.isRetreatingAfterStrike(), "depois da bicada deveria recuar");
        helper.succeed();
    }

    /** A Kelenken existe como espécie: carnívora, solitária, com a bicada. */
    @GameTest(template = EMPTY, batch = "special_beak")
    public static void theKelenkenIsASpecies(GameTestHelper helper) {
        var species = Species.of(helper.getLevel().registryAccess(), ModEntities.KELENKEN.get()).orElse(null);
        helper.assertTrue(species != null, "a Kelenken não carregou");
        BehaviorProfile behavior = species.behavior().orElseThrow();
        helper.assertTrue(behavior.prey().isPresent() && behavior.huntSpecial() == BehaviorProfile.HuntSpecial.BEAK_STRIKE,
                "a Kelenken deveria caçar com a bicada");
        helper.assertTrue(species.packBonus().isEmpty(), "a Kelenken é a mais forte sozinha, sem bônus de bando");
        helper.succeed();
    }
}
