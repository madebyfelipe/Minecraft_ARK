package dev.madebyfelipe.iceagesurvival.outpost;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Quem já leu este terminal: cada pessoa recupera um registro por terminal, uma vez só. */
public class MilitaryTerminalBlockEntity extends BlockEntity {
    private final Set<UUID> readers = new HashSet<>();

    public MilitaryTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Outposts.MILITARY_TERMINAL_ENTITY.get(), pos, state);
    }

    /** Marca a leitura; {@code true} se é a primeira desta pessoa neste terminal. */
    public boolean markRead(UUID reader) {
        boolean added = readers.add(reader);
        if (added) {
            setChanged();
        }
        return added;
    }

    public boolean wasReadBy(UUID reader) {
        return readers.contains(reader);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readers.clear();
        for (Tag element : tag.getList("Readers", Tag.TAG_INT_ARRAY)) {
            readers.add(NbtUtils.loadUUID(element));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        readers.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put("Readers", list);
    }
}
