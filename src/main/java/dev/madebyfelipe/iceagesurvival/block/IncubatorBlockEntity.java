package dev.madebyfelipe.iceagesurvival.block;

import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.CreatureEggItem;
import dev.madebyfelipe.iceagesurvival.menu.IncubatorMenu;
import dev.madebyfelipe.iceagesurvival.registry.ModBlockEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.BreedingProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/**
 * Choca o ovo do espaço 0. O ovo só avança aquecido: por uma fonte de calor
 * ({@code #iceagesurvival:heat_sources}) a até {@link #HEAT_RADIUS} blocos, ou por combustível
 * queimando no espaço 1, gasto só enquanto há ovo e não há outra fonte de calor. Frio, o
 * ovo para onde está — não perde o que já chocou. Pronto, o filhote nasce em cima da incubadora.
 */
public class IncubatorBlockEntity extends StationBlockEntity {
    public static final int EGG_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int HEAT_RADIUS = 2;
    private static final int HEAT_CHECK_INTERVAL = 20;

    /** Dados para a tela: progresso, total, combustível restante, duração do combustível, calor de bloco. */
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_TOTAL = 1;
    public static final int DATA_BURN = 2;
    public static final int DATA_BURN_DURATION = 3;
    public static final int DATA_BLOCK_HEAT = 4;
    public static final int DATA_COUNT = 5;

    private int progress;
    private int burnTime;
    private int burnDuration;
    private boolean blockHeat;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> incubationTicks();
                case DATA_BURN -> burnTime;
                case DATA_BURN_DURATION -> burnDuration;
                case DATA_BLOCK_HEAT -> blockHeat ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR.get(), pos, state, 2);
    }

    /** Ticks até o ovo do espaço chocar; 0 se não houver ovo válido. */
    private int incubationTicks() {
        ItemStack egg = getItem(EGG_SLOT);
        if (level == null || !egg.is(ModItems.CREATURE_EGG.get())) {
            return 0;
        }
        return CreatureEggItem.species(egg)
                .flatMap(type -> Species.of(level.registryAccess(), type))
                .flatMap(Species::breeding)
                .map(breeding -> breeding.incubationSeconds() * 20)
                .orElse(0);
    }

    public boolean isHeated() {
        return blockHeat || burnTime > 0;
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }
        if (level.getGameTime() % HEAT_CHECK_INTERVAL == 0) {
            blockHeat = heatNearby();
        }
        int total = incubationTicks();
        if (total <= 0) {
            progress = 0;
            if (burnTime > 0) {
                burnTime--;
            }
            return;
        }
        if (!blockHeat && burnTime <= 0) {
            igniteFuel();
        }
        boolean heated = isHeated();
        if (burnTime > 0) {
            burnTime--;
        }
        if (heated) {
            progress++;
            setChanged();
            if (progress >= total) {
                hatch();
            }
        }
    }

    private boolean heatNearby() {
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-HEAT_RADIUS, -HEAT_RADIUS, -HEAT_RADIUS),
                worldPosition.offset(HEAT_RADIUS, HEAT_RADIUS, HEAT_RADIUS))) {
            if (level.getBlockState(pos).is(ModTags.HEAT_SOURCES)) {
                return true;
            }
        }
        return false;
    }

    private void igniteFuel() {
        ItemStack fuel = getItem(FUEL_SLOT);
        int duration = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
        if (duration <= 0) {
            return;
        }
        burnTime = burnDuration = duration;
        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty() && !remainder.isEmpty()) {
            setItem(FUEL_SLOT, remainder);
        }
        setChanged();
    }

    private void hatch() {
        ItemStack egg = getItem(EGG_SLOT);
        EntityType<?> type = CreatureEggItem.species(egg).orElse(null);
        progress = 0;
        if (type == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        PrehistoricCreature baby = PrehistoricCreature.spawnOffspring(serverLevel, type, CreatureEggItem.genome(egg),
                CreatureEggItem.owner(egg), Vec3.atBottomCenterOf(worldPosition.above()));
        if (baby == null) {
            return;
        }
        setItem(EGG_SLOT, ItemStack.EMPTY);
        level.playSound(null, worldPosition, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == EGG_SLOT ? stack.is(ModItems.CREATURE_EGG.get())
                : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.iceagesurvival.incubator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new IncubatorMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }
}
