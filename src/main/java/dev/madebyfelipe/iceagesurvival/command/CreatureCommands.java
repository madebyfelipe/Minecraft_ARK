package dev.madebyfelipe.iceagesurvival.command;

import dev.madebyfelipe.iceagesurvival.core.command.CreatureOrder;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.List;
import net.minecraft.network.chat.Component;
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

    /** @return se a ordem foi aceita */
    public static boolean setOrder(Player player, PrehistoricCreature creature, CreatureOrder order) {
        if (!canCommand(player, creature)) {
            return false;
        }
        if (!creature.rollObedience()) {
            player.displayClientMessage(Component.translatable("iceagesurvival.command.ignored", creature.getName()), true);
            return false;
        }
        creature.setOrder(order);
        player.displayClientMessage(
                Component.translatable("iceagesurvival.command.order." + order.id(), creature.getName()), true);
        return true;
    }

    /**
     * Manda todas as criaturas do jogador ao alcance, cuja ordem permita lutar, atacarem o alvo.
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
                creature -> canCommand(player, creature) && creature.order().fightsBack() && creature != target);
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
                && creature.isOwner(player)
                && !creature.isUnconscious()
                && creature.distanceToSqr(player) <= COMMAND_RANGE * COMMAND_RANGE;
    }

    private static boolean isValidTarget(Player player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.distanceToSqr(player) > TARGET_RANGE * TARGET_RANGE) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        return !(target instanceof Player other) || player.canHarmPlayer(other);
    }
}
