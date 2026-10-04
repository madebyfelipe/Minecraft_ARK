package dev.madebyfelipe.iceagesurvival.command;

import dev.madebyfelipe.iceagesurvival.core.command.Whistle;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Validação e execução, no servidor, dos comandos que um jogador dá às suas criaturas.
 * Nada aqui confia no cliente: dono, distância e alvo são conferidos de novo.
 */
public final class CreatureCommands {
    /** Distância máxima entre o jogador e a criatura comandada. */
    public static final double COMMAND_RANGE = 32.0;
    /** Distância máxima entre o jogador e o alvo de uma ordem de ataque. */
    public static final double TARGET_RANGE = 48.0;

    private CreatureCommands() {
    }

    /**
     * Assobio, como no ARK: vale para a criatura mirada, se for do jogador, ou senão para todas
     * as dele num raio de {@link #COMMAND_RANGE}. Cada uma sorteia a obediência.
     *
     * @param aimed criatura sob a mira do jogador, ou nulo
     * @return quantas criaturas obedeceram
     */
    public static int whistle(Player player, Whistle whistle, @Nullable PrehistoricCreature aimed) {
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, whistle.pitch());
        List<PrehistoricCreature> hearing = aimed != null && canCommand(player, aimed)
                ? List.of(aimed)
                : player.level().getEntitiesOfClass(PrehistoricCreature.class,
                        player.getBoundingBox().inflate(COMMAND_RANGE), creature -> canCommand(player, creature));
        int obeyed = 0;
        for (PrehistoricCreature creature : hearing) {
            if (creature.rollObedience()) {
                apply(creature, whistle);
                if (whistle.movement().filter(movement -> movement == dev.madebyfelipe.iceagesurvival.core.command.Movement.FOLLOW).isPresent()) {
                    creature.setLeader(player); // segue quem mandou, dono ou alguém do time
                }
                obeyed++;
            }
        }
        Component command = Component.translatable("iceagesurvival.whistle." + whistle.id());
        Component message;
        if (hearing.isEmpty()) {
            message = Component.translatable("iceagesurvival.whistle.none", command);
        } else if (hearing.size() == 1) {
            message = obeyed == 1
                    ? Component.translatable("iceagesurvival.whistle.one", hearing.get(0).getName(), command)
                    : Component.translatable("iceagesurvival.command.ignored", hearing.get(0).getName());
        } else {
            message = Component.translatable("iceagesurvival.whistle.many", command, obeyed, hearing.size());
        }
        player.displayClientMessage(message, true);
        return obeyed;
    }

    /** Aplica o assobio a uma criatura, sem conferir dono nem obediência. */
    public static void apply(PrehistoricCreature creature, Whistle whistle) {
        whistle.movement().ifPresent(creature::setMovement);
        whistle.stance().ifPresent(creature::setStance);
    }

    /**
     * Manda todas as criaturas do jogador ao alcance, cuja postura permita lutar, atacarem o alvo.
     *
     * @return quantas criaturas obedeceram
     */
    public static int orderAttack(Player player, LivingEntity target) {
        if (!isValidTarget(player, target)) {
            return 0;
        }
        List<PrehistoricCreature> creatures = player.level().getEntitiesOfClass(
                PrehistoricCreature.class,
                player.getBoundingBox().inflate(COMMAND_RANGE),
                creature -> canCommand(player, creature) && creature.stance().fightsBack() && creature != target);
        int obeyed = 0;
        for (PrehistoricCreature creature : creatures) {
            if (creature.rollObedience()) {
                creature.setTarget(target);
                obeyed++;
            }
        }
        player.displayClientMessage(creatures.isEmpty()
                ? Component.translatable("iceagesurvival.command.attack.none")
                : Component.translatable("iceagesurvival.command.attack", obeyed, creatures.size(), target.getName()), true);
        return obeyed;
    }

    private static boolean canCommand(Player player, PrehistoricCreature creature) {
        return creature.isAlive()
                && creature.canCommand(player)
                && !creature.isUnconscious()
                && creature.distanceToSqr(player) <= COMMAND_RANGE * COMMAND_RANGE;
    }

    private static boolean isValidTarget(Player player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.distanceToSqr(player) > TARGET_RANGE * TARGET_RANGE) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())
                || target instanceof PrehistoricCreature creature && creature.canCommand(player)) {
            return false;
        }
        return !(target instanceof Player other) || player.canHarmPlayer(other);
    }
}
