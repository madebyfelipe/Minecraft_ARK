package dev.madebyfelipe.iceagesurvival.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.world.WildSpawner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Comandos de teste, em {@code /ias}. Existem para não refazer a cadeia
 * torpor → alimentar → domesticar a cada recompilação: {@code /ias tame} e
 * {@code /ias spawn smilodon 100 1 tamed} entregam o estado de meio de jogo direto.
 *
 * <p>Nível de permissão 2 (operador ou mundo com cheats), como o {@code /gamemode}.
 * Nenhuma mecânica vive aqui: cada subcomando só chama o sistema correspondente.
 *
 * <p>Subcomando sem seletor age na criatura que o jogador está mirando (ou na mais próxima);
 * com {@code <criaturas>} age em todas as do seletor.
 */
public final class DebugCommands {
    private static final int PERMISSION = 2;
    /** Alcance da mira quando o comando não recebe um seletor de entidades. */
    private static final double AIM_RANGE = 64.0;
    /** Alcance da busca pela criatura mais próxima, quando a mira não pega nada. */
    private static final double FALLBACK_RANGE = 12.0;
    /** A quantos blocos à frente de quem chamou o {@code spawn} a criatura nasce. */
    private static final double SPAWN_AHEAD = 3.0;

    private static final SimpleCommandExceptionType NO_TARGET =
            new SimpleCommandExceptionType(Component.translatable("iceagesurvival.debug.no_target"));
    private static final SimpleCommandExceptionType UNKNOWN_SPECIES =
            new SimpleCommandExceptionType(Component.translatable("iceagesurvival.debug.unknown_species"));

    private static final String[] SPAWN_STATES = {"wild", "tamed", "knocked"};

    private static final SuggestionProvider<CommandSourceStack> SPECIES_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(speciesNames(), builder);

    private DebugCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ias")
                .requires(source -> source.hasPermission(PERMISSION))
                .then(creatureCommand("tame", (creature, context) ->
                        creature.debugTame(context.getSource().getPlayerOrException())))
                .then(creatureCommand("knockout", (creature, context) ->
                        creature.setTorpor(creature.maxTorpor())))
                .then(creatureCommand("wake", (creature, context) -> creature.setTorpor(0)))
                .then(creatureCommand("saddle", (creature, context) ->
                        creature.setSaddled(!creature.isSaddled())))
                .then(valueCommand("torpor", DoubleArgumentType.doubleArg(0), (creature, context) ->
                        creature.setTorpor(DoubleArgumentType.getDouble(context, "valor"))))
                .then(valueCommand("level", IntegerArgumentType.integer(1, 100_000), (creature, context) ->
                        creature.setCreatureLevel(IntegerArgumentType.getInteger(context, "valor"))))
                .then(valueCommand("affinity", DoubleArgumentType.doubleArg(0, PrehistoricCreature.MAX_AFFINITY),
                        (creature, context) -> creature.setAffinity(
                                (float) DoubleArgumentType.getDouble(context, "valor"))))
                .then(Commands.literal("ride").executes(DebugCommands::ride))
                .then(Commands.literal("info").executes(DebugCommands::info))
                .then(Commands.literal("kit").executes(DebugCommands::kit))
                .then(Commands.literal("spawns").executes(DebugCommands::spawns))
                .then(Commands.literal("repopulate").executes(DebugCommands::repopulate))
                .then(spawnCommand()));
    }

    // ---- Estrutura dos subcomandos ----

    private interface CreatureAction {
        void apply(PrehistoricCreature creature, CommandContext<CommandSourceStack> context)
                throws CommandSyntaxException;
    }

    /** {@code /ias <nome> [criaturas]} */
    private static LiteralArgumentBuilder<CommandSourceStack> creatureCommand(String name, CreatureAction action) {
        return Commands.literal(name)
                .executes(context -> applyTo(context, aimedOrNearest(context), action))
                .then(Commands.argument("criaturas", EntityArgument.entities())
                        .executes(context -> applyTo(context, selected(context), action)));
    }

    /** {@code /ias <nome> <valor> [criaturas]} */
    private static LiteralArgumentBuilder<CommandSourceStack> valueCommand(
            String name, ArgumentType<?> type, CreatureAction action) {
        return Commands.literal(name)
                .then(Commands.argument("valor", type)
                        .executes(context -> applyTo(context, aimedOrNearest(context), action))
                        .then(Commands.argument("criaturas", EntityArgument.entities())
                                .executes(context -> applyTo(context, selected(context), action))));
    }

    private static int applyTo(CommandContext<CommandSourceStack> context, List<PrehistoricCreature> creatures,
                               CreatureAction action) throws CommandSyntaxException {
        for (PrehistoricCreature creature : creatures) {
            action.apply(creature, context);
        }
        int count = creatures.size();
        context.getSource().sendSuccess(() -> Component.translatable("iceagesurvival.debug.applied", count), true);
        return count;
    }

    // ---- Ações ----

    /** Domestica, sela, enche a afinidade e monta: o caminho curto para testar montaria. */
    private static int ride(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PrehistoricCreature creature = aimedOrNearest(context).get(0);
        if (!creature.isTame()) {
            creature.debugTame(player);
        }
        creature.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        creature.setTorpor(0);
        if (!creature.isSaddled() && creature.canBeSaddled()) {
            creature.setSaddled(true);
        }
        boolean riding = creature.ride(player);
        context.getSource().sendSuccess(() -> Component.translatable(
                riding ? "iceagesurvival.debug.riding" : "iceagesurvival.debug.not_riding",
                creature.getName()), true);
        return riding ? 1 : 0;
    }

    private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        PrehistoricCreature creature = aimedOrNearest(context).get(0);
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal(
                creature.getName().getString() + " · " + EntityType.getKey(creature.getType())), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "nível %d · vida %.0f/%.0f · torpor %.0f/%.0f%s",
                creature.creatureLevel(), creature.getHealth(), creature.getMaxHealth(),
                creature.torpor(), creature.maxTorpor(),
                creature.isUnconscious() ? " · inconsciente" : "")), false);
        source.sendSuccess(() -> Component.literal(creature.isTame()
                ? String.format("domesticada · afinidade %.0f/%.0f · %s · %s · sela: %s",
                        creature.affinity(), PrehistoricCreature.MAX_AFFINITY, creature.movement().id(),
                        creature.stance().id(), saddleState(creature))
                : "selvagem"), false);
        source.sendSuccess(() -> Component.literal(creature.statPoints().toString()), false);
        return 1;
    }

    private static String saddleState(PrehistoricCreature creature) {
        if (!creature.canBeSaddled()) {
            return "espécie não montável";
        }
        return creature.isSaddled() ? "selada" : "sem sela";
    }

    private static int kit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        give(player, new ItemStack(Items.BOW));
        give(player, new ItemStack(ModItems.TRANQ_ARROW.get(), 64));
        give(player, new ItemStack(ModItems.NARCOTIC.get(), 32));
        give(player, new ItemStack(Items.SADDLE, 4));
        give(player, new ItemStack(Items.COOKED_BEEF, 64));
        context.getSource().sendSuccess(() -> Component.translatable("iceagesurvival.debug.kit"), true);
        return 1;
    }

    /** Diagnóstico da reposição de fauna no lugar onde o jogador está. */
    private static int spawns(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        CommandSourceStack source = context.getSource();

        ResourceLocation biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getKey(level.getBiome(player.blockPosition()).value());
        source.sendSuccess(() -> Component.literal("bioma: " + biome), false);

        List<WildSpawner.Report> reports = WildSpawner.survey(level, player);
        if (reports.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("iceagesurvival.debug.spawns.none"), false);
        }
        for (WildSpawner.Report report : reports) {
            source.sendSuccess(() -> Component.literal(String.format(
                    "%s · %d/%d no raio · peso %d · grupo %d–%d",
                    EntityType.getKey(report.type()).getPath(), report.nearby(), report.profile().maxNearby(),
                    report.profile().weight(), report.profile().groupMin(), report.profile().groupMax())), false);
        }
        return reports.size();
    }

    private static int repopulate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int spawned = WildSpawner.trySpawnAround(player.serverLevel(), player);
        context.getSource().sendSuccess(
                () -> Component.translatable("iceagesurvival.debug.repopulate", spawned), true);
        return spawned;
    }

    // ---- /ias spawn <especie> [nivel] [quantidade] [estado] ----

    private static LiteralArgumentBuilder<CommandSourceStack> spawnCommand() {
        return Commands.literal("spawn")
                .then(Commands.argument("especie", StringArgumentType.word())
                        .suggests(SPECIES_SUGGESTIONS)
                        .executes(context -> spawn(context, 0, 1, "wild"))
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 100_000))
                                .executes(context -> spawn(context, level(context), 1, "wild"))
                                .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> spawn(context, level(context), count(context), "wild"))
                                        .then(Commands.argument("estado", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(SPAWN_STATES, builder))
                                                .executes(context -> spawn(context, level(context), count(context),
                                                        StringArgumentType.getString(context, "estado")))))));
    }

    private static int level(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "nivel");
    }

    private static int count(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "quantidade");
    }

    private static int spawn(CommandContext<CommandSourceStack> context, int creatureLevel, int count, String state)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        EntityType<?> type = speciesType(StringArgumentType.getString(context, "especie"));
        Vec3 at = player.position().add(player.getLookAngle().multiply(SPAWN_AHEAD, 0.0, SPAWN_AHEAD));
        BlockPos pos = BlockPos.containing(at);

        int spawned = 0;
        for (int index = 0; index < count; index++) {
            if (!(type.spawn(level, pos, MobSpawnType.COMMAND) instanceof PrehistoricCreature creature)) {
                continue;
            }
            if (creatureLevel > 0) {
                creature.setCreatureLevel(creatureLevel);
            }
            switch (state) {
                case "tamed" -> creature.debugTame(player);
                case "knocked" -> creature.setTorpor(creature.maxTorpor());
                default -> {
                }
            }
            spawned++;
        }
        int total = spawned;
        context.getSource().sendSuccess(() -> Component.translatable(
                "iceagesurvival.debug.spawned", total, type.getDescription()), true);
        return total;
    }

    private static List<String> speciesNames() {
        List<String> names = new ArrayList<>();
        for (var holder : ModEntities.LAND_CREATURES) {
            names.add(holder.getId().getPath());
        }
        names.add(ModEntities.TEST_CREATURE.getId().getPath());
        return names;
    }

    private static EntityType<?> speciesType(String name) throws CommandSyntaxException {
        for (var holder : ModEntities.LAND_CREATURES) {
            if (holder.getId().getPath().equals(name)) {
                return holder.get();
            }
        }
        if (ModEntities.TEST_CREATURE.getId().getPath().equals(name)) {
            return ModEntities.TEST_CREATURE.get();
        }
        throw UNKNOWN_SPECIES.create();
    }

    // ---- Alvos ----

    private static List<PrehistoricCreature> selected(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "criaturas");
        List<PrehistoricCreature> creatures = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof PrehistoricCreature creature) {
                creatures.add(creature);
            }
        }
        if (creatures.isEmpty()) {
            throw NO_TARGET.create();
        }
        return creatures;
    }

    /** A criatura sob a mira; se não houver, a mais próxima do jogador. */
    private static List<PrehistoricCreature> aimedOrNearest(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB search = player.getBoundingBox().expandTowards(look.scale(AIM_RANGE)).inflate(2.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(AIM_RANGE)), search,
                entity -> entity instanceof PrehistoricCreature, AIM_RANGE * AIM_RANGE);
        if (hit != null && hit.getEntity() instanceof PrehistoricCreature creature) {
            return List.of(creature);
        }
        return List.of(nearest(player));
    }

    private static PrehistoricCreature nearest(ServerPlayer player) throws CommandSyntaxException {
        PrehistoricCreature closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (PrehistoricCreature creature : player.level().getEntitiesOfClass(PrehistoricCreature.class,
                player.getBoundingBox().inflate(FALLBACK_RANGE))) {
            double distance = creature.distanceToSqr(player);
            if (distance < closestDistance) {
                closest = creature;
                closestDistance = distance;
            }
        }
        if (closest == null) {
            throw NO_TARGET.create();
        }
        return closest;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
