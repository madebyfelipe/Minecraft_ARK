package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.effect.BleedingEffect;
import dev.madebyfelipe.iceagesurvival.endgame.ArenaAltarBlock;
import dev.madebyfelipe.iceagesurvival.endgame.BossChargeGoal;
import dev.madebyfelipe.iceagesurvival.endgame.BossPhase;
import dev.madebyfelipe.iceagesurvival.endgame.ReturnToAltarGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.ChaseGoal;
import dev.madebyfelipe.iceagesurvival.registry.ModEffects;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O boss da Etapa 10 (D47): o Giganotosaurus da arena da caverna, chamado pelo tributo no altar
 * ({@link ArenaAltarBlock}). O "sangrador": dentes de faca, mordida fraca para o tamanho (um terço da do T-Rex) e
 * presa que morre pela ferida — caçava titanossauros cortando, não esmagando.
 *
 * <p>Três fases pela vida ({@link BossPhase}):
 * <ol>
 *   <li>100–50%: caça e corta — a mordida abre e empilha sangramento ({@link BleedingEffect#cut}).</li>
 *   <li>abaixo de 50%: ruge (a pose de rugido do Jurassic Reborn), puxa quem está perto para junto dele e passa a
 *   investir derrubando ({@link BossChargeGoal}); 30% do dano não passa.</li>
 *   <li>abaixo de 25%: ferido e frenético — a pose de ferido, mais rápido, investidas mais seguidas e a mordida corta
 *   mais fundo; 50% do dano não passa.</li>
 * </ol>
 *
 * <p>Não é fauna: sem domesticação nem torpor, não foge, não some, não come, não disputa território nem fêmea. Ataca
 * jogadores e qualquer criatura (domesticada ou não) dentro da arena e fica preso a ela: longe demais do altar, larga
 * o alvo e volta ({@link ReturnToAltarGoal}). A barra de boss aparece para quem está na arena.
 */
public class GiganotosaurusBoss extends LandCreature {
    /** Raio da arena em volta do altar: quem está dentro é alvo; o boss não persegue para fora. */
    public static final int DEFAULT_ARENA_RADIUS = 40;
    /** Até onde, além da arena, a barra de boss aparece. */
    private static final double BOSS_BAR_MARGIN = 8.0;
    /** Sem altar (ovo gerador, comando): a barra aparece para quem está a até esta distância. */
    private static final double BOSS_BAR_RADIUS_WITHOUT_ALTAR = 48.0;
    private static final double CHASE_SPEED = 1.35;
    /** O rugido da passagem para a fase 2: parado, sem morder. */
    public static final int ROAR_TICKS = 50;
    /** A pose de ferido da passagem para a fase 3. */
    private static final int FRENZY_TICKS = 30;
    /** O rugido puxa quem está até esta distância. */
    private static final double GATHER_RADIUS = 20.0;
    /** Força do puxão do rugido (velocidade horizontal somada, em blocos/tick). */
    private static final double GATHER_PULL = 0.9;
    /** A investida bate com esta fração do ataque. */
    private static final float CHARGE_DAMAGE_FACTOR = 0.75F;
    /** Empurrão da investida: para a frente (na direção da corrida) e para cima, em blocos/tick. */
    private static final double CHARGE_KNOCK_FORWARD = 1.8;
    private static final double CHARGE_KNOCK_UP = 0.6;
    private static final int EXPERIENCE = 250;
    private static final UUID PHASE_SPEED_MODIFIER = UUID.fromString("9b2f1d7e-3c4a-4e8b-a6f1-5d0c2e7b8a93");

    private static final String TAG_ALTAR = IceAgeSurvival.MODID + ".boss_altar";
    private static final String TAG_ARENA = IceAgeSurvival.MODID + ".boss_arena";
    private static final String TAG_PHASE = IceAgeSurvival.MODID + ".boss_phase";

    private final ServerBossEvent bossEvent = new ServerBossEvent(getType().getDescription(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private BlockPos altar;
    private int arenaRadius = DEFAULT_ARENA_RADIUS;
    /** A fase mais alta já alcançada (o rugido e a pose de ferido saem uma vez só). */
    private int reachedPhase = 1;
    /** A fase cuja velocidade está aplicada; o modificador não vai no save. */
    private int speedPhase;
    private int roarTicks;
    private boolean pendingFrenzy;
    private int chargeCooldown;

    public GiganotosaurusBoss(EntityType<? extends LandCreature> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    // ---- IA ----

    /** Nada da fauna: sem caça, fuga, cautela, território, rivalidade, carniça nem ordens. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ReturnToAltarGoal(this, CHASE_SPEED));
        goalSelector.addGoal(2, new BossChargeGoal(this));
        goalSelector.addGoal(3, new ChaseGoal(this, CHASE_SPEED));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::inArena));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                this::isArenaFoe));
    }

    /** Criatura (do mod, ou bicho domesticado de qualquer mod) dentro da arena, que não seja outro boss. */
    private boolean isArenaFoe(LivingEntity other) {
        boolean creature = other instanceof PrehistoricCreature prehistoric && !(other instanceof GiganotosaurusBoss)
                && !prehistoric.isUnconscious() && !prehistoric.isCorpse();
        boolean pet = !(other instanceof PrehistoricCreature) && other instanceof OwnableEntity ownable
                && ownable.getOwnerUUID() != null;
        return (creature || pet) && inArena(other);
    }

    /** Dentro do raio da arena (sem altar, vale tudo). */
    public boolean inArena(Entity entity) {
        return altar == null || entity.distanceToSqr(Vec3.atBottomCenterOf(altar)) <= (double) arenaRadius * arenaRadius;
    }

    // ---- Altar e arena ----

    /** Liga o boss ao altar que o chamou: é para lá que ele volta. */
    public void setAltar(@Nullable BlockPos pos) {
        altar = pos == null ? null : pos.immutable();
        applyLeash();
    }

    @Nullable
    public BlockPos altar() {
        return altar;
    }

    public int arenaRadius() {
        return arenaRadius;
    }

    /** Raio da arena em volta do altar (a frente da caverna pode ajustar ao tamanho da sala). */
    public void setArenaRadius(int radius) {
        arenaRadius = Math.max(4, radius);
        applyLeash();
    }

    /** Distância ao quadrado até o altar, ou 0 sem altar. */
    public double distanceToAltarSqr() {
        return altar == null ? 0.0 : distanceToSqr(Vec3.atBottomCenterOf(altar));
    }

    private void applyLeash() {
        if (altar != null) {
            restrictTo(altar, arenaRadius);
        } else {
            clearRestriction();
        }
    }

    /** Puxado longe demais e sem conseguir voltar andando: reaparece ao lado do altar. */
    public void teleportToAltar() {
        if (altar == null || !(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 spot = ArenaAltarBlock.spawnSpot(level, altar, this);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 30,
                1.5, 1.5, 1.5, 0.02);
        getNavigation().stop();
        setTarget(null);
        teleportTo(spot.x, spot.y, spot.z);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, spot.x, spot.y + 2, spot.z, 30,
                1.5, 1.5, 1.5, 0.02);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            // O applyBehavior da espécie limpa a restrição (sem território): o altar a põe de volta.
            applyLeash();
        }
    }

    // ---- Fases ----

    /** A fase pela vida de agora; vale também no cliente. */
    public BossPhase phase() {
        return BossPhase.of(getHealth() / getMaxHealth());
    }

    public boolean isRoaring() {
        return roarTicks > 0;
    }

    /** Rugindo, fica plantado no lugar. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || roarTicks > 0;
    }

    /** Pronto para outra investida: na fase que investe, sem rugir, passada a espera. */
    public boolean chargeReady() {
        return phase().charges() && !isRoaring() && chargeCooldown <= 0;
    }

    public void startChargeCooldown() {
        chargeCooldown = Math.max(phase().chargeCooldownTicks(), 20);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        BossPhase phase = phase();
        if (phase.number() > reachedPhase) {
            enterPhase(phase);
        }
        if (speedPhase != phase.number()) {
            applyPhaseSpeed(phase);
        }
        if (roarTicks > 0) {
            roarTicks--;
            getNavigation().stop();
            if (roarTicks == 0 && pendingFrenzy) {
                pendingFrenzy = false;
                frenzy();
            }
        }
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 == 0) {
            updateBossBarViewers();
        }
    }

    private void enterPhase(BossPhase phase) {
        boolean roars = reachedPhase < 2;
        reachedPhase = phase.number();
        bossEvent.setColor(phase == BossPhase.THREE ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW);
        if (roars) {
            roar();
            // Um golpe que pulou direto para a fase 3: a pose de ferido sai quando o rugido acaba.
            pendingFrenzy = phase == BossPhase.THREE;
        } else if (phase == BossPhase.THREE) {
            frenzy();
        }
        if (phase == BossPhase.THREE) {
            bossEvent.setDarkenScreen(true);
        }
    }

    /**
     * Fase 2: o rugido. Fica parado {@link #ROAR_TICKS}, puxa quem está a até {@link #GATHER_RADIUS} blocos para
     * junto dele (jogadores e as criaturas que lutam na arena) e, acabado o rugido, investe.
     */
    private void roar() {
        roarTicks = ROAR_TICKS;
        chargeCooldown = ROAR_TICKS;
        getNavigation().stop();
        playAlert();
        startAction(CreatureAction.ROAR, ROAR_TICKS);
        Vec3 center = position();
        for (LivingEntity other : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(GATHER_RADIUS),
                other -> other != this && other.isAlive() && isGatherable(other))) {
            Vec3 toward = new Vec3(center.x - other.getX(), 0.0, center.z - other.getZ());
            if (toward.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 pull = toward.normalize().scale(GATHER_PULL);
            other.setDeltaMovement(other.getDeltaMovement().add(pull.x, 0.3, pull.z));
            other.hurtMarked = true;
        }
    }

    private boolean isGatherable(LivingEntity other) {
        if (other instanceof Player player) {
            return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && inArena(player);
        }
        return isArenaFoe(other) && !(other instanceof PrehistoricCreature creature && creature.isUnconscious());
    }

    /** Fase 3: ferido e frenético — a pose de ferido e o grito. */
    private void frenzy() {
        startAction(CreatureAction.INJURED, FRENZY_TICKS);
        playSound(SoundEvents.RAVAGER_STUNNED, 3.0F, 0.6F);
    }

    private void applyPhaseSpeed(BossPhase phase) {
        speedPhase = phase.number();
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(PHASE_SPEED_MODIFIER);
        if (phase.speedBonus() > 0.0) {
            speed.addTransientModifier(new AttributeModifier(PHASE_SPEED_MODIFIER, "boss_phase", phase.speedBonus(),
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ---- Golpes ----

    /** A mordida que corta: o que acerta sangra, mais fundo na fase 3. Rugindo, não morde. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (isRoaring()) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            BossPhase phase = phase();
            BleedingEffect.cut(living, this, phase.bleedLevelsPerBite(), phase.bleedLevelCap(), phase.bleedTicks());
        }
        return hit;
    }

    /** A investida acertou: bate com parte do ataque e derruba, jogando para a frente e para cima. */
    public void chargeHit(LivingEntity victim, Vec3 direction) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * CHARGE_DAMAGE_FACTOR;
        victim.hurt(damageSources().mobAttack(this), damage);
        Vec3 away = new Vec3(victim.getX() - getX(), 0.0, victim.getZ() - getZ());
        Vec3 side = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.6);
        victim.setDeltaMovement(direction.x * CHARGE_KNOCK_FORWARD + side.x, CHARGE_KNOCK_UP,
                direction.z * CHARGE_KNOCK_FORWARD + side.z);
        victim.hurtMarked = true;
        victim.hasImpulse = true;
    }

    /** Resistência por fase: 0 / 30% / 50% do dano não passa. O {@code /kill} passa inteiro. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount = phase().reduce(amount);
        }
        return super.hurt(source, amount);
    }

    /** O sangrador não sangra pelo próprio corte. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != ModEffects.BLEEDING.get() && super.canBeAffected(effect);
    }

    // ---- O que ele não é ----

    /** Não se doma: o torpor não pega. */
    @Override
    public void addTorpor(double amount, @Nullable Player source) {
    }

    @Override
    public void setTorpor(double value) {
        super.setTorpor(0.0);
    }

    @Override
    public boolean countsForGroupSpacing() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    /** Sempre adulto. */
    @Override
    public void setAge(int age) {
        super.setAge(Math.max(0, age));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public int getExperienceReward() {
        return EXPERIENCE;
    }

    // ---- Barra de boss ----

    private void updateBossBarViewers() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Set<ServerPlayer> inside = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && seesBossBar(player)) {
                inside.add(player);
            }
        }
        for (ServerPlayer player : List.copyOf(bossEvent.getPlayers())) {
            if (!inside.contains(player)) {
                bossEvent.removePlayer(player);
            }
        }
        inside.forEach(bossEvent::addPlayer);
    }

    private boolean seesBossBar(ServerPlayer player) {
        if (altar == null) {
            return player.distanceToSqr(this) <= BOSS_BAR_RADIUS_WITHOUT_ALTAR * BOSS_BAR_RADIUS_WITHOUT_ALTAR;
        }
        double radius = arenaRadius + BOSS_BAR_MARGIN;
        return player.distanceToSqr(Vec3.atBottomCenterOf(altar)) <= radius * radius;
    }

    /** Quem vê a barra agora (para os testes). */
    public boolean showsBossBarTo(ServerPlayer player) {
        return bossEvent.getPlayers().contains(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void remove(RemovalReason reason) {
        bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) {
            bossEvent.setProgress(0.0F);
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_DEATH, SoundSource.HOSTILE, 4.0F, 0.5F);
        }
    }

    // ---- Save ----

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (altar != null) {
            compound.putLong(TAG_ALTAR, altar.asLong());
        }
        compound.putInt(TAG_ARENA, arenaRadius);
        compound.putInt(TAG_PHASE, reachedPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        altar = compound.contains(TAG_ALTAR) ? BlockPos.of(compound.getLong(TAG_ALTAR)) : null;
        if (compound.contains(TAG_ARENA)) {
            arenaRadius = Math.max(4, compound.getInt(TAG_ARENA));
        }
        reachedPhase = Math.max(1, compound.getInt(TAG_PHASE));
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
        applyLeash();
    }
}
