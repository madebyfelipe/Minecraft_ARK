package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.core.mount.FlightModel;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.entity.ai.ChaseGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.FleeWhenWeakGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.FollowHerdGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.HerdTravelGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.RivalryGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.YieldGoal;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.entity.ai.StalkGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.FollowMotherGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.WaryGoal;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.PackBonusProfile;
import com.github.darkpred.morehitboxes.api.EntityHitboxData;
import com.github.darkpred.morehitboxes.api.EntityHitboxDataFactory;
import com.github.darkpred.morehitboxes.api.GeckoLibMultiPartEntity;
import com.github.darkpred.morehitboxes.api.MultiPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.UUID;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Criatura terrestre genérica. O comportamento sai inteiro do {@link BehaviorProfile} da
 * espécie e as animações seguem a convenção {@code animation.<especie>.<idle|walk|attack|unconscious>},
 * então uma espécie nova precisa de um tipo de entidade registrado, um JSON e assets — não de uma classe.
 */
public class LandCreature extends PrehistoricCreature implements GeoEntity, GeckoLibMultiPartEntity<LandCreature> {
    private static final String ATTACK_CONTROLLER = "attack";
    private static final String ATTACK_TRIGGER = "attack";
    /** Amplitude da passada acima da qual a criatura está correndo, não andando. */
    private static final float RUN_LIMB_SWING = 0.75F;

    private static final double CHASE_SPEED = 1.35;
    private static final double STALK_SPEED = 0.55;
    private static final int TARGET_MEMORY_TICKS = 200;
    private static final double FLEE_SPEED = 1.4;
    private static final double FOLLOW_SPEED = 1.2;
    /**
     * Produto velocidade × modificador que dá um passeio calmo (~1,8 bloco/s). A velocidade
     * real cresce com o quadrado desse produto, então o modificador é calculado por espécie.
     */
    private static final double CALM_SPEED_PRODUCT = 0.2;
    private static final UUID PACK_SPEED_MODIFIER = UUID.fromString("3c9ed6b5-a71d-45d4-8f41-e2be799c4be2");
    private static final UUID PACK_ATTACK_MODIFIER = UUID.fromString("68cab421-5347-42e2-a707-7f22e330431c");
    private static final int PACK_CHECK_INTERVAL_TICKS = 20;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final EntityHitboxData<LandCreature> hitboxData = EntityHitboxDataFactory.create(this);

    public LandCreature(EntityType<? extends LandCreature> type, Level level) {
        super(type, level);
    }

    @Override
    public EntityHitboxData<LandCreature> getEntityHitboxData() {
        return hitboxData;
    }

    @Override
    public boolean partHurt(MultiPart<LandCreature> part, DamageSource source, float amount) {
        return hurt(source, amount);
    }

    @Override
    protected void registerGoals() {
        BehaviorProfile behavior = behavior().orElse(BehaviorProfile.PASSIVE);
        double baseSpeed = species().map(species -> species.stats().entry(Stat.SPEED).base()).orElse(0.25);
        double calm = Math.max(0.4, Math.min(1.0, CALM_SPEED_PRODUCT / Math.max(baseSpeed, 0.01)));

        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FleeWhenWeakGoal(this, FLEE_SPEED));
        goalSelector.addGoal(1, new YieldGoal(this, FLEE_SPEED));
        if (behavior.huntStyle() == BehaviorProfile.HuntStyle.STALK) {
            goalSelector.addGoal(2, new StalkGoal(this, STALK_SPEED));
        }
        // Espécie cautelosa persegue quem a feriu na velocidade da investida.
        double chase = behavior.wariness().map(WarinessProfile::chargeSpeed).orElse(CHASE_SPEED);
        goalSelector.addGoal(3, new ChaseGoal(this, chase));
        if (behavior.wariness().isPresent()) {
            goalSelector.addGoal(2, new WaryGoal(this, calm));
        }
        goalSelector.addGoal(5, new FollowMotherGoal(this, calm * 1.6));
        addOrderGoals(2, 4, FOLLOW_SPEED);
        if (behavior.herdRadius() > 0) {
            goalSelector.addGoal(5, new FollowHerdGoal(this, calm * 1.5, behavior.herdRadius()));
            if (behavior.migrates()) {
                goalSelector.addGoal(6, new HerdTravelGoal(this, calm, behavior.herdRadius()));
            } else {
                goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, calm));
            }
        } else {
            goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, calm));
        }
        goalSelector.addGoal(7, new MoveTowardsRestrictionGoal(this, calm * 1.2));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        if (behavior.aggressive()) {
            // Lembra do jogador que sumiu atrás das árvores por 10 s, não os 3 s do vanilla.
            targetSelector.addGoal(4, new PlayerTargetGoal(this).setUnseenMemoryTicks(TARGET_MEMORY_TICKS));
        }
        behavior.prey().ifPresent(prey -> targetSelector.addGoal(5, new HuntGoal(this, prey)));
        if (behavior.ecology().rivals().isPresent()) {
            targetSelector.addGoal(6, new RivalryGoal(this));
        }
    }

    /**
     * Agressão ao jogador no raio {@code aggro_radius}, que o estresse alarga (até 2×). O alcance
     * do goal do vanilla é fixado quando ele é criado, antes dos atributos da espécie; aqui ele é
     * recalculado a cada procura.
     */
    private static final class PlayerTargetGoal extends NonTameRandomTargetGoal<Player> {
        PlayerTargetGoal(LandCreature creature) {
            super(creature, Player.class, true, null);
        }

        // O construtor do vanilla já chama getFollowDistance(): usa o mob herdado, não um campo nosso.
        private LandCreature creature() {
            return (LandCreature) mob;
        }

        @Override
        protected double getFollowDistance() {
            LandCreature creature = creature();
            double aggro = creature.behavior().map(BehaviorProfile::aggroRadius).orElse(16.0);
            return aggro * Stress.perceptionMultiplier(creature.stress());
        }

        @Override
        public boolean canUse() {
            targetConditions.range(getFollowDistance());
            return creature().yieldingFrom() == null && super.canUse();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % PACK_CHECK_INTERVAL_TICKS == 0) {
            updatePackBonus();
        }
    }

    private void updatePackBonus() {
        PackBonusProfile bonus = species().flatMap(species -> species.packBonus()).orElse(null);
        boolean inPack = bonus != null && level().getEntitiesOfClass(
                        LandCreature.class, getBoundingBox().inflate(bonus.radius()),
                        other -> other.getType() == getType() && other.isAlive())
                .size() >= bonus.minimumAllies() + 1;
        updateModifier(Attributes.MOVEMENT_SPEED, PACK_SPEED_MODIFIER,
                inPack ? bonus.speedMultiplier() : 0.0);
        updateModifier(Attributes.ATTACK_DAMAGE, PACK_ATTACK_MODIFIER,
                inPack ? bonus.attackMultiplier() : 0.0);
    }

    private void updateModifier(net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                UUID id, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount <= 0.0) {
            instance.removeModifier(id);
        } else {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(
                    id, "allosaurus_pack_bonus", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @Override
    public void threatDisplay() {
        super.threatDisplay();
        // O gesto de ameaça é o golpe de cabeça no ar: o rinoceronte balança o chifre e bufa.
        triggerAnim(ATTACK_CONTROLLER, ATTACK_TRIGGER);
    }

    @Override
    protected void swingAttack() {
        super.swingAttack();
        triggerAnim(ATTACK_CONTROLLER, ATTACK_TRIGGER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(getType());
        CreatureAppearance appearance = CreatureAppearance.forEntity(typeId).orElse(null);
        String prefix = appearance != null
                ? appearance.animationPrefix()
                : "animation." + typeId.getPath() + ".";
        String idleName = appearance == null ? "idle" : appearance.idle();
        String walkName = appearance == null ? "walk" : appearance.walk();
        String attackName = appearance == null ? "attack" : appearance.attack();
        String unconsciousName = appearance == null ? "unconscious" : appearance.unconscious();
        String flyName = appearance == null ? walkName : appearance.fly();
        String runName = appearance == null ? walkName : appearance.run();
        String diveName = appearance == null ? flyName : appearance.dive();
        RawAnimation idle = RawAnimation.begin().thenLoop(prefix + idleName);
        RawAnimation walk = RawAnimation.begin().thenLoop(prefix + walkName);
        RawAnimation unconscious = RawAnimation.begin().thenLoop(prefix + unconsciousName);
        RawAnimation fly = RawAnimation.begin().thenLoop(prefix + flyName);
        RawAnimation run = RawAnimation.begin().thenLoop(prefix + runName);
        RawAnimation dive = RawAnimation.begin().thenLoop(prefix + diveName);
        // PLAY_ONCE explícito: o thenPlay usa o "loop" do arquivo, e o ataque do T-Rex e do Elasmotério
        // no Revival vem marcado como loop — o golpe ficava repetindo para sempre.
        RawAnimation attack = RawAnimation.begin().then(prefix + attackName, Animation.LoopType.PLAY_ONCE);

        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (isUnconscious()) {
                return state.setAndContinue(unconscious);
            }
            if (isFlying()) {
                // A inclinação da trajetória vem na rotação, que chega a todos os clientes.
                return state.setAndContinue(getXRot() >= FlightModel.DIVE_PITCH ? dive : fly);
            }
            if (!state.isMoving()) {
                return state.setAndContinue(idle);
            }
            // Passada larga (investida, fuga): a animação de corrida, se a espécie tiver uma.
            return state.setAndContinue(state.getLimbSwingAmount() > RUN_LIMB_SWING ? run : walk);
        }));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(ATTACK_TRIGGER, attack));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
