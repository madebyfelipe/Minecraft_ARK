package dev.madebyfelipe.iceagesurvival.effect;

import dev.madebyfelipe.iceagesurvival.core.ecology.TailClub;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Perna quebrada pela clava do Anquilossauro ({@link TailClub}): quem leva o golpe perde
 * {@link TailClub#LEG_BREAK_SLOWDOWN} da velocidade e não pula enquanto dura. Vale para o jogador, os mobs vanilla e as
 * criaturas do mod — o predador que espreitava pelas costas sai mancando.
 */
public class BrokenLegEffect extends MobEffect {
    public BrokenLegEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B5A2B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "5d7e2c41-93a8-4f0b-b6e1-2a9c4d8f1e37",
                -TailClub.LEG_BREAK_SLOWDOWN, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /**
     * Sem pulo: o impulso para cima é desfeito. O jogador pula no próprio cliente, e o efeito vai para o cliente dele,
     * então este ouvinte roda dos dois lados.
     */
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.BROKEN_LEG.get())) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.min(motion.y, 0.0), motion.z);
        }
    }
}
