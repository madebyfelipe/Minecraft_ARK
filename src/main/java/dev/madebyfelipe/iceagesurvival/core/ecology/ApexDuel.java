package dev.madebyfelipe.iceagesurvival.core.ecology;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O desafio a um apex (T-Rex, Espinossauro). Quem traz a cabeça de outro da espécie — prova de que já caçou um — é
 * reconhecido como caçador hábil e pode desafiá-lo. Vale o desafiante e até {@link #MAX_CREATURES} criaturas dele;
 * uma a mais, ou qualquer outro que interfira, cancela o ritual. Vencido, o apex não morre: cai e pode ser domado.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class ApexDuel {
    /** Criaturas do desafiante que podem lutar junto. */
    public static final int MAX_CREATURES = 3;
    /** O desafiante que se afasta mais que isto abandona o desafio. */
    public static final double MAX_DISTANCE = 48.0;

    public enum Verdict {
        /** Golpe dentro das regras (ou dano do ambiente). */
        OK,
        /** Interferência: o ritual acaba. */
        CANCEL
    }

    private final UUID challenger;
    private final Set<UUID> creatures = new HashSet<>();

    public ApexDuel(UUID challenger) {
        this.challenger = challenger;
    }

    public UUID challenger() {
        return challenger;
    }

    public int creatures() {
        return creatures.size();
    }

    /**
     * Um golpe no apex.
     *
     * @param player        o jogador que bateu, ou null
     * @param creature      a criatura que bateu, ou null
     * @param creatureOwner o dono da criatura, ou null se selvagem
     */
    public Verdict hitBy(UUID player, UUID creature, UUID creatureOwner) {
        if (player != null) {
            return player.equals(challenger) ? Verdict.OK : Verdict.CANCEL;
        }
        if (creature != null) {
            if (!challenger.equals(creatureOwner)) {
                return Verdict.CANCEL;
            }
            creatures.add(creature);
            return creatures.size() > MAX_CREATURES ? Verdict.CANCEL : Verdict.OK;
        }
        return Verdict.OK;
    }
}
