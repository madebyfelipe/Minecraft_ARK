package dev.madebyfelipe.iceagesurvival.compat;

import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade (opcional): esconde a caixa dele sobre as criaturas do mod, que já têm o painel próprio sob a
 * mira ({@code CreatureHud}) — com os dois, um ficava por cima do outro. Só o Jade carrega esta
 * classe, e só quando está instalado.
 */
@WailaPlugin
public class JadeCompat implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        for (var creature : ModEntities.LAND_CREATURES) {
            registration.hideTarget(creature.get());
        }
    }
}
