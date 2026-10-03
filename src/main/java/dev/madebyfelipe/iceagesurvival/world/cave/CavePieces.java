package dev.madebyfelipe.iceagesurvival.world.cave;

import dev.madebyfelipe.iceagesurvival.registry.ModBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;

/**
 * As peças da caverna da arena. Cada uma só escreve dentro do chunk que está sendo decorado ({@code chunkBox}), e toda
 * escolha "aleatória" de bloco vem de um hash da posição, para que a peça saia igual dos dois lados de uma divisa de
 * chunk.
 */
public final class CavePieces {
    private CavePieces() {
    }

    /** Um trecho reto de túnel: uma cápsula entre dois pontos. */
    public static class Tunnel extends StructurePiece {
        private final Vec3 from;
        private final Vec3 to;
        private final float radius;

        public Tunnel(Vec3 from, Vec3 to, float radius) {
            super(ModStructures.CAVE_TUNNEL.get(), 0, box(from, to, radius));
            this.from = from;
            this.to = to;
            this.radius = radius;
        }

        public Tunnel(CompoundTag tag) {
            super(ModStructures.CAVE_TUNNEL.get(), tag);
            this.from = new Vec3(tag.getDouble("FromX"), tag.getDouble("FromY"), tag.getDouble("FromZ"));
            this.to = new Vec3(tag.getDouble("ToX"), tag.getDouble("ToY"), tag.getDouble("ToZ"));
            this.radius = tag.getFloat("Radius");
        }

        private static BoundingBox box(Vec3 from, Vec3 to, float radius) {
            int pad = Mth.ceil(radius) + 2;
            return new BoundingBox(Mth.floor(Math.min(from.x, to.x)) - pad, Mth.floor(Math.min(from.y, to.y)) - pad,
                    Mth.floor(Math.min(from.z, to.z)) - pad, Mth.floor(Math.max(from.x, to.x)) + pad,
                    Mth.floor(Math.max(from.y, to.y)) + pad, Mth.floor(Math.max(from.z, to.z)) + pad);
        }

        public Vec3 from() {
            return from;
        }

        public Vec3 to() {
            return to;
        }

        public float radius() {
            return radius;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putDouble("FromX", from.x);
            tag.putDouble("FromY", from.y);
            tag.putDouble("FromZ", from.z);
            tag.putDouble("ToX", to.x);
            tag.putDouble("ToY", to.y);
            tag.putDouble("ToZ", to.z);
            tag.putFloat("Radius", radius);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            CaveCarving.carve(level, boundingBox, chunkBox, (x, y, z) -> distanceToSegment(x, y, z) - radius);
        }

        double distanceToSegment(double x, double y, double z) {
            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double dz = to.z - from.z;
            double lengthSq = dx * dx + dy * dy + dz * dz;
            double t = lengthSq == 0 ? 0
                    : Mth.clamp(((x - from.x) * dx + (y - from.y) * dy + (z - from.z) * dz) / lengthSq, 0, 1);
            double px = from.x + dx * t - x;
            double py = from.y + dy * t - y;
            double pz = from.z + dz * t - z;
            return Math.sqrt(px * px + py * py + pz * pz);
        }
    }

    /** Uma sala: um elipsoide com o chão achatado, musgo e alguns ossos espalhados. */
    public static class Room extends StructurePiece {
        private final BlockPos center;
        private final int radiusX;
        private final int radiusY;
        private final int radiusZ;

        public Room(BlockPos center, int radiusX, int radiusY, int radiusZ) {
            super(ModStructures.CAVE_ROOM.get(), 0, new BoundingBox(center.getX() - radiusX - 2,
                    center.getY() - radiusY - 2, center.getZ() - radiusZ - 2, center.getX() + radiusX + 2,
                    center.getY() + radiusY + 2, center.getZ() + radiusZ + 2));
            this.center = center;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.radiusZ = radiusZ;
        }

        public Room(CompoundTag tag) {
            super(ModStructures.CAVE_ROOM.get(), tag);
            this.center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
            this.radiusX = tag.getInt("RX");
            this.radiusY = tag.getInt("RY");
            this.radiusZ = tag.getInt("RZ");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("CX", center.getX());
            tag.putInt("CY", center.getY());
            tag.putInt("CZ", center.getZ());
            tag.putInt("RX", radiusX);
            tag.putInt("RY", radiusY);
            tag.putInt("RZ", radiusZ);
        }

        private double depth(double x, double y, double z) {
            double nx = (x - center.getX() - 0.5) / radiusX;
            double ny = (y - center.getY() - 0.5) / radiusY;
            double nz = (z - center.getZ() - 0.5) / radiusZ;
            double ellipse = (Math.sqrt(nx * nx + ny * ny + nz * nz) - 1) * Math.min(radiusX, Math.min(radiusY, radiusZ));
            double floor = floorY() - y;
            return Math.max(ellipse, floor);
        }

        public BlockPos center() {
            return center;
        }

        /** O chão plano fica na metade de baixo do elipsoide. */
        public int floorY() {
            return center.getY() - radiusY / 2;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            CaveCarving.carve(level, boundingBox, chunkBox, this::depth);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int y = floorY();
            for (int x = boundingBox.minX(); x <= boundingBox.maxX(); x++) {
                for (int z = boundingBox.minZ(); z <= boundingBox.maxZ(); z++) {
                    pos.set(x, y, z);
                    if (!chunkBox.isInside(pos) || depth(x + 0.5, y + 0.5, z + 0.5) > 0) {
                        continue;
                    }
                    double roll = CaveCarving.roll(x, y, z);
                    BlockPos below = pos.below();
                    if (roll < 0.18 && level.getBlockState(below).isSolidRender(level, below)) {
                        level.setBlock(below, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                    } else if (roll < 0.205 && level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, CaveCarving.bone(x, z), 2);
                    }
                }
            }
        }
    }

    /**
     * A boca da caverna na superfície: um anel de pedregulho musgoso e duas costelas de osso, longe da direção da
     * rampa para não caírem no buraco.
     */
    public static class Entrance extends StructurePiece {
        private final BlockPos mouth;
        private final float shaftAngle;
        private final int seed;

        public Entrance(BlockPos mouth, float shaftAngle, int seed) {
            super(ModStructures.CAVE_ENTRANCE.get(), 0, new BoundingBox(mouth.getX() - 7, mouth.getY() - 12,
                    mouth.getZ() - 7, mouth.getX() + 7, mouth.getY() + 12, mouth.getZ() + 7));
            this.mouth = mouth;
            this.shaftAngle = shaftAngle;
            this.seed = seed;
        }

        public Entrance(CompoundTag tag) {
            super(ModStructures.CAVE_ENTRANCE.get(), tag);
            this.mouth = new BlockPos(tag.getInt("MX"), tag.getInt("MY"), tag.getInt("MZ"));
            this.shaftAngle = tag.getFloat("Angle");
            this.seed = tag.getInt("Seed");
        }

        public BlockPos mouth() {
            return mouth;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("MX", mouth.getX());
            tag.putInt("MY", mouth.getY());
            tag.putInt("MZ", mouth.getZ());
            tag.putFloat("Angle", shaftAngle);
            tag.putInt("Seed", seed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            RandomSource local = RandomSource.create(seed);
            // Costelas: dos dois lados da rampa (±90°), curvando para a boca.
            for (double side : new double[] {Math.PI / 2, -Math.PI / 2}) {
                double angle = shaftAngle + side + (local.nextDouble() - 0.5) * 0.4;
                rib(level, chunkBox, angle, 3 + local.nextInt(2));
            }
            // Pedregulhos: no arco de trás, longe da rampa.
            int boulders = 4 + local.nextInt(3);
            for (int i = 0; i < boulders; i++) {
                double angle = shaftAngle + Math.PI + (local.nextDouble() - 0.5) * Math.PI * 1.1;
                double radius = 4.5 + local.nextDouble() * 1.5;
                int x = mouth.getX() + Mth.floor(Math.cos(angle) * radius);
                int z = mouth.getZ() + Mth.floor(Math.sin(angle) * radius);
                int height = local.nextInt(3) == 0 ? 2 : 1;
                BlockPos ground = surface(level, x, z);
                for (int dy = 0; dy < height; dy++) {
                    BlockState stone = dy == 0 || local.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                            : Blocks.COBBLESTONE.defaultBlockState();
                    place(level, chunkBox, ground.above(dy), stone);
                }
            }
        }

        private void rib(WorldGenLevel level, BoundingBox chunkBox, double angle, int height) {
            double radius = 4.5;
            int x = mouth.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = mouth.getZ() + Mth.floor(Math.sin(angle) * radius);
            BlockPos ground = surface(level, x, z);
            BlockState upright = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (int dy = 0; dy < height; dy++) {
                place(level, chunkBox, ground.above(dy), upright);
            }
            // A ponta dobra para dentro, na direção da boca.
            int inX = x - Mth.floor(Math.signum(Math.cos(angle)) * (Math.abs(Math.cos(angle)) > 0.5 ? 1 : 0));
            int inZ = z - Mth.floor(Math.signum(Math.sin(angle)) * (Math.abs(Math.sin(angle)) > 0.5 ? 1 : 0));
            Direction.Axis axis = Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle)) ? Direction.Axis.X : Direction.Axis.Z;
            place(level, chunkBox, new BlockPos(inX, ground.getY() + height, inZ),
                    Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
        }

        /** O primeiro bloco livre acima do chão da coluna. */
        private static BlockPos surface(WorldGenLevel level, int x, int z) {
            return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
        }

        private static void place(WorldGenLevel level, BoundingBox chunkBox, BlockPos pos, BlockState state) {
            if (chunkBox.isInside(pos) && level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, state, 2);
            }
        }
    }

    /**
     * A arena: 40×40 de chão, 18 de altura livre, paredes, teto e piso de três blocos de deepslate. Por dentro, a
     * face é de pedregulho de deepslate, basalto liso e pedregulho musgoso — nenhum deles deixa uma nascente d'água
     * vanilla brotar na parede. Uma porta de 3×4 numa das paredes, larga para um jogador e estreita demais para o boss.
     * Luz fraca de líquen brilhante no teto e nas paredes; o altar no centro do chão.
     */
    public static class Arena extends StructurePiece {
        public static final int HALF = 20;
        public static final int HEIGHT = 18;
        public static final int SHELL = 3;
        public static final int DOOR_WIDTH = 3;
        public static final int DOOR_HEIGHT = 4;
        /** Quanto o corredor da porta avança para fora da parede, para encontrar o túnel. */
        public static final int DOOR_REACH = 4;

        private final int centerX;
        private final int floorY;
        private final int centerZ;
        private final Direction door;

        public Arena(int centerX, int floorY, int centerZ, Direction door) {
            super(ModStructures.CAVE_ARENA.get(), 0, new BoundingBox(centerX - HALF - SHELL - DOOR_REACH - 1,
                    floorY - SHELL + 1, centerZ - HALF - SHELL - DOOR_REACH - 1,
                    centerX + HALF + SHELL + DOOR_REACH, floorY + HEIGHT + SHELL, centerZ + HALF + SHELL + DOOR_REACH));
            this.centerX = centerX;
            this.floorY = floorY;
            this.centerZ = centerZ;
            this.door = door;
        }

        public Arena(CompoundTag tag) {
            super(ModStructures.CAVE_ARENA.get(), tag);
            this.centerX = tag.getInt("AX");
            this.floorY = tag.getInt("AY");
            this.centerZ = tag.getInt("AZ");
            this.door = Direction.from2DDataValue(tag.getInt("Door"));
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("AX", centerX);
            tag.putInt("AY", floorY);
            tag.putInt("AZ", centerZ);
            tag.putInt("Door", door.get2DDataValue());
        }

        public BlockPos altarPos() {
            return new BlockPos(centerX, floorY + 1, centerZ);
        }

        public int floorY() {
            return floorY;
        }

        public Direction door() {
            return door;
        }

        /** O retângulo do chão livre: de {@code centerX - HALF} a {@code centerX + HALF - 1}. */
        public boolean isInterior(int x, int y, int z) {
            int lx = x - centerX;
            int lz = z - centerZ;
            int ly = y - floorY;
            return lx >= -HALF && lx < HALF && lz >= -HALF && lz < HALF && ly >= 1 && ly <= HEIGHT;
        }

        /** Dentro da casca (parede, piso ou teto), sem contar o miolo. */
        private boolean isInShell(int x, int y, int z) {
            int lx = x - centerX;
            int lz = z - centerZ;
            int ly = y - floorY;
            return lx >= -HALF - SHELL && lx < HALF + SHELL && lz >= -HALF - SHELL && lz < HALF + SHELL
                    && ly >= 1 - SHELL && ly <= HEIGHT + SHELL && !isInterior(x, y, z);
        }

        /** O corredor da porta: atravessa a parede e avança {@link #DOOR_REACH} blocos para fora. */
        public boolean isDoorway(int x, int y, int z) {
            int ly = y - floorY;
            if (ly < 1 || ly > DOOR_HEIGHT) {
                return false;
            }
            // Coordenada ao longo da porta (para fora) e de lado.
            int out = (x - centerX) * door.getStepX() + (z - centerZ) * door.getStepZ();
            int across = door.getAxis() == Direction.Axis.X ? z - centerZ : x - centerX;
            // Para os lados positivos, a parede começa em HALF; para os negativos, em HALF + 1 (o miolo vai de -HALF a
            // HALF - 1), então "out" mede o mesmo nos dois.
            int firstWall = door.getAxisDirection() == Direction.AxisDirection.POSITIVE ? HALF : HALF + 1;
            return Math.abs(across) <= DOOR_WIDTH / 2 && out >= firstWall && out < firstWall + SHELL + DOOR_REACH;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int minX = Math.max(boundingBox.minX(), chunkBox.minX());
            int maxX = Math.min(boundingBox.maxX(), chunkBox.maxX());
            int minZ = Math.max(boundingBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(boundingBox.maxZ(), chunkBox.maxZ());
            int minY = Math.max(boundingBox.minY(), chunkBox.minY());
            int maxY = Math.min(boundingBox.maxY(), chunkBox.maxY());
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);
                        BlockState state = blockAt(x, y, z);
                        if (state != null) {
                            if (level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                                level.setBlock(pos, state, 2);
                            }
                        } else if (nextToDoorway(x, y, z) && !level.getFluidState(pos).isEmpty()) {
                            level.setBlock(pos, CaveCarving.seal(y), 2);
                        }
                    }
                }
            }
        }

        private boolean nextToDoorway(int x, int y, int z) {
            for (Direction direction : Direction.values()) {
                if (isDoorway(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())) {
                    return true;
                }
            }
            return false;
        }

        /** O bloco da arena nessa posição, ou {@code null} se a arena não mexe nela. */
        BlockState blockAt(int x, int y, int z) {
            if (isDoorway(x, y, z)) {
                return CaveCarving.CAVE_AIR;
            }
            if (isInterior(x, y, z)) {
                return interior(x, y, z);
            }
            if (!isInShell(x, y, z)) {
                return null;
            }
            boolean face = false;
            for (Direction direction : Direction.values()) {
                if (isInterior(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ())) {
                    face = true;
                    break;
                }
            }
            if (!face) {
                return Blocks.DEEPSLATE.defaultBlockState();
            }
            double roll = CaveCarving.roll(x, y, z);
            if (y == floorY) {
                if (Math.abs(x - centerX) <= 1 && Math.abs(z - centerZ) <= 1) {
                    return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                }
                if (Math.abs(x - centerX) <= 3 && Math.abs(z - centerZ) <= 3) {
                    return Blocks.DEEPSLATE_TILES.defaultBlockState();
                }
                return roll < 0.6 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                        : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            }
            if (roll < 0.1) {
                return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            }
            if (roll < 0.25) {
                return Blocks.SMOOTH_BASALT.defaultBlockState();
            }
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }

        private BlockState interior(int x, int y, int z) {
            if (x == centerX && z == centerZ && y == floorY + 1) {
                return ModBlocks.ARENA_ALTAR.get().defaultBlockState();
            }
            double roll = CaveCarving.roll(x, y, z);
            int ly = y - floorY;
            if (ly == HEIGHT && roll < 0.06) {
                return lichen(Direction.UP);
            }
            Direction wall = adjacentWall(x, z);
            if (wall != null && ly >= 2 && ly <= 12 && roll < 0.03) {
                return lichen(wall);
            }
            if (ly == 1 && wall != null && roll < 0.05 && !nearDoor(x, z)) {
                return CaveCarving.bone(x, z);
            }
            return CaveCarving.CAVE_AIR;
        }

        private Direction adjacentWall(int x, int z) {
            int lx = x - centerX;
            int lz = z - centerZ;
            if (lx == -HALF) {
                return Direction.WEST;
            }
            if (lx == HALF - 1) {
                return Direction.EAST;
            }
            if (lz == -HALF) {
                return Direction.NORTH;
            }
            if (lz == HALF - 1) {
                return Direction.SOUTH;
            }
            return null;
        }

        private boolean nearDoor(int x, int z) {
            int across = door.getAxis() == Direction.Axis.X ? z - centerZ : x - centerX;
            return Math.abs(across) <= DOOR_WIDTH;
        }

        private static BlockState lichen(Direction face) {
            return Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(face), true);
        }
    }

    /** Escavação comum de túneis e salas, com a vedação contra água. */
    static final class CaveCarving {
        static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();

        private CaveCarving() {
        }

        interface Shape {
            /** Negativo ou zero dentro; até 1 é a casca, onde fluidos viram pedra. */
            double depth(double x, double y, double z);
        }

        /**
         * Escava o que estiver dentro da forma. Na casca (até um bloco além da borda), água e lava viram pedra: um
         * túnel que passa perto de um aquífero ou do fundo do mar não se alaga.
         */
        static void carve(WorldGenLevel level, BoundingBox pieceBox, BoundingBox chunkBox, Shape shape) {
            int minX = Math.max(pieceBox.minX(), chunkBox.minX());
            int maxX = Math.min(pieceBox.maxX(), chunkBox.maxX());
            int minZ = Math.max(pieceBox.minZ(), chunkBox.minZ());
            int maxZ = Math.min(pieceBox.maxZ(), chunkBox.maxZ());
            int minY = Math.max(pieceBox.minY(), Math.max(chunkBox.minY(), level.getMinBuildHeight() + 1));
            int maxY = Math.min(pieceBox.maxY(), Math.min(chunkBox.maxY(), level.getMaxBuildHeight() - 2));
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        double depth = shape.depth(x + 0.5, y + 0.5, z + 0.5);
                        if (depth > 1) {
                            continue;
                        }
                        pos.set(x, y, z);
                        BlockState current = level.getBlockState(pos);
                        if (current.getDestroySpeed(level, pos) < 0) {
                            continue;
                        }
                        if (depth <= 0) {
                            if (!current.isAir()) {
                                level.setBlock(pos, CAVE_AIR, 2);
                            }
                            // Neve, capim e flores que ficariam flutuando sobre a boca.
                            above.setWithOffset(pos, Direction.UP);
                            BlockState top = level.getBlockState(above);
                            if (!top.isAir() && top.getFluidState().isEmpty() && !top.canSurvive(level, above)) {
                                level.setBlock(above, CAVE_AIR, 2);
                            }
                        } else if (!current.getFluidState().isEmpty()) {
                            level.setBlock(pos, seal(y), 2);
                        }
                    }
                }
            }
        }

        static BlockState seal(int y) {
            return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }

        /** Um número em [0, 1) fixo para cada posição. */
        static double roll(int x, int y, int z) {
            long seed = Mth.getSeed(x, y, z);
            return ((seed >>> 16) & 0xFFFF) / 65536.0;
        }

        static BlockState bone(int x, int z) {
            Direction.Axis axis = ((x * 31 + z) & 1) == 0 ? Direction.Axis.X : Direction.Axis.Z;
            return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
        }
    }
}
