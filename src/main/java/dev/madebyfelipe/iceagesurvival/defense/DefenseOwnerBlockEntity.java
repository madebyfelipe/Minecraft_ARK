package dev.madebyfelipe.iceagesurvival.defense;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O dono de uma defesa: quem a colocou (UUID e nome, o nome para achar o time dele mesmo offline). Vai também para
 * o cliente, que precisa dele para a colisão da cobertura de folhagem com a montaria que ele conduz.
 */
public class DefenseOwnerBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";

    public DefenseOwnerBlockEntity(BlockPos pos, BlockState state) {
        this(DefenseBlocks.OWNER_ENTITY.get(), pos, state);
    }

    protected DefenseOwnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Dono ou aliado dele (jogador). */
    public boolean isOwnerOrAlly(Entity entity) {
        return level != null && DefenseOwnership.isOwnerOrAlly(level, owner, ownerName, entity);
    }

    /** Dono, aliado ou domesticada de um deles: a armadilha poupa. */
    public boolean isFriend(Entity entity) {
        return level != null && DefenseOwnership.isFriend(level, owner, ownerName, entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
