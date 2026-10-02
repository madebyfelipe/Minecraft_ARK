package dev.madebyfelipe.iceagesurvival.primal;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Estação de golpes (tora de corte, bigorna de pedra): um item por vez; cada clique com a ferramenta
 * certa na mão principal conta um golpe e gasta 1 de durabilidade. No último golpe o resultado salta
 * da estação.
 */
public abstract class HitStationBlockEntity extends PrimalStationBlockEntity {
    private int hits;
    private int totalHits;

    protected HitStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    protected abstract RecipeType<HitRecipe> recipeType();

    /** A ferramenta que golpeia esta estação. */
    public abstract boolean isTool(ItemStack stack);

    /** Som de cada golpe. */
    protected abstract SoundEvent hitSound();

    /** Altura do topo da estação, em blocos, de onde saem as lascas e o resultado. */
    protected abstract double topHeight();

    public Optional<HitRecipe> recipeFor(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(recipeType(), new SimpleContainer(stack), level);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return recipeFor(stack).isPresent();
    }

    public int hits() {
        return hits;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, int aimed) {
        ItemStack held = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && isTool(held) && !items.get(0).isEmpty()
                && !player.isSecondaryUseActive()) {
            boolean client = level == null || level.isClientSide;
            if (!client) {
                strike(player, hand);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return super.interact(player, hand, aimed);
    }

    /** Um golpe da ferramenta na mão {@code hand}. */
    public void strike(Player player, InteractionHand hand) {
        if (level == null || items.get(0).isEmpty()) {
            return;
        }
        ItemStack input = items.get(0);
        player.getItemInHand(hand).hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
        level.playSound(null, worldPosition, hitSound(), SoundSource.BLOCKS, 0.8F,
                0.8F + level.random.nextFloat() * 0.4F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, input),
                    worldPosition.getX() + 0.5, worldPosition.getY() + topHeight() + 0.1, worldPosition.getZ() + 0.5,
                    6, 0.15, 0.05, 0.15, 0.05);
        }
        if (++hits < totalHits) {
            setChanged();
            return;
        }
        // A receita pode ter saído num /reload: devolve o item como entrou.
        ItemStack result = recipeFor(input)
                .map(recipe -> recipe.assemble(new SimpleContainer(input), level.registryAccess()))
                .orElse(input.copy());
        take(0);
        ItemEntity drop = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + topHeight() + 0.1,
                worldPosition.getZ() + 0.5, result);
        drop.setDeltaMovement(level.random.nextGaussian() * 0.05, 0.2, level.random.nextGaussian() * 0.05);
        level.addFreshEntity(drop);
    }

    @Override
    protected void onInserted(int slot) {
        hits = 0;
        totalHits = recipeFor(items.get(slot)).map(HitRecipe::hits).orElse(1);
    }

    @Override
    protected void onRemoved(int slot) {
        hits = 0;
        totalHits = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Hits", hits);
        tag.putInt("TotalHits", totalHits);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hits = tag.getInt("Hits");
        totalHits = tag.getInt("TotalHits");
    }
}
