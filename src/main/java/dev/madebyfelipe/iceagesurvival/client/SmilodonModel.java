package dev.madebyfelipe.iceagesurvival.client;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.Smilodon;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SmilodonModel extends DefaultedEntityGeoModel<Smilodon> {
    public SmilodonModel() {
        super(IceAgeSurvival.id("smilodon"), true);
    }

    @Override
    public void setCustomAnimations(Smilodon animatable, long instanceId, AnimationState<Smilodon> animationState) {
        // A cabeça acompanha o olhar só enquanto a criatura está consciente.
        if (!animatable.isUnconscious()) {
            super.setCustomAnimations(animatable, instanceId, animationState);
        }
    }
}
