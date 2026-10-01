package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Situation;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Tuning;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Os confrontos que o jogo deve produzir, com os portes de colisão reais (largura × altura do
 * registro de entidades). Os dois lados são avaliados pela mesma conta de força — porte relativo ×
 * grupo ÷ grupo —, então a tabela é o contrato: mudar um limiar que vire uma linha é decisão de
 * design, não ajuste de teste.
 *
 * <p>Colunas: o herbívoro e quantos dele; o predador, quantos dele e se está com fome (caçando o
 * herbívoro) ou saciado (só de passagem); se o herbívoro foge; se o predador cede só de ser
 * encarado; se cede a uma investida. Sem fuga, o herbívoro encara, blefa ou investe.
 */
class ConfrontationTableTest {
    /** Ameaça já perto, sem chegar ao raio de investida: o caso em que a força decide. */
    private static final double DISTANCE = 8.0;
    private static final Tuning HERBIVORE = new Tuning(32, 4, 0.6, 0.2, 0.0, 1.0, 32);
    private static final DoubleSupplier NO_LUCK = () -> 0.99;

    private static double[] body(String species) {
        return switch (species) {
            case "dodo" -> new double[] {0.7, 0.9};
            case "velociraptor" -> new double[] {0.6, 0.9};
            case "smilodon" -> new double[] {1.3, 2.3};
            case "utahraptor" -> new double[] {1.2, 2.3};
            case "elasmotherium" -> new double[] {1.8, 2.4};
            case "allosaurus" -> new double[] {1.8, 3.4};
            case "direbear" -> new double[] {2.0, 3.0};
            case "mammoth" -> new double[] {3.0, 4.65};
            case "tyrannosaurus" -> new double[] {2.7, 5.4};
            default -> throw new IllegalArgumentException(species);
        };
    }

    private static double size(String own, String other) {
        double[] a = body(own);
        double[] b = body(other);
        return ThreatResponse.sizeRatio(a[0], a[1], b[0], b[1]);
    }

    @ParameterizedTest(name = "{1} {0} × {3} {2} (fome {4})")
    @CsvSource({
            // herbívoro,  n, predador,      n, fome,  herbívoro foge, cede ao bufo, cede à investida
            // Velociraptor do tamanho real (um peru grande): nem um bando de quatro encara um rinoceronte.
            "elasmotherium, 1, velociraptor,  4, true,  false, true,  true",
            "elasmotherium, 1, velociraptor,  4, false, false, true,  true",
            "elasmotherium, 1, velociraptor,  1, false, false, true,  true",
            "elasmotherium, 1, velociraptor,  1, true,  false, true,  true",
            "elasmotherium, 1, smilodon,      1, true,  true,  false, false",
            "elasmotherium, 1, smilodon,      1, false, false, false, true",
            "mammoth,       1, utahraptor,    1, true,  false, true,  true",
            "mammoth,       1, smilodon,      1, true,  false, true,  true",
            "mammoth,       2, allosaurus,    3, true,  true,  false, false",
            "mammoth,       4, tyrannosaurus, 1, true,  false, true,  true",
            "mammoth,       1, tyrannosaurus, 1, true,  true,  false, false",
            "elasmotherium, 1, tyrannosaurus, 1, true,  true,  false, false",
            "dodo,          6, smilodon,      1, true,  true,  false, false",
    })
    void confrontation(String herbivore, int herd, String predator, int pack, boolean committed,
                       boolean herbivoreFlees, boolean staredDown, boolean chargedOff) {
        // Do lado do herbívoro: o caçador e o bando dele contra a manada. Com fome, ele está caçando.
        Situation hunted = new Situation(DISTANCE, true, false, false, false,
                size(herbivore, predator), committed, pack, 0.0);
        Tuning tuning = herbivore.equals("dodo") ? new Tuning(16, 0, 0, 0, 0, 0.4, 16) : HERBIVORE;
        Reaction reaction = ThreatResponse.reactToHunter(hunted, tuning, herd, NO_LUCK);
        assertEquals(herbivoreFlees, reaction == Reaction.FLEE, "reação do herbívoro: " + reaction);

        // Do lado do predador: a mesma conta, invertida.
        double power = ThreatResponse.confrontationPower(size(predator, herbivore), herd, pack);
        assertEquals(staredDown, ThreatResponse.deters(power, false, committed), "bufo, força " + power);
        assertEquals(chargedOff, ThreatResponse.deters(power, true, committed), "investida, força " + power);
    }
}
