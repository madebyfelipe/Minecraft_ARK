package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.effect.BleedingEffect;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A espada serrilhada (D47): espada de diamante com um dente serrilhado do Giganotosaurus. O golpe corpo a corpo dela
 * abre um sangramento curto ({@link BleedingEffect#SWORD_TICKS}, um nível, sem empilhar) — o corte do boss, em
 * pequeno. O item continua um {@code SwordItem} de diamante; o corte vem deste evento.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public final class SerratedSword {
    private SerratedSword() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity().level().isClientSide || source.is(DamageTypeTags.IS_PROJECTILE)
                || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))) {
            return;
        }
        if (source.getDirectEntity() instanceof LivingEntity attacker && source.getEntity() == attacker
                && attacker.getMainHandItem().is(ModItems.SERRATED_SWORD.get())) {
            BleedingEffect.cut(event.getEntity(), attacker, 1, 1, BleedingEffect.SWORD_TICKS);
        }
    }
}
