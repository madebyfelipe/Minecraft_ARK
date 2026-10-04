package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Reaction;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Situation;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse.Tuning;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;

class ThreatResponseTest {
    /** O Elasmotério: alerta a 14, investe a 4, blefa metade das vezes. */
    private static final Tuning RHINO = new Tuning(14, 4, 0.55, 0.35, 0.04, 0.5, 14);
    private static final Tuning DODO = new Tuning(8, 0, 0, 0, 0, 0.4, 12);
    private static final DoubleSupplier LUCKY = () -> 0.0;
    private static final DoubleSupplier UNLUCKY = () -> 0.99;

    private static Situation at(double distance) {
        return new Situation(distance, false, false, false, false, 0.2);
    }

    private static Situation hunter(double distance, double sizeRatio, int predators) {
        return new Situation(distance, false, false, false, false, sizeRatio, false, predators, 0.0);
    }

    @Test
    void farAwayIsIgnored() {
        assertEquals(Reaction.IGNORE, ThreatResponse.react(at(20), RHINO, UNLUCKY));
    }

    @Test
    void sneakingHalvesTheSenses() {
        Situation sneaking = new Situation(10, false, true, false, false, 0.2);
        assertEquals(Reaction.IGNORE, ThreatResponse.react(sneaking, RHINO, UNLUCKY));
        assertEquals(Reaction.ALERT, ThreatResponse.react(at(10), RHINO, UNLUCKY));
    }

    @Test
    void standingStillAtADistanceOnlyGetsAStare() {
        assertEquals(Reaction.ALERT, ThreatResponse.react(at(10), RHINO, UNLUCKY));
    }

    @Test
    void tooCloseForTheFirstTimeIsAWarningBluff() {
        // Colado mas já notado: o primeiro é aviso, tenha a sorte que tiver.
        assertEquals(Reaction.BLUFF, ThreatResponse.react(at(3), RHINO, LUCKY));
        assertEquals(Reaction.BLUFF, ThreatResponse.react(at(3), RHINO, UNLUCKY));
    }

    @Test
    void tooCloseAfterAWarningChargesForReal() {
        Situation warned = new Situation(3, false, false, false, false, 0.2, false, 1, 0.0, true);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(warned, RHINO, LUCKY));
        assertEquals(Reaction.CHARGE, ThreatResponse.react(warned, RHINO, UNLUCKY));
    }

    @Test
    void aMotherTooCloseChargesWithoutAWarning() {
        Situation mother = new Situation(3, false, false, false, true, 0.2);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(mother, RHINO, LUCKY));
    }

    @Test
    void huntersTooCloseStillGetTheChargeWithoutAWarning() {
        // O aviso é para o jogador: o predador colado leva a investida de cara.
        assertEquals(Reaction.CHARGE, ThreatResponse.reactToHunter(hunter(3, 0.2, 1), RHINO, 1, LUCKY));
        assertEquals(Reaction.CHARGE, ThreatResponse.reactToHunter(hunter(3, 0.2, 1), RHINO, 1, UNLUCKY));
    }

    @Test
    void surprisedAtCloseRangeChargesWithoutWarning() {
        Situation surprised = new Situation(5.5, false, false, true, false, 0.2);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(surprised, RHINO, UNLUCKY));
        Situation noticedEarlier = new Situation(5.5, false, false, false, false, 0.2);
        assertEquals(Reaction.ALERT, ThreatResponse.react(noticedEarlier, RHINO, UNLUCKY));
    }

    @Test
    void approachingCloseIsBluffOrCharge() {
        Situation pushing = new Situation(7, true, false, false, false, 0.2);
        assertEquals(Reaction.BLUFF, ThreatResponse.react(pushing, RHINO, LUCKY));
        assertEquals(Reaction.CHARGE, ThreatResponse.react(pushing, RHINO, UNLUCKY));
    }

    @Test
    void approachingFromFarSometimesMakesItBackOff() {
        Situation approaching = new Situation(12, true, false, false, false, 0.2);
        assertEquals(Reaction.RETREAT, ThreatResponse.react(approaching, RHINO, () -> 0.2));
        assertEquals(Reaction.ALERT, ThreatResponse.react(approaching, RHINO, UNLUCKY));
    }

    @Test
    void aMotherWithCalfChargesWithoutBluffingAndHearsFurther() {
        Situation withCalf = new Situation(8, false, false, false, true, 0.2);
        assertEquals(Reaction.CHARGE, ThreatResponse.react(withCalf, RHINO, LUCKY));
        Tuning shortSighted = new Tuning(6, 3, 0.5, 0.5, 0, 0.5, 14);
        assertEquals(Reaction.IGNORE, ThreatResponse.react(at(10), shortSighted, UNLUCKY));
        assertEquals(Reaction.ALERT, ThreatResponse.react(new Situation(10, false, false, false, true, 0.2),
                shortSighted, UNLUCKY));
    }

    @Test
    void somethingMuchBiggerIsFledFrom() {
        Situation rex = new Situation(10, false, false, false, true, 3.0);
        assertEquals(Reaction.FLEE, ThreatResponse.react(rex, RHINO, LUCKY));
    }

    @Test
    void speciesWithoutChargeAlwaysFlee() {
        assertEquals(Reaction.FLEE, ThreatResponse.react(at(5), DODO, LUCKY));
        assertEquals(Reaction.IGNORE, ThreatResponse.react(at(9), DODO, LUCKY));
    }

    @Test
    void sometimesItChargesOutOfNowhere() {
        assertEquals(Reaction.CHARGE, ThreatResponse.react(at(12), RHINO, () -> 0.01));
    }

    @Test
    void largerPreyIntimidatesASmallHunterWithoutBeingHuntedFirst() {
        Tuning largeHerbivore = new Tuning(36, 3, 0.7, 0.4, 0.02, 0.5, 36);
        assertEquals(Reaction.BLUFF, ThreatResponse.reactToHunter(hunter(12, 0.2, 1),
                largeHerbivore, 1, () -> 0.5));
        assertEquals(Reaction.CHARGE, ThreatResponse.reactToHunter(hunter(12, 0.2, 1),
                largeHerbivore, 1, () -> 0.9));
    }

    @Test
    void hunterPackCanOutmatchPreyEvenWhenEachPredatorIsSmaller() {
        assertEquals(Reaction.FLEE, ThreatResponse.reactToHunter(hunter(12, 0.4, 4),
                RHINO, 1, LUCKY));
    }

    @Test
    void herdCanFaceALargerHunterTogether() {
        assertEquals(Reaction.CHARGE, ThreatResponse.reactToHunter(hunter(8, 1.2, 1),
                RHINO, 2, UNLUCKY));
    }

    @Test
    void predatorFleesAHerbivoreThatOutmatchesIt() {
        assertEquals(Reaction.FLEE,
                ThreatResponse.reactToIntimidation(hunter(8, 2.5, 1), 1, 1, false, true),
                "bem maior, só encarando: corre, mesmo caçando");
        assertEquals(Reaction.FLEE,
                ThreatResponse.reactToIntimidation(hunter(8, 0.5, 1), 1, 4, false, true),
                "manada de quatro contra um predador");
    }

    @Test
    void comparableHerbivoreOnlyDetersASatedPredatorWithACharge() {
        assertEquals(Reaction.ALERT,
                ThreatResponse.reactToIntimidation(hunter(8, 1.2, 1), 1, 1, false, false),
                "porte parecido, só bufando: o predador encara de volta");
        assertEquals(Reaction.RETREAT,
                ThreatResponse.reactToIntimidation(hunter(8, 1.2, 1), 1, 1, true, false),
                "saciado, a investida veio: sai andando");
        assertFalse(ThreatResponse.deters(1.2, true, true), "com fome ou caçando, segura a posição");
        assertFalse(ThreatResponse.deters(1.62, true, true), "três Alossauros famintos contra dois mamutes");
    }

    @Test
    void aStrongPackIsNotDeterredByASingleHerbivore() {
        // Elasmotério contra quatro Velociraptores: ~5,9 ÷ 4.
        double power = ThreatResponse.confrontationPower(5.9, 1, 4);
        assertFalse(ThreatResponse.deters(power, false, false));
        assertFalse(ThreatResponse.deters(power, true, true));
        assertTrue(ThreatResponse.deters(power, true, false));
        assertFalse(ThreatResponse.deters(ThreatResponse.confrontationPower(5.9, 1, 3), false, true),
                "três ainda seguram o bufo");
        assertTrue(ThreatResponse.deters(ThreatResponse.confrontationPower(5.9, 1, 2), false, true),
                "dois, não");
    }

    // ---- O jogador e os herbívoros do mod: o que os dados prometem ----

    private static final Path SPECIES = Path.of("src/main/resources/data/iceagesurvival/iceagesurvival/species");
    /** As que investem com chifres ou cabeça; o Anquilossauro, que vira a cauda, fica à parte. */
    private static final List<String> CHARGERS = List.of("elasmotherium", "stegosaurus", "brontosaurus",
            "triceratops", "mammoth");
    private static final List<String> HERBIVORES = List.of("elasmotherium", "stegosaurus", "ankylosaurus",
            "brontosaurus", "triceratops", "mammoth");
    /** Passo do jogador entre duas decisões do animal (uma por segundo): andando 4,3 blocos, correndo 5,6. */
    private static final double WALK = 4.3;
    private static final double SPRINT = 5.6;

    private static JsonObject wariness(String species) {
        try (Reader reader = Files.newBufferedReader(SPECIES.resolve(species + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("behavior")
                    .getAsJsonObject("wariness");
        } catch (IOException e) {
            throw new AssertionError("não leu " + species, e);
        }
    }

    private static double number(JsonObject block, String key, double fallback) {
        return block.has(key) ? block.get(key).getAsDouble() : fallback;
    }

    /** Os números gerais da espécie (predadores e o resto), com os padrões do codec. */
    private static Tuning generalTuning(String species) {
        JsonObject w = wariness(species);
        return new Tuning(number(w, "alert_radius", 12), number(w, "charge_radius", 0), number(w, "bluff_chance", 0),
                number(w, "retreat_chance", 0), number(w, "charge_chance", 0), number(w, "sneak_factor", 0.5),
                number(w, "calf_radius", 12));
    }

    /** Os números contra o jogador: o bloco {@code player}, e o geral no que ele não troca. */
    private static Tuning playerTuning(String species) {
        JsonObject w = wariness(species);
        assertTrue(w.has("player"), species + " precisa de números próprios contra o jogador");
        Tuning g = generalTuning(species);
        JsonObject p = w.getAsJsonObject("player");
        return new Tuning(number(p, "alert_radius", g.alertRadius()), number(p, "charge_radius", g.chargeRadius()),
                number(p, "bluff_chance", g.bluffChance()), number(p, "retreat_chance", g.retreatChance()),
                number(p, "charge_chance", g.chargeChance()), g.sneakFactor(), number(p, "calf_radius", g.calfRadius()));
    }

    private static Situation playerAt(double distance, boolean approaching) {
        return new Situation(distance, approaching, false, false, false, 0.2);
    }

    /** Fração dos sorteios (uniformes em [0, 1)) em que a reação é {@code wanted}. */
    private static double share(Situation situation, Tuning tuning, Reaction wanted) {
        int hits = 0;
        int rolls = 1000;
        for (int i = 0; i < rolls; i++) {
            double value = (i + 0.5) / rolls;
            if (ThreatResponse.react(situation, tuning, () -> value) == wanted) {
                hits++;
            }
        }
        return (double) hits / rolls;
    }

    @Test
    void theirWatchfulnessAgainstThePlayerIsWellBelowTheGeneralOne() {
        for (String species : HERBIVORES) {
            Tuning general = generalTuning(species);
            Tuning player = playerTuning(species);
            assertTrue(player.alertRadius() <= general.alertRadius() / 2.0,
                    species + " ainda se incomoda com o jogador a " + player.alertRadius());
            assertTrue(player.alertRadius() <= 14, species + " alerta a " + player.alertRadius());
            assertTrue(player.calfRadius() <= 16, species + " com filhote, a " + player.calfRadius());
            assertTrue(player.chargeRadius() <= general.chargeRadius(), species + " investe de mais longe");
            assertEquals(Reaction.IGNORE, ThreatResponse.react(playerAt(player.alertRadius() + 1, true), player,
                    LUCKY), species + " nota o jogador além do raio");
        }
    }

    @Test
    void aCloseApproachIsMostlyABluffNotACharge() {
        for (String species : CHARGERS) {
            Tuning tuning = playerTuning(species);
            // No meio da zona entre o raio de investida e a confrontação (60% do alerta).
            double distance = (tuning.chargeRadius() + tuning.alertRadius() * ThreatResponse.CONFRONT_FRACTION) / 2;
            double bluffs = share(playerAt(distance, true), tuning, Reaction.BLUFF);
            double charges = share(playerAt(distance, true), tuning, Reaction.CHARGE);
            assertTrue(bluffs >= 0.70 && bluffs <= 0.95, species + ": blefe em " + bluffs + " das aproximações");
            assertEquals(1.0, bluffs + charges, 1e-9, species + " só blefa ou investe aqui");
        }
    }

    @Test
    void nobodyChargesOutOfNowhereAtAPlayerJustStandingThere() {
        for (String species : HERBIVORES) {
            Tuning tuning = playerTuning(species);
            double distance = (tuning.chargeRadius() + tuning.alertRadius()) / 2;
            assertTrue(share(playerAt(distance, false), tuning, Reaction.CHARGE) <= 0.02,
                    species + " investe do nada com frequência demais");
        }
    }

    @Test
    void aPlayerWalkingStraightInGetsABluffBeforeAnyRealCharge() {
        for (String species : CHARGERS) {
            Tuning tuning = playerTuning(species);
            for (double step : new double[] {WALK, SPRINT}) {
                Random random = new Random(7);
                int bluffs = 0;
                int charges = 0;
                for (int walk = 0; walk < 4000; walk++) {
                    Reaction first = firstDisplay(tuning, step, random);
                    bluffs += first == Reaction.BLUFF ? 1 : 0;
                    charges += first == Reaction.CHARGE ? 1 : 0;
                }
                assertTrue(bluffs + charges > 0, species + " nunca reagiu ao jogador que entra");
                double share = (double) bluffs / (bluffs + charges);
                assertTrue(share >= 0.75, species + " blefa só " + share + " das vezes (passo " + step + ")");
            }
        }
    }

    /**
     * A primeira reação visível (blefe, investida ou recuo) de um animal que acaba de notar o jogador na borda do
     * alerta, enquanto ele anda reto para cima dele, um passo por decisão.
     */
    private static Reaction firstDisplay(Tuning tuning, double step, Random random) {
        double distance = tuning.alertRadius() - 0.5;
        double last = distance;
        boolean first = true;
        for (; distance > 0; distance -= step) {
            Situation situation = new Situation(distance, distance < last - 0.25, false, first, false, 0.2);
            Reaction reaction = ThreatResponse.react(situation, tuning, random::nextDouble);
            first = false;
            last = distance;
            if (reaction == Reaction.BLUFF || reaction == Reaction.CHARGE || reaction == Reaction.RETREAT) {
                return reaction;
            }
        }
        return Reaction.ALERT; // chegou em cima sem nenhuma reação: o teste que conta blefes reprova
    }

    @Test
    void aWarnedPlayerWhoStaysTooCloseIsChargedForReal() {
        for (String species : CHARGERS) {
            Tuning tuning = playerTuning(species);
            double colado = tuning.chargeRadius() / 2;
            Situation unwarned = new Situation(colado, false, false, false, false, 0.2);
            Situation warned = new Situation(colado, false, false, false, false, 0.2, false, 1, 0.0, true);
            assertEquals(Reaction.BLUFF, ThreatResponse.react(unwarned, tuning, UNLUCKY), species + " sem aviso");
            assertEquals(Reaction.CHARGE, ThreatResponse.react(warned, tuning, LUCKY), species + " avisado");
        }
    }

    @Test
    void theSurpriseChargeOnlyReachesAFewBlocks() {
        for (String species : CHARGERS) {
            Tuning tuning = playerTuning(species);
            double reach = tuning.chargeRadius() * ThreatResponse.SURPRISE_MULTIPLIER;
            assertTrue(reach <= 5.0, species + " investe de surpresa a " + reach);
            Situation surprised = new Situation(reach, false, false, true, false, 0.2);
            assertEquals(Reaction.CHARGE, ThreatResponse.react(surprised, tuning, UNLUCKY), species);
            Situation farther = new Situation(reach + 0.5, false, false, true, false, 0.2);
            assertTrue(ThreatResponse.react(farther, tuning, UNLUCKY) != Reaction.CHARGE, species);
        }
    }

    @Test
    void aMotherWithACalfStillChargesButFromAShorterWay() {
        for (String species : CHARGERS) {
            Tuning general = generalTuning(species);
            Tuning tuning = playerTuning(species);
            double confront = Math.max(tuning.alertRadius(), tuning.calfRadius()) * ThreatResponse.CONFRONT_FRACTION;
            Situation near = new Situation(confront - 0.1, false, false, false, true, 0.2);
            assertEquals(Reaction.CHARGE, ThreatResponse.react(near, tuning, LUCKY), species + " mãe");
            assertTrue(confront <= 10.0, species + " com filhote investe a " + confront);
            assertTrue(confront < general.calfRadius() * ThreatResponse.CONFRONT_FRACTION, species);
        }
    }

    @Test
    void theAnkylosaurusKeepsNoBluffAndWarnsWithTheTailBeforeTheClubReaches() {
        JsonObject w = wariness("ankylosaurus");
        assertEquals("tail_club", w.get("defense").getAsString());
        Tuning tuning = playerTuning("ankylosaurus");
        assertEquals(0.0, tuning.bluffChance(), "vira a cauda em vez de blefar: a postura é o aviso");
        assertTrue(tuning.chargeRadius() >= TailClub.REACH,
                "a cauda vira a " + tuning.chargeRadius() + ", e a clava alcança " + TailClub.REACH);
        assertTrue(tuning.alertRadius() <= 12, "alerta a " + tuning.alertRadius());
    }

    @Test
    void relativePersonalityIsPreserved() {
        // Brontossauro e Mamute, os mais tolerantes; Tricerátops e Anquilossauro, os mais esquentados.
        double tolerant = Math.min(playerTuning("brontosaurus").bluffChance(), playerTuning("mammoth").bluffChance());
        for (String other : List.of("elasmotherium", "stegosaurus", "triceratops")) {
            assertTrue(tolerant >= playerTuning(other).bluffChance(), other + " blefa mais que os tolerantes");
        }
        for (String hot : List.of("triceratops", "ankylosaurus")) {
            for (String other : List.of("elasmotherium", "stegosaurus", "brontosaurus", "mammoth")) {
                assertTrue(playerTuning(hot).chargeRadius() > playerTuning(other).chargeRadius(),
                        hot + " investe de menos longe que " + other);
                assertTrue(playerTuning(hot).chargeChance() >= playerTuning(other).chargeChance(),
                        hot + " investe do nada menos que " + other);
            }
        }
    }
}
