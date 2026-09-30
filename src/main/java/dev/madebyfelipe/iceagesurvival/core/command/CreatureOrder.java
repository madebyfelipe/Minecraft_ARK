package dev.madebyfelipe.iceagesurvival.core.command;

/** Ordem permanente de uma criatura domesticada. Atacar um alvo é uma ação à parte, não uma ordem. */
public enum CreatureOrder {
    /** Acompanha o dono e só revida se for atacada. */
    FOLLOW("follow", true, true, false),
    /** Não sai do lugar nem luta. */
    STAY("stay", false, false, false),
    /** Acompanha o dono e ataca quem o ferir ou quem ele atacar. */
    DEFEND("defend", true, true, true),
    /** Acompanha o dono, não luta e corre de quem a ferir. */
    FLEE("flee", true, false, false);

    private final String id;
    private final boolean followsOwner;
    private final boolean fightsBack;
    private final boolean defendsOwner;

    CreatureOrder(String id, boolean followsOwner, boolean fightsBack, boolean defendsOwner) {
        this.id = id;
        this.followsOwner = followsOwner;
        this.fightsBack = fightsBack;
        this.defendsOwner = defendsOwner;
    }

    /** Nome usado em NBT e nas chaves de tradução. */
    public String id() {
        return id;
    }

    public boolean followsOwner() {
        return followsOwner;
    }

    /** Se revida quando atacada; também é o que permite receber ordem de ataque. */
    public boolean fightsBack() {
        return fightsBack;
    }

    public boolean defendsOwner() {
        return defendsOwner;
    }

    /** Próxima ordem ao alternar pelo atalho. */
    public CreatureOrder next() {
        CreatureOrder[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static CreatureOrder byId(String id, CreatureOrder fallback) {
        for (CreatureOrder order : values()) {
            if (order.id.equals(id)) {
                return order;
            }
        }
        return fallback;
    }
}
