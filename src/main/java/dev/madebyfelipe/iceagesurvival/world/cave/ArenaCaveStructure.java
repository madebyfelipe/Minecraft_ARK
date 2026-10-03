package dev.madebyfelipe.iceagesurvival.world.cave;

import com.mojang.serialization.Codec;
import dev.madebyfelipe.iceagesurvival.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A caverna da arena (D47): uma entrada em terra seca, túneis sinuosos que descem, salas, um salão profundo e a arena
 * fechada do Giganotosaurus com o altar no centro. Posicionada por anéis concêntricos (structure_set), longe do spawn.
 *
 * <p>Tudo é decidido aqui, no início da estrutura, a partir do ruído do gerador — nada depende de blocos já gerados.
 * A entrada só nasce em terra acima do nível do mar e sem água em volta; sem um ponto seco perto do anel, essa caverna
 * não existe (as outras do anel continuam).
 */
public class ArenaCaveStructure extends Structure {
    public static final Codec<ArenaCaveStructure> CODEC = simpleCodec(ArenaCaveStructure::new);

    /** Quantos chunks em volta do ponto do anel a entrada pode ser procurada. */
    static final int ENTRANCE_SEARCH_CHUNKS = 3;

    public ArenaCaveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState randomState = context.randomState();
        ChunkPos chunk = context.chunkPos();
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        int seaLevel = generator.getSeaLevel();

        BlockPos entrance = findDryEntrance(generator, height, randomState, centerX, centerZ, seaLevel);
        if (entrance == null) {
            return Optional.empty();
        }
        int arenaFloor = ArenaCaveLayout.arenaFloorFor(entrance.getY(), height.getMinBuildHeight());
        ArenaCaveLayout.Plan plan = ArenaCaveLayout.plan(context.random(), centerX, centerZ, entrance, arenaFloor,
                (x, z) -> generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, randomState));
        // A posição do stub é a do bioma conferido: o centro da arena, lá embaixo.
        return Optional.of(new GenerationStub(plan.arenaCenter(), builder -> plan.pieces().forEach(builder::addPiece)));
    }

    /** Procura em espiral, a partir do centro do chunk do anel, um ponto de terra seca acima do mar. */
    private static BlockPos findDryEntrance(ChunkGenerator generator, LevelHeightAccessor height, RandomState state,
            int centerX, int centerZ, int seaLevel) {
        for (int ring = 0; ring <= ENTRANCE_SEARCH_CHUNKS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = centerX + dx * 16;
                    int z = centerZ + dz * 16;
                    int ground = dryGround(generator, height, state, x, z, seaLevel);
                    if (ground == Integer.MIN_VALUE) {
                        continue;
                    }
                    boolean dryAround = true;
                    for (int[] offset : new int[][] {{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
                        if (dryGround(generator, height, state, x + offset[0], z + offset[1], seaLevel)
                                == Integer.MIN_VALUE) {
                            dryAround = false;
                            break;
                        }
                    }
                    if (dryAround) {
                        return new BlockPos(x, ground, z);
                    }
                }
            }
        }
        return null;
    }

    /** A altura do chão se a coluna é terra seca acima do mar; {@code Integer.MIN_VALUE} se é água. */
    private static int dryGround(ChunkGenerator generator, LevelHeightAccessor height, RandomState state, int x, int z,
            int seaLevel) {
        int withWater = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, state);
        int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, state);
        if (withWater != ground || ground <= seaLevel + 1) {
            return Integer.MIN_VALUE;
        }
        return ground;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ARENA_CAVE.get();
    }
}
