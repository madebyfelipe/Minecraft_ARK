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
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.BodyProfile;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import dev.madebyfelipe.iceagesurvival.species.SoundProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.TamingProfile;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Base de toda criatura do mod. Guarda os pontos de atributo do indivíduo e os
 * aplica sobre a curva da {@link Species} correspondente ao tipo da entidade,
 * e conduz o ciclo torpor → inconsciente → alimentação → domesticada.
 *
 * <p>Dono e estado domesticado vêm de {@link TamableAnimal}. O controle da montaria
 * reaproveita o modelo de veículo do vanilla ({@code travelRidden}): o cliente de quem
 * monta simula o movimento e o servidor valida, como num cavalo.
 */
public abstract class PrehistoricCreature extends TamableAnimal implements PlayerRideableJumping {
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
    private static final EntityDataAccessor<Boolean> DATA_SADDLED =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);

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
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_TAMER = "Tamer";
    private static final String TAG_SADDLED = "Saddled";

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
    /** Uma fileira de baú. */
    public static final int INVENTORY_SIZE = 9;
    private static final double INVENTORY_REACH = 8.0;
    /** Item que sela uma criatura montável. */
    public static final net.minecraft.world.item.Item SADDLE_ITEM = Items.SADDLE;
    /** Alcance da mordida de quem monta, a partir da caixa de colisão da criatura. */
    public static final double RIDDEN_ATTACK_REACH = 3.0;
    /** Quantos blocos à frente do corpo a mordida de quem monta quebra. */
    private static final double BITE_DEPTH = 2.0;
    /** Ticks entre dois ataques de quem monta. */
    private static final int RIDDEN_ATTACK_COOLDOWN = 20;
    /** Recuo da ré de quem monta, como no cavalo: andar para trás é bem mais lento. */
    private static final float RIDDEN_BACKWARD_FACTOR = 0.25F;
    /** Fator lateral de quem monta, como no cavalo. */
    private static final float RIDDEN_STRAFE_FACTOR = 0.5F;

    private StatPoints statPoints = StatPoints.NONE;
    private boolean statsRolled;
    private double torpor;
    private TamingSession tamingSession = new TamingSession();
    /** Game time a partir do qual a criatura aceita comer de novo. */
    private long nextRiderAttackTime;
    /** Durante o golpe de quem monta: a animação e o som já saíram, {@link #doHurtTarget} não repete. */
    private boolean attackSwung;
    private long nextFeedTime;
    private float affinity;
    /** Centro do território: onde a criatura entrou no mundo pela primeira vez. */
    @Nullable
    private BlockPos homePos;
    /** Quem derrubou a criatura: só essa pessoa mexe no inventário e é ela quem fica com a criatura. */
    @Nullable
    private UUID tamerUUID;
    private boolean breaksLeaves;
    /** Carga do pulo enviada pelo cliente de quem monta, de 0 a 1. */
    private float playerJumpPendingScale;
    /** Se o impulso do pulo já foi aplicado e a criatura ainda não voltou ao chão. */
    private boolean ridingJump;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public boolean stillValid(Player player) {
            return canAccessInventory(player)
                    && player.distanceToSqr(PrehistoricCreature.this) <= INVENTORY_REACH * INVENTORY_REACH;
        }
    };

    protected PrehistoricCreature(EntityType<? extends PrehistoricCreature> type, Level level) {
        super(type, level);
    }

    /**
     * Regra de spawn natural: em chão firme e na superfície (inclusive sobre neve e gelo, e sob
     * copas de árvore), nunca dentro de cavernas. Usa o mapa de altura, que não depende de luz.
     */
    public static boolean checkSurfaceSpawnRules(EntityType<? extends PrehistoricCreature> type,
                                                 ServerLevelAccessor level, MobSpawnType reason,
                                                 BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() >= surface && level.getBlockState(below).isValidSpawn(level, below, type);
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
        builder.define(DATA_SADDLED, false);
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

    /** Montaria da espécie; vazio se a espécie não pode ser montada. */
    public Optional<MountProfile> mountProfile() {
        return species().flatMap(Species::mount);
    }

    // ---- Sons ----

    private Optional<SoundProfile> soundProfile() {
        return species().flatMap(Species::sounds);
    }

    @Nullable
    private SoundEvent speciesSound(Function<SoundProfile, Optional<ResourceLocation>> which) {
        return soundProfile().flatMap(which).map(SoundProfile::event).orElse(null);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        // Inconsciente não faz barulho.
        return isUnconscious() ? null : speciesSound(SoundProfile::ambient);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return speciesSound(SoundProfile::hurt);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return speciesSound(SoundProfile::death);
    }

    @Override
    protected float getSoundVolume() {
        return soundProfile().map(SoundProfile::volume).orElse(1.0F);
    }

    /** O golpe em si, acerte ou não: toca o som de ataque. As subclasses somam a animação. */
    protected void swingAttack() {
        SoundEvent attack = speciesSound(SoundProfile::attack);
        if (attack != null) {
            playSound(attack, getSoundVolume(), getVoicePitch());
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!attackSwung) {
            swingAttack();
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean acquired = target != null && getTarget() == null;
        super.setTarget(target);
        if (acquired && !level().isClientSide && getTarget() == target) {
            SoundEvent alert = speciesSound(SoundProfile::alert);
            if (alert != null) {
                playSound(alert, getSoundVolume(), getVoicePitch());
            }
        }
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

        BodyProfile body = species.body().orElse(BodyProfile.DEFAULT);
        setBase(Attributes.KNOCKBACK_RESISTANCE, body.knockbackResistance());
        setBase(Attributes.STEP_HEIGHT, body.stepHeight());
        breaksLeaves = body.breaksLeaves();
        if (breaksLeaves) {
            // Sem isto o pathfinder contorna copas que a criatura consegue atravessar.
            setPathfindingMalus(PathType.LEAVES, 0.0F);
        }
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

    public void addTorpor(double amount) {
        addTorpor(amount, null);
    }

    /**
     * Aplica torpor vindo de um tranquilizante. Criaturas domesticadas são imunes.
     *
     * @param source quem aplicou; se este torpor derrubar a criatura, ela passa a ser dessa pessoa para domesticar
     */
    public void addTorpor(double amount, @Nullable Player source) {
        Optional<TamingProfile> profile = tamingProfile();
        if (level().isClientSide || amount <= 0 || isTame() || profile.isEmpty()) {
            return;
        }
        boolean wasConscious = !isUnconscious();
        setTorpor(torpor + amount * profile.get().torporMultiplier());
        if (wasConscious && isUnconscious() && source != null) {
            tamerUUID = source.getUUID();
        }
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
        ejectPassengers();
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
    }

    private void wakeUp() {
        entityData.set(DATA_UNCONSCIOUS, false);
        tamingSession.reset();
        entityData.set(DATA_TAMING_PROGRESS, 0.0F);
        tamerUUID = null;
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
        if (isUnconscious() && !isTame()) {
            eatFromInventory();
        }
        LivingEntity target = getTarget();
        if (isTame() && target != null
                && (!target.isAlive() || distanceToSqr(target) > TAMED_TARGET_LEASH * TAMED_TARGET_LEASH)) {
            setTarget(null);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && breaksLeaves && horizontalCollision && !isUnconscious()
                && EventHooks.canEntityGrief(level(), this)) {
            breakLeavesInTheWay();
        }
    }

    private void breakLeavesInTheWay() {
        AABB box = getBoundingBox().inflate(0.2);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (level().getBlockState(pos).getBlock() instanceof LeavesBlock) {
                level().destroyBlock(pos, true, this);
            }
        }
    }

    // ---- Manada ----

    /**
     * Defesa em grupo: faz as criaturas selvagens da mesma espécie por perto, que ainda não
     * tenham alvo, atacarem quem feriu esta. Uma varredura por agressão sofrida.
     */
    public void alertHerd(@Nullable LivingEntity attacker) {
        Optional<BehaviorProfile> behavior = behavior();
        if (attacker == null || isTame() || behavior.isEmpty() || !behavior.get().groupDefense()) {
            return;
        }
        double range = Math.max(behavior.get().herdRadius(), behavior.get().aggroRadius());
        for (PrehistoricCreature other : level().getEntitiesOfClass(
                PrehistoricCreature.class, getBoundingBox().inflate(range),
                candidate -> candidate != this && candidate.getType() == getType() && !candidate.isTame()
                        && !candidate.isUnconscious() && candidate.getTarget() == null)) {
            other.setTarget(attacker);
        }
    }

    // ---- Inventário ----

    public SimpleContainer inventory() {
        return inventory;
    }

    @Nullable
    public UUID tamerUUID() {
        return tamerUUID;
    }

    /**
     * Domesticada: só o dono. Selvagem: só enquanto inconsciente, e só quem a derrubou
     * (ou qualquer um, se ninguém a derrubou).
     */
    public boolean canAccessInventory(Player player) {
        if (!isAlive()) {
            return false;
        }
        if (isTame()) {
            return isOwner(player);
        }
        return isUnconscious() && (tamerUUID == null || tamerUUID.equals(player.getUUID()));
    }

    private void openInventory(Player player) {
        if (!canAccessInventory(player)) {
            player.displayClientMessage(Component.translatable("iceagesurvival.taming.not_yours"), true);
            return;
        }
        if (!isTame() && tamerUUID == null) {
            tamerUUID = player.getUUID();
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, opener) ->
                        new ChestMenu(MenuType.GENERIC_9x1, containerId, playerInventory, inventory, 1),
                getDisplayName()));
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (isSaddled()) {
            spawnAtLocation(new ItemStack(SADDLE_ITEM));
        }
        Containers.dropContents(level(), this, inventory);
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

    // ---- Montaria ----

    /** Se a espécie aceita sela; disponível também no cliente (vem dos dados sincronizados). */
    public boolean canBeSaddled() {
        return mountProfile().isPresent();
    }

    /** Disponível também no cliente. */
    public boolean isSaddled() {
        return entityData.get(DATA_SADDLED);
    }

    public void setSaddled(boolean saddled) {
        entityData.set(DATA_SADDLED, saddled);
        if (!saddled) {
            ejectPassengers();
        }
    }

    /**
     * Se este jogador pode montar agora. Exige ser o dono, a criatura selada, acordada e com
     * afinidade suficiente — uma criatura que acabou de ser domesticada ainda não deixa montar.
     */
    public boolean canBeRiddenBy(Player player) {
        Optional<MountProfile> mount = mountProfile();
        return mount.isPresent()
                && isSaddled()
                && isAlive()
                && !isUnconscious()
                && isOwner(player)
                && affinity >= mount.get().minAffinity();
    }

    /**
     * Coloca o jogador na criatura, se ela aceitar. Como no cavalo, quem monta de verdade é
     * o servidor; no cliente a chamada só confirma que a interação valeu.
     */
    public boolean ride(Player player) {
        if (!canBeRiddenBy(player) || isVehicle()) {
            return false;
        }
        if (level().isClientSide) {
            return true;
        }
        // Montar solta uma criatura que estava mandada ficar.
        if (!order().followsOwner()) {
            setOrder(CreatureOrder.FOLLOW);
        }
        getNavigation().stop();
        setTarget(null);
        player.setYRot(getYRot());
        player.setXRot(getXRot());
        return player.startRiding(this);
    }

    /**
     * Ataque de quem monta. A mordida sempre acontece — recarga, animação, som e, para as
     * espécies com {@code break_hardness}, os blocos à frente — e só depois se procura quem
     * morder: o alvo mirado, se estiver ao alcance, ou a primeira criatura na área da
     * mordida. Nunca o próprio dono nem as criaturas dele; o servidor confere tudo porque o
     * cliente só diz em quem mirou.
     *
     * @param target em quem quem monta mirou; nulo se a mira não pegou ninguém
     * @return se a mordida aconteceu (não se acertou alguém)
     */
    public boolean attackAsMount(Player rider, @Nullable LivingEntity target) {
        if (getControllingPassenger() != rider || !isAlive() || isUnconscious()
                || level().getGameTime() < nextRiderAttackTime) {
            return false;
        }
        nextRiderAttackTime = level().getGameTime() + RIDDEN_ATTACK_COOLDOWN;
        swingAttack();
        if (rider instanceof ServerPlayer serverRider) {
            breakBlocksInBite(serverRider);
        }
        LivingEntity victim = canBiteAsMount(rider, target) ? target : level()
                .getEntitiesOfClass(LivingEntity.class, biteArea(), candidate -> canBiteAsMount(rider, candidate))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (victim != null) {
            attackSwung = true;
            try {
                doHurtTarget(victim);
            } finally {
                attackSwung = false;
            }
        }
        return true;
    }

    private boolean canBiteAsMount(Player rider, @Nullable LivingEntity target) {
        if (target == null || target == this || target == rider || !target.isAlive() || hasPassenger(target)) {
            return false;
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable && rider.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        if (target instanceof Player other && !rider.canHarmPlayer(other)) {
            return false;
        }
        return getBoundingBox().inflate(RIDDEN_ATTACK_REACH).intersects(target.getBoundingBox());
    }

    /** O corpo esticado para a frente pelo alcance da mordida. */
    private AABB biteArea() {
        return getBoundingBox().expandTowards(Vec3.directionFromRotation(0.0F, getYRot()).scale(RIDDEN_ATTACK_REACH));
    }

    /**
     * Quebra os blocos na frente do corpo, do chão em que pisa até o topo da cabeça, com
     * dureza até o limite da espécie e, se ela tiver {@code break_blocks}, só os daquela tag.
     * Os blocos dropam como se quebrados à mão. Respeita {@code mobGriefing}, a proteção do spawn e
     * os eventos de quebra de bloco (mods de proteção de terreno), como se fosse quem monta
     * quebrando, e nunca quebra bloco com inventário.
     */
    private void breakBlocksInBite(ServerPlayer rider) {
        MountProfile mount = mountProfile().orElse(null);
        if (mount == null || mount.breakHardness() <= 0.0F || !EventHooks.canEntityGrief(level(), this)) {
            return;
        }
        float maxHardness = mount.breakHardness();
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        double halfWidth = getBbWidth() / 2.0;
        int minY = Mth.floor(getY() + 0.01);
        int maxY = Mth.floor(getY() + getBbHeight() - 0.01);
        java.util.Set<BlockPos> bitten = new java.util.LinkedHashSet<>();
        for (double depth = halfWidth + 0.5; depth <= halfWidth + BITE_DEPTH; depth += 1.0) {
            for (double lateral = -halfWidth; lateral <= halfWidth + 1.0E-3; lateral += Math.min(1.0, halfWidth)) {
                Vec3 column = position().add(forward.scale(depth)).add(side.scale(lateral));
                for (int y = minY; y <= maxY; y++) {
                    bitten.add(BlockPos.containing(column.x, y, column.z));
                }
            }
        }
        for (BlockPos pos : bitten) {
            var state = level().getBlockState(pos);
            float hardness = state.getDestroySpeed(level(), pos);
            if (state.isAir() || hardness < 0.0F || hardness > maxHardness || state.hasBlockEntity()
                    || mount.breakBlocks().isPresent() && !state.is(mount.breakBlocks().get())
                    || !level().mayInteract(rider, pos)
                    || CommonHooks.fireBlockBreak(level(), rider.gameMode.getGameModeForPlayer(), rider, pos, state).isCanceled()) {
                continue;
            }
            level().destroyBlock(pos, true, this);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (isSaddled() && !isUnconscious() && getFirstPassenger() instanceof Player player && isOwner(player)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float strafe = player.xxa * RIDDEN_STRAFE_FACTOR;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= RIDDEN_BACKWARD_FACTOR;
        }
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        double multiplier = mountProfile().map(MountProfile::speedMultiplier).orElse(1.0);
        return (float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * multiplier);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        // A criatura aponta para onde quem monta olha; o passo do pescoço é metade, como no cavalo.
        setRot(player.getYRot(), player.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
        getNavigation().stop();
        if (!isControlledByLocalInstance()) {
            return;
        }
        if (onGround()) {
            ridingJump = false;
            if (playerJumpPendingScale > 0.0F) {
                executeRidersJump(playerJumpPendingScale, travelVector);
            }
            playerJumpPendingScale = 0.0F;
        }
    }

    private void executeRidersJump(float scale, Vec3 travelVector) {
        double strength = mountProfile().map(MountProfile::jumpStrength).orElse(0.0) * scale * getBlockJumpFactor();
        if (strength <= 0.0) {
            return;
        }
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x, strength, movement.z);
        ridingJump = true;
        hasImpulse = true;
        CommonHooks.onLivingJump(this);
        if (travelVector.z > 0.0) {
            // Pulo para frente ganha um empurrão na direção em que a criatura olha.
            float sin = Mth.sin(getYRot() * (float) (Math.PI / 180.0));
            float cos = Mth.cos(getYRot() * (float) (Math.PI / 180.0));
            setDeltaMovement(getDeltaMovement().add(-0.4F * sin * scale, 0.0, 0.4F * cos * scale));
        }
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        if (!canJump()) {
            return;
        }
        playerJumpPendingScale = jumpPower >= 90 ? 1.0F : 0.4F + 0.4F * Math.max(jumpPower, 0) / 90.0F;
    }

    @Override
    public boolean canJump() {
        return isSaddled() && mountProfile().map(mount -> mount.jumpStrength() > 0).orElse(false);
    }

    @Override
    public void handleStartJump(int jumpPower) {
        playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
    }

    @Override
    public void handleStopJump() {
    }

    /** Onde quem monta se senta. Fica nos dados da espécie, junto do resto do corpo. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, mountProfile()
                .map(mount -> mount.seatHeight(dimensions.height()))
                .orElseGet(() -> (double) dimensions.height()), 0.0);
    }

    @Override
    public boolean isPushedByFluid() {
        // Com alguém montado, a correnteza não arrasta a criatura para fora do controle de quem monta.
        return !isVehicle() && super.isPushedByFluid();
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
            if (stack.is(ModItems.NARCOTIC)) {
                if (!level().isClientSide) {
                    usePlayerItem(player, hand, stack);
                    addTorpor(ServerConfig.NARCOTIC_TORPOR.get(), player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            // A comida vai no inventário; a criatura come sozinha no ritmo dela.
            if (!level().isClientSide) {
                openInventory(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isUnconscious() && isOwner(player)) {
            if (player.isSecondaryUseActive()) {
                if (!level().isClientSide) {
                    openInventory(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(SADDLE_ITEM) && canBeSaddled() && !isSaddled()) {
                if (!level().isClientSide) {
                    usePlayerItem(player, hand, stack);
                    setSaddled(true);
                    playSound(SoundEvents.HORSE_SADDLE, 0.5F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            Optional<TamingProfile.Food> food = tamingProfile().flatMap(p -> p.foodFor(stack));
            // Alimentar tem preferência; passada a fome, a mesma mão cheia de carne monta.
            if (food.isPresent() && level().getGameTime() >= nextFeedTime) {
                if (!level().isClientSide) {
                    feedTamed(player, hand, stack, food.get());
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (canBeSaddled()) {
                if (ride(player)) {
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
                if (!level().isClientSide) {
                    player.displayClientMessage(mountRefusal(), true);
                }
                return InteractionResult.sidedSuccess(false);
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Por que a criatura não deixou montar, para dizer a quem tentou. */
    private Component mountRefusal() {
        if (!isSaddled()) {
            return Component.translatable("iceagesurvival.mount.needs_saddle", getName());
        }
        float required = mountProfile().map(MountProfile::minAffinity).orElse(0.0F);
        if (affinity < required) {
            return Component.translatable("iceagesurvival.mount.needs_affinity", getName(), (int) required);
        }
        return Component.translatable("iceagesurvival.mount.occupied", getName());
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

    /** Inconsciente, come uma unidade do melhor alimento que houver no inventário, respeitando o intervalo. */
    private void eatFromInventory() {
        Optional<TamingProfile> profile = tamingProfile();
        long now = level().getGameTime();
        if (profile.isEmpty() || tamerUUID == null || now < nextFeedTime) {
            return;
        }
        int bestSlot = -1;
        TamingProfile.Food best = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            Optional<TamingProfile.Food> food = profile.get().foodFor(inventory.getItem(slot));
            if (food.isPresent() && (best == null || food.get().quality() > best.quality())) {
                bestSlot = slot;
                best = food.get();
            }
        }
        if (best == null) {
            return;
        }
        inventory.removeItem(bestSlot, 1);
        tamingSession.feed(best.value(), best.quality());
        nextFeedTime = now + profile.get().feedIntervalSeconds() * 20L;

        double required = requiredFood(profile.get());
        if (tamingSession.isComplete(required)) {
            completeTaming(tamerUUID);
        } else {
            entityData.set(DATA_TAMING_PROGRESS, (float) tamingSession.progress(required));
        }
    }

    /**
     * Domestica na hora, com a eficiência e os níveis bônus de uma domesticação perfeita e
     * afinidade cheia. Só para os comandos de teste — o caminho de jogo é torpor e alimento.
     */
    public void debugTame(Player owner) {
        if (level().isClientSide) {
            return;
        }
        tamingSession = new TamingSession();
        tamingProfile().ifPresent(profile -> tamingSession.feed(requiredFood(profile), 1.0));
        completeTaming(owner.getUUID());
        setAffinity(MAX_AFFINITY);
    }

    /**
     * Refaz os pontos de atributo da criatura num nível dado, como se ela tivesse nascido
     * nele. Usado pelos comandos de teste. (Não é {@code setLevel}, que no {@code Entity}
     * do vanilla troca o mundo da entidade.)
     */
    public void setCreatureLevel(int newLevel) {
        if (level().isClientSide) {
            return;
        }
        species().ifPresent(species -> setStatPoints(StatPoints.rollWild(newLevel, randomGenerator()), species));
        setHealth(getMaxHealth());
        setTorpor(Math.min(torpor, maxTorpor()));
    }

    private void completeTaming(UUID owner) {
        double effectiveness = tamingSession.effectiveness();
        int bonus = TamingRules.bonusPoints(
                statPoints.level(), ServerConfig.TAMING_BONUS_LEVEL_FRACTION.get(), effectiveness);
        species().ifPresent(species -> setStatPoints(statPoints.addRandom(bonus, randomGenerator()), species));
        affinity = (float) (effectiveness * MAX_INITIAL_AFFINITY);

        setOrder(DEFAULT_ORDER);
        Player player = level().getPlayerByUUID(owner);
        if (player != null) {
            tame(player);
        } else {
            // O dono pode ter saído do servidor enquanto a criatura comia.
            setTame(true, true);
            setOwnerUUID(owner);
        }
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
        compound.put(TAG_INVENTORY, inventory.createTag(registryAccess()));
        if (tamerUUID != null) {
            compound.putUUID(TAG_TAMER, tamerUUID);
        }
        compound.putBoolean(TAG_SADDLED, isSaddled());
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
        inventory.fromTag(compound.getList(TAG_INVENTORY, Tag.TAG_COMPOUND), registryAccess());
        tamerUUID = compound.hasUUID(TAG_TAMER) ? compound.getUUID(TAG_TAMER) : null;
        entityData.set(DATA_SADDLED, compound.getBoolean(TAG_SADDLED));
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
