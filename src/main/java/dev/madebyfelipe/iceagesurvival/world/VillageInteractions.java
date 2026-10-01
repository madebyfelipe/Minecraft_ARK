package dev.madebyfelipe.iceagesurvival.world;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Fauna e vilas. Os predadores caçam aldeões e mercadores (estão em {@code large_prey}, com a mesma
 * regra de fome do jogador e do gado); o golem de ferro, que só defende a vila de monstros do vanilla,
 * passa a atacar também o predador selvagem que chega perto. Os aldeões entram em pânico quando
 * atacados, pelo cérebro do vanilla.
 */
public final class VillageInteractions {
    /** Prioridade do alvo extra do golem: igual à dos monstros no vanilla. */
    private static final int GOLEM_TARGET_PRIORITY = 3;

    private VillageInteractions() {
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof IronGolem golem) {
            golem.targetSelector.addGoal(GOLEM_TARGET_PRIORITY, new NearestAttackableTargetGoal<>(
                    golem, PrehistoricCreature.class, 5, false, false, VillageInteractions::threatensVillage));
        }
    }

    /** Predador selvagem, acordado: o golem trata como um monstro. Herbívoros e domesticados ficam em paz. */
    public static boolean threatensVillage(LivingEntity entity) {
        return entity instanceof PrehistoricCreature creature && !creature.isTame() && !creature.isUnconscious()
                && creature.behavior().map(BehaviorProfile::aggressive).orElse(false);
    }
}
