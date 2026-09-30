package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.core.stats.StatProfile;
import dev.madebyfelipe.iceagesurvival.core.stats.WildLevels;
import dev.madebyfelipe.iceagesurvival.species.Species;
import java.util.Optional;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Base de toda criatura do mod. Guarda os pontos de atributo do indivíduo e os
 * aplica sobre a curva da {@link Species} correspondente ao tipo da entidade.
 *
 * <p>Dono e estado domesticado vêm de {@link TamableAnimal}.
 */
public abstract class PrehistoricCreature extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.INT);
    private static final String TAG_STAT_POINTS = "StatPoints";

    private StatPoints statPoints = StatPoints.NONE;
    private boolean statsRolled;

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
    }

    public Optional<Species> species() {
        return Species.of(level().registryAccess(), getType());
    }

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
        if (!level().isClientSide && !statsRolled) {
            rollWildStats();
        }
    }

    private void rollWildStats() {
        Optional<Species> species = species();
        if (species.isEmpty()) {
            IceAgeSurvival.LOGGER.warn("Sem definição de espécie para {}; atributos padrão mantidos",
                    EntityType.getKey(getType()));
            return;
        }
        RandomGenerator generator = random::nextLong;
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(TAG_STAT_POINTS, Tag.TAG_COMPOUND)) {
            CompoundTag saved = compound.getCompound(TAG_STAT_POINTS);
            StatPoints points = StatPoints.NONE;
            for (Stat stat : Stat.values()) {
                points = points.with(stat, Math.max(0, saved.getInt(stat.id())));
            }
            Optional<Species> species = species();
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
