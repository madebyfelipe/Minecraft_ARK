package dev.madebyfelipe.iceagesurvival.core.command;

import java.util.Optional;

/**
 * Assobio do dono, como no ARK: cada um muda só o movimento ou só a postura das criaturas
 * que o ouvem. Atacar um alvo é uma ação à parte.
 */
public enum Whistle {
    FOLLOW(Movement.FOLLOW, null, 1.6F),
    STAY(Movement.STAY, null, 1.0F),
    PASSIVE(null, Stance.PASSIVE, 0.8F),
    FLEE(null, Stance.FLEE, 0.7F),
    NEUTRAL(null, Stance.NEUTRAL, 1.2F),
    DEFEND(null, Stance.DEFEND, 1.4F);

    private final Movement movement;
    private final Stance stance;
    private final float pitch;

    Whistle(Movement movement, Stance stance, float pitch) {
        this.movement = movement;
        this.stance = stance;
        this.pitch = pitch;
    }

    public Optional<Movement> movement() {
        return Optional.ofNullable(movement);
    }

    public Optional<Stance> stance() {
        return Optional.ofNullable(stance);
    }

    /** Tom do assobio, para dar para distinguir de ouvido. */
    public float pitch() {
        return pitch;
    }

    /** Chave de tradução do que o assobio manda, sem prefixo: {@code follow}, {@code passive}... */
    public String id() {
        return movement != null ? movement.id() : stance.id();
    }
}
