package dev.madebyfelipe.iceagesurvival.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Paliçada de espinhos: bloco de estacas e espinhos. Como o cacto, a colisão é um pouco menor que o bloco, e quem
 * encosta leva o dano e é empurrado para trás. Poupa o dono, os aliados e as domesticadas deles.
 */
public class ThornPalisadeBlock extends OwnedDefenseBlock {
    public static final float DAMAGE = 2.0F;
    public static final double KNOCKBACK = 0.6;
    private static final VoxelShape COLLISION = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    public ThornPalisadeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos,
            @javax.annotation.Nullable net.minecraft.world.entity.Mob mob) {
        return BlockPathTypes.DAMAGE_OTHER;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()
                || spares(level, pos, entity)) {
            return;
        }
        if (living.hurt(DefenseBlocks.trapDamage(level), DAMAGE)) {
            // Para longe do centro do bloco, na horizontal; o knockback empurra no sentido oposto ao vetor dado.
            Vec3 away = living.position().subtract(Vec3.atCenterOf(pos)).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-4) {
                away = Vec3.directionFromRotation(0.0F, living.getYRot()).scale(-1.0);
            }
            living.knockback(KNOCKBACK, -away.x, -away.z);
        }
    }
}
