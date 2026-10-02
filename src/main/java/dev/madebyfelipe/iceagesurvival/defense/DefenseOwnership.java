package dev.madebyfelipe.iceagesurvival.defense;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;

/**
 * Quem é "de casa" para uma defesa: o dono (quem colocou), os aliados dele (mesmo time do scoreboard) e as criaturas
 * domesticadas de um deles. O resto — selvagens, mobs, outros jogadores e as domesticadas deles — é estranho.
 * Defesa sem dono (posta por comando) não tem ninguém de casa.
 */
public final class DefenseOwnership {
    private DefenseOwnership() {
    }

    /** Se {@code entity} é o dono ou um aliado dele (só jogadores). */
    public static boolean isOwnerOrAlly(Level level, @Nullable UUID owner, String ownerName, Entity entity) {
        if (owner == null || !(entity instanceof Player player)) {
            return false;
        }
        if (owner.equals(player.getUUID())) {
            return true;
        }
        Team ownerTeam = ownerName.isEmpty() ? null : level.getScoreboard().getPlayersTeam(ownerName);
        return ownerTeam != null && ownerTeam.isAlliedTo(player.getTeam());
    }

    /** Se {@code entity} é de casa: dono, aliado, ou domesticada de um deles. */
    public static boolean isFriend(Level level, @Nullable UUID owner, String ownerName, Entity entity) {
        if (owner == null) {
            return false;
        }
        if (entity instanceof Player) {
            return isOwnerOrAlly(level, owner, ownerName, entity);
        }
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            if (owner.equals(pet.getOwnerUUID())) {
                return true;
            }
            LivingEntity petOwner = pet.getOwner();
            return petOwner != null && isOwnerOrAlly(level, owner, ownerName, petOwner);
        }
        return false;
    }
}
