package dev.madebyfelipe.iceagesurvival.core.mount;

/**
 * Decolagem por salto (quad launch), a dos grandes pterossauros: agacha, vaulta com os braços e as pernas a cerca de
 * 2,5 m do chão e só então bate as asas — sai parado, sem corrida (Habib 2008; Padian et al. 2021, SVP Memoir 19).
 * Por isso o Quetzalcoatlus precisa de céu aberto: com galhos ou teto logo acima, não há como abrir as asas e fica no
 * chão.
 *
 * <p>O salto é balístico, com a gravidade e o arrasto do ar do vanilla: a cada tick o corpo sobe a velocidade atual,
 * e a velocidade perde a gravidade e o arrasto. Quando ela zera (o topo do salto), começa o voo.
 *
 * <p>Sem classes do Minecraft (D10).
 */
public final class LeapTakeoff {
    /** Gravidade do vanilla, em blocos/tick². */
    public static final double GRAVITY = 0.08;
    /** Arrasto do ar no eixo vertical, por tick. */
    public static final double DRAG = 0.98;
    /** Ticks agachada antes do salto. */
    public static final int CROUCH_TICKS = 6;
    /** Blocos de céu aberto que a decolagem pede acima da caixa de colisão: sem folhas nem bloco sólido. */
    public static final int SKY_CLEARANCE = 4;

    private LeapTakeoff() {
    }

    /** A velocidade vertical do salto no tick seguinte. */
    public static double nextSpeed(double speed) {
        return (speed - GRAVITY) * DRAG;
    }

    /** A altura (blocos) que um salto com esta velocidade inicial (blocos/tick) alcança. */
    public static double apexHeight(double launchSpeed) {
        double height = 0.0;
        double speed = launchSpeed;
        for (int tick = 0; tick < 200 && speed > 0.0; tick++) {
            height += speed;
            speed = nextSpeed(speed);
        }
        return height;
    }

    /** A velocidade vertical inicial (blocos/tick) de um salto que alcança {@code height} blocos. */
    public static double launchSpeed(double height) {
        if (height <= 0.0) {
            return 0.0;
        }
        double low = 0.0;
        double high = 4.0;
        for (int step = 0; step < 50; step++) {
            double middle = (low + high) / 2.0;
            if (apexHeight(middle) < height) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    /** Ticks do salto, do chão ao topo. */
    public static int leapTicks(double launchSpeed) {
        int ticks = 0;
        double speed = launchSpeed;
        while (speed > 0.0 && ticks < 200) {
            ticks++;
            speed = nextSpeed(speed);
        }
        return ticks;
    }
}
