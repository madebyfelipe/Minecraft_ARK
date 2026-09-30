package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.ai.ChaseGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.FleeWhenWeakGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.FollowHerdGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.StalkGoal;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Criatura terrestre genérica. O comportamento sai inteiro do {@link BehaviorProfile} da
 * espécie e as animações seguem a convenção {@code animation.<especie>.<idle|walk|attack|unconscious>},
 * então uma espécie nova precisa de um tipo de entidade registrado, um JSON e assets — não de uma classe.
 */
public class LandCreature extends PrehistoricCreature implements GeoEntity {
    private static final String ATTACK_CONTROLLER = "attack";
    private static final String ATTACK_TRIGGER = "attack";

    private static final double CHASE_SPEED = 1.25;
    private static final double STALK_SPEED = 0.55;
    private static final int TARGET_MEMORY_TICKS = 200;
    private static final double FLEE_SPEED = 1.4;
    private static final double FOLLOW_SPEED = 1.2;
    /**
     * Produto velocidade × modificador que dá um passeio calmo (~1,8 bloco/s). A velocidade
     * real cresce com o quadrado desse produto, então o modificador é calculado por espécie.
     */
    private static final double CALM_SPEED_PRODUCT = 0.2;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public LandCreature(EntityType<? extends LandCreature> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        BehaviorProfile behavior = behavior().orElse(BehaviorProfile.PASSIVE);
        double baseSpeed = species().map(species -> species.stats().entry(Stat.SPEED).base()).orElse(0.25);
        double calm = Math.clamp(CALM_SPEED_PRODUCT / Math.max(baseSpeed, 0.01), 0.4, 1.0);

        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeWhenWeakGoal(this, FLEE_SPEED));
        if (behavior.huntStyle() == BehaviorProfile.HuntStyle.STALK) {
            goalSelector.addGoal(2, new StalkGoal(this, STALK_SPEED));
        }
        goalSelector.addGoal(3, new ChaseGoal(this, CHASE_SPEED));
        addOrderGoals(2, 4, FOLLOW_SPEED);
        if (behavior.herdRadius() > 0) {
            goalSelector.addGoal(5, new FollowHerdGoal(this, calm * 1.5, behavior.herdRadius()));
        }
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, calm * 1.2));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, calm));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        if (behavior.aggressive()) {
            // Lembra do jogador que sumiu atrás das árvores por 10 s, não os 3 s do vanilla.
            targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Player.class, true, null)
                    .setUnseenMemoryTicks(TARGET_MEMORY_TICKS));
        }
        behavior.prey().ifPresent(prey -> targetSelector.addGoal(5, new HuntGoal(this, prey)));
    }

    @Override
    protected void swingAttack() {
        super.swingAttack();
        triggerAnim(ATTACK_CONTROLLER, ATTACK_TRIGGER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String prefix = "animation." + BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath() + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(prefix + "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(prefix + "walk");
        RawAnimation unconscious = RawAnimation.begin().thenLoop(prefix + "unconscious");
        RawAnimation attack = RawAnimation.begin().thenPlay(prefix + "attack");

        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (isUnconscious()) {
                return state.setAndContinue(unconscious);
            }
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(ATTACK_TRIGGER, attack));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
