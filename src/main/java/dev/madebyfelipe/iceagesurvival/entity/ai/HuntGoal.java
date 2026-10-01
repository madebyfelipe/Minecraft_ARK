package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.core.ecology.HuntChoice;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.EcologyProfile;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Caçada do predador selvagem, movida pela fome ({@link Hunger}) e pela escolha da presa mais fácil
 * ({@link HuntChoice}).
 *
 * <ul>
 *   <li>Faminto, procura pelo faro em todo o {@code hunt_radius} (não precisa ver a presa);
 *       oportunista, só pega presa fácil e perto; saciado, descansa.</li>
 *   <li>Nunca caça criatura domesticada: os animais da base do jogador não viram comida.</li>
 *   <li>Caçador de bando arrasta o bando, e o bando encara presa maior (a manada de mamutes).</li>
 *   <li>A presa e a manada dela ficam sabendo ({@link PrehistoricCreature#onHunted}) e disparam.</li>
 *   <li>A perseguição tem fôlego ({@code chase_seconds}) e alcance: a presa que abre distância
 *       escapa, e o predador frustrado espera antes de tentar de novo.</li>
 * </ul>
 */
public class HuntGoal extends Goal {
    /** Intervalo da procura com fome; oportunista procura com metade da frequência. */
    private static final int SCAN_INTERVAL = 40;
    /** Além deste múltiplo do raio de caça, a presa escapou. */
    private static final double ESCAPE_FACTOR = 1.5;

    private final PrehistoricCreature creature;
    private final TagKey<EntityType<?>> prey;
    private int scanCooldown;
    @Nullable
    private LivingEntity quarry;

    public HuntGoal(PrehistoricCreature creature, TagKey<EntityType<?>> prey) {
        this.creature = creature;
        this.prey = prey;
        setFlags(EnumSet.of(Flag.TARGET));
        // Desencontra a primeira procura dos predadores que nasceram juntos.
        scanCooldown = creature.getRandom().nextInt(SCAN_INTERVAL);
    }

    public static boolean isPrey(PrehistoricCreature hunter, LivingEntity candidate, TagKey<EntityType<?>> prey) {
        if (!candidate.isAlive() || !candidate.getType().is(prey) || candidate.getType() == hunter.getType()) {
            return false;
        }
        if (candidate instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        return !(candidate instanceof PrehistoricCreature creature) || !creature.isUnconscious();
    }

    /** Se este caçador, com fome, atacaria esta presa agora (tamanho, manada, bando). */
    public static boolean wouldHunt(PrehistoricCreature hunter, LivingEntity candidate, TagKey<EntityType<?>> prey) {
        if (!isPrey(hunter, candidate, prey)) {
            return false;
        }
        HuntGoal probe = new HuntGoal(hunter, prey);
        return HuntChoice.score(probe.prospect(candidate), hunter.ecology().huntRadius(), probe.packSize()) >= 0;
    }

    private HuntChoice.Prey prospect(LivingEntity candidate) {
        return new HuntChoice.Prey(creature.distanceTo(candidate), sizeRatio(candidate), candidate.isBaby(),
                candidate.getHealth() / candidate.getMaxHealth(),
                !(candidate instanceof PrehistoricCreature herdAnimal) || herdAnimal.isIsolated());
    }

    private boolean able() {
        return !creature.isTame() && !creature.isBaby() && !creature.isUnconscious() && !creature.isVehicle()
                && creature.yieldingFrom() == null;
    }

    @Override
    public boolean canUse() {
        if (!able() || creature.getTarget() != null || creature.recentlyFailedHunt()) {
            return false;
        }
        if (--scanCooldown > 0) {
            return false;
        }
        Hunger.Drive drive = creature.hungerDrive();
        scanCooldown = drive == Hunger.Drive.HUNTING ? SCAN_INTERVAL : SCAN_INTERVAL * 2;
        if (drive == Hunger.Drive.SATED) {
            return false;
        }
        quarry = choose(drive);
        return quarry != null;
    }

    @Nullable
    private LivingEntity choose(Hunger.Drive drive) {
        EcologyProfile ecology = creature.ecology();
        double radius = ecology.huntRadius();
        List<LivingEntity> found = creature.level().getEntitiesOfClass(LivingEntity.class,
                creature.getBoundingBox().inflate(radius, 12.0, radius), other -> isPrey(creature, other, prey));
        if (found.isEmpty()) {
            return null;
        }
        List<HuntChoice.Prey> options = new ArrayList<>(found.size());
        for (LivingEntity candidate : found) {
            options.add(prospect(candidate));
        }
        int chosen = HuntChoice.choose(options, radius, packSize(), drive);
        return chosen < 0 ? null : found.get(chosen);
    }

    /** Quantos da espécie caçam juntos aqui (1 = sozinho). */
    private int packSize() {
        int herdRadius = creature.behavior().map(BehaviorProfile::herdRadius).orElse(0);
        if (herdRadius <= 0) {
            return 1;
        }
        return 1 + creature.level().getEntitiesOfClass(PrehistoricCreature.class,
                creature.getBoundingBox().inflate(herdRadius), other -> other != creature
                        && other.getType() == creature.getType() && !other.isTame() && !other.isBaby()
                        && !other.isUnconscious()).size();
    }

    /** Tamanho relativo: razão dos volumes de colisão elevada a 2/3, a escala de uma área. */
    private double sizeRatio(LivingEntity other) {
        double own = creature.getBbWidth() * creature.getBbWidth() * creature.getBbHeight();
        double theirs = other.getBbWidth() * other.getBbWidth() * other.getBbHeight();
        return own <= 0 ? 1.0 : Math.pow(theirs / own, 2.0 / 3.0);
    }

    @Override
    public void start() {
        creature.setTarget(quarry);
        creature.beginHunt();
        creature.rallyPack(quarry);
        if (quarry instanceof PrehistoricCreature hunted) {
            hunted.onHunted(creature, packSize());
        }
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = creature.getTarget();
        if (!able() || target == null || target != quarry || !target.isAlive()) {
            return false;
        }
        EcologyProfile ecology = creature.ecology();
        return creature.huntTicks() < ecology.chaseSeconds() * 20L
                && creature.distanceTo(target) < ecology.huntRadius() * ESCAPE_FACTOR;
    }

    @Override
    public void tick() {
        // A presa segue se sabendo caçada enquanto o predador vem.
        if (quarry instanceof PrehistoricCreature hunted && creature.tickCount % 20 == 0) {
            hunted.stillHunted(creature, packSize());
        }
    }

    @Override
    public void stop() {
        LivingEntity target = creature.getTarget();
        boolean escaped = quarry != null && quarry.isAlive() && (target == quarry || target == null);
        if (target == quarry) {
            creature.setTarget(null);
        }
        if (escaped) {
            creature.huntFailed();
        } else {
            creature.endHunt();
        }
        quarry = null;
    }
}
