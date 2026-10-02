package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.util.Random;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Fôlego de voo: atributo só dos voadores, gasto no ar, recarregado no chão. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class FlightStaminaTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    /** O Pteranodonte tem fôlego de voo e recebe pontos nele; quem não voa, nem uma coisa nem outra. */
    @GameTest(template = EMPTY, batch = "flight_stamina")
    public static void onlyFlyersHaveFlightStamina(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var ptero = Species.of(access, ModEntities.PTERANODON.get()).orElseThrow().stats();
        var dodo = Species.of(access, ModEntities.DODO.get()).orElseThrow().stats();
        helper.assertTrue(ptero.flies() && ptero.value(Stat.FLIGHT_STAMINA, 0) >= 20, "o Pteranodonte deveria ter fôlego");
        helper.assertFalse(dodo.flies(), "o dodô não voa");
        StatPoints dodoPoints = StatPoints.rollWild(100, new Random(3), dodo.scalableStats());
        helper.assertTrue(dodoPoints.get(Stat.FLIGHT_STAMINA) == 0, "ponto de fôlego de voo num dodô");
        int flightPoints = 0;
        for (int seed = 0; seed < 20; seed++) {
            flightPoints += StatPoints.rollWild(100, new Random(seed), ptero.scalableStats()).get(Stat.FLIGHT_STAMINA);
        }
        helper.assertTrue(flightPoints > 0, "o Pteranodonte nunca recebe pontos de fôlego de voo");
        helper.succeed();
    }

    /** Esgotado no ar, o Pteranodonte selvagem para de voar e plana até o chão; lá, recupera. */
    @GameTest(template = ARENA, batch = "flight_stamina_air", timeoutTicks = 400)
    public static void anExhaustedPteranodonGlidesDownAndRecovers(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ptero = helper.spawn(ModEntities.PTERANODON.get(), 12, 8, 12);
        ptero.setWildFlying(true);
        ptero.setFlightStaminaFraction(0.002F);
        float[] landedAt = {-1};
        helper.onEachTick(() -> {
            if (landedAt[0] < 0 && ptero.onGround()) {
                helper.assertFalse(ptero.isFlying(), "chegou ao chão ainda voando");
                helper.assertTrue(ptero.isFlightExhausted(), "deveria estar esgotado ao pousar");
                landedAt[0] = ptero.flightStaminaFraction();
            } else if (landedAt[0] >= 0 && ptero.flightStaminaFraction() > landedAt[0] + 0.05F) {
                helper.assertFalse(ptero.isFlying(), "decolou ainda esgotado");
                helper.succeed();
            }
        });
    }
}
