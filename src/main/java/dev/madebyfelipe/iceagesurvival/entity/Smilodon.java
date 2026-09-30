package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.entity.ai.FleeWhenWeakGoal;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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

/** Predador territorial: ataca quem entra no território, recua quando ferido e volta para casa. */
public class Smilodon extends PrehistoricCreature implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.smilodon.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.smilodon.walk");
    private static final RawAnimation UNCONSCIOUS = RawAnimation.begin().thenLoop("animation.smilodon.unconscious");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.smilodon.bite");
    private static final String ATTACK_CONTROLLER = "attack";
    private static final String BITE_TRIGGER = "bite";

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public Smilodon(EntityType<? extends Smilodon> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeWhenWeakGoal(this, 1.4));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        addOrderGoals(2, 4, 1.2);
        targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Player.class, true,
                player -> behavior().map(BehaviorProfile::aggressive).orElse(false)));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim(ATTACK_CONTROLLER, BITE_TRIGGER);
        return super.doHurtTarget(target);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (isUnconscious()) {
                return state.setAndContinue(UNCONSCIOUS);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(BITE_TRIGGER, BITE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
