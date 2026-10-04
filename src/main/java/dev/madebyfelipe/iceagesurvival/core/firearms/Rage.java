package dev.madebyfelipe.iceagesurvival.core.firearms;

import java.util.ArrayDeque;

/**
 * A fúria do Dino Crisis 2 (D58): baleado demais em pouco tempo, o bicho fica vermelho e, por um tempo, segura 75% de
 * qualquer tiro ({@link Ballistics#RAGE_FACTOR}) e não cambaleia. Castiga quem descarrega a arma leve num bicho grande;
 * o rifle antitanque não provoca ({@link Firearm#buildsRage()}). Sem classes do Minecraft.
 *
 * <p>Números propostos: {@link #THRESHOLD} acertos em {@link #WINDOW_TICKS} ticks enfurecem por {@link #DURATION_TICKS}.
 * A submetralhadora (10 tiros/s) enfurece em 0,6 s quem não morreu antes — o raptor marrom morre com 4; o canhão e o
 * antitanque, de 1 e 0,5 tiro/s, nunca chegam lá.
 */
public final class Rage {
    /** Acertos dentro da janela que enfurecem. */
    public static final int THRESHOLD = 6;
    /** Janela dos acertos, em ticks (3 s). */
    public static final int WINDOW_TICKS = 60;
    /** Quanto dura a fúria, em ticks (5 s). */
    public static final int DURATION_TICKS = 100;

    private final ArrayDeque<Long> hits = new ArrayDeque<>();
    private long enragedUntil = Long.MIN_VALUE;

    /** Se o bicho está enfurecido neste tick. */
    public boolean enraged(long now) {
        return now < enragedUntil;
    }

    /** Até que tick dura a fúria atual (passado, se não há). */
    public long enragedUntil() {
        return enragedUntil;
    }

    /**
     * Registra um acerto. Na fúria os acertos não contam (ela não se estende); acaba a fúria, a conta recomeça do zero.
     *
     * @return se este acerto enfureceu o bicho
     */
    public boolean hit(long now, boolean buildsRage) {
        if (!buildsRage || enraged(now)) {
            return false;
        }
        while (!hits.isEmpty() && hits.peekFirst() <= now - WINDOW_TICKS) {
            hits.pollFirst();
        }
        hits.addLast(now);
        if (hits.size() >= THRESHOLD) {
            hits.clear();
            enragedUntil = now + DURATION_TICKS;
            return true;
        }
        return false;
    }

    /** Sem acertos recentes e sem fúria: pode ser esquecido. */
    public boolean idle(long now) {
        return !enraged(now) && (hits.isEmpty() || hits.peekLast() <= now - WINDOW_TICKS);
    }
}
