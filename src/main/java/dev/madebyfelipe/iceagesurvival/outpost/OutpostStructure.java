package dev.madebyfelipe.iceagesurvival.outpost;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Um posto militar: uma {@link OutpostPiece} só, na superfície, girada ao acaso. Nasce em terra seca acima do mar e
 * num chão quase plano (os cantos e o meio a no máximo {@link #MAX_SLOPE} de diferença); fora disso, aquele ponto do
 * espalhamento fica sem posto. O chão do template vai na altura do ponto mais baixo; a adaptação de terreno do JSON
 * ({@code beard_box}) completa o que faltar por baixo.
 */
public class OutpostStructure extends Structure {
    public static final Codec<OutpostStructure> CODEC = simpleCodec(OutpostStructure::new);
    static final int MAX_SLOPE = 3;

    public OutpostStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState state = context.randomState();
        ChunkPos chunk = context.chunkPos();
        Rotation rotation = Rotation.getRandom(context.random());
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        int seaLevel = generator.getSeaLevel();

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int[] corner : new int[][] {{0, 0}, {OutpostPiece.SIZE - 1, 0}, {0, OutpostPiece.SIZE - 1},
                {OutpostPiece.SIZE - 1, OutpostPiece.SIZE - 1}, {OutpostPiece.SIZE / 2, OutpostPiece.SIZE / 2}}) {
            int cx = x + corner[0];
            int cz = z + corner[1];
            int withWater = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, height, state);
            int ground = generator.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, height, state);
            if (withWater != ground || ground <= seaLevel) {
                return Optional.empty();
            }
            lowest = Math.min(lowest, ground);
            highest = Math.max(highest, ground);
        }
        if (highest - lowest > MAX_SLOPE) {
            return Optional.empty();
        }
        BlockPos origin = new BlockPos(x, lowest, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                new OutpostPiece(context.structureTemplateManager(), origin, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return Outposts.OUTPOST.get();
    }
}
