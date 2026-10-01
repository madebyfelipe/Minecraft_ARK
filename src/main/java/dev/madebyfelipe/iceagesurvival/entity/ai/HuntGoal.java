package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Predador selvagem caça, de tempos em tempos, as presas listadas na tag da espécie.
 *
 * <ul>
 *   <li>Nunca caça criatura domesticada: os animais da base do jogador não viram comida.
 *   <li>Caçador solitário (sem manada) só ataca uma presa de manada se ela estiver isolada —
 *       o T-Rex pega o bronto desgarrado, não o do meio do grupo.
 *   <li>Caçador de bando arrasta o bando: ao escolher a presa, os da mesma espécie por perto
 *       partem junto. Por isso um bando pode atacar uma manada.
 *   <li>Depois de abater uma presa, fica saciado por {@code sated_seconds} e não caça.
 * </ul>
 */
public class HuntGoal extends NearestAttackableTargetGoal<LivingEntity> {
    /**
     * A procura só acontece, em média, uma vez a cada este número de ticks. Mantém barata a
     * varredura de entidades e impede que os predadores acabem com a fauna em volta.
     */
    private static final int AVERAGE_TICKS_BETWEEN_HUNTS = 600;

    private final PrehistoricCreature creature;

    public HuntGoal(PrehistoricCreature creature, TagKey<EntityType<?>> prey) {
        super(creature, LivingEntity.class, AVERAGE_TICKS_BETWEEN_HUNTS, true, false,
                candidate -> isPrey(creature, candidate, prey));
        this.creature = creature;
    }

    public static boolean isPrey(PrehistoricCreature hunter, LivingEntity candidate, TagKey<EntityType<?>> prey) {
        if (!candidate.getType().is(prey) || candidate.getType() == hunter.getType()) {
            return false;
        }
        if (candidate instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        boolean packHunter = hunter.behavior().map(BehaviorProfile::herdRadius).orElse(0) > 0;
        return packHunter || !(candidate instanceof PrehistoricCreature herdAnimal) || herdAnimal.isIsolated();
    }

    @Override
    public boolean canUse() {
        // Recém-alimentado, o predador descansa: a presa por perto não corre perigo por uns minutos.
        return !creature.isTame() && !creature.isBaby() && !creature.isSated() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        creature.rallyPack(target);
    }
}
