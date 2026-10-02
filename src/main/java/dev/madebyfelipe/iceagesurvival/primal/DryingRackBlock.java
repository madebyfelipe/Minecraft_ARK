package dev.madebyfelipe.iceagesurvival.primal;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Varal de secagem: quatro espaços em cima, seca só de dia (como no Primal Stage). É o jeito
 * primitivo de fazer charque: carne crua, sem açúcar, devagar.
 */
public class DryingRackBlock extends PrimalStationBlock {
    public DryingRackBlock(Properties properties) {
        super(properties, box(0, 0, 0, 16, 15, 16));
    }

    @Override
    protected Supplier<? extends BlockEntityType<? extends PrimalStationBlockEntity>> type() {
        return PrimalStations.DRYING_RACK_ENTITY;
    }

    @Override
    protected int slotAt(BlockState state, Vec3 local) {
        return quadrant(local);
    }

    /**
     * Dia pelo relógio do mundo, como o vanilla conta para dormir (0–12 541 e depois de 23 460),
     * fora das dimensões de tempo parado. Pelo relógio e não pelo brilho do céu, para a chuva não parar
     * o varal (no Primal Stage, {@code isDay}) e para o teste poder acertar a hora.
     */
    public static boolean isDaylight(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long time = level.getDayTime() % 24_000L;
        return time < 12_542L || time > 23_459L;
    }

    /** O que está no varal. */
    public static class Station extends TimedStationBlockEntity {
        public Station(BlockPos pos, BlockState state) {
            super(PrimalStations.DRYING_RACK_ENTITY.get(), pos, state, 4);
        }

        @Override
        protected RecipeType<TimedRecipe> recipeType() {
            return PrimalStations.DRYING_RECIPE.get();
        }

        @Override
        public boolean canWork() {
            return level != null && isDaylight(level);
        }
    }
}
