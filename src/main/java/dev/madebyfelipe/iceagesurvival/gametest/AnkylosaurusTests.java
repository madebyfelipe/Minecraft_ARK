package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.TailClub;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TailClubStrike;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.DungProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O Anquilossauro: a clava da cauda (golpe atrás e no flanco, nunca pela frente; perna quebrada), a cautela que vira
 * de costas em vez de fugir, o duelo da mesma espécie que não mata, a mordida montada que minera e o esterco.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class AnkylosaurusTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";

    /** Põe a criatura olhando para {@code yaw} (corpo, cabeça e o {@code yRot} que a cauda usa). */
    private static void face(Mob mob, float yaw) {
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);
    }

    /** Anquilossauro sem IA, olhando +z (yaw 0): atrás dele é −z. */
    private static LandCreature still(GameTestHelper helper, int x, int z) {
        LandCreature ankylosaurus = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), x, 0, z);
        face(ankylosaurus, 0.0F);
        return ankylosaurus;
    }

    private static boolean brokenLeg(LivingEntity entity) {
        return entity.hasEffect(ModEffects.BROKEN_LEG.get());
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    private static boolean threatBehind(Mob creature, Entity threat) {
        return TailClub.inRearArc(creature.getYRot(), threat.getX() - creature.getX(), threat.getZ() - creature.getZ());
    }

    // ---- Espécie ----

    /** A espécie carrega: caixa 2,6 × 2,2, solitária, longe do spawn, clava na cautela e esterco. */
    @GameTest(template = EMPTY, batch = "ankylo_species")
    public static void theAnkylosaurusIsASpecies(GameTestHelper helper) {
        EntityType<LandCreature> type = ModEntities.ANKYLOSAURUS.get();
        helper.assertTrue(Math.abs(type.getWidth() - 2.6F) < 1e-3 && Math.abs(type.getHeight() - 2.2F) < 1e-3,
                "caixa " + type.getWidth() + " × " + type.getHeight());
        Species species = Species.of(helper.getLevel().registryAccess(), type).orElse(null);
        helper.assertTrue(species != null, "o Anquilossauro não carregou");
        BehaviorProfile behavior = species.behavior().orElseThrow();
        helper.assertTrue(behavior.herdRadius() == 0, "deveria ser solitário: herd_radius " + behavior.herdRadius());
        helper.assertTrue(behavior.prey().isEmpty(), "o Anquilossauro é herbívoro");
        WarinessProfile wariness = behavior.wariness().orElse(null);
        helper.assertTrue(wariness != null && wariness.tailClub(), "a cautela deveria ter a defesa tail_club");
        helper.assertTrue(Math.abs(wariness.sneakFactor() - 0.5) < 1e-6, "sneak_factor " + wariness.sneakFactor());
        helper.assertTrue(species.spawn().orElseThrow().minDistance() == 300,
                "min_distance " + species.spawn().orElseThrow().minDistance());
        DungProfile dung = species.dung().orElse(null);
        helper.assertTrue(dung != null && dung.intervalSeconds() == 600 && Math.abs(dung.foodPerDung() - 60) < 1e-6,
                "esterco " + dung);
        helper.succeed();
    }

    // ---- Reflexo da cauda ----

    /** O predador parado atrás, ao alcance, leva a clavada sem ter sido notado e sai de perna quebrada. */
    @GameTest(template = ARENA, batch = "ankylo_rear", timeoutTicks = 60)
    public static void aPredatorBehindGetsTheTailClub(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 8);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 8, 0, 5);
        helper.assertTrue(TailClubStrike.inReach(ankylosaurus, smilodon), "o Smilodon deveria estar ao alcance da cauda");
        float start = smilodon.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(smilodon.getHealth() < start || smilodon.isDeadOrDying(), "o Smilodon não levou o golpe");
            helper.assertTrue(smilodon.isDeadOrDying() || brokenLeg(smilodon), "o golpe não quebrou a perna");
        });
    }

    /** No flanco, a 45° da linha de trás, a cauda também alcança. */
    @GameTest(template = ARENA, batch = "ankylo_flank", timeoutTicks = 60)
    public static void aPredatorOnTheRearFlankGetsTheTailClub(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 8);
        // ~45° de −z para +x, a ~3,5 blocos do centro.
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 10, 0, 6);
        helper.assertTrue(TailClubStrike.inReach(ankylosaurus, smilodon), "o Smilodon no flanco deveria estar ao alcance");
        float start = smilodon.getHealth();
        helper.succeedWhen(() -> helper.assertTrue(smilodon.getHealth() < start || smilodon.isDeadOrDying(),
                "o Smilodon no flanco não levou o golpe"));
    }

    /** O mesmo predador à frente, colado e ao alcance, não leva golpe: pela frente o Anquilossauro não tem arma. */
    @GameTest(template = ARENA, batch = "ankylo_front", timeoutTicks = 80)
    public static void aPredatorInFrontIsNotStruck(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 8);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 8, 0, 11);
        helper.assertFalse(TailClubStrike.inReach(ankylosaurus, smilodon), "à frente não deveria estar ao alcance");
        float start = smilodon.getHealth();
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(smilodon.getHealth() == start, "o Smilodon à frente foi golpeado");
            helper.assertFalse(brokenLeg(smilodon), "o Smilodon à frente ficou de perna quebrada");
            helper.succeed();
        });
    }

    /** Fora do alcance, atrás, ninguém leva golpe. */
    @GameTest(template = ARENA, batch = "ankylo_far", timeoutTicks = 80)
    public static void aPredatorBehindButFarIsNotStruck(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 12);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 8, 0, 6);
        helper.assertFalse(TailClubStrike.inReach(ankylosaurus, smilodon), "a 6 blocos não deveria estar ao alcance");
        float start = smilodon.getHealth();
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(smilodon.getHealth() == start, "o Smilodon longe foi golpeado");
            helper.succeed();
        });
    }

    /** Entre um golpe e outro há a recarga de 30 ticks; depois dela, o monstro atrás leva o segundo. */
    @GameTest(template = ARENA, batch = "ankylo_cooldown", timeoutTicks = 140)
    public static void theTailClubHasACooldown(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 10);
        Ravager ravager = helper.spawnWithNoFreeWill(EntityType.RAVAGER, 8, 0, 6);
        float start = ravager.getHealth();
        long[] firstHit = {-1};
        float[] afterFirst = {0};
        Vec3 spot = ravager.position();
        helper.onEachTick(() -> {
            // A clavada arremessa: o monstro volta ao mesmo lugar, para medir só a recarga.
            ravager.setDeltaMovement(Vec3.ZERO);
            ravager.teleportTo(spot.x, spot.y, spot.z);
            long now = helper.getTick();
            if (firstHit[0] < 0) {
                if (ravager.getHealth() < start) {
                    firstHit[0] = now;
                    afterFirst[0] = ravager.getHealth();
                }
                return;
            }
            long since = now - firstHit[0];
            if (since < TailClub.SWING_COOLDOWN_TICKS - 2) {
                helper.assertTrue(ravager.getHealth() == afterFirst[0],
                        "segundo golpe " + since + " ticks depois do primeiro, antes da recarga");
            } else if (ravager.getHealth() < afterFirst[0] || ravager.isDeadOrDying()) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(130, () -> helper.fail("primeiro golpe no tick " + firstHit[0] + ", vida " + start
                + " → " + ravager.getHealth() + ", ao alcance " + TailClubStrike.inReach(ankylosaurus, ravager)
                + ", ravager em " + helper.relativePos(ravager.blockPosition())));
    }

    /** O jogador agachado é percebido mais perto: a 3,5 blocos atrás, agachado não leva; em pé, leva. */
    @GameTest(template = ARENA, batch = "ankylo_sneak", timeoutTicks = 120)
    public static void aSneakingPlayerOnlyGetsHitCloser(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 10);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(8.5, 0, 7.0)));
        player.setShiftKeyDown(true);
        float start = player.getHealth();
        helper.runAtTickTime(40, () -> {
            helper.assertFalse(TailClubStrike.inReach(ankylosaurus, player), "agachado a 3,5 blocos não deveria estar ao alcance");
            helper.assertTrue(player.getHealth() == start, "o jogador agachado foi golpeado");
            player.setShiftKeyDown(false);
        });
        helper.runAtTickTime(42, () -> helper.succeedWhen(() -> helper.assertTrue(
                player.getHealth() < start || player.isDeadOrDying(), "o jogador em pé não foi golpeado")));
    }

    /** Domesticado, só golpeia o próprio alvo: o predador atrás não leva até virar alvo. */
    @GameTest(template = ARENA, batch = "ankylo_tame", timeoutTicks = 120)
    public static void aTameAnkylosaurusOnlyStrikesItsTarget(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = still(helper, 8, 8);
        Player owner = helper.makeMockSurvivalPlayer();
        ankylosaurus.tame(owner);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 8, 0, 5);
        float start = smilodon.getHealth();
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(smilodon.getHealth() == start, "domesticado golpeou quem não era o alvo");
            ankylosaurus.setTarget(smilodon);
            face(ankylosaurus, 0.0F);
        });
        helper.runAtTickTime(41, () -> helper.succeedWhen(() -> helper.assertTrue(
                smilodon.getHealth() < start || smilodon.isDeadOrDying(), "domesticado não golpeou o próprio alvo")));
    }

    // ---- Perna quebrada ----

    /** A clavada quebra a perna do mob vanilla (−60% de velocidade), não a de outro Anquilossauro. */
    @GameTest(template = ARENA, batch = "ankylo_leg", timeoutTicks = 40)
    public static void theBrokenLegSlowsButSparesKin(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 4, 0, 4);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 4, 0, 7);
        LandCreature rival = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 12, 0, 4);

        double speed = golem.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(ankylosaurus.doHurtTarget(golem), "o golpe no golem não acertou");
        helper.assertTrue(brokenLeg(golem), "o golem deveria ficar de perna quebrada");
        double slowed = golem.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(slowed - speed * (1.0 - TailClub.LEG_BREAK_SLOWDOWN)) < 1e-4,
                "velocidade " + speed + " → " + slowed + ", esperado −60%");
        var effect = golem.getEffect(ModEffects.BROKEN_LEG.get());
        helper.assertTrue(effect != null && effect.getDuration() <= TailClub.LEG_BREAK_TICKS
                && effect.getDuration() > TailClub.LEG_BREAK_TICKS - 5, "duração " + effect);

        helper.assertTrue(ankylosaurus.doHurtTarget(rival), "o golpe no rival não acertou");
        helper.assertFalse(brokenLeg(rival), "outro Anquilossauro não deveria quebrar a perna");
        helper.succeed();
    }

    /** Com a perna quebrada, o pulo não sobe. */
    @GameTest(template = ARENA, batch = "ankylo_jump", timeoutTicks = 40)
    public static void aBrokenLegCannotJump(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        helper.runAtTickTime(5, () -> {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.BROKEN_LEG.get(),
                    TailClub.LEG_BREAK_TICKS));
            player.setDeltaMovement(Vec3.ZERO);
            player.jumpFromGround();
            helper.assertTrue(player.getDeltaMovement().y <= 0.0, "pulou com a perna quebrada: " + player.getDeltaMovement());
            helper.succeed();
        });
    }

    // ---- Cautela ----

    private static void bracesAgainst(GameTestHelper helper, EntityType<LandCreature> threatType, int threatZ) {
        HuntTests.clearStrays(helper);
        LandCreature ankylosaurus = helper.spawn(ModEntities.ANKYLOSAURUS.get(), 12, 0, 6);
        face(ankylosaurus, 0.0F);
        LandCreature threat = helper.spawnWithNoFreeWill(threatType, 12, 0, threatZ);
        Vec3 start = ankylosaurus.position();
        helper.assertFalse(threatBehind(ankylosaurus, threat), "a ameaça deveria começar à frente");
        helper.onEachTick(() -> helper.assertTrue(horizontalDistance(ankylosaurus.position(), start) < 2.5,
                "o Anquilossauro saiu do lugar (fugiu?): " + horizontalDistance(ankylosaurus.position(), start)));
        helper.runAtTickTime(HuntTests.CHUNK_SETUP_TICKS, () -> helper.succeedWhen(() -> {
            helper.assertTrue(threatBehind(ankylosaurus, threat), "não virou a cauda para a ameaça: yaw "
                    + ankylosaurus.getYRot() + ", ângulo da traseira "
                    + TailClub.angleFromRear(ankylosaurus.getYRot(), threat.getX() - ankylosaurus.getX(),
                            threat.getZ() - ankylosaurus.getZ()));
        }));
    }

    /** Diante de um Smilodon a 6 blocos, para e gira de costas para ele, sem fugir. */
    @GameTest(template = ARENA, batch = "ankylo_brace", timeoutTicks = 300)
    public static void theWaryAnkylosaurusTurnsItsTailToAPredator(GameTestHelper helper) {
        bracesAgainst(helper, ModEntities.SMILODON.get(), 12);
    }

    /** Nem diante de um T-Rex, que outro herbívoro do porte dele fugiria, ele foge: vira a cauda. */
    @GameTest(template = ARENA, batch = "ankylo_brace_rex", timeoutTicks = 300)
    public static void theWaryAnkylosaurusTurnsItsTailEvenToATyrannosaurus(GameTestHelper helper) {
        bracesAgainst(helper, ModEntities.TYRANNOSAURUS.get(), 15);
    }

    // ---- Duelo ----

    /** A clavada de um selvagem em outro da mesma espécie não leva a vítima abaixo de 40% da vida. */
    @GameTest(template = ARENA, batch = "ankylo_duel", timeoutTicks = 40)
    public static void aDuelNeverTakesTheRivalBelowTheFloor(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature attacker = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 4, 0, 4);
        LandCreature victim = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 10, 0, 4);
        float floor = victim.getMaxHealth() * (float) TailClub.DUEL_FLOOR;
        victim.hurt(attacker.damageSources().mobAttack(attacker), 10_000.0F);
        helper.assertTrue(victim.isAlive() && !victim.isCorpse() && victim.getHealth() >= floor - 0.01F,
                "a vítima do duelo ficou com " + victim.getHealth() + " de " + victim.getMaxHealth());
        helper.assertTrue(victim.getHealth() < victim.getMaxHealth(), "o golpe do duelo deveria ferir");

        // Já no piso, outra clavada não tira mais nada.
        victim.invulnerableTime = 0;
        victim.hurt(attacker.damageSources().mobAttack(attacker), 10_000.0F);
        helper.assertTrue(victim.isAlive() && victim.getHealth() >= floor - 0.01F,
                "a segunda clavada passou do piso: " + victim.getHealth());

        // O piso é só do duelo: um predador mata.
        LandCreature other = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 16, 0, 4);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 16, 0, 10);
        other.hurt(smilodon.damageSources().mobAttack(smilodon), 10_000.0F);
        helper.assertTrue(other.isDeadOrDying() || other.isCorpse(),
                "o piso do duelo não deveria valer para o Smilodon: vida " + other.getHealth());
        helper.succeed();
    }

    // ---- Montaria ----

    /** A mordida montada minera à frente: pedra dá pedregulho, minério de ferro dá ferro bruto; obsidiana, ouro e diamante ficam. */
    @GameTest(template = EMPTY, batch = "ankylo_mount", timeoutTicks = 40)
    public static void theMountedBiteMinesStoneAndIron(GameTestHelper helper) {
        var center = helper.absoluteVec(new Vec3(4, 0, 4));
        helper.getLevel().getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(20.0), mob -> true)
                .forEach(Entity::discard);
        ServerPlayer owner = PredatorTests.survivalPlayer(helper);
        LandCreature ankylosaurus = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 4, 0, 4);
        ankylosaurus.tame(owner);
        ankylosaurus.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        ankylosaurus.setSaddled(true);
        face(ankylosaurus, 0.0F);
        owner.setPos(ankylosaurus.position());
        helper.assertTrue(ankylosaurus.ride(owner), "deveria montar");

        helper.setBlock(4, 1, 6, Blocks.STONE);
        helper.setBlock(3, 0, 7, Blocks.IRON_ORE);
        helper.setBlock(5, 1, 7, Blocks.OBSIDIAN);
        helper.setBlock(4, 0, 7, Blocks.GOLD_ORE);
        helper.setBlock(4, 2, 7, Blocks.DIAMOND_ORE);

        helper.assertTrue(ankylosaurus.attackAsMount(owner, null), "a mordida deveria sair sem alvo");

        helper.assertBlockNotPresent(Blocks.STONE, 4, 1, 6);
        helper.assertBlockNotPresent(Blocks.IRON_ORE, 3, 0, 7);
        helper.assertBlockPresent(Blocks.OBSIDIAN, 5, 1, 7);
        helper.assertBlockPresent(Blocks.GOLD_ORE, 4, 0, 7);
        helper.assertBlockPresent(Blocks.DIAMOND_ORE, 4, 2, 7);
        helper.assertItemEntityPresent(Items.COBBLESTONE, new BlockPos(4, 1, 6), 2.0);
        helper.assertItemEntityPresent(Items.RAW_IRON, new BlockPos(3, 0, 7), 2.0);
        helper.assertItemEntityNotPresent(Items.STONE, new BlockPos(4, 1, 6), 3.0);
        helper.assertItemEntityNotPresent(Items.IRON_ORE, new BlockPos(3, 0, 7), 3.0);
        helper.succeed();
    }

    // ---- Esterco ----

    private static int dungAround(GameTestHelper helper, Entity center) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, center.getBoundingBox().inflate(4.0),
                item -> item.getItem().is(ModItems.DUNG.get())).stream().mapToInt(item -> item.getItem().getCount()).sum();
    }

    /** A cada 60 de alimento digerido, um esterco; 30 sozinho ainda não; o filhote não esterca. */
    @GameTest(template = ARENA, batch = "ankylo_dung", timeoutTicks = 40)
    public static void digestingDropsDung(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature adult = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 5, 0, 5);
        int before = dungAround(helper, adult);
        adult.digest(30.0);
        helper.assertTrue(dungAround(helper, adult) == before, "30 de alimento já soltou esterco");
        adult.digest(30.0);
        helper.assertTrue(dungAround(helper, adult) == before + 1, "60 de alimento deveria soltar um esterco");
        adult.digest(120.0);
        helper.assertTrue(dungAround(helper, adult) == before + 3, "mais 120 deveriam soltar mais dois");

        LandCreature baby = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 16, 0, 16);
        baby.setBaby(true);
        int babyBefore = dungAround(helper, baby);
        baby.digest(120.0);
        helper.assertTrue(dungAround(helper, baby) == babyBefore, "o filhote não deveria estercar");
        helper.succeed();
    }

    /** {@code dropDung} solta um esterco no chão. */
    @GameTest(template = ARENA, batch = "ankylo_dung_drop", timeoutTicks = 40)
    public static void dropDungDropsOne(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature adult = helper.spawnWithNoFreeWill(ModEntities.ANKYLOSAURUS.get(), 5, 0, 5);
        int before = dungAround(helper, adult);
        adult.dropDung();
        helper.assertTrue(dungAround(helper, adult) == before + 1, "dropDung deveria soltar um esterco");
        helper.succeed();
    }

    /** O esterco aduba: usado num broto de trigo, faz crescer e é gasto. */
    @GameTest(template = EMPTY, batch = "ankylo_fertilize", timeoutTicks = 40)
    public static void dungFertilizesCrops(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.FARMLAND);
        helper.setBlock(1, 2, 1, Blocks.WHEAT);
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack dung = new ItemStack(ModItems.DUNG.get(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, dung);
        BlockPos crop = helper.absolutePos(new BlockPos(1, 2, 1));
        var result = dung.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false)));
        helper.assertTrue(result.consumesAction(), "usar o esterco no trigo não fez nada: " + result);
        int age = helper.getLevel().getBlockState(crop).getValue(CropBlock.AGE);
        helper.assertTrue(age > 0, "o trigo não cresceu: idade " + age);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3, "o esterco não foi gasto");
        helper.succeed();
    }
}
