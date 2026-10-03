package dev.madebyfelipe.iceagesurvival.entity;

/**
 * Gesto que a criatura está fazendo, sincronizado para o cliente ({@link PrehistoricCreature#currentAction()}).
 * Os modelos GeckoLib recebem os gestos pelo {@code triggerAnim}; os modelos por poses (Jurassic Reborn) escolhem
 * a pose por este valor. Acrescentar só no fim: o número de cada um vai pela rede.
 */
public enum CreatureAction {
    NONE,
    /** Golpe ({@code swingAttack}). */
    ATTACK,
    /** Ameaça ou rugido ({@code threatDisplay}, duelo do apex). */
    ROAR,
    /** Chamada do sentinela. */
    CALL,
    /** Comendo (carniça, peixe apanhado). */
    EAT,
    /** Parado na água rasa, à espreita de peixe. */
    FISH,
    /** O bote da pesca. */
    FISH_STRIKE,
    /** Ferido e frenético: a fase 3 do Giganotosaurus da arena (D47). */
    INJURED;

    private static final CreatureAction[] VALUES = values();

    public static CreatureAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }
}
