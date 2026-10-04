package dev.madebyfelipe.iceagesurvival.core.firearms;

/**
 * As quatro famílias de munição (escolha do Felipe, D58): cada item de munição é um tiro, e uma família serve a todas
 * as armas que a usam.
 */
public enum AmmoFamily {
    /** Balas leves: pistola, submetralhadora e metralhadora pesada. */
    LIGHT_ROUNDS("light_rounds"),
    /** Cartuchos da escopeta. */
    SHELLS("shotgun_shells"),
    /** Células do canhão sólido. */
    SOLID_CELLS("solid_cell"),
    /** Projéteis pesados do rifle antitanque. */
    HEAVY_ROUNDS("heavy_rounds");

    private final String itemId;

    AmmoFamily(String itemId) {
        this.itemId = itemId;
    }

    /** O nome do item ({@code iceagesurvival:<id>}). */
    public String itemId() {
        return itemId;
    }
}
