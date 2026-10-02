package dev.madebyfelipe.iceagesurvival.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Armadilha de espetos: sem colisão, fere a cada segundo quem está em cima e o deixa lento. Poupa o dono, os aliados
 * e as domesticadas deles.
 */
public class SpikeTrapBlock extends OwnedDefenseBlock {
    public static final float DAMAGE = 3.0F;
    /** Um golpe por segundo em cada vítima, por mais espetos que ela pise. */
    public static final int INTERVAL_TICKS = 20;
    public static final int SLOWNESS_TICKS = 40;
    private static final String LAST_HIT = "iceagesurvival:spike_trap_hit";
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);

    public SpikeTrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()
                || spares(level, pos, entity)) {
            return;
        }
        long now = level.getGameTime();
        var data = living.getPersistentData();
        if (data.contains(LAST_HIT) && now - data.getLong(LAST_HIT) < INTERVAL_TICKS && now >= data.getLong(LAST_HIT)) {
            return;
        }
        data.putLong(LAST_HIT, now);
        living.hurt(DefenseBlocks.trapDamage(level), DAMAGE);
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 1));
    }
}
