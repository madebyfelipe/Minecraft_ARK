package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.temperature.EnvironmentColdSource;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Início de jogo: dodô (comida e pena), Elasmotério (pele, montaria sem sela, calor) e a roupa de pena. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class EarlyGameTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void earlyCreaturesAreCommonNearSpawn(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        SpawnProfile dodo = Species.of(registries, ModEntities.DODO.get()).orElseThrow().spawn().orElseThrow();
        SpawnProfile elasmo = Species.of(registries, ModEntities.ELASMOTHERIUM.get()).orElseThrow().spawn().orElseThrow();
        SpawnProfile smilodon = Species.of(registries, ModEntities.SMILODON.get()).orElseThrow().spawn().orElseThrow();
        SpawnProfile pteranodon = Species.of(registries, ModEntities.PTERANODON.get()).orElseThrow().spawn().orElseThrow();
        SpawnProfile velociraptor = Species.of(registries, ModEntities.VELOCIRAPTOR.get()).orElseThrow().spawn().orElseThrow();
        helper.assertTrue(dodo.minDistance() == 0 && elasmo.minDistance() == 0, "devem nascer já no spawn");
        helper.assertTrue(smilodon.minDistance() == 0 && smilodon.groupMax() == 1,
                "Smilodon solitário deveria ocupar o lugar do bando de raptores na área inicial");
        helper.assertTrue(velociraptor.minDistance() == 300,
                "bandos de Velociraptor só deveriam aparecer depois da zona inicial");
        helper.assertTrue(pteranodon.minDistance() == velociraptor.minDistance(),
                "Pteranodonte deveria compartilhar a distância mínima dos raptores");
        helper.assertTrue(dodo.weight() > smilodon.weight() && elasmo.weight() > smilodon.weight(),
                "deveriam ser mais comuns que um predador do meio");
        helper.assertTrue(dodo.maxNearby() >= 8, "dodôs deveriam ser abundantes: " + dodo.maxNearby());
        helper.assertTrue(WildSpawner.effectiveMaximumPopulation()
                        == (int) (ServerConfig.WILD_SPAWN_MAX_TOTAL.get() * 0.7),
                "o teto total deve refletir a redução de aproximadamente 30%");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dodoDropsMeatAndFeathers(GameTestHelper helper) {
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        dodo.kill();
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityPresent(ModItems.DODO_MEAT.get(), new BlockPos(1, 2, 1), 3.0);
            helper.assertItemEntityPresent(ModItems.DODO_FEATHER.get(), new BlockPos(1, 2, 1), 3.0);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void elasmotheriumDropsPelt(GameTestHelper helper) {
        LandCreature elasmo = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 1, 2, 1);
        elasmo.kill();
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityPresent(ModItems.PELT.get(), new BlockPos(1, 2, 1), 3.0);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void elasmotheriumIsRiddenWithoutSaddle(GameTestHelper helper) {
        Player owner = PredatorTests.survivalPlayer(helper);
        LandCreature elasmo = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 1, 2, 1);
        elasmo.tame(owner);
        elasmo.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        owner.setPos(elasmo.position());
        helper.assertFalse(elasmo.canBeSaddled(), "o Elasmotério não usa sela");
        helper.assertFalse(elasmo.isSaddled(), "não deveria estar selado");
        helper.assertTrue(elasmo.ride(owner), "deveria montar sem sela");
        helper.assertTrue(elasmo.getControllingPassenger() == owner, "quem monta sem sela conduz");
        helper.assertTrue(elasmo.inventory().getContainerSize() == 9, "inventário pequeno de carga: "
                + elasmo.inventory().getContainerSize());
        MountProfile mount = elasmo.mountProfile().orElseThrow();
        helper.assertTrue(mount.jumpStrength() == 0.0, "criatura grande não pula");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void elasmotheriumWarmsRiderAndNeighbours(GameTestHelper helper) {
        Player owner = PredatorTests.survivalPlayer(helper);
        LandCreature elasmo = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 2, 2, 2);
        // Um Elasmotério solto de uma cena vizinha encostado no jogador "perto" dava o calor máximo.
        helper.getLevel().getEntitiesOfClass(PrehistoricCreature.class, elasmo.getBoundingBox().inflate(20.0),
                other -> other != elasmo).forEach(PrehistoricCreature::discard);
        elasmo.tame(owner);
        elasmo.setAffinity(PrehistoricCreature.MAX_AFFINITY);

        Player far = helper.makeMockSurvivalPlayer();
        far.setPos(elasmo.position().add(12.0, 0.0, 0.0));
        helper.assertTrue(EnvironmentColdSource.bodyHeat(far) == 0.0, "longe não aquece");

        Player near = helper.makeMockSurvivalPlayer();
        near.setPos(elasmo.position().add(elasmo.getBbWidth() / 2.0 + 2.0, 0.0, 0.0));
        double nearHeat = EnvironmentColdSource.bodyHeat(near);
        helper.assertTrue(nearHeat > 0.0, "perto deveria aquecer");

        owner.setPos(elasmo.position());
        helper.assertTrue(elasmo.ride(owner), "deveria montar");
        double riding = EnvironmentColdSource.bodyHeat(owner);
        helper.assertTrue(riding > nearHeat, "montado aquece mais que perto: " + riding + " x " + nearHeat);
        helper.succeed();
    }

    /**
     * Pena: entre o couro e a pele. Completa, cobre o frio do dia na taiga nevada (1,0) mas não o da
     * noite (1,2): à noite o jogador precisa de abrigo, fogo ou do calor do Elasmotério.
     */
    @GameTest(template = EMPTY)
    public static void featherArmorSitsBetweenLeatherAndFur(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        dress(player, ModItems.FEATHER_HELMET.get(), ModItems.FEATHER_CHESTPLATE.get(),
                ModItems.FEATHER_LEGGINGS.get(), ModItems.FEATHER_BOOTS.get());
        double feather = EnvironmentColdSource.insulation(player);
        dress(player, ModItems.FUR_HELMET.get(), ModItems.FUR_CHESTPLATE.get(),
                ModItems.FUR_LEGGINGS.get(), ModItems.FUR_BOOTS.get());
        double fur = EnvironmentColdSource.insulation(player);
        helper.assertTrue(Math.abs(feather - 1.0) < 1e-6, "pena completa isola " + feather);
        helper.assertTrue(feather > 0.8 && feather < fur, "pena deveria ficar entre couro (0,8) e pele: " + feather);
        helper.succeed();
    }

    private static void dress(Player player, net.minecraft.world.item.Item... pieces) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            player.setItemSlot(slots[i], new ItemStack(pieces[i]));
        }
    }
}
