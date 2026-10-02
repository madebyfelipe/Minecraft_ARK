package dev.madebyfelipe.iceagesurvival.client.tabula;

import java.util.List;

/**
 * Uma animação por poses já resolvida em quadros ({@link TabulaRig}). O tempo de cada passo é quanto leva para
 * chegar àquela pose a partir da anterior; entre duas poses a interpolação é linear. Em laço, a primeira pose é
 * alcançada a partir da última no tempo dela; tocada uma vez, a animação começa na primeira pose e para na última
 * (a entrada suave vem do {@link PosePlayer}).
 */
public final class PoseClip {
    private static final float MIN_TICKS = 0.01F;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private final float[][] frames;
    private final float[] ticks;
    private final float duration;
    private final float period;

    public PoseClip(List<float[]> frames, float[] ticks) {
        if (frames.isEmpty() || frames.size() != ticks.length) {
            throw new IllegalArgumentException("Uma duração por pose, e ao menos uma pose");
        }
        this.frames = frames.toArray(new float[0][]);
        int size = this.frames[0].length;
        for (float[] frame : this.frames) {
            if (frame.length != size) {
                throw new IllegalArgumentException("Quadros de tamanhos diferentes");
            }
        }
        this.ticks = new float[ticks.length];
        float once = 0.0F;
        for (int i = 0; i < ticks.length; i++) {
            this.ticks[i] = Math.max(MIN_TICKS, ticks[i]);
            if (i > 0) {
                once += this.ticks[i];
            }
        }
        this.duration = once;
        this.period = once + this.ticks[0];
    }

    public int frameCount() {
        return frames.length;
    }

    public int frameSize() {
        return frames[0].length;
    }

    /** Duração tocada uma vez: da primeira à última pose. Zero com uma pose só. */
    public float duration() {
        return duration;
    }

    /** Duração de uma volta em laço (inclui o retorno da última pose à primeira). */
    public float period() {
        return period;
    }

    /** Fração [0, 1) da volta em que o tempo cai, para trocar de laço sem perder o passo. */
    public float phase(float time) {
        float t = time % period;
        return (t < 0 ? t + period : t) / period;
    }

    /** Escreve em {@code out} o quadro no instante {@code time} (em ticks desde o início). */
    public void sample(float time, boolean loop, float[] out) {
        int last = frames.length - 1;
        if (last == 0) {
            System.arraycopy(frames[0], 0, out, 0, out.length);
            return;
        }
        if (!loop) {
            float t = Math.max(0.0F, time);
            for (int k = 1; k <= last; k++) {
                if (t < ticks[k]) {
                    lerp(frames[k - 1], frames[k], t / ticks[k], out);
                    return;
                }
                t -= ticks[k];
            }
            System.arraycopy(frames[last], 0, out, 0, out.length);
            return;
        }
        float t = time % period;
        if (t < 0) {
            t += period;
        }
        for (int k = 1; k <= last; k++) {
            if (t < ticks[k]) {
                lerp(frames[k - 1], frames[k], t / ticks[k], out);
                return;
            }
            t -= ticks[k];
        }
        // O trecho de volta: da última pose para a primeira.
        lerp(frames[last], frames[0], Math.min(1.0F, t / ticks[0]), out);
    }

    /**
     * Interpolação de quadros. Nas rotações vai pelo caminho curto: de 179° a -179° são 2°, não 358°.
     */
    public static void lerp(float[] from, float[] to, float amount, float[] out) {
        for (int i = 0; i < out.length; i++) {
            float delta = to[i] - from[i];
            if (i % TabulaRig.STRIDE >= 3) {
                delta = wrap(delta);
            }
            out[i] = from[i] + delta * amount;
        }
    }

    private static float wrap(float radians) {
        float r = radians % TWO_PI;
        if (r > Math.PI) {
            r -= TWO_PI;
        } else if (r < -Math.PI) {
            r += TWO_PI;
        }
        return r;
    }
}
