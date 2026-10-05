package dev.madebyfelipe.iceagesurvival.core.firearms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.madebyfelipe.iceagesurvival.core.firearms.Ballistics.Target;
import org.junit.jupiter.api.Test;

class BallisticsTest {
    private static final double CLOSE = 3.0;
    private static final double FAR = 15.0;

    /** Tiros até a vida acabar, aplicando o dano convertido como o jogo aplica. */
    private static int shots(Firearm gun, double distance, Target target, double health) {
        int shots = 0;
        while (health > 0) {
            health -= (float) Ballistics.damage(gun, distance, target, false);
            shots++;
        }
        return shots;
    }

    @Test
    void raptorDiesInTheDinoCrisisNumberOfShots() {
        // Raptor marrom/verde do DC2: 800 de vida. Velociraptor do mod: 16 de vida base.
        Target raptor = new Target(16, 800, false);
        assertEquals(2, shots(Firearm.HANDGUN, FAR, raptor, 16));
        assertEquals(4, shots(Firearm.SUBMACHINE_GUN, FAR, raptor, 16));
        assertEquals(2, shots(Firearm.SHOTGUN, CLOSE, raptor, 16));
        assertEquals(2, shots(Firearm.SHOTGUN, FAR, raptor, 16));
        assertEquals(1, shots(Firearm.HEAVY_MACHINE_GUN, FAR, raptor, 16));
        assertEquals(1, shots(Firearm.SOLID_CANNON, FAR, raptor, 16));
        assertEquals(1, shots(Firearm.ANTI_TANK_RIFLE, FAR, raptor, 16));
    }

    @Test
    void redRaptorTakesTwiceAsMany() {
        Target red = new Target(110, 1600, false);
        assertEquals(4, shots(Firearm.HANDGUN, FAR, red, 110));
        assertEquals(8, shots(Firearm.SUBMACHINE_GUN, FAR, red, 110));
        assertEquals(3, shots(Firearm.SHOTGUN, CLOSE, red, 110));
        assertEquals(4, shots(Firearm.SHOTGUN, FAR, red, 110));
        assertEquals(2, shots(Firearm.HEAVY_MACHINE_GUN, FAR, red, 110));
        assertEquals(2, shots(Firearm.SOLID_CANNON, FAR, red, 110));
        assertEquals(1, shots(Firearm.ANTI_TANK_RIFLE, FAR, red, 110));
    }

    @Test
    void allosaurusHideHoldsTheLightWeapons() {
        // DC2: a pistola faz 80 no Alossauro (5000 de vida); o canhão e o antitanque passam inteiros.
        assertEquals(80.0, Ballistics.dc2Damage(Firearm.HANDGUN, FAR, true, false), 1e-9);
        assertEquals(63, Ballistics.hitsToKill(Firearm.HANDGUN, FAR, 5000, true));
        assertEquals(42, Ballistics.hitsToKill(Firearm.SHOTGUN, CLOSE, 5000, true));
        assertEquals(7, Ballistics.hitsToKill(Firearm.HEAVY_MACHINE_GUN, FAR, 5000, true));
        assertEquals(4, Ballistics.hitsToKill(Firearm.SOLID_CANNON, FAR, 5000, true));
        assertEquals(4, Ballistics.hitsToKill(Firearm.ANTI_TANK_RIFLE, FAR, 5000, true));
        Target allosaurus = new Target(160, 5000, true);
        assertEquals(7, shots(Firearm.HEAVY_MACHINE_GUN, FAR, allosaurus, 160));
        assertEquals(4, shots(Firearm.SOLID_CANNON, FAR, allosaurus, 160));
    }

    @Test
    void shotgunHitsHarderUpClose() {
        assertEquals(600, Firearm.SHOTGUN.dc2Damage(2.0), 1e-9);
        assertEquals(400, Firearm.SHOTGUN.dc2Damage(10.0), 1e-9);
        assertEquals(400, Firearm.HANDGUN.dc2Damage(2.0), 1e-9);
    }

    @Test
    void rageHoldsThreeQuartersOfEveryShot() {
        Target raptor = new Target(16, 800, false);
        double calm = Ballistics.damage(Firearm.ANTI_TANK_RIFLE, FAR, raptor, false);
        double enraged = Ballistics.damage(Firearm.ANTI_TANK_RIFLE, FAR, raptor, true);
        assertEquals(calm * 0.25, enraged, 1e-9);
    }

    @Test
    void creaturesWithoutTheBlockUseTheRaptorScale() {
        Target generic = Target.generic();
        assertEquals(8.0, Ballistics.damage(Firearm.HANDGUN, FAR, generic, false), 1e-9);
        assertEquals(32.0, Ballistics.damage(Firearm.ANTI_TANK_RIFLE, FAR, generic, false), 1e-9);
    }

    @Test
    void strongerIndividualsTakeMoreShots() {
        Target raptor = new Target(16, 800, false, 16 * 1.6);
        // Um raptor de nível alto com 60% a mais de vida aguenta mais que os 2 tiros de pistola do nível 1.
        assertTrue(shots(Firearm.HANDGUN, FAR, raptor, 16 * 1.6) > 2);
    }

    @Test
    void levelGrowsShotsByTheSquareRootOfHealth() {
        // Raptor com 4× a vida base: o tiro dobra, e ele aguenta o dobro dos tiros do nível 1 (4), não o quádruplo.
        Target strong = new Target(16, 800, false, 64);
        assertEquals(2.0, strong.levelFactor(), 1e-9);
        assertEquals(4, shots(Firearm.HANDGUN, FAR, strong, 64));
        // Dodô de nível alto (34 de vida, base 8): três tiros de pistola em vez de cinco.
        assertEquals(3, shots(Firearm.HANDGUN, FAR, new Target(8, 400, false, 34), 34));
        // Vida abaixo da base (filhote) não enfraquece o tiro.
        assertEquals(1.0, new Target(16, 800, false, 8).levelFactor(), 1e-9);
    }

    @Test
    void onlyTheLightWeaponsAreHeldByTheHide() {
        for (Firearm gun : Firearm.values()) {
            boolean held = Ballistics.dc2Damage(gun, FAR, true, false) < gun.dc2Damage(FAR);
            assertEquals(gun.light(), held, gun.id());
        }
        assertTrue(Firearm.HANDGUN.light() && Firearm.SHOTGUN.light() && Firearm.SUBMACHINE_GUN.light());
    }
}
