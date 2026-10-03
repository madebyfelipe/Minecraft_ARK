package dev.madebyfelipe.iceagesurvival.core.ecology;

/**
 * Engolir inteira ({@code hunt_special: swallow}), o golpe do Quetzalcoatlus. Os azdarquídeos eram caçadores a pé,
 * como a cegonha-marabu e o calau-de-chão: andavam de quatro pelo campo aberto e apanhavam com o bico longo e sem
 * dentes a presa pequena, que engoliam de uma vez (Witton &amp; Naish 2008, 2015). A presa maior que isso não cabe no
 * bico: leva só a bicada.
 *
 * <p>Filhote nunca é engolido: a caça a filhotes ficou de fora por decisão do Felipe (D34).
 * Sem classes do Minecraft (D10).
 */
public final class Swallow {
    /**
     * Engole inteira a presa até este porte relativo ({@link ThreatResponse#sizeRatio}: a presa ÷ o caçador). Para o
     * Quetzalcoatlus (2,2 × 4,5) cabem o dodô (0,07), o Velociraptor e o Ornitholestes (0,08), o coelho, a galinha, o
     * sapo e o peixe; o porco (0,10) e o Galimimo (0,29) já não.
     */
    public static final double MAX_SIZE_RATIO = 0.1;

    private Swallow() {
    }

    /**
     * Se a presa cabe inteira no bico.
     *
     * @param sizeRatio porte da presa ÷ o do caçador
     * @param baby      a presa é filhote
     */
    public static boolean swallows(double sizeRatio, boolean baby) {
        return !baby && sizeRatio <= MAX_SIZE_RATIO;
    }
}
