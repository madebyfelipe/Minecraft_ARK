package dev.madebyfelipe.iceagesurvival.core.command;

/** Como a criatura domesticada reage a uma briga. Independe do {@link Movement}, como no ARK. */
public enum Stance {
    /** Não luta nem foge. */
    PASSIVE("passive", false, false, false),
    /** Não luta e corre de quem a ferir. */
    FLEE("flee", false, false, true),
    /** Revida quando atacada. */
    NEUTRAL("neutral", true, false, false),
    /** Revida e ataca quem ferir o dono ou quem o dono atacar. */
    DEFEND("defend", true, true, false);

    private final String id;
    private final boolean fightsBack;
    private final boolean defendsOwner;
    private final boolean fleesWhenHurt;

    Stance(String id, boolean fightsBack, boolean defendsOwner, boolean fleesWhenHurt) {
        this.id = id;
        this.fightsBack = fightsBack;
        this.defendsOwner = defendsOwner;
        this.fleesWhenHurt = fleesWhenHurt;
    }

    /** Nome usado em NBT e nas chaves de tradução. */
    public String id() {
        return id;
    }

    /** Se revida quando atacada; também é o que permite receber ordem de ataque. */
    public boolean fightsBack() {
        return fightsBack;
    }

    public boolean defendsOwner() {
        return defendsOwner;
    }

    public boolean fleesWhenHurt() {
        return fleesWhenHurt;
    }

    public static Stance byId(String id, Stance fallback) {
        for (Stance stance : values()) {
            if (stance.id.equals(id)) {
                return stance;
            }
        }
        return fallback;
    }
}
