package dev.madebyfelipe.iceagesurvival.client.tabula;

import dev.madebyfelipe.iceagesurvival.entity.CreatureAction;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import java.util.Random;

/**
 * Qual animação do Jurassic Reborn a criatura toca agora, a partir do estado que chega ao cliente. A ordem é a
 * mesma do controlador {@code movement} do {@link LandCreature} (GeckoLib), com o que só os modelos por poses têm:
 * morte, corpo caído, natação, gestos sincronizados ({@link LandCreature#currentAction()}) e olhadas de lado.
 */
final class CreaturePoses {
    static final String IDLE = "IDLE";
    static final String WALKING = "WALKING";
    static final String RUNNING = "RUNNING";
    static final String SWIMMING = "SWIMMING";
    static final String SLEEPING = "SLEEPING";
    static final String DYING = "DYING";
    static final String ATTACKING = "ATTACKING";
    static final String ROARING = "ROARING";
    static final String CALLING = "CALLING";
    static final String INJURED = "INJURED";
    static final String EATING = "EATING";
    static final String FISH_LOOKING = "FISH_LOOKING";
    static final String LOOKING_LEFT = "LOOKING_LEFT";
    static final String LOOKING_RIGHT = "LOOKING_RIGHT";

    /** Mesma marca do {@code LandCreature}: passada acima disto é corrida. */
    private static final float RUN_LIMB_SWING = 0.75F;
    /** Mesma marca do GeckoLib ({@code AnimationState.isMoving}). */
    private static final float MOVING_LIMB_SWING = 0.15F;
    /** A morte do vanilla dura 20 ticks até a entidade sumir: a queda termina um pouco antes. */
    private static final float DEATH_TICKS = 18.0F;
    private static final float MIN_STRIDE_RATE = 0.5F;
    private static final float MAX_STRIDE_RATE = 3.0F;
    private static final int LOOK_MIN_DELAY = 200;
    private static final int LOOK_EXTRA_DELAY = 400;

    private CreaturePoses() {
    }

    /** Estado de animação de uma entidade no cliente. */
    static final class State {
        final PosePlayer player = new PosePlayer();
        private final Random random;
        private CreatureAction lastAction = CreatureAction.NONE;
        private String gesture;
        private CreatureAction gestureAction;
        private float gestureUntil;
        private float nextLook = Float.NaN;

        State(int seed) {
            random = new Random(seed);
        }
    }

    /** A escolha: animação, se em laço, velocidade e se troca mantendo o passo. */
    record Choice(String clip, boolean loop, float rate, boolean keepPhase) {
        static Choice loop(String clip) {
            return new Choice(clip, true, 1.0F, false);
        }

        static Choice once(String clip) {
            return new Choice(clip, false, 1.0F, false);
        }
    }

    static Choice choose(LandCreature creature, State state, BakedTabulaModel model, float now, float limbSwingAmount,
                         float renderScale) {
        if (creature.isCorpse()) {
            // Corpo da domesticada: cai e fica na última pose da morte.
            return Choice.once(model.has(DYING) ? DYING : SLEEPING);
        }
        if (creature.deathTime > 0 || !creature.isAlive()) {
            PoseClip dying = model.clip(DYING);
            float rate = dying == null || dying.duration() <= 0 ? 1.0F : dying.duration() / DEATH_TICKS;
            return new Choice(DYING, false, rate, false);
        }
        if (creature.isUnconscious() || creature.isResting()) {
            // O SLEEPING do Jurassic Reborn é o ato de deitar (de pé → deitada de lado), não um laço: tocado em
            // laço, voltava da última pose à primeira e o bicho levantava e deitava de novo a cada volta. Toca uma
            // vez e fica na última pose, como a morte.
            return Choice.once(SLEEPING);
        }

        CreatureAction action = creature.currentAction();
        if (action != state.lastAction) {
            state.lastAction = action;
            String gesture = gestureOf(action);
            if (gesture != null && model.has(gesture)) {
                startGesture(state, gesture, action, model, now);
            }
        }

        boolean swimming = creature.isInWater() && !creature.onGround();
        boolean moving = limbSwingAmount > MOVING_LIMB_SWING;
        Choice base;
        if (swimming) {
            base = Choice.loop(SWIMMING);
        } else if (moving) {
            String clip = limbSwingAmount > RUN_LIMB_SWING ? RUNNING : WALKING;
            base = new Choice(clip, true, strideRate(creature, model, clip, renderScale), true);
        } else {
            base = Choice.loop(IDLE);
        }
        boolean idle = base.clip().equals(IDLE);

        // Gestos contínuos: enquanto o servidor os mantiver.
        if (action == CreatureAction.EAT && model.has(EATING)) {
            return Choice.loop(EATING);
        }
        if (action == CreatureAction.FISH && model.has(FISH_LOOKING)) {
            return Choice.loop(FISH_LOOKING);
        }
        // Gestos de uma vez: vão até o fim, a menos que o gesto já tenha acabado no servidor e ela esteja andando.
        if (state.gesture != null) {
            if (now < state.gestureUntil && (action == state.gestureAction || idle)) {
                return Choice.once(state.gesture);
            }
            state.gesture = null;
        }
        if (idle) {
            // De vez em quando, parada, olha para um lado.
            if (Float.isNaN(state.nextLook)) {
                state.nextLook = now + LOOK_MIN_DELAY + state.random.nextInt(LOOK_EXTRA_DELAY);
            } else if (now >= state.nextLook) {
                state.nextLook = now + LOOK_MIN_DELAY + state.random.nextInt(LOOK_EXTRA_DELAY);
                String look = state.random.nextBoolean() ? LOOKING_LEFT : LOOKING_RIGHT;
                if (model.has(look)) {
                    startGesture(state, look, null, model, now);
                    return Choice.once(look);
                }
            }
        }
        return base;
    }

    private static void startGesture(State state, String gesture, CreatureAction action, BakedTabulaModel model,
                                     float now) {
        state.gesture = gesture;
        state.gestureAction = action;
        state.gestureUntil = now + model.clip(gesture).duration() + PosePlayer.FADE_TICKS;
    }

    private static String gestureOf(CreatureAction action) {
        return switch (action) {
            case ATTACK, FISH_STRIKE -> ATTACKING;
            case ROAR -> ROARING;
            case CALL -> CALLING;
            case INJURED -> INJURED;
            default -> null;
        };
    }

    /** Ritmo da passada pela velocidade real: o pé não escorrega no chão nem pedala parado. */
    private static float strideRate(LandCreature creature, BakedTabulaModel model, String clipName,
                                    float renderScale) {
        PoseClip clip = model.clip(clipName);
        float stride = model.appearance().strideBlocks() * renderScale;
        if (clip == null || stride <= 0.0F) {
            return 1.0F;
        }
        double dx = creature.getX() - creature.xo;
        double dz = creature.getZ() - creature.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        float rate = speed * clip.period() / stride;
        return Math.max(MIN_STRIDE_RATE, Math.min(MAX_STRIDE_RATE, rate));
    }

    /** Pálpebras fechadas: dormindo, desmaiada, morta, ou piscando (3 ticks a cada ~5 s, fora de fase entre elas). */
    static boolean eyelidsClosed(LandCreature creature) {
        if (creature.isCorpse() || creature.deathTime > 0 || creature.isUnconscious() || creature.isResting()) {
            return true;
        }
        int period = 90 + Math.floorMod(creature.getId() * 37, 60);
        return Math.floorMod(creature.tickCount + creature.getId() * 13, period) < 3;
    }

    /** Pode mexer a cabeça para olhar: consciente, viva e não caída. */
    static boolean looksAround(LandCreature creature) {
        return creature.isAlive() && creature.deathTime <= 0 && !creature.isUnconscious() && !creature.isResting();
    }
}
