package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.titan.TitanPhase;
import dev.madebyfelipe.iceagesurvival.effect.BleedingEffect;
import dev.madebyfelipe.iceagesurvival.entity.ai.ChaseGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.ReturnToLairGoal;
import dev.madebyfelipe.iceagesurvival.entity.ai.TitanChargeGoal;
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
 * O Titanovenator limiarensis, o apex tiranossaurídeo do Projeto Limiar e o boss do endgame (dossiê nº 017): um
 * Tyrannosaurinae de 15–17 m e 13–18 t, bem maior que o Rex (1,6×), que não chega ao topo da cadeia: reorganiza a
 * cadeia em torno de si. Modelo derivado do Rex do Revival em runtime ({@code client/titan}); animações autorais.
 *
 * <p>Três fases pela vida ({@link TitanPhase}):
 * <ol>
 *   <li>100–66%: territorial. Caça e morde; a mordida ancora, puxa e abre sangramento.</li>
 *   <li>abaixo de 66%: o rugido longo ({@code roar_phase}) empurra tudo para longe e ele passa a investir derrubando
 *   ({@link TitanChargeGoal}); 30% do dano não passa.</li>
 *   <li>abaixo de 33%: frenesi — outro rugido, mais rápido, investidas mais seguidas, a mordida corta mais fundo e a
 *   cauda varre quem o cerca; 50% do dano não passa.</li>
 * </ol>
 *
 * <p>Não é fauna: sem domesticação nem torpor, não foge, não some, não come, não disputa território. Ataca jogadores e
 * qualquer criatura (domesticada ou não) dentro do covil e fica preso a ele ({@link #setLair}): longe demais, larga o
 * alvo e volta ({@link ReturnToLairGoal}). A base militar chama o boss por {@link #summon}. Sem covil (ovo gerador,
 * {@code /summon}), vale tudo e a barra de boss aparece para quem está perto.
 */
public class TitanovenatorBoss extends LandCreature {
    /** Raio do covil em volta do ponto que a base marca: quem está dentro é alvo; o boss não persegue para fora. */
    public static final int DEFAULT_LAIR_RADIUS = 48;
    /** Até onde, além do covil, a barra de boss aparece. */
    private static final double BOSS_BAR_MARGIN = 8.0;
    /** Sem covil: a barra aparece para quem está a até esta distância. */
    private static final double BOSS_BAR_RADIUS_WITHOUT_LAIR = 64.0;
    private static final double CHASE_SPEED = 1.35;
    /** O rugido de troca de fase: parado, sem morder. Casa com a animação {@code roar_phase} (3 s). */
    public static final int ROAR_TICKS = 60;
    /** Em que ponto do rugido o peito solta o ar e a onda empurra quem está perto. */
    private static final int ROAR_BLAST_AT = ROAR_TICKS - 24;
    private static final double ROAR_BLAST_RADIUS = 16.0;
    private static final double ROAR_BLAST_PUSH = 1.0;
    /** A mordida ancora: puxa a vítima de volta para junto da boca, e um pouco para cima. */
    private static final double BITE_PULL = 0.45;
    private static final double BITE_LIFT = 0.25;
    /** A pancada de corpo (a cauda e o quadril): alcance, atraso do golpe depois da animação e espera. */
    private static final double SLAM_RADIUS = 8.0;
    private static final int SLAM_DELAY_TICKS = 12;
    private static final int SLAM_COOLDOWN_TICKS = 100;
    private static final float SLAM_DAMAGE_FACTOR = 0.6F;
    private static final double SLAM_KNOCK = 1.1;
    private static final double SLAM_KNOCK_UP = 0.45;
    /** A investida bate com esta fração do ataque. */
    private static final float CHARGE_DAMAGE_FACTOR = 0.75F;
    /** Empurrão da investida: para a frente e para cima, em blocos/tick. */
    private static final double CHARGE_KNOCK_FORWARD = 2.0;
    private static final double CHARGE_KNOCK_UP = 0.7;
    private static final int EXPERIENCE = 500;
    private static final UUID PHASE_SPEED_MODIFIER = UUID.fromString("5d3a8c1e-72b4-4f09-9e6a-0c1d2b3a4f57");

    private static final String TAG_LAIR = IceAgeSurvival.MODID + ".boss_lair";
    private static final String TAG_RADIUS = IceAgeSurvival.MODID + ".boss_lair_radius";
    private static final String TAG_PHASE = IceAgeSurvival.MODID + ".boss_phase";

    private final ServerBossEvent bossEvent = new ServerBossEvent(getType().getDescription(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private BlockPos lair;
    private int lairRadius = DEFAULT_LAIR_RADIUS;
    /** A fase mais alta já alcançada: cada troca de fase ruge uma vez só. */
    private int reachedPhase = 1;
    /** A fase cuja velocidade está aplicada; o modificador não vai no save. */
    private int speedPhase;
    private int roarTicks;
    private int chargeCooldown;
    private int slamCooldown;
    private int slamIn;

    public TitanovenatorBoss(EntityType<? extends LandCreature> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /**
     * Chama o boss no covil: nasce no ponto dado e fica preso a ele. É o gancho da base militar; sem base, vale um
     * {@code /summon}.
     */
    @Nullable
    public static TitanovenatorBoss summon(ServerLevel level, BlockPos lair, int radius) {
        EntityType<?> type = dev.madebyfelipe.iceagesurvival.registry.ModEntities.TITANOVENATOR.get();
        if (!(type.create(level) instanceof TitanovenatorBoss boss)) {
            return null;
        }
        boss.moveTo(lair.getX() + 0.5, lair.getY(), lair.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        boss.setLair(lair, radius);
        level.addFreshEntity(boss);
        return boss;
    }

    // ---- Animações ----

    @Override
    protected boolean hasPhaseAnimations() {
        return true;
    }

    @Override
    protected String animationSuffix() {
        return phase().animationSuffix();
    }

    // ---- IA ----

    /** Nada da fauna: sem caça, fuga, cautela, território, rivalidade, carniça nem ordens. */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ReturnToLairGoal(this, CHASE_SPEED));
        goalSelector.addGoal(2, new TitanChargeGoal(this));
        goalSelector.addGoal(3, new ChaseGoal(this, CHASE_SPEED));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::inLair));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                this::isLairFoe));
    }

    /** Criatura (do mod, ou bicho domesticado de qualquer mod) dentro do covil, que não seja outro boss. */
    private boolean isLairFoe(LivingEntity other) {
        boolean creature = other instanceof PrehistoricCreature prehistoric && !(other instanceof TitanovenatorBoss)
                && !prehistoric.isUnconscious() && !prehistoric.isCorpse();
        boolean pet = !(other instanceof PrehistoricCreature) && other instanceof OwnableEntity ownable
                && ownable.getOwnerUUID() != null;
        return (creature || pet) && inLair(other);
    }

    /** Dentro do raio do covil (sem covil, vale tudo). */
    public boolean inLair(Entity entity) {
        return lair == null || entity.distanceToSqr(Vec3.atBottomCenterOf(lair)) <= (double) lairRadius * lairRadius;
    }

    // ---- Covil ----

    /** Liga o boss ao covil: é para lá que ele volta. */
    public void setLair(@Nullable BlockPos pos, int radius) {
        lair = pos == null ? null : pos.immutable();
        lairRadius = Math.max(8, radius);
        applyLeash();
    }

    @Nullable
    public BlockPos lair() {
        return lair;
    }

    public int lairRadius() {
        return lairRadius;
    }

    /** Distância ao quadrado até o covil, ou 0 sem covil. */
    public double distanceToLairSqr() {
        return lair == null ? 0.0 : distanceToSqr(Vec3.atBottomCenterOf(lair));
    }

    private void applyLeash() {
        if (lair != null) {
            restrictTo(lair, lairRadius);
        } else {
            clearRestriction();
        }
    }

    /** Puxado longe demais e sem conseguir voltar andando: reaparece no covil. */
    public void teleportToLair() {
        if (lair == null || !(level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 4, getZ(), 40,
                3.0, 3.0, 3.0, 0.02);
        getNavigation().stop();
        setTarget(null);
        teleportTo(lair.getX() + 0.5, lair.getY(), lair.getZ() + 0.5);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 4, getZ(), 40,
                3.0, 3.0, 3.0, 0.02);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            // O applyBehavior da espécie limpa a restrição (sem território): o covil a põe de volta.
            applyLeash();
        }
    }

    // ---- Fases ----

    /** A fase pela vida de agora; vale também no cliente. */
    public TitanPhase phase() {
        return TitanPhase.of(getHealth() / getMaxHealth());
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
        TitanPhase phase = phase();
        if (phase.number() > reachedPhase) {
            enterPhase(phase);
        }
        if (speedPhase != phase.number()) {
            applyPhaseSpeed(phase);
        }
        if (roarTicks > 0) {
            roarTicks--;
            getNavigation().stop();
            if (roarTicks == ROAR_BLAST_AT) {
                roarBlast();
            }
        }
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        tickSlam(phase);
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(TitanPhase.REGEN_PER_SECOND);
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 == 0) {
            updateBossBarViewers();
        }
    }

    private void enterPhase(TitanPhase phase) {
        reachedPhase = phase.number();
        bossEvent.setColor(phase == TitanPhase.THREE ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW);
        if (phase == TitanPhase.THREE) {
            bossEvent.setDarkenScreen(true);
        }
        roar();
    }

    /** O rugido de troca de fase: parado por {@link #ROAR_TICKS}, sem morder, com a onda no meio. */
    private void roar() {
        roarTicks = ROAR_TICKS;
        chargeCooldown = ROAR_TICKS;
        slamIn = 0;
        getNavigation().stop();
        playAlert();
        startAction(CreatureAction.ROAR, ROAR_TICKS);
        gesture("roar_phase");
    }

    /** O peito solta o ar: tudo o que está perto é empurrado para longe, sem dano (dossiê §16: sentir a vibração). */
    private void roarBlast() {
        playSound(SoundEvents.RAVAGER_ROAR, 6.0F, 0.5F);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 60,
                    4.0, 1.0, 4.0, 0.05);
        }
        for (LivingEntity other : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(ROAR_BLAST_RADIUS), other -> other != this && other.isAlive()
                        && !(other instanceof Player player && !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)))) {
            Vec3 away = new Vec3(other.getX() - getX(), 0.0, other.getZ() - getZ());
            if (away.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 push = away.normalize().scale(ROAR_BLAST_PUSH);
            other.setDeltaMovement(other.getDeltaMovement().add(push.x, 0.35, push.z));
            other.hurtMarked = true;
        }
    }

    private void applyPhaseSpeed(TitanPhase phase) {
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

    /**
     * A mordida: dentes grossos que ancoram. O que acerta sangra (mais fundo na fase 3) e é puxado de volta para junto
     * da boca — a tração cervical do dossiê §7. Rugindo, não morde.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (isRoaring()) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            TitanPhase phase = phase();
            BleedingEffect.cut(living, this, phase.bleedLevelsPerBite(), phase.bleedLevelCap(), phase.bleedTicks());
            Vec3 toward = new Vec3(getX() - living.getX(), 0.0, getZ() - living.getZ());
            if (toward.lengthSqr() > 1.0E-4) {
                Vec3 pull = toward.normalize().scale(BITE_PULL);
                living.setDeltaMovement(pull.x, BITE_LIFT, pull.z);
                living.hurtMarked = true;
            }
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

    /**
     * A pancada de corpo (dossiê §7, impacto; §12, a cauda): cercado por dois ou mais inimigos, ou em frenesi com um
     * deles colado, gira o quadril e varre a cauda. O golpe sai no meio da animação {@code attack_2}.
     */
    private void tickSlam(TitanPhase phase) {
        if (slamCooldown > 0) {
            slamCooldown--;
        }
        if (slamIn > 0) {
            if (--slamIn == 0) {
                slam();
            }
            return;
        }
        if (slamCooldown > 0 || isRoaring() || tickCount % 10 != 0 || getTarget() == null) {
            return;
        }
        int foes = slamFoes().size();
        if (foes >= 2 || phase == TitanPhase.THREE && foes >= 1) {
            slamIn = SLAM_DELAY_TICKS;
            slamCooldown = SLAM_COOLDOWN_TICKS;
            gesture("attack_2");
        }
    }

    private List<LivingEntity> slamFoes() {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLAM_RADIUS, 1.0, SLAM_RADIUS),
                other -> other != this && other.isAlive() && (other instanceof Player player
                        && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && inLair(player) || isLairFoe(other)));
    }

    private void slam() {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SLAM_DAMAGE_FACTOR;
        playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
        for (LivingEntity victim : slamFoes()) {
            victim.hurt(damageSources().mobAttack(this), damage);
            Vec3 away = new Vec3(victim.getX() - getX(), 0.0, victim.getZ() - getZ());
            if (away.lengthSqr() > 1.0E-4) {
                Vec3 push = away.normalize().scale(SLAM_KNOCK);
                victim.setDeltaMovement(push.x, SLAM_KNOCK_UP, push.z);
                victim.hurtMarked = true;
            }
        }
    }

    /**
     * O couro e a resistência da fase ({@link TitanPhase#absorb}); o {@code /kill} passa inteiro. Sem a pausa de
     * invulnerabilidade do vanilla depois do golpe: numa tropa, a mordida de cada um conta, não só a primeira de cada
     * meio segundo.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount = phase().absorb(amount);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            invulnerableTime = Math.min(invulnerableTime, 10);
        }
        return hurt;
    }

    /** O boss não sangra pelo próprio corte. */
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
        if (lair == null) {
            return player.distanceToSqr(this) <= BOSS_BAR_RADIUS_WITHOUT_LAIR * BOSS_BAR_RADIUS_WITHOUT_LAIR;
        }
        double radius = lairRadius + BOSS_BAR_MARGIN;
        return player.distanceToSqr(Vec3.atBottomCenterOf(lair)) <= radius * radius;
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
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_DEATH, SoundSource.HOSTILE, 6.0F, 0.4F);
        }
    }

    // ---- Save ----

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (lair != null) {
            compound.putLong(TAG_LAIR, lair.asLong());
        }
        compound.putInt(TAG_RADIUS, lairRadius);
        compound.putInt(TAG_PHASE, reachedPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        lair = compound.contains(TAG_LAIR) ? BlockPos.of(compound.getLong(TAG_LAIR)) : null;
        if (compound.contains(TAG_RADIUS)) {
            lairRadius = Math.max(8, compound.getInt(TAG_RADIUS));
        }
        reachedPhase = Math.max(1, compound.getInt(TAG_PHASE));
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
        applyLeash();
    }
}
