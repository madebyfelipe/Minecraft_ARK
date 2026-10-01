package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TranqArrowItem extends ArrowItem {
    public TranqArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter) {
        return new TranqArrow(level, shooter, ammo.copyWithCount(1), shooter.getUseItem());
    }
}
