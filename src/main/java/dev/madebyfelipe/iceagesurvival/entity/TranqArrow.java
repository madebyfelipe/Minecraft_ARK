package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Flecha de dano baixo que aplica torpor em criaturas do mod. */
public class TranqArrow extends AbstractArrow {
    private static final double BASE_DAMAGE = 0.5;
    /** Velocidade de uma flecha disparada de um arco totalmente puxado. */
    private static final double FULL_DRAW_SPEED = 3.0;

    /** Bônus de torpor por nível de Força, igual ao do dano clássico do arco (+25% por nível, +25% fixo). */
    private static final double POWER_BONUS_PER_LEVEL = 0.25;
    private static final String WEAPON_TAG = "Weapon";
    private static final String FIXED_TORPOR_TAG = "FixedTorpor";
    private static final String PICKUP_TAG = "Pickup";

    private double speedAtImpact;
    private ItemStack weapon = ItemStack.EMPTY;
    /** Torpor fixo de arma de fogo (rifle, besta de dardos); 0 = o do arco, pela velocidade do impacto. */
    private double fixedTorpor;
    /** O que se recolhe do chão: a flecha tranquilizante, ou o dardo. */
    private ItemStack pickupStack = ItemStack.EMPTY;

    public TranqArrow(EntityType<? extends TranqArrow> type, Level level) {
        super(type, level);
        this.weapon = ItemStack.EMPTY;
        setBaseDamage(BASE_DAMAGE);
    }

    public TranqArrow(Level level, LivingEntity owner, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.TRANQ_ARROW.get(), owner, level);
        this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
        setBaseDamage(BASE_DAMAGE);
    }

    /** Disparo de arma tranquilizante: torpor fixo, recolhe-se a munição que foi usada. */
    public TranqArrow(Level level, LivingEntity owner, ItemStack ammo, double fixedTorpor) {
        super(ModEntities.TRANQ_ARROW.get(), owner, level);
        this.fixedTorpor = fixedTorpor;
        this.pickupStack = ammo.copyWithCount(1);
        setBaseDamage(BASE_DAMAGE);
    }

    public double fixedTorpor() {
        return fixedTorpor;
    }

    public TranqArrow(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.TRANQ_ARROW.get(), x, y, z, level);
        this.weapon = weapon == null ? ItemStack.EMPTY : weapon.copy();
        setBaseDamage(BASE_DAMAGE);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        speedAtImpact = getDeltaMovement().length();
        super.onHitEntity(result);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (target instanceof PrehistoricCreature creature && !creature.isApex()) {
            // O apex não cai com tranquilizante: só vencendo o desafio.
            double torpor = fixedTorpor > 0.0 ? fixedTorpor
                    : ServerConfig.TRANQ_ARROW_TORPOR.get() * speedAtImpact / FULL_DRAW_SPEED * powerMultiplier();
            creature.addTorpor(torpor, getOwner() instanceof Player player ? player : null);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!weapon.isEmpty()) {
            tag.put(WEAPON_TAG, weapon.save(new CompoundTag()));
        }
        if (fixedTorpor > 0.0) {
            tag.putDouble(FIXED_TORPOR_TAG, fixedTorpor);
        }
        if (!pickupStack.isEmpty()) {
            tag.put(PICKUP_TAG, pickupStack.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        weapon = tag.contains(WEAPON_TAG, CompoundTag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(WEAPON_TAG))
                : ItemStack.EMPTY;
        fixedTorpor = tag.getDouble(FIXED_TORPOR_TAG);
        pickupStack = tag.contains(PICKUP_TAG, CompoundTag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound(PICKUP_TAG))
                : ItemStack.EMPTY;
    }

    /** Multiplicador de torpor pela Força do arco que disparou; 1 sem encantamento ou sem arco. */
    private double powerMultiplier() {
        if (weapon.isEmpty()) {
            return 1.0;
        }
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, weapon);
        return enchantmentLevel > 0
                ? 1.0 + POWER_BONUS_PER_LEVEL * (enchantmentLevel + 1)
                : 1.0;
    }

    @Override
    protected ItemStack getPickupItem() {
        if (!pickupStack.isEmpty()) {
            return pickupStack.copy();
        }
        return new ItemStack(ModItems.TRANQ_ARROW.get());
    }
}
