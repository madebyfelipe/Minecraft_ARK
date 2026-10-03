package dev.madebyfelipe.iceagesurvival.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Sangramento da mordida cortante do Giganotosaurus (D47). Esqueleto — a frente do boss define o dano por segundo.
 */
public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0303);
    }
}
