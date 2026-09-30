package dev.madebyfelipe.iceagesurvival.core.command;

/** Se a criatura domesticada acompanha o dono ou fica onde está. Independe da {@link Stance}, como no ARK. */
public enum Movement {
    FOLLOW("follow"),
    STAY("stay");

    private final String id;

    Movement(String id) {
        this.id = id;
    }

    /** Nome usado em NBT e nas chaves de tradução. */
    public String id() {
        return id;
    }

    public static Movement byId(String id, Movement fallback) {
        for (Movement movement : values()) {
            if (movement.id.equals(id)) {
                return movement;
            }
        }
        return fallback;
    }
}
