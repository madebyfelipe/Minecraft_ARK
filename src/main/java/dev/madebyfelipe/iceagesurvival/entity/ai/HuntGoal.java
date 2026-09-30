package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/** Predador selvagem caça, de tempos em tempos, as presas listadas na tag da espécie. */
public class HuntGoal extends NearestAttackableTargetGoal<LivingEntity> {
    /**
     * A procura só acontece, em média, uma vez a cada este número de ticks. Mantém barata a
     * varredura de entidades e impede que os predadores acabem com a fauna em volta.
     */
    private static final int AVERAGE_TICKS_BETWEEN_HUNTS = 600;

    private final PrehistoricCreature creature;

    public HuntGoal(PrehistoricCreature creature, TagKey<EntityType<?>> prey) {
        super(creature, LivingEntity.class, AVERAGE_TICKS_BETWEEN_HUNTS, true, false,
                candidate -> candidate.getType().is(prey));
        this.creature = creature;
    }

    @Override
    public boolean canUse() {
        return !creature.isTame() && super.canUse();
    }
}
