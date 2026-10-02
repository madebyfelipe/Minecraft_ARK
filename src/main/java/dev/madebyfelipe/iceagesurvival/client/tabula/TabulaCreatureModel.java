package dev.madebyfelipe.iceagesurvival.client.tabula;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;

/**
 * O {@link EntityModel} das criaturas com modelo Tabula. Um por renderer; o modelo montado vem do
 * {@link TabulaModels} a cada desenho ({@link #bind}), porque os renderers nascem antes de os recursos recarregarem.
 *
 * <p>O estado de animação de cada entidade fica num {@link WeakHashMap}: some sozinho quando o cliente solta a
 * entidade. Tudo isto roda só na thread de render.
 */
public final class TabulaCreatureModel extends EntityModel<LandCreature> {
    /** Quanto do olhar o pescoço e a cabeça seguem, e até onde. */
    private static final float LOOK_SHARE = 0.85F;
    private static final float MAX_LOOK_YAW = 60.0F;
    private static final float MAX_LOOK_PITCH = 35.0F;
    private static final float BREATH_ANGLE = 0.012F;
    private static final float SLEEP_BREATH_ANGLE = 0.025F;
    private static final float BREATH_PERIOD = 60.0F;
    private static final float SLEEP_BREATH_PERIOD = 90.0F;

    private final Map<LandCreature, CreaturePoses.State> states = new WeakHashMap<>();
    private BakedTabulaModel baked;
    private float now;
    private float renderScale = 1.0F;
    private boolean eyelidsClosed;

    /** Prende o modelo montado e o relógio deste desenho. */
    void bind(BakedTabulaModel baked, float now, float renderScale) {
        this.baked = baked;
        this.now = now;
        this.renderScale = renderScale;
    }

    boolean eyelidsClosed() {
        return eyelidsClosed;
    }

    @Override
    public void setupAnim(LandCreature creature, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        if (baked == null) {
            return;
        }
        CreaturePoses.State state = states.computeIfAbsent(creature, entity -> new CreaturePoses.State(entity.getId()));
        CreaturePoses.Choice choice = CreaturePoses.choose(creature, state, baked, now, limbSwingAmount, renderScale);
        PoseClip clip = resolve(choice.clip());
        boolean sameClip = clip == baked.clip(choice.clip());
        baked.apply(state.player.update(now, clip, sameClip ? choice.loop() : true, choice.rate(),
                choice.keepPhase()));

        boolean asleep = creature.isUnconscious() || creature.isResting();
        if (creature.isAlive() && !creature.isCorpse() && creature.deathTime <= 0) {
            float period = asleep ? SLEEP_BREATH_PERIOD : BREATH_PERIOD;
            baked.addBreathing(asleep ? SLEEP_BREATH_ANGLE : BREATH_ANGLE, now * Mth.TWO_PI / period);
        }
        if (CreaturePoses.looksAround(creature) && !creature.isCorpse()) {
            float yaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -MAX_LOOK_YAW, MAX_LOOK_YAW);
            float pitch = Mth.clamp(headPitch, -MAX_LOOK_PITCH, MAX_LOOK_PITCH);
            baked.addLook(yaw * LOOK_SHARE * Mth.DEG_TO_RAD, pitch * LOOK_SHARE * Mth.DEG_TO_RAD);
        }
        eyelidsClosed = CreaturePoses.eyelidsClosed(creature);
    }

    /** A animação pedida, ou IDLE se a espécie não a tem, ou qualquer uma. */
    private PoseClip resolve(String name) {
        PoseClip clip = baked.clip(name);
        if (clip == null) {
            clip = baked.clip(CreaturePoses.IDLE);
        }
        return clip != null ? clip : baked.anyClip();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        if (baked != null) {
            baked.root().render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
