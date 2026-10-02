package dev.madebyfelipe.iceagesurvival.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cobertura de folhagem por cima do fosso (que o jogador cava). Parece chão firme — e é, para qualquer jogador e para
 * as domesticadas do dono e dos aliados dele. Para toda outra criatura ou mob não tem colisão: ela afunda, a
 * cobertura se desfaz e ela cai no fosso.
 */
public class FoliageCoverBlock extends OwnedDefenseBlock {
    public FoliageCoverBlock(Properties properties) {
        super(properties);
    }

    /** Se a cobertura aguenta {@code entity}: jogador, ou domesticada do dono ou de um aliado. */
    public static boolean holds(BlockGetter level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) {
            return true;
        }
        return spares(level, pos, entity);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Sem entidade (o cálculo de caminho, inclusive): chão firme. A armadilha engana quem planeja a rota.
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null
                && !holds(level, pos, entityContext.getEntity())) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity.isAlive() && !holds(level, pos, entity)) {
            level.destroyBlock(pos, false, entity);
        }
    }
}
