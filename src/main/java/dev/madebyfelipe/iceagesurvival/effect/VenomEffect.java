package dev.madebyfelipe.iceagesurvival.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Peçonha da Megalania: anticoagulante (sangra e não regenera) e hipotensora (lentidão que cresce e, nas criaturas do
 * mod, choque — torpor). Fry et al. 2009.
 */
public class VenomEffect extends MobEffect {
    public VenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B8E23);
    }
}
