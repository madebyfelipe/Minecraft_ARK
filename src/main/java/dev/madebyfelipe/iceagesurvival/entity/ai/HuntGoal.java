package dev.madebyfelipe.iceagesurvival.entity.ai;

import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntChoice;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
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
import net.minecraft.world.entity.player.Player;

/**
 * Caçada do predador selvagem, movida pela fome ({@link Hunger}) e pela escolha da presa mais fácil
 * ({@link HuntChoice}).
 *
 * <ul>
 *   <li>Faminto, procura pelo faro em todo o {@code hunt_radius} (não precisa ver a presa);
 *       oportunista, só pega presa fácil e perto; saciado, descansa.</li>
 *   <li>Nunca caça criatura domesticada: os animais da base do jogador não viram comida.</li>
 *   <li>Caçador de bando arrasta o bando, e o bando encara presa maior (a manada de mamutes).</li>
 *   <li>A presa e a manada dela ficam sabendo ({@link PrehistoricCreature#onHunted}) e disparam — quem
 *       espreita ({@link StalkGoal}) só se revela no bote.</li>
 *   <li>A perseguição tem fôlego ({@code chase_seconds}) e alcance: a presa que abre
 *       {@link #CHASE_GIVE_UP_DISTANCE} blocos escapa, e o predador frustrado espera antes de tentar de novo.</li>
 * </ul>
 */
public class HuntGoal extends Goal {
    /** Raio em que jogadores e criaturas domesticadas contam como o bando de um jogador. */
    private static final double PLAYER_ALLY_RADIUS = 16.0;
    /** Intervalo da procura com fome; oportunista procura com metade da frequência. */
    private static final int SCAN_INTERVAL = 40;
    /** Espreitando, além deste múltiplo do raio de caça a presa escapou. */
    private static final double ESCAPE_FACTOR = 1.5;
    /** Na perseguição (depois da disparada), a presa que abre esta distância do predador escapou. */
    public static final double CHASE_GIVE_UP_DISTANCE = 30.0;

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
        boolean playerPrey = candidate instanceof Player
                && hunter.behavior().map(behavior -> behavior.prey().isPresent() && behavior.huntsPlayers()).orElse(false);
        // O jogador é presa da tabela de dieta; não precisa estar na tag de presas.
        if (!candidate.isAlive() || (!playerPrey && !candidate.getType().is(prey))
                || candidate.getType() == hunter.getType()) {
            return false;
        }
        if (candidate instanceof Player player && !net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
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
        Hunger.Drive drive = hunter.hungerDrive();
        if (candidate instanceof Player && drive != Hunger.Drive.HUNTING) {
            return false;
        }
        HuntGoal probe = new HuntGoal(hunter, prey);
        return HuntChoice.choose(List.of(probe.prospect(candidate)), hunter.huntRadius(),
                probe.packSize(), drive) == 0;
    }

    private HuntChoice.Prey prospect(LivingEntity candidate) {
        int defenders = defenders(candidate);
        boolean isolated = candidate instanceof PrehistoricCreature herdAnimal ? herdAnimal.isIsolated() : defenders <= 1;
        int preference = creature.behavior().map(behavior -> behavior.preference(candidate.getType()))
                .orElse(dev.madebyfelipe.iceagesurvival.species.DietEntry.DEFAULT_PREFERENCE);
        return new HuntChoice.Prey(creature.distanceTo(candidate), creature.sizeRatioOf(candidate), candidate.isBaby(),
                candidate.getHealth() / candidate.getMaxHealth(), isolated, preference, defenders);
    }

    /**
     * Quantos defendem a presa junto. Criatura do mod: o bando dela. Jogador: ele, os outros jogadores e as
     * criaturas domesticadas deles por perto — o bando do jogador.
     */
    private static int defenders(LivingEntity candidate) {
        if (candidate instanceof PrehistoricCreature herdAnimal) {
            return herdAnimal.fightingGroup();
        }
        if (candidate instanceof Player player) {
            return 1 + player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(PLAYER_ALLY_RADIUS),
                    other -> other != player && other.isAlive()
                            && (other instanceof Player ally && !ally.isSpectator()
                            || other instanceof OwnableEntity pet && pet.getOwnerUUID() != null)).size();
        }
        return 1;
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
        return pick(drive, true);
    }

    /**
     * Uma conta só para todas as presas, jogador incluído: preferência da dieta, porte, facilidade e quem
     * a defende. O jogador só entra com fome de verdade, como as presas grandes.
     */
    @Nullable
    private LivingEntity pick(Hunger.Drive drive, boolean shadow) {
        double radius = creature.huntRadius();
        List<LivingEntity> found = creature.level().getEntitiesOfClass(LivingEntity.class,
                creature.getBoundingBox().inflate(radius, 12.0, radius),
                other -> isPrey(creature, other, prey) && (!(other instanceof Player) || drive == Hunger.Drive.HUNTING));
        if (found.isEmpty()) {
            return null;
        }
        List<HuntChoice.Prey> options = new ArrayList<>(found.size());
        for (LivingEntity candidate : found) {
            options.add(prospect(candidate));
        }
        int chosen = HuntChoice.choose(options, radius, packSize(), drive);
        if (chosen < 0 && shadow && drive == Hunger.Drive.HUNTING && creature.stalks()) {
            // Nenhuma dá para atacar agora: acompanha a manada esperando uma se desgarrar (StalkGoal).
            chosen = HuntChoice.chooseToStalk(options, radius, packSize());
        }
        return chosen < 0 ? null : found.get(chosen);
    }

    /**
     * A presa que o predador com fome escolheria agora, jogador incluído (pela mesma conta). Nula se
     * nenhuma vale a caçada.
     */
    @Nullable
    public static LivingEntity bestPrey(PrehistoricCreature hunter) {
        var preyTag = hunter.behavior().flatMap(BehaviorProfile::prey).orElse(null);
        if (preyTag == null || hunter.isSated()) {
            return null;
        }
        return new HuntGoal(hunter, preyTag).pick(hunter.hungerDrive(), false);
    }

    /** Na perseguição, a presa abriu mais de {@link #CHASE_GIVE_UP_DISTANCE} blocos: o predador desiste. */
    public static boolean escaped(PrehistoricCreature hunter, LivingEntity prey) {
        return hunter.distanceTo(prey) > CHASE_GIVE_UP_DISTANCE;
    }

    /** Quantos da espécie caçam juntos aqui (1 = sozinho). */
    private int packSize() {
        return creature.fightingGroup();
    }

    @Override
    public void start() {
        creature.setTarget(quarry);
        creature.beginHunt();
        creature.rallyPack(quarry);
        // Quem espreita não se anuncia: a manada só se sabe caçada no bote (StalkGoal).
        if (quarry instanceof PrehistoricCreature hunted && !creature.stalks()) {
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
        // O fôlego conta da disparada: rondando a manada, o predador ainda não correu.
        if (creature.isStalking()) {
            return creature.distanceTo(target) < creature.huntRadius() * ESCAPE_FACTOR;
        }
        return creature.huntTicks() < ecology.chaseSeconds() * 20L && !escaped(creature, target);
    }

    @Override
    public void tick() {
        // A presa segue se sabendo caçada enquanto o predador vem.
        if (quarry instanceof PrehistoricCreature hunted && !creature.isStalking() && creature.tickCount % 20 == 0) {
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
