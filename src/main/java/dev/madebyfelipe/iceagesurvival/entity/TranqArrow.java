package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Flecha de dano baixo que aplica torpor em criaturas do mod. */
public class TranqArrow extends AbstractArrow {
    private static final double BASE_DAMAGE = 0.5;
    /** Velocidade de uma flecha disparada de um arco totalmente puxado. */
    private static final double FULL_DRAW_SPEED = 3.0;

    private double speedAtImpact;

    public TranqArrow(EntityType<? extends TranqArrow> type, Level level) {
        super(type, level);
        setBaseDamage(BASE_DAMAGE);
    }

    public TranqArrow(Level level, LivingEntity owner, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.TRANQ_ARROW.get(), owner, level, pickupItem, weapon);
        setBaseDamage(BASE_DAMAGE);
    }

    public TranqArrow(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {
        super(ModEntities.TRANQ_ARROW.get(), x, y, z, level, pickupItem, weapon);
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
        if (target instanceof PrehistoricCreature creature) {
            creature.addTorpor(ServerConfig.TRANQ_ARROW_TORPOR.get() * speedAtImpact / FULL_DRAW_SPEED,
                    getOwner() instanceof Player player ? player : null);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.TRANQ_ARROW.get());
    }
}
