package dev.madebyfelipe.iceagesurvival.client.tabula;

/**
 * O tocador de poses de uma entidade: o relógio da animação em curso e a transição entre animações. Trocar de
 * animação não salta: o quadro em que estava se funde ao da nova durante {@value #FADE_TICKS} ticks.
 *
 * <p>O relógio anda pelo tempo que o chamador passa (ticks de jogo com a fração do quadro), não por chamada:
 * desenhar a mesma entidade duas vezes no mesmo quadro (a prévia do painel de status) não a acelera.
 */
public final class PosePlayer {
    /** Duração da fusão entre duas animações. */
    public static final float FADE_TICKS = 5.0F;
    /** Maior passo de relógio de uma vez: depois de um tempo fora da tela, a animação não dá um salto enorme. */
    private static final float MAX_STEP = 5.0F;
    /** Recuo de relógio maior que isto é outro relógio (entidade recriada): recomeça a contar. */
    private static final float CLOCK_RESET = 20.0F;

    private PoseClip clip;
    private boolean loop;
    private float time;
    private float lastNow = Float.NaN;
    private float[] output;
    private float[] sampled;
    private float[] fadeFrom;
    private float fadeElapsed;

    /**
     * Avança e devolve o quadro atual (o vetor é do tocador; não guarde).
     *
     * @param now       relógio em ticks (com a fração do quadro)
     * @param next      a animação que deve tocar
     * @param loop      em laço, ou uma vez parando na última pose
     * @param rate      velocidade (1 = a do arquivo)
     * @param keepPhase trocando de um laço para outro, continua da mesma fração da volta (andar ↔ correr)
     */
    public float[] update(float now, PoseClip next, boolean loop, float rate, boolean keepPhase) {
        float step = 0.0F;
        if (!Float.isNaN(lastNow)) {
            if (now < lastNow - CLOCK_RESET) {
                lastNow = now;
            }
            step = Math.min(MAX_STEP, Math.max(0.0F, now - lastNow));
        }
        if (Float.isNaN(lastNow) || now > lastNow) {
            lastNow = now;
        }

        if (next != clip) {
            boolean fresh = output == null || output.length != next.frameSize();
            float start = 0.0F;
            if (keepPhase && clip != null && this.loop && loop) {
                start = clip.phase(time) * next.period();
            } else if (fresh && !loop) {
                // Visto pela primeira vez já no meio de um gesto de uma vez (um corpo caído): fica no fim dele.
                start = next.duration();
            }
            if (fresh) {
                output = new float[next.frameSize()];
                sampled = new float[next.frameSize()];
                fadeFrom = null;
            } else {
                fadeFrom = output.clone();
                fadeElapsed = 0.0F;
            }
            clip = next;
            this.loop = loop;
            time = start;
        } else {
            time += step * rate;
            if (fadeFrom != null) {
                fadeElapsed += step;
            }
        }

        if (fadeFrom == null) {
            clip.sample(time, loop, output);
            return output;
        }
        clip.sample(time, loop, sampled);
        float amount = fadeElapsed / FADE_TICKS;
        if (amount >= 1.0F) {
            fadeFrom = null;
            System.arraycopy(sampled, 0, output, 0, output.length);
        } else {
            // Suaviza as pontas da fusão (smoothstep).
            PoseClip.lerp(fadeFrom, sampled, amount * amount * (3.0F - 2.0F * amount), output);
        }
        return output;
    }

    /** A animação em curso, ou {@code null} antes da primeira atualização. */
    public PoseClip clip() {
        return clip;
    }

    /** Tempo dentro da animação em curso, em ticks já multiplicados pela velocidade. */
    public float time() {
        return time;
    }

    /** Tocada uma vez e já na última pose. */
    public boolean finished() {
        return clip != null && !loop && time >= clip.duration();
    }
}
