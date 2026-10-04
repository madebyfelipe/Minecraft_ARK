package dev.madebyfelipe.iceagesurvival.outpost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * O posto: uma sala de concreto de {@link #SIZE}×{@link #SIZE} com a porta aberta na frente, janelas gradeadas dos
 * lados, gelo entrando pelas frestas e neve acumulada na entrada. No fundo, de frente para a porta, o terminal sobre
 * a bancada, com um barril e uma cama de campanha.
 *
 * <p>Coordenadas locais: a porta fica em z = 0 e o fundo em z = {@code SIZE - 1}; a orientação da peça gira tudo (e,
 * como nas peças do vanilla, o estado local virado para z = 0 é o SOUTH, que a orientação espelha e gira). As
 * escolhas "aleatórias" (rachaduras de gelo, neve) vêm de um hash da posição local, para a peça sair igual dos dois
 * lados de uma divisa de chunk.
 */
public class OutpostPiece extends StructurePiece {
    public static final int SIZE = 9;
    static final int HEIGHT = 6;
    /** O terminal, em coordenadas locais. */
    static final int TERMINAL_X = SIZE / 2;
    static final int TERMINAL_Y = 2;
    static final int TERMINAL_Z = SIZE - 2;

    public OutpostPiece(BlockPos origin, Direction facing) {
        super(Outposts.OUTPOST_PIECE.get(), 0, StructurePiece.makeBoundingBox(origin.getX(), origin.getY(),
                origin.getZ(), facing, SIZE, HEIGHT + 1, SIZE));
        setOrientation(facing);
    }

    public OutpostPiece(CompoundTag tag) {
        super(Outposts.OUTPOST_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
    }

    /** Onde fica o terminal no mundo. */
    public BlockPos terminalPos() {
        return getWorldPos(TERMINAL_X, TERMINAL_Y, TERMINAL_Z);
    }

    /** O vão da porta (o de baixo), no mundo. */
    public BlockPos doorPos() {
        return getWorldPos(SIZE / 2, 1, 0);
    }

    private static boolean crack(int x, int y, int z, int chance) {
        return Math.floorMod((int) Mth.getSeed(x, y, z), 100) < chance;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockState foundation = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState wall = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        int top = SIZE - 1;

        // Alicerce até o chão e piso.
        for (int x = 0; x <= top; x++) {
            for (int z = 0; z <= top; z++) {
                fillColumnDown(level, foundation, x, -1, z, box);
                placeBlock(level, Blocks.POLISHED_ANDESITE.defaultBlockState(), x, 0, z, box);
            }
        }
        // Paredes (pilares escuros nos cantos), interior vazio e laje.
        for (int y = 1; y < HEIGHT; y++) {
            for (int x = 0; x <= top; x++) {
                for (int z = 0; z <= top; z++) {
                    boolean edgeX = x == 0 || x == top;
                    boolean edgeZ = z == 0 || z == top;
                    if (edgeX && edgeZ) {
                        placeBlock(level, foundation, x, y, z, box);
                    } else if (edgeX || edgeZ) {
                        placeBlock(level, crack(x, y, z, 12) ? ice : wall, x, y, z, box);
                    } else {
                        placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                    }
                }
            }
        }
        for (int x = 0; x <= top; x++) {
            for (int z = 0; z <= top; z++) {
                placeBlock(level, crack(x, HEIGHT, z, 8) ? ice : foundation, x, HEIGHT, z, box);
                placeBlock(level, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + Math.floorMod(
                        (int) Mth.getSeed(x, HEIGHT + 1, z), 3)), x, HEIGHT + 1, z, box);
            }
        }
        // Porta aberta (sem folha) e neve que entrou por ela.
        int door = SIZE / 2;
        placeBlock(level, Blocks.AIR.defaultBlockState(), door, 1, 0, box);
        placeBlock(level, Blocks.AIR.defaultBlockState(), door, 2, 0, box);
        for (int dx = -1; dx <= 1; dx++) {
            for (int z = 1; z <= 2; z++) {
                if (crack(door + dx, 1, z, 70)) {
                    placeBlock(level, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3 - z),
                            door + dx, 1, z, box);
                }
            }
        }
        // Janelas gradeadas nas laterais.
        BlockState bars = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true)
                .setValue(IronBarsBlock.SOUTH, true);
        for (int y = 2; y <= 3; y++) {
            placeBlock(level, bars, 0, y, door, box);
            placeBlock(level, bars, top, y, door, box);
        }
        // Bancada com o terminal virado para a porta, barril, cama de campanha e a luz pendurada.
        for (int dx = -1; dx <= 1; dx++) {
            placeBlock(level, Blocks.SMOOTH_STONE.defaultBlockState(), TERMINAL_X + dx, 1, TERMINAL_Z, box);
        }
        placeBlock(level, Outposts.MILITARY_TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), TERMINAL_X, TERMINAL_Y, TERMINAL_Z, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 1, 1, top - 1, box);
        placeBlock(level, Blocks.GRAY_CARPET.defaultBlockState(), top - 1, 1, 2, box);
        placeBlock(level, Blocks.GRAY_CARPET.defaultBlockState(), top - 1, 1, 3, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), door, HEIGHT - 1,
                door, box);
    }
}
