package dev.madebyfelipe.iceagesurvival.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * O boss da Etapa 10 (D47): o Giganotosaurus da arena da caverna. Esqueleto — a frente do boss preenche as fases, a
 * resistência por fase, a mordida que faz sangrar e a barra de boss.
 */
public class GiganotosaurusBoss extends LandCreature {
    public GiganotosaurusBoss(EntityType<? extends LandCreature> type, Level level) {
        super(type, level);
    }
}
