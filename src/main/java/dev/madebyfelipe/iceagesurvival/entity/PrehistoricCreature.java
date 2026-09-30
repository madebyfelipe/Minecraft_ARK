package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.command.CreatureOrder;
import dev.madebyfelipe.iceagesurvival.core.command.Obedience;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.core.stats.StatProfile;
import dev.madebyfelipe.iceagesurvival.core.stats.WildLevels;
import dev.madebyfelipe.iceagesurvival.core.taming.TamingRules;
import dev.madebyfelipe.iceagesurvival.core.taming.TamingSession;
import dev.madebyfelipe.iceagesurvival.entity.ai.OrderGoals;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.TamingProfile;
import java.util.Optional;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Base de toda criatura do mod. Guarda os pontos de atributo do indivíduo e os
 * aplica sobre a curva da {@link Species} correspondente ao tipo da entidade,
 * e conduz o ciclo torpor → inconsciente → alimentação → domesticada.
 *
 * <p>Dono e estado domesticado vêm de {@link TamableAnimal}.
 */
public abstract class PrehistoricCreature extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TORPOR_FRACTION =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_UNCONSCIOUS =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TAMING_PROGRESS =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_ORDER =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BYTE);

    private static final String TAG_STAT_POINTS = "StatPoints";
    private static final String TAG_TORPOR = "Torpor";
    private static final String TAG_UNCONSCIOUS = "Unconscious";
    private static final String TAG_TAMING = "Taming";
    private static final String TAG_TAMING_FOOD = "Food";
    private static final String TAG_TAMING_QUALITY = "Quality";
    private static final String TAG_TAMING_DAMAGE = "Damage";
    private static final String TAG_NEXT_FEED_TIME = "NextFeedTime";
    private static final String TAG_AFFINITY = "Affinity";
    private static final String TAG_HOME = "Home";
    private static final String TAG_ORDER = "Order";

    private static final int TORPOR_UPDATE_INTERVAL_TICKS = 20;
    /** Afinidade inicial de uma domesticação com eficiência de 100%. */
    private static final float MAX_INITIAL_AFFINITY = 50.0F;
    public static final float MAX_AFFINITY = 100.0F;
    /** Ordem de uma criatura recém-domesticada. */
    private static final CreatureOrder DEFAULT_ORDER = CreatureOrder.DEFEND;
    /** Espera entre duas alimentações de uma criatura já domesticada. */
    private static final int TAMED_FEED_INTERVAL_TICKS = 30 * 20;
    /** Afinidade ganha ao dar o alimento preferido a uma criatura domesticada. */
    private static final float AFFINITY_PER_FEED = 5.0F;
    /** Uma criatura domesticada larga o alvo que se afastar mais que isto. */
    private static final double TAMED_TARGET_LEASH = 40.0;

    private StatPoints statPoints = StatPoints.NONE;
    private boolean statsRolled;
    private double torpor;
    private TamingSession tamingSession = new TamingSession();
    /** Game time a partir do qual a criatura aceita comer de novo. */
    private long nextFeedTime;
    private float affinity;
    /** Centro do território: onde a criatura entrou no mundo pela primeira vez. */
    @Nullable
    private BlockPos homePos;

    protected PrehistoricCreature(EntityType<? extends PrehistoricCreature> type, Level level) {
        super(type, level);
    }

    /** Atributos que toda criatura precisa ter registrados para os stats serem aplicados. */
    public static AttributeSupplier.Builder createBaseAttributes() {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LEVEL, 1);
        builder.define(DATA_TORPOR_FRACTION, 0.0F);
        builder.define(DATA_UNCONSCIOUS, false);
        builder.define(DATA_TAMING_PROGRESS, 0.0F);
        builder.define(DATA_ORDER, (byte) DEFAULT_ORDER.ordinal());
    }

    public Optional<Species> species() {
        return Species.of(level().registryAccess(), getType());
    }

    public Optional<BehaviorProfile> behavior() {
        return species().flatMap(Species::behavior);
    }

    private Optional<TamingProfile> tamingProfile() {
        return species().flatMap(Species::taming);
    }

    // ---- Atributos ----

    public StatPoints statPoints() {
        return statPoints;
    }

    /** Nível do indivíduo; disponível também no cliente. */
    public int creatureLevel() {
        return entityData.get(DATA_LEVEL);
    }

    /** Torpor necessário para derrubar este indivíduo. */
    public double maxTorpor() {
        return species().map(species -> species.stats().value(Stat.TORPOR, statPoints)).orElse(0.0);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level().isClientSide) {
            return;
        }
        if (!statsRolled) {
            rollWildStats();
        }
        if (homePos == null) {
            homePos = blockPosition();
        }
        applyBehavior();
    }

    /** Aplica percepção e território da espécie. Criaturas domesticadas não têm território. */
    private void applyBehavior() {
        Optional<BehaviorProfile> behavior = behavior();
        behavior.ifPresent(profile -> setBase(Attributes.FOLLOW_RANGE, profile.aggroRadius()));
        int territory = behavior.map(BehaviorProfile::territoryRadius).orElse(0);
        if (!isTame() && territory > 0 && homePos != null) {
            restrictTo(homePos, territory);
        } else {
            clearRestriction();
        }
    }

    @Override
    protected void applyTamingSideEffects() {
        super.applyTamingSideEffects();
        if (!level().isClientSide) {
            applyBehavior();
        }
    }

    private RandomGenerator randomGenerator() {
        return random::nextLong;
    }

    private void rollWildStats() {
        Optional<Species> species = species();
        if (species.isEmpty()) {
            IceAgeSurvival.LOGGER.warn("Sem definição de espécie para {}; atributos padrão mantidos",
                    EntityType.getKey(getType()));
            return;
        }
        RandomGenerator generator = randomGenerator();
        int level = WildLevels.roll(ServerConfig.MAX_WILD_LEVEL.get(), ServerConfig.WILD_LEVEL_STEP.get(), generator);
        setStatPoints(StatPoints.rollWild(level, generator), species.get());
        setHealth(getMaxHealth());
    }

    private void setStatPoints(StatPoints points, Species species) {
        statPoints = points;
        statsRolled = true;
        entityData.set(DATA_LEVEL, points.level());

        StatProfile profile = species.stats();
        setBase(Attributes.MAX_HEALTH, profile.value(Stat.HEALTH, points));
        setBase(Attributes.ATTACK_DAMAGE, profile.value(Stat.ATTACK, points));
        setBase(Attributes.MOVEMENT_SPEED, profile.value(Stat.SPEED, points));
        setBase(Attributes.ARMOR, profile.value(Stat.ARMOR, points));
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // ---- Torpor ----

    public double torpor() {
        return torpor;
    }

    /** De 0 a 1; disponível também no cliente. */
    public float torporFraction() {
        return entityData.get(DATA_TORPOR_FRACTION);
    }

    /** Disponível também no cliente. */
    public boolean isUnconscious() {
        return entityData.get(DATA_UNCONSCIOUS);
    }

    /** Aplica torpor vindo de um tranquilizante. Criaturas domesticadas são imunes. */
    public void addTorpor(double amount) {
        Optional<TamingProfile> profile = tamingProfile();
        if (level().isClientSide || amount <= 0 || isTame() || profile.isEmpty()) {
            return;
        }
        setTorpor(torpor + amount * profile.get().torporMultiplier());
    }

    /** Define o torpor diretamente, derrubando ou acordando a criatura conforme o caso. */
    public void setTorpor(double value) {
        double max = maxTorpor();
        torpor = Math.clamp(value, 0.0, max);
        entityData.set(DATA_TORPOR_FRACTION, max > 0 ? (float) (torpor / max) : 0.0F);
        if (!isUnconscious() && max > 0 && torpor >= max) {
            knockOut();
        } else if (isUnconscious() && torpor <= 0) {
            wakeUp();
        }
    }

    private void knockOut() {
        entityData.set(DATA_UNCONSCIOUS, true);
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
    }

    private void wakeUp() {
        entityData.set(DATA_UNCONSCIOUS, false);
        tamingSession.reset();
        entityData.set(DATA_TAMING_PROGRESS, 0.0F);
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isUnconscious();
    }

    // Inconsciente, a criatura não sai do lugar: nem empurrada, nem por recuo de golpe.
    @Override
    public boolean isPushable() {
        return !isUnconscious() && super.isPushable();
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!isUnconscious()) {
            super.knockback(strength, x, z);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % TORPOR_UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        if (torpor > 0) {
            double decayPerSecond = tamingProfile().map(TamingProfile::torporDecayPerSecond).orElse(0.0);
            setTorpor(torpor - decayPerSecond * TORPOR_UPDATE_INTERVAL_TICKS / 20.0);
        }
        LivingEntity target = getTarget();
        if (isTame() && target != null
                && (!target.isAlive() || distanceToSqr(target) > TAMED_TARGET_LEASH * TAMED_TARGET_LEASH)) {
            setTarget(null);
        }
    }

    // ---- Comandos ----

    /** Se o jogador é o dono. Compara pelo UUID, então vale mesmo com o dono fora do mundo. */
    public boolean isOwner(Player player) {
        return isTame() && player.getUUID().equals(getOwnerUUID());
    }

    /** Ordem atual; disponível também no cliente. Só tem efeito em criaturas domesticadas. */
    public CreatureOrder order() {
        CreatureOrder[] all = CreatureOrder.values();
        int index = entityData.get(DATA_ORDER);
        return index >= 0 && index < all.length ? all[index] : DEFAULT_ORDER;
    }

    public void setOrder(CreatureOrder order) {
        entityData.set(DATA_ORDER, (byte) order.ordinal());
        setOrderedToSit(!order.followsOwner());
        if (!order.fightsBack()) {
            setTarget(null);
        }
        if (!order.followsOwner()) {
            getNavigation().stop();
        }
    }

    /** Sorteia se a criatura obedece a um comando, conforme a afinidade. */
    public boolean rollObedience() {
        return Obedience.obeys(affinity, MAX_AFFINITY, ServerConfig.MIN_OBEDIENCE.get(), randomGenerator());
    }

    public void setAffinity(float value) {
        affinity = Math.clamp(value, 0.0F, MAX_AFFINITY);
    }

    /**
     * Registra os goals que fazem uma criatura domesticada cumprir ordens. Chamar em
     * {@code registerGoals}; as prioridades dos goals de movimento ficam por conta da espécie.
     */
    protected void addOrderGoals(int stayPriority, int followPriority, double followSpeed) {
        goalSelector.addGoal(stayPriority, new OrderGoals.Stay(this));
        goalSelector.addGoal(followPriority, new OrderGoals.Follow(this, followSpeed, 8.0F, 3.0F));
        targetSelector.addGoal(1, new OrderGoals.DefendOwner(this));
        targetSelector.addGoal(2, new OrderGoals.AssistOwner(this));
        targetSelector.addGoal(3, new OrderGoals.Retaliate(this));
    }

    // ---- Domesticação ----

    /** De 0 a 1; disponível também no cliente. */
    public float tamingProgress() {
        return entityData.get(DATA_TAMING_PROGRESS);
    }

    /** De 0 a {@link #MAX_AFFINITY}; só no servidor. */
    public float affinity() {
        return affinity;
    }

    private double requiredFood(TamingProfile profile) {
        return TamingRules.requiredFood(profile.requiredFood(), profile.requiredFoodPerLevel(), statPoints.level());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isUnconscious() && !isTame()) {
            tamingSession.recordDamage(amount / getMaxHealth());
        }
        return hurt;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isUnconscious() && !isTame()) {
            ItemStack stack = player.getItemInHand(hand);
            Optional<TamingProfile> profile = tamingProfile();
            Optional<TamingProfile.Food> food = profile.flatMap(p -> p.foodFor(stack));
            if (food.isPresent()) {
                if (!level().isClientSide) {
                    feedUnconscious(player, hand, stack, profile.get(), food.get());
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (!isUnconscious() && isOwner(player)) {
            ItemStack stack = player.getItemInHand(hand);
            Optional<TamingProfile.Food> food = tamingProfile().flatMap(p -> p.foodFor(stack));
            if (food.isPresent()) {
                if (!level().isClientSide) {
                    feedTamed(player, hand, stack, food.get());
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Alimentar uma criatura domesticada cura e aumenta a afinidade; é opcional, não manutenção. */
    private void feedTamed(Player player, InteractionHand hand, ItemStack stack, TamingProfile.Food food) {
        long now = level().getGameTime();
        if (now < nextFeedTime) {
            player.displayClientMessage(Component.translatable("iceagesurvival.taming.not_hungry"), true);
            return;
        }
        usePlayerItem(player, hand, stack);
        heal((float) food.value());
        setAffinity(affinity + (float) (AFFINITY_PER_FEED * food.quality()));
        nextFeedTime = now + TAMED_FEED_INTERVAL_TICKS;
        level().broadcastEntityEvent(this, (byte) 7);
    }

    private void feedUnconscious(Player player, InteractionHand hand, ItemStack stack,
                                 TamingProfile profile, TamingProfile.Food food) {
        long now = level().getGameTime();
        if (now < nextFeedTime) {
            player.displayClientMessage(Component.translatable("iceagesurvival.taming.not_hungry"), true);
            return;
        }
        usePlayerItem(player, hand, stack);
        tamingSession.feed(food.value(), food.quality());
        nextFeedTime = now + profile.feedIntervalSeconds() * 20L;

        double required = requiredFood(profile);
        if (tamingSession.isComplete(required)) {
            completeTaming(player);
        } else {
            entityData.set(DATA_TAMING_PROGRESS, (float) tamingSession.progress(required));
        }
    }

    private void completeTaming(Player player) {
        double effectiveness = tamingSession.effectiveness();
        int bonus = TamingRules.bonusPoints(
                statPoints.level(), ServerConfig.TAMING_BONUS_LEVEL_FRACTION.get(), effectiveness);
        species().ifPresent(species -> setStatPoints(statPoints.addRandom(bonus, randomGenerator()), species));
        affinity = (float) (effectiveness * MAX_INITIAL_AFFINITY);

        setOrder(DEFAULT_ORDER);
        tame(player);
        setTorpor(0);
        level().broadcastEntityEvent(this, (byte) 7); // corações de domesticação do TamableAnimal
    }

    // ---- Persistência ----

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (statsRolled) {
            CompoundTag points = new CompoundTag();
            for (Stat stat : Stat.values()) {
                points.putInt(stat.id(), statPoints.get(stat));
            }
            compound.put(TAG_STAT_POINTS, points);
        }
        compound.putDouble(TAG_TORPOR, torpor);
        compound.putBoolean(TAG_UNCONSCIOUS, isUnconscious());
        CompoundTag taming = new CompoundTag();
        taming.putDouble(TAG_TAMING_FOOD, tamingSession.foodValue());
        taming.putDouble(TAG_TAMING_QUALITY, tamingSession.qualityWeighted());
        taming.putDouble(TAG_TAMING_DAMAGE, tamingSession.damageFraction());
        compound.put(TAG_TAMING, taming);
        compound.putLong(TAG_NEXT_FEED_TIME, nextFeedTime);
        compound.putFloat(TAG_AFFINITY, affinity);
        compound.putString(TAG_ORDER, order().id());
        if (homePos != null) {
            compound.put(TAG_HOME, NbtUtils.writeBlockPos(homePos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        Optional<Species> species = species();
        if (compound.contains(TAG_STAT_POINTS, Tag.TAG_COMPOUND)) {
            CompoundTag saved = compound.getCompound(TAG_STAT_POINTS);
            StatPoints points = StatPoints.NONE;
            for (Stat stat : Stat.values()) {
                points = points.with(stat, Math.max(0, saved.getInt(stat.id())));
            }
            if (species.isPresent()) {
                // A vida atual já foi lida pelo super; reaplicar os atributos não a altera.
                float health = getHealth();
                setStatPoints(points, species.get());
                setHealth(health);
            } else {
                statPoints = points;
                statsRolled = true;
            }
        }

        CompoundTag taming = compound.getCompound(TAG_TAMING);
        tamingSession = new TamingSession(
                taming.getDouble(TAG_TAMING_FOOD), taming.getDouble(TAG_TAMING_QUALITY), taming.getDouble(TAG_TAMING_DAMAGE));
        nextFeedTime = compound.getLong(TAG_NEXT_FEED_TIME);
        affinity = Math.clamp(compound.getFloat(TAG_AFFINITY), 0.0F, MAX_AFFINITY);
        homePos = NbtUtils.readBlockPos(compound, TAG_HOME).orElse(null);
        entityData.set(DATA_ORDER, (byte) CreatureOrder.byId(compound.getString(TAG_ORDER), DEFAULT_ORDER).ordinal());

        double max = maxTorpor();
        torpor = Math.clamp(compound.getDouble(TAG_TORPOR), 0.0, max);
        entityData.set(DATA_TORPOR_FRACTION, max > 0 ? (float) (torpor / max) : 0.0F);
        entityData.set(DATA_UNCONSCIOUS, compound.getBoolean(TAG_UNCONSCIOUS) && torpor > 0);
        entityData.set(DATA_TAMING_PROGRESS, species.flatMap(Species::taming)
                .map(profile -> (float) tamingSession.progress(requiredFood(profile)))
                .orElse(0.0F));
    }

    // A reprodução vanilla (alimentar dois adultos) não se aplica; o mod tem sistema próprio.
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
}
