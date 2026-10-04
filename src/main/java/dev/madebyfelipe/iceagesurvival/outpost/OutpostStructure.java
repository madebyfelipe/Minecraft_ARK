package dev.madebyfelipe.iceagesurvival.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Um posto militar: uma {@link OutpostPiece} só, com o template dado no JSON da estrutura ({@code "template"}), na
 * superfície e girada ao acaso. Nasce em terra seca acima do mar e num chão quase plano (os cantos e o meio do
 * template a no máximo {@link #MAX_SLOPE} de diferença); fora disso, aquele ponto do espalhamento fica sem posto. O
 * chão do template vai na altura do ponto mais baixo; a adaptação de terreno do JSON ({@code beard_box}) completa o
 * que faltar por baixo.
 */
public class OutpostStructure extends Structure {
    public static final Codec<OutpostStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(OutpostStructure::template))
            .apply(instance, OutpostStructure::new));
    static final int MAX_SLOPE = 3;

    private final ResourceLocation template;

    public OutpostStructure(StructureSettings settings, ResourceLocation template) {
        super(settings);
        this.template = template;
    }

    public ResourceLocation template() {
        return template;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState state = context.randomState();
        ChunkPos chunk = context.chunkPos();
        Rotation rotation = Rotation.getRandom(context.random());
        Vec3i size = context.structureTemplateManager().getOrCreate(template).getSize();
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        int seaLevel = generator.getSeaLevel();

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int lastX = size.getX() - 1;
        int lastZ = size.getZ() - 1;
        for (int[] corner : new int[][] {{0, 0}, {lastX, 0}, {0, lastZ}, {lastX, lastZ}, {lastX / 2, lastZ / 2}}) {
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
                new OutpostPiece(context.structureTemplateManager(), template, origin, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return Outposts.OUTPOST.get();
    }
}
