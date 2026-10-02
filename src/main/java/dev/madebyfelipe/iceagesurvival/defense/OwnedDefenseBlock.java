package dev.madebyfelipe.iceagesurvival.defense;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Defesa com dono (armadilhas, espinhos, cobertura): quem colocou fica na block entity. */
public abstract class OwnedDefenseBlock extends BaseEntityBlock implements DefenseBlock {
    protected OwnedDefenseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DefenseOwnerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof DefenseOwnerBlockEntity owned) {
            owned.setOwner(player);
        }
    }

    /** A block entity do dono, se houver. */
    @Nullable
    protected static DefenseOwnerBlockEntity owned(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DefenseOwnerBlockEntity owned ? owned : null;
    }

    /** Se a armadilha poupa {@code entity}: dono, aliado ou domesticada de um deles. */
    protected static boolean spares(BlockGetter level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        DefenseOwnerBlockEntity owned = owned(level, pos);
        return owned != null && owned.isFriend(entity);
    }
}
