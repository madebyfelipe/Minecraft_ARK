package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.firearms.AmmoFamily;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmHits;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmItem;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmShots;
import dev.madebyfelipe.iceagesurvival.firearm.Firearms;
import dev.madebyfelipe.iceagesurvival.firearm.SolidCannonShot;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * As armas de fogo do estilo Dino Crisis 2 (D58): o dano convertido pela vida do bicho do DC2, o couro, a fúria, o
 * rifle que atravessa, a recarga, a esfera do canhão e o Titanovenator, que nenhuma arma derruba.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class FirearmTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final float EPSILON = 0.01F;

    private static Player shooter(GameTestHelper helper, Vec3 at) {
        Player player = helper.makeMockSurvivalPlayer();
        player.moveTo(helper.absoluteVec(at));
        return player;
    }

    /** Os pés onde o olho fica na altura do meio de um porco no chão da arena: o raio corre reto, rente a ele. */
    private static Vec3 eyeLevelWithAPig(double x, double z) {
        return new Vec3(x, 1 + EntityType.PIG.getHeight() / 2 - 1.62, z);
    }

    /** Um tiro direto (sem o raio), à distância dada; devolve quanto de vida saiu. */
    private static float shot(GameTestHelper helper, Player player, LivingEntity target, Firearm gun, double distance) {
        float before = target.getHealth();
        FirearmHits.hit(helper.getLevel(), player, target, gun, distance, player.position());
        return before - target.getHealth();
    }

    /** O jogador com a arma carregada na mão, mirando o centro do alvo. */
    private static ItemStack armed(Player player, Firearm gun, LivingEntity target) {
        ItemStack stack = new ItemStack(Firearms.gun(gun));
        FirearmItem.setRounds(stack, gun.magazine());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
        return stack;
    }

    /** No raptor (800 de vida no DC2, 16 aqui) a pistola tira 8: dois tiros no nível 1, como no DC2. */
    @GameTest(template = EMPTY, batch = "firearms")
    public static void theHandgunTakesHalfOfALevelOneRaptor(GameTestHelper helper) {
        LandCreature raptor = helper.spawnWithNoFreeWill(ModEntities.VELOCIRAPTOR.get(), 2, 2, 2);
        Player player = shooter(helper, new Vec3(0.5, 2, 0.5));
        float dealt = shot(helper, player, raptor, Firearm.HANDGUN, 4.0);
        helper.assertTrue(Math.abs(dealt - 8.0F) < EPSILON, "a pistola deveria tirar 8 do raptor: " + dealt);
        raptor.discard();
        helper.succeed();
    }

    /** O couro do Alossauro (5000 no DC2) segura 80% da pistola; o antitanque passa inteiro. */
    @GameTest(template = ARENA, batch = "firearms", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theAllosaurusHideHoldsTheHandgunButNotTheAntiTankRifle(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature allosaurus = helper.spawnWithNoFreeWill(ModEntities.ALLOSAURUS.get(), 12, 1, 12);
        Player player = shooter(helper, new Vec3(3.5, 1, 12.5));
        float handgun = shot(helper, player, allosaurus, Firearm.HANDGUN, 9.0);
        float antiTank = shot(helper, player, allosaurus, Firearm.ANTI_TANK_RIFLE, 9.0);
        helper.assertTrue(Math.abs(handgun - 400 * 0.2F * 160 / 5000) < EPSILON, "pistola no couro: " + handgun);
        helper.assertTrue(Math.abs(antiTank - 1600F * 160 / 5000) < EPSILON, "antitanque no Alossauro: " + antiTank);
        allosaurus.discard();
        helper.succeed();
    }

    /** A escopeta: 600 do DC2 até 5 blocos, 400 depois. */
    @GameTest(template = EMPTY, batch = "firearms")
    public static void theShotgunHitsHarderUpClose(GameTestHelper helper) {
        LandCreature brontosaurus = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 2, 2, 2);
        Player player = shooter(helper, new Vec3(0.5, 2, 0.5));
        float close = shot(helper, player, brontosaurus, Firearm.SHOTGUN, 3.0);
        float far = shot(helper, player, brontosaurus, Firearm.SHOTGUN, 12.0);
        helper.assertTrue(Math.abs(close / far - 1.5F) < 0.01F, "perto/longe deveria ser 600/400: " + close + "/" + far);
        brontosaurus.discard();
        helper.succeed();
    }

    /** Seis tiros seguidos enfurecem: dali em diante o bicho segura 75%. O antitanque não provoca. */
    @GameTest(template = EMPTY, batch = "firearms")
    public static void sprayingABigBeastEnragesItButTheAntiTankRifleDoesNot(GameTestHelper helper) {
        LandCreature sprayed = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 2, 2, 2);
        LandCreature sniped = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 6, 2, 6);
        Player player = shooter(helper, new Vec3(0.5, 2, 0.5));
        float calm = shot(helper, player, sprayed, Firearm.HEAVY_MACHINE_GUN, 8.0);
        for (int i = 1; i < 6; i++) {
            shot(helper, player, sprayed, Firearm.HEAVY_MACHINE_GUN, 8.0);
        }
        helper.assertTrue(FirearmHits.enraged(sprayed), "seis tiros seguidos deveriam enfurecer");
        float enraged = shot(helper, player, sprayed, Firearm.HEAVY_MACHINE_GUN, 8.0);
        helper.assertTrue(Math.abs(enraged - calm * 0.25F) < EPSILON, "na fúria passa 25%: " + enraged + " de " + calm);
        for (int i = 0; i < 4; i++) {
            shot(helper, player, sniped, Firearm.ANTI_TANK_RIFLE, 8.0);
        }
        helper.assertTrue(sniped.isAlive() && !FirearmHits.enraged(sniped), "o antitanque não deveria enfurecer");
        sprayed.discard();
        sniped.discard();
        helper.succeed();
    }

    /** O clique de verdade: a pistola acerta na hora, gasta um tiro do pente e entra na espera. */
    @GameTest(template = ARENA, batch = "firearms", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void firingSpendsARoundAndHitsInstantly(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 12, 1, 12);
        Player player = shooter(helper, new Vec3(4.5, 1, 12.5));
        ItemStack stack = armed(player, Firearm.HANDGUN, pig);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(pig.getHealth() <= pig.getMaxHealth() - 8.0F + EPSILON, "o tiro não acertou: " + pig.getHealth());
        helper.assertTrue(FirearmItem.rounds(stack) == Firearm.HANDGUN.magazine() - 1, "não gastou um tiro do pente");
        helper.assertTrue(player.getCooldowns().isOnCooldown(stack.getItem()), "sem espera entre os tiros");
        pig.discard();
        helper.succeed();
    }

    /** O último tiro do pente já começa a recarga, sem precisar de outro clique. */
    @GameTest(template = ARENA, batch = "firearms", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theLastRoundStartsTheReloadByItself(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 12, 1, 12);
        Player player = shooter(helper, new Vec3(4.5, 1, 12.5));
        ItemStack stack = armed(player, Firearm.ANTI_TANK_RIFLE, pig);
        FirearmItem.setRounds(stack, 1);
        player.getInventory().add(new ItemStack(Firearms.ammo(AmmoFamily.HEAVY_ROUNDS), 3));
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(FirearmItem.rounds(stack) == 3, "não recarregou depois do último tiro: " + FirearmItem.rounds(stack));
        helper.assertTrue(player.getInventory().countItem(Firearms.ammo(AmmoFamily.HEAVY_ROUNDS)) == 0,
                "a recarga não tirou os projéteis do inventário");
        helper.assertTrue(player.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F) > 0.9F, "sem a espera da recarga");
        pig.discard();
        helper.succeed();
    }

    /** O antitanque atravessa a linha; a pistola para no primeiro. */
    @GameTest(template = ARENA, batch = "firearms", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theAntiTankRiflePiercesTheLine(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        Pig front = helper.spawnWithNoFreeWill(EntityType.PIG, 10, 1, 12);
        Pig back = helper.spawnWithNoFreeWill(EntityType.PIG, 16, 1, 12);
        Player player = shooter(helper, eyeLevelWithAPig(3.5, 12.5));
        ItemStack handgun = armed(player, Firearm.HANDGUN, back);
        handgun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(front.getHealth() < front.getMaxHealth(), "a pistola não pegou o da frente");
        helper.assertTrue(back.getHealth() == back.getMaxHealth(), "a pistola atravessou");
        front.setHealth(front.getMaxHealth());
        ItemStack rifle = armed(player, Firearm.ANTI_TANK_RIFLE, back);
        rifle.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(front.isDeadOrDying() || front.getHealth() < front.getMaxHealth(), "o antitanque errou o da frente");
        helper.assertTrue(back.isDeadOrDying() || back.getHealth() < back.getMaxHealth(), "o antitanque não atravessou");
        front.discard();
        back.discard();
        helper.succeed();
    }

    /** O tiro instantâneo acerta os bichos grandes, que têm as partes do More Hitboxes. */
    @GameTest(template = ARENA, batch = "firearms", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void hitscanShotsHitBigMultipartCreatures(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature brontosaurus = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 15, 1, 12);
        Player player = shooter(helper, new Vec3(3.5, 1, 12.5));
        ItemStack stack = armed(player, Firearm.ANTI_TANK_RIFLE, brontosaurus);
        float before = brontosaurus.getHealth();
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        float expected = 1600F * 600 / 10000;
        helper.assertTrue(Math.abs(before - brontosaurus.getHealth() - expected) < 0.05F,
                "o antitanque deveria tirar " + expected + " do Brontossauro: " + (before - brontosaurus.getHealth()));
        brontosaurus.discard();
        helper.succeed();
    }

    /** O tiro passa pela criatura domesticada de quem atira e pega o bicho de trás. */
    @GameTest(template = ARENA, batch = "firearms", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void shotsPassThroughTheShootersOwnCreatures(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        Player player = shooter(helper, eyeLevelWithAPig(3.5, 12.5));
        LandCreature pet = helper.spawnWithNoFreeWill(ModEntities.VELOCIRAPTOR.get(), 8, 1, 12);
        pet.tame(player);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 14, 1, 12);
        ItemStack stack = armed(player, Firearm.HANDGUN, pig);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(pet.getHealth() == pet.getMaxHealth(), "o tiro feriu a criatura do próprio atirador");
        helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "o tiro não passou para o porco");
        pet.discard();
        pig.discard();
        helper.succeed();
    }

    /** A recarga tira a munição da família da arma do inventário; sem ela, o pente fica vazio. */
    @GameTest(template = EMPTY, batch = "firearms")
    public static void reloadingTakesTheRightAmmoFromTheInventory(GameTestHelper helper) {
        Player player = shooter(helper, new Vec3(0.5, 2, 0.5));
        ItemStack gun = new ItemStack(Firearms.gun(Firearm.HANDGUN));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().add(new ItemStack(Firearms.ammo(AmmoFamily.SHELLS), 20));
        FirearmShots.reload(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(FirearmItem.rounds(gun) == 0, "carregou a pistola com cartuchos de escopeta");
        player.getCooldowns().removeCooldown(gun.getItem());
        player.getInventory().add(new ItemStack(Firearms.ammo(AmmoFamily.LIGHT_ROUNDS), 20));
        FirearmShots.reload(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(FirearmItem.rounds(gun) == Firearm.HANDGUN.magazine(), "o pente não encheu: " + FirearmItem.rounds(gun));
        helper.assertTrue(player.getInventory().countItem(Firearms.ammo(AmmoFamily.LIGHT_ROUNDS)) == 5,
                "deveriam sobrar 5 balas");
        helper.assertTrue(player.getCooldowns().isOnCooldown(gun.getItem()), "recarregou sem espera");
        helper.succeed();
    }

    /**
     * A esfera do canhão voa e estoura no bicho, com o dano do DC2. Lote próprio: a esfera leva alguns ticks, e o
     * {@code clearStrays} dos testes vizinhos tiraria o alvo da arena no meio do voo.
     */
    @GameTest(template = ARENA, batch = "firearms_cannon", setupTicks = HuntTests.CHUNK_SETUP_TICKS, timeoutTicks = 60)
    public static void theSolidCannonSphereFliesAndBursts(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature brontosaurus = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 15, 1, 12);
        Player player = shooter(helper, new Vec3(3.5, 1, 12.5));
        ItemStack stack = armed(player, Firearm.SOLID_CANNON, brontosaurus);
        float before = brontosaurus.getHealth();
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        Vec3 corner = helper.absoluteVec(Vec3.ZERO);
        helper.assertFalse(helper.getLevel().getEntitiesOfClass(SolidCannonShot.class,
                new AABB(corner, corner.add(24, 12, 24))).isEmpty(), "a esfera não saiu");
        float expected = 1500F * 600 / 10000;
        helper.succeedWhen(() -> helper.assertTrue(Math.abs(before - brontosaurus.getHealth() - expected) < 0.05F,
                "a esfera deveria tirar " + expected + ": " + (before - brontosaurus.getHealth())));
    }

    /** O Titanovenator é o Giganotossauro do DC2: o couro blindado segura até o antitanque. */
    @GameTest(template = EMPTY, batch = "firearms")
    public static void noFirearmHurtsTheTitanovenator(GameTestHelper helper) {
        TitanovenatorBoss boss = (TitanovenatorBoss) helper.spawnWithNoFreeWill(ModEntities.TITANOVENATOR.get(), 3, 2, 3);
        Player player = shooter(helper, new Vec3(0.5, 2, 0.5));
        for (Firearm gun : Firearm.values()) {
            shot(helper, player, boss, gun, 6.0);
        }
        helper.assertTrue(boss.getHealth() == boss.getMaxHealth(), "uma arma de fogo feriu o boss: " + boss.getHealth());
        boss.discard();
        helper.succeed();
    }

    /** Toda espécie do mod tem o bloco firearm, menos o boss (de propósito) e a criatura de teste. */
    @GameTest(template = EMPTY, batch = "firearms")
    public static void everySpeciesKnowsItsDinoCrisisToughness(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(
                dev.madebyfelipe.iceagesurvival.species.Species.REGISTRY_KEY);
        for (var entry : registry.entrySet()) {
            String id = entry.getKey().location().getPath();
            boolean exempt = id.equals("titanovenator") || id.equals("test_creature");
            helper.assertTrue(exempt || entry.getValue().firearm().isPresent(), id + " sem o bloco firearm");
        }
        helper.assertTrue(PrehistoricCreature.class.isAssignableFrom(TitanovenatorBoss.class), "o boss mudou de base");
        helper.succeed();
    }
}
