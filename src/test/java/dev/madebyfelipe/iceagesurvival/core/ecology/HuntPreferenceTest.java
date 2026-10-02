package dev.madebyfelipe.iceagesurvival.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.ecology.HuntChoice.Prey;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tabela de dieta e manada que se defende na escolha de presa. */
class HuntPreferenceTest {
    private static final double RADIUS = 64.0;

    /** Porte de uma criatura diante de um Alossauro (1,8 × 3,4). */
    private static double vsAllosaurus(double width, double height) {
        return ThreatResponse.sizeRatio(1.8, 3.4, width, height);
    }

    @Test
    void theFavouriteBeatsTheEasiest() {
        // O dodô está mais perto e é mais fácil, mas o Galimimo é a presa preferida do Alossauro.
        Prey dodo = new Prey(8, vsAllosaurus(0.7, 0.9), false, 1.0, true, 0, 1);
        Prey gallimimus = new Prey(30, vsAllosaurus(1.2, 2.3), false, 1.0, true, 3, 1);
        assertEquals(1, HuntChoice.choose(List.of(dodo, gallimimus), RADIUS, 2, Hunger.Drive.HUNTING));
    }

    @Test
    void theLastResortIsTakenWhenNothingElseIsNear() {
        Prey dodo = new Prey(8, vsAllosaurus(0.7, 0.9), false, 1.0, true, 0, 1);
        assertEquals(0, HuntChoice.choose(List.of(dodo), RADIUS, 2, Hunger.Drive.HUNTING));
    }

    @Test
    void aDefendedHerdIsTooMuchForThePack() {
        double bronto = vsAllosaurus(4.0, 8.0);
        Prey inHerd = new Prey(20, bronto, false, 1.0, false, 2, 3);
        assertTrue(HuntChoice.score(inHerd, RADIUS, 3) < 0, "a manada de brontos não é presa do bando");
        Prey mammothPair = new Prey(20, vsAllosaurus(3.0, 4.65), false, 1.0, false, 1, 2);
        assertTrue(HuntChoice.score(mammothPair, RADIUS, 3) < 0, "nem dois mamutes juntos");
    }

    @Test
    void theStragglerStaysOnTheMenu() {
        // Um mamute desgarrado ainda cabe no bando de três; a manada inteira, não.
        Prey straggler = new Prey(20, vsAllosaurus(3.0, 4.65), false, 1.0, true, 1, 1);
        assertTrue(HuntChoice.score(straggler, RADIUS, 3) > 0);
    }

    @Test
    void aLoneHunterShadowsAHerdItWouldNotAttack() {
        // Sozinho, o Alossauro não ataca o Galimimo no meio da manada, mas o acompanha esperando que se desgarre.
        Prey inHerd = new Prey(30, vsAllosaurus(1.2, 2.3), false, 1.0, false, 3, 3);
        assertEquals(-1, HuntChoice.choose(List.of(inHerd), RADIUS, 1, Hunger.Drive.HUNTING));
        assertEquals(0, HuntChoice.chooseToStalk(List.of(inHerd), RADIUS, 1));
    }

    @Test
    void nothingIsShadowedThatWouldBeTooMuchEvenAlone() {
        // Um Brontossauro, nem desgarrado, é presa do Alossauro sozinho: não vale acompanhar a manada.
        Prey bronto = new Prey(30, vsAllosaurus(4.0, 8.0), false, 1.0, false, 2, 3);
        assertEquals(-1, HuntChoice.chooseToStalk(List.of(bronto), RADIUS, 1));
    }

    @Test
    void aStragglerIsHuntedNotShadowed() {
        Prey straggler = new Prey(30, vsAllosaurus(1.2, 2.3), false, 1.0, true, 3, 1);
        assertEquals(-1, HuntChoice.chooseToStalk(List.of(straggler), RADIUS, 1));
    }

    @Test
    void aPlayerWithAlliesIsDefendedLikeAHerd() {
        double player = vsAllosaurus(0.6, 1.8);
        Prey alone = new Prey(10, player, false, 1.0, true, 1, 1);
        Prey withFriends = new Prey(10, player, false, 1.0, false, 1, 6);
        assertTrue(HuntChoice.score(alone, RADIUS, 1) > 0, "sozinho, é presa");
        assertTrue(HuntChoice.score(withFriends, RADIUS, 1) < 0, "com o bando dele por perto, o Alossauro solitário não arrisca");
    }
}
