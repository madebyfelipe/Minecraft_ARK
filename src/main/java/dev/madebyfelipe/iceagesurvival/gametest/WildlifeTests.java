package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.world.GroupSpacing;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Ecologia: como a fauna reage a jogadores, predadores e aos próprios filhotes. O Elasmotério é o
 * caso completo (rinoceronte: solitário, cauteloso, investe quando surpreendido ou com filhote).
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class WildlifeTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    /** Um lote por cena: animais de testes vizinhos se enxergariam (os raios de alerta passam de 28 blocos). */
    private static final String BATCH = "wildlife";

    @GameTest(template = EMPTY)
    public static void elasmotheriumIsSolitaryWithRareFamilies(GameTestHelper helper) {
        Species elasmo = Species.of(helper.getLevel().registryAccess(), ModEntities.ELASMOTHERIUM.get()).orElseThrow();
        BehaviorProfile behavior = elasmo.behavior().orElseThrow();
        SpawnProfile spawn = elasmo.spawn().orElseThrow();
        helper.assertTrue(behavior.herdRadius() == 0 && !behavior.groupDefense(), "Elasmotério não anda em manada");
        helper.assertTrue(!behavior.aggressive(), "não caça o jogador: só reage");
        helper.assertTrue(spawn.groupMin() == 1 && spawn.groupMax() == 1, "nasce sozinho");
        var family = spawn.family().orElseThrow();
        helper.assertTrue(family.calfChance() > 0 && family.calfChance() <= 0.3, "mãe com filhote deveria ser rara");
        helper.assertTrue(family.mateChance() > 0 && family.mateChance() < 0.5, "casal com filhote, mais raro ainda");
        helper.assertTrue(behavior.wariness().orElseThrow().chargeRadius() > 0, "investe quando surpreendido");
        helper.succeed();
    }

    /** Chegar colado num Elasmotério sem ele ter visto: investida, com o golpe que joga para o alto. */
    @GameTest(template = ARENA, batch = BATCH + "_1", timeoutTicks = 200)
    public static void surprisedElasmotheriumCharges(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 4, 0, 4);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 0, 8.0)));
        float start = player.getHealth();
        helper.onEachTick(() -> {
            if (player.getHealth() < start) {
                helper.succeed();
            }
        });
    }

    /**
     * O jogador foge de um Tricerátopo em direção ao mamute: o mamute encara a ameaça maior (o Tricerátopo atrás
     * dele), não quem corre (pedido do Felipe, 2026-10-03). O Tricerátopo fica sem vontade própria, marcado como
     * agressivo atrás do jogador, para a cena não depender da investida dele.
     */
    @GameTest(template = ARENA, batch = BATCH + "_flee_to_mammoth", timeoutTicks = 200)
    public static void theMammothFacesWhatChasesThePlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature mammoth = helper.spawn(ModEntities.MAMMOTH.get(), 4, 0, 10);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(10.5, 0, 10.5)));
        LandCreature triceratops = helper.spawnWithNoFreeWill(ModEntities.TRICERATOPS.get(), 20, 0, 10);
        // Adultos: o sorteio de família pode pôr filhote, e filhote não é ameaça para ninguém.
        mammoth.setAge(0);
        triceratops.setAge(0);
        triceratops.setTarget(player);
        triceratops.setAggressive(true);
        helper.succeedWhen(() -> helper.assertTrue(mammoth.confronting() == triceratops,
                "o mamute encara " + mammoth.confronting() + ", não o Tricerátopo atrás do jogador"));
    }

    /** Agachado e a uma distância educada, o jogador passa: o faro conta mais que a vista. */
    @GameTest(template = ARENA, batch = BATCH + "_2", timeoutTicks = 120)
    public static void sneakingPastAtADistanceDoesNotProvoke(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 2, 0, 1);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.setShiftKeyDown(true);
        // Alerta de 28 blocos; agachado, metade: a 21 blocos passa sem ser notado.
        player.moveTo(helper.absoluteVec(new Vec3(2.5, 0, 22.5)));
        float start = player.getHealth();
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(player.getHealth() == start, "o jogador agachado foi atacado");
            helper.assertFalse(elasmo.isAggressive(), "o Elasmotério investiu contra quem passou agachado longe");
            helper.succeed();
        });
    }

    /** Ferir o filhote põe a mãe atrás de quem feriu. */
    @GameTest(template = EMPTY)
    public static void hurtingACalfBringsTheMother(GameTestHelper helper) {
        LandCreature mother = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 2, 2, 2);
        LandCreature calf = helper.spawnWithNoFreeWill(ModEntities.ELASMOTHERIUM.get(), 6, 2, 2);
        calf.setAge(-24000);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(8.5, 2, 2.5)));
        calf.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(mother.getTarget() == player, "a mãe deveria ir atrás de quem feriu o filhote");
        helper.assertTrue(mother.hasCalfNearby(14), "a mãe deveria perceber o filhote por perto");
        helper.succeed();
    }

    /** Diante de algo muito maior, foge — mesmo um animal que investe contra o jogador. */
    @GameTest(template = ARENA, batch = BATCH + "_3", timeoutTicks = 160)
    public static void elasmotheriumFleesFromTyrannosaurus(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature rex = helper.spawnWithNoFreeWill(ModEntities.TYRANNOSAURUS.get(), 8, 0, 4);
        LandCreature elasmo = helper.spawn(ModEntities.ELASMOTHERIUM.get(), 8, 0, 12);
        double start = elasmo.distanceTo(rex);
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(elasmo.distanceTo(rex) > start + 3.0,
                    "o Elasmotério deveria ter fugido do T-Rex: " + start + " → " + elasmo.distanceTo(rex));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH + "_4", timeoutTicks = 160)
    public static void dodoRunsFromAWolf(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        var wolf = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 8, 0, 8);
        LandCreature dodo = helper.spawn(ModEntities.DODO.get(), 8, 0, 12);
        double start = dodo.distanceTo(wolf);
        helper.runAtTickTime(140, () -> {
            helper.assertTrue(dodo.distanceTo(wolf) > start + 3.0,
                    "o dodô deveria ter fugido do lobo: " + start + " → " + dodo.distanceTo(wolf));
            helper.succeed();
        });
    }

    /** Um Brontossauro em investida vira uma ameaça visível; o Velociraptor foge pelo porte. */
    @GameTest(template = ARENA, batch = BATCH + "_5", timeoutTicks = 160)
    public static void velociraptorFleesFromThreateningBrontosaurus(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature bronto = helper.spawnWithNoFreeWill(ModEntities.BRONTOSAURUS.get(), 8, 0, 4);
        bronto.setAggressive(true);
        LandCreature raptor = helper.spawn(ModEntities.VELOCIRAPTOR.get(), 8, 0, 10);
        Vec3 start = raptor.position();
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(raptor.position().distanceTo(start) > 3.0,
                    "o Velociraptor não fugiu do Brontossauro agressivo: " + start + " → " + raptor.position());
            helper.succeed();
        });
    }

    private static void largeHerbivoreDrivesOffSatedVelociraptor(GameTestHelper helper,
                                                                  EntityType<LandCreature> herbivoreType) {
        HuntTests.clearStrays(helper);
        LandCreature herbivore = helper.spawn(herbivoreType, 8, 0, 4);
        LandCreature raptor = helper.spawn(ModEntities.VELOCIRAPTOR.get(), 8, 0, 12);
        raptor.setTicksSinceMeal(0);
        double start = raptor.distanceTo(herbivore);
        helper.onEachTick(() -> {
            // Encarar já basta para quem é bem maior: a investida nem sempre chega a vir.
            if ((herbivore.isAggressive() || raptor.isIntimidatedBy(herbivore))
                    && raptor.distanceTo(herbivore) > start + 3.0) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(140, () -> helper.fail(herbivoreType.getDescriptionId()
                + " não afastou o Velociraptor saciado; distância " + start + " → "
                + raptor.distanceTo(herbivore) + ", agressivo=" + herbivore.isAggressive()));
    }

    /** Mesmo saciado e sem caçar mamute, o pequeno predador é expulso pela manada. */
    @GameTest(template = ARENA, batch = BATCH + "_mammoth", timeoutTicks = 160)
    public static void mammothHerdDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.MAMMOTH.get());
    }

    @GameTest(template = ARENA, batch = BATCH + "_brontosaurus", timeoutTicks = 160)
    public static void brontosaurusDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.BRONTOSAURUS.get());
    }

    /**
     * O Smilodon caça espreitando, e a espreita tinha a mesma prioridade da reação a ameaças: o
     * mamute bufando e investindo não o tirava da presa.
     */
    @GameTest(template = ARENA, batch = BATCH + "_stalker", timeoutTicks = 240)
    public static void mammothDrivesOffStalkingSmilodon(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), 8, 0, 14);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 8, 0, 4);
        smilodon.setTicksSinceMeal(20L * 3600);
        LandCreature[] mammoth = {null};
        double[] start = {0};

        helper.onEachTick(() -> {
            if (mammoth[0] == null && smilodon.getTarget() == pig) {
                mammoth[0] = helper.spawn(ModEntities.MAMMOTH.get(), 8, 0, 9);
                start[0] = smilodon.distanceTo(mammoth[0]);
            } else if (mammoth[0] != null && smilodon.getTarget() != pig && smilodon.isIntimidatedBy(mammoth[0])
                    && smilodon.distanceTo(mammoth[0]) > start[0] + 3.0) {
                helper.succeed();
            }
        });
        helper.runAtTickTime(220, () -> helper.fail("o Mamute não afastou o Smilodon; alvo=" + smilodon.getTarget()
                + ", espreitando=" + smilodon.isStalking()
                + ", intimidado=" + (mammoth[0] != null && smilodon.isIntimidatedBy(mammoth[0]))
                + ", distância=" + (mammoth[0] == null ? "sem encontro" : smilodon.distanceTo(mammoth[0]))));
    }

    @GameTest(template = ARENA, batch = BATCH + "_hunted", timeoutTicks = 220)
    public static void largeHerbivoreDrivesOffPredatorThatIsAlreadyHunting(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature utahraptor = helper.spawn(ModEntities.UTAHRAPTOR.get(), 8, 0, 9);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 14, 0, 9);
        utahraptor.setTicksSinceMeal(20L * 3600);
        LandCreature[] mammoth = {null};
        Vec3[] predatorStart = {null};
        boolean[] confronted = {false};

        helper.onEachTick(() -> {
            if (mammoth[0] == null && utahraptor.getTarget() == pig) {
                predatorStart[0] = utahraptor.position();
                mammoth[0] = helper.spawn(ModEntities.MAMMOTH.get(), 8, 0, 4);
            } else if (mammoth[0] != null) {
                // Bufar basta: o mamute tem mais que o dobro da força do Utahraptor.
                confronted[0] |= mammoth[0].isAggressive() || utahraptor.isIntimidatedBy(mammoth[0]);
                if (confronted[0] && utahraptor.getTarget() != pig
                        && utahraptor.position().distanceTo(predatorStart[0]) > 3.0) {
                    helper.succeed();
                }
            }
        });
        helper.runAtTickTime(200, () -> helper.fail("o Mamute não interrompeu a caça do Utahraptor; alvo="
                + utahraptor.getTarget() + ", distância="
                + (mammoth[0] == null ? "sem encontro" : utahraptor.distanceTo(mammoth[0]))
                + ", agressivo=" + (mammoth[0] != null && mammoth[0].isAggressive())));
    }

    @GameTest(template = ARENA, batch = BATCH + "_stegosaurus", timeoutTicks = 160)
    public static void stegosaurusDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.STEGOSAURUS.get());
    }

    @GameTest(template = ARENA, batch = BATCH + "_elasmotherium", timeoutTicks = 160)
    public static void elasmotheriumDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.ELASMOTHERIUM.get());
    }

    /** Depois de abater a presa, o predador come e para de caçar por um tempo. */
    @GameTest(template = EMPTY)
    public static void predatorIsSatedAfterAKill(GameTestHelper helper) {
        LandCreature wolf = helper.spawnWithNoFreeWill(ModEntities.DIRE_WOLF.get(), 3, 2, 3);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 5, 2, 3);
        wolf.setTicksSinceMeal(20L * 3600);
        helper.assertFalse(wolf.isSated(), "começa com fome");
        wolf.killedEntity(helper.getLevel(), pig);
        helper.assertTrue(wolf.isSated(), "deveria estar saciado depois de abater a presa");
        helper.succeed();
    }

    /** Ferido no chão, o Pteranodonte selvagem decola e ganha altura em vez de fugir a pé. */
    @GameTest(template = ARENA, batch = BATCH + "_ptero", timeoutTicks = 200)
    public static void wildPteranodonTakesOffWhenHurt(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ptero = helper.spawn(ModEntities.PTERANODON.get(), 8, 0, 8);
        double startY = ptero.getY();
        helper.runAfterDelay(5, () -> ptero.hurt(helper.getLevel().damageSources().playerAttack(helper.makeMockPlayer()), 1.0F));
        helper.succeedWhen(() -> {
            helper.assertTrue(ptero.isFlying(), "o Pteranodonte deveria estar voando");
            helper.assertTrue(ptero.getY() > startY + 4.0, "deveria ter subido: " + startY + " → " + ptero.getY());
        });
    }

    /** Domesticado, não sai voando sozinho: o voo dele é o montado. */
    @GameTest(template = ARENA, batch = BATCH + "_ptero_tame", timeoutTicks = 120)
    public static void tamePteranodonStaysGrounded(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature ptero = helper.spawn(ModEntities.PTERANODON.get(), 8, 0, 8);
        ptero.tame(helper.makeMockPlayer());
        helper.runAfterDelay(5, () -> ptero.hurt(helper.getLevel().damageSources().playerAttack(helper.makeMockPlayer()), 1.0F));
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(!ptero.isFlying(), "domesticado não deveria decolar sozinho");
            helper.succeed();
        });
    }

    /**
     * Uma manada de Brontossauro e um T-Rex a cada 300 blocos: a marca do primeiro grupo deixa os
     * membros dele nascerem perto e barra outro grupo na região. Coordenadas longe da área de testes.
     */
    @GameTest(template = EMPTY)
    public static void bigSpeciesKeepOneGroupPerRegion(GameTestHelper helper) {
        var level = helper.getLevel();
        for (EntityType<?> type : new EntityType<?>[] {ModEntities.BRONTOSAURUS.get(), ModEntities.TYRANNOSAURUS.get()}) {
            helper.assertTrue(GroupSpacing.spacing(level, type) == 300, "espaçamento de 300 blocos");
            // O mundo de teste guarda as marcas entre rodadas: sorteio largo para não cair numa região já usada.
            int base = 2_000_000 + (type == ModEntities.TYRANNOSAURUS.get() ? 1_000 : 0) + level.random.nextInt(12_000) * 2000;
            BlockPos first = new BlockPos(base, 80, base);
            helper.assertTrue(GroupSpacing.permitsSpawn(level, type, first), "primeiro grupo da região");
            helper.assertTrue(GroupSpacing.permitsSpawn(level, type, first.offset(4, 0, -3)), "membro do mesmo grupo");
            helper.assertTrue(!GroupSpacing.permitsSpawn(level, type, first.offset(150, 0, 0)), "segundo grupo a 150 blocos");
            helper.assertTrue(!GroupSpacing.permitsSpawn(level, type, first.offset(0, 0, 290)), "segundo grupo a 290 blocos");
            helper.assertTrue(GroupSpacing.permitsSpawn(level, type, first.offset(0, 0, 310)), "outra região, a 310 blocos");
        }
        helper.assertTrue(GroupSpacing.spacing(level, ModEntities.DODO.get()) == 0, "dodôs sem espaçamento");
        helper.succeed();
    }

    /** Galimimo: o corredor arisco — foge de quem chega perto, a pé ou montado é a montaria terrestre mais rápida. */
    @GameTest(template = EMPTY)
    public static void gallimimusIsTheFastestGroundMount(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Species galli = Species.of(registries, ModEntities.GALLIMIMUS.get()).orElseThrow();
        double galliRide = galli.stats().entry(dev.madebyfelipe.iceagesurvival.core.stats.Stat.SPEED).base()
                * galli.mount().orElseThrow().speedMultiplier();
        for (var creature : ModEntities.LAND_CREATURES) {
            Species other = Species.of(registries, creature.get()).orElseThrow();
            if (creature.get() == ModEntities.GALLIMIMUS.get() || other.mount().isEmpty() || other.mount().get().flying()) {
                continue;
            }
            double ride = other.stats().entry(dev.madebyfelipe.iceagesurvival.core.stats.Stat.SPEED).base()
                    * other.mount().get().speedMultiplier();
            helper.assertTrue(galliRide > ride, creature.getId() + " montado corre mais que o Galimimo");
        }
        BehaviorProfile behavior = galli.behavior().orElseThrow();
        helper.assertTrue(!behavior.aggressive() && behavior.herdRadius() > 0, "pacífico e de bando");
        helper.assertTrue(behavior.wariness().orElseThrow().players(), "arisco com gente");
        helper.assertTrue(galli.spawn().orElseThrow().minDistance() == 0, "nasce já perto do spawn");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH + "_galli", timeoutTicks = 200)
    public static void gallimimusRunsFromThePlayer(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        LandCreature galli = helper.spawn(ModEntities.GALLIMIMUS.get(), 8, 0, 10);
        ServerPlayer player = PredatorTests.survivalPlayer(helper);
        player.moveTo(helper.absoluteVec(new Vec3(8.5, 0, 6.5)));
        double start = galli.distanceTo(player);
        helper.runAtTickTime(160, () -> {
            helper.assertTrue(galli.distanceTo(player) > start + 4.0,
                    "o Galimimo deveria ter corrido do jogador: " + start + " → " + galli.distanceTo(player));
            helper.succeed();
        });
    }

    /** Tricerátopo: herbívoro de manada que encara e afasta o predador, como o mamute e o bronto. */
    @GameTest(template = ARENA, batch = BATCH + "_triceratops", timeoutTicks = 160)
    public static void triceratopsDrivesOffSatedVelociraptor(GameTestHelper helper) {
        largeHerbivoreDrivesOffSatedVelociraptor(helper, ModEntities.TRICERATOPS.get());
    }

    @GameTest(template = EMPTY)
    public static void triceratopsIsADefensiveHerdTank(GameTestHelper helper) {
        Species trike = Species.of(helper.getLevel().registryAccess(), ModEntities.TRICERATOPS.get()).orElseThrow();
        BehaviorProfile behavior = trike.behavior().orElseThrow();
        var wariness = behavior.wariness().orElseThrow();
        helper.assertTrue(!behavior.aggressive() && behavior.groupDefense() && behavior.herdRadius() > 0,
                "pacífico, de manada, defende o grupo");
        helper.assertTrue(wariness.chargeRadius() > 0 && wariness.knockback() >= 1.0, "investe com os chifres e arremessa");
        helper.assertTrue(trike.mount().isPresent(), "montável");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE)
                .wrapAsHolder(ModEntities.TRICERATOPS.get())
                .is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(IceAgeSurvival.MODID, "large_prey"))), "presa dos grandes predadores");
        helper.succeed();
    }
}
