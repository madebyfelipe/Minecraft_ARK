package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Quem monta uma montaria voadora não sufoca nos galhos.
 *
 * <p>O assento fica bem acima da caixa de colisão da montaria — no Quetzalcoatlus, a 6,3 blocos de uma caixa de 4,5,
 * que é menor que o desenho para não inflar o porte das regras de ecologia (D42). A caixa passa por baixo do galho e a
 * cabeça de quem monta entra nele: o vanilla dava o dano de sufocar a cada meio segundo, como se a montaria ferisse o
 * próprio cavaleiro ao roçar a copa. Só o dano de dentro de bloco ({@code in_wall}) é cancelado; o resto (queda dele
 * mesmo ao desmontar, golpes, fogo) segue valendo.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public final class FlightMountRider {
    private FlightMountRider() {
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().is(DamageTypes.IN_WALL)
                && event.getEntity().getVehicle() instanceof PrehistoricCreature mount && mount.isFlightMount()) {
            event.setCanceled(true);
        }
    }
}
