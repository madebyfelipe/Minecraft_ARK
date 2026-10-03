package dev.madebyfelipe.iceagesurvival.effect;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.VenomBite;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Peçonha da Megalania. As glândulas do varanídeo soltam um coquetel anticoagulante e hipotensor: a presa sangra sem
 * coagular e entra em choque (Fry et al. 2009, sobre o dragão-de-komodo e, por parentesco, a Megalania).
 *
 * <ul>
 *   <li>Nas criaturas do mod: <b>choque</b> — soma torpor a cada segundo ({@link #TORPOR_PER_SECOND} do torpor máximo),
 *   até derrubar. A domesticada é imune, como a qualquer torpor; o apex também (D30: só cai no desafio).</li>
 *   <li>No jogador e nos mobs vanilla: <b>sangramento</b> — perde vida a cada segundo e não regenera enquanto dura
 *   ({@link VenomBite#onHeal}).</li>
 *   <li>Em todos, a pressão cai: um pouco de lentidão.</li>
 * </ul>
 *
 * <p>O antídoto ({@code AntidoteItem}) tira o efeito.
 */
public class VenomEffect extends MobEffect {
    /** Quanto dura uma mordida: 30 s. Cada mordida nova recomeça a conta. */
    public static final int DURATION_TICKS = 600;
    /** Fração do torpor máximo que o choque soma por segundo numa criatura do mod. */
    public static final double TORPOR_PER_SECOND = 0.05;
    /** Vida que o sangramento tira por segundo do jogador e dos mobs vanilla: 10 de vida em 30 s. */
    public static final float BLEED_PER_SECOND = 1.0F / 3.0F;
    /** A pressão cai: −15% de velocidade. */
    public static final double SLOWDOWN = -0.15;

    public VenomEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B8E23);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "0f3c5a8e-6c2b-4a51-9d0e-7b1f6a2c9e44", SLOWDOWN,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        if (entity instanceof PrehistoricCreature creature) {
            VenomBite.shock(creature, TORPOR_PER_SECOND * (amplifier + 1));
        } else {
            VenomBite.bleed(entity, BLEED_PER_SECOND * (amplifier + 1));
        }
    }
}
