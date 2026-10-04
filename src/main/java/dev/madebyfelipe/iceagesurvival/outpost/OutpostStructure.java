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
 * Um posto militar ou a base: uma {@link OutpostPiece} só, com o template dado no JSON da estrutura
 * ({@code "template"}), na superfície e girada ao acaso. Nasce em terra seca acima do mar e num chão quase plano (os
 * cantos e o meio do template a no máximo {@link #MAX_SLOPE} de diferença). Com {@code "search_radius"} (em chunks)
 * procura esse chão em volta do ponto do espalhamento; sem achar, o ponto fica sem estrutura, a não ser com
 * {@code "required": true} (a base, que é única): aí ela nasce no ponto mesmo. O chão do template vai na altura do
 * ponto mais baixo; a adaptação de terreno do JSON ({@code beard_box}) completa o que faltar por baixo.
 */
public class OutpostStructure extends Structure {
    public static final Codec<OutpostStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(OutpostStructure::template),
            Codec.intRange(0, 6).optionalFieldOf("search_radius", 0).forGetter(structure -> structure.searchRadius),
            Codec.BOOL.optionalFieldOf("required", false).forGetter(structure -> structure.required))
            .apply(instance, OutpostStructure::new));
    static final int MAX_SLOPE = 3;

    private final ResourceLocation template;
    private final int searchRadius;
    private final boolean required;

    public OutpostStructure(StructureSettings settings, ResourceLocation template, int searchRadius, boolean required) {
        super(settings);
        this.template = template;
        this.searchRadius = searchRadius;
        this.required = required;
    }

    public ResourceLocation template() {
        return template;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Rotation rotation = Rotation.getRandom(context.random());
        Vec3i size = context.structureTemplateManager().getOrCreate(template).getSize();
        ChunkPos chunk = context.chunkPos();
        BlockPos origin = null;
        for (int ring = 0; ring <= searchRadius && origin == null; ring++) {
            for (int dx = -ring; dx <= ring && origin == null; dx++) {
                for (int dz = -ring; dz <= ring && origin == null; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        origin = groundAt(context, size, chunk.getMinBlockX() + dx * 16, chunk.getMinBlockZ() + dz * 16,
                                false);
                    }
                }
            }
        }
        if (origin == null && required) {
            origin = groundAt(context, size, chunk.getMinBlockX(), chunk.getMinBlockZ(), true);
        }
        if (origin == null) {
            return Optional.empty();
        }
        BlockPos start = origin;
        return Optional.of(new GenerationStub(start, builder -> builder.addPiece(
                new OutpostPiece(context.structureTemplateManager(), template, start, rotation))));
    }

    /**
     * O canto do template em ({@code x}, {@code z}), na altura do chão mais baixo dos cantos e do meio; {@code null} se
     * ali tem água, fica abaixo do mar ou é inclinado demais (a não ser com {@code anyway}).
     */
    @javax.annotation.Nullable
    private static BlockPos groundAt(GenerationContext context, Vec3i size, int x, int z, boolean anyway) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState state = context.randomState();
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
            if (!anyway && (withWater != ground || ground <= seaLevel)) {
                return null;
            }
            lowest = Math.min(lowest, Math.max(ground, seaLevel + 1));
            highest = Math.max(highest, ground);
        }
        if (!anyway && highest - lowest > MAX_SLOPE) {
            return null;
        }
        return new BlockPos(x, lowest, z);
    }

    @Override
    public StructureType<?> type() {
        return Outposts.OUTPOST.get();
    }
}
