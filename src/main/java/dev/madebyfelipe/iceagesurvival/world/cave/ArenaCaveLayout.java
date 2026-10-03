package dev.madebyfelipe.iceagesurvival.world.cave;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.Vec3;

/**
 * O traçado da caverna da arena, separado do gerador para poder ser testado com um relevo de mentira.
 *
 * <p>Ordem das peças (a ordem importa: cada uma escava por cima da anterior, e a arena vem por último para que nenhum
 * túnel fure as paredes dela):
 * <ol>
 *   <li>a rampa da entrada, dois trechos íngremes saindo da superfície;</li>
 *   <li>a marca na superfície (pedregulho musgoso e costelas de osso) em volta da boca;</li>
 *   <li>o trecho sinuoso, que gira em volta do centro descendo no máximo ~0,6 bloco por bloco andado, com salas;</li>
 *   <li>o salão profundo, acima e ao lado da arena;</li>
 *   <li>a descida final até a porta da arena;</li>
 *   <li>a arena.</li>
 * </ol>
 * O trecho sinuoso nunca desce abaixo de {@link #MEANDER_ABOVE_FLOOR} acima do chão da arena, então passa por cima do
 * teto dela; só a descida final, vinda de fora pela direção da porta, chega ao nível do chão.
 */
public final class ArenaCaveLayout {
    /** Chão da arena num overworld de -64 a 320: fundo, mas longe da bedrock e da cidade antiga típica. */
    public static final int DEFAULT_ARENA_FLOOR = -30;
    /** A arena começa pelo menos tanto abaixo da entrada. */
    static final int MIN_DEPTH_BELOW_ENTRANCE = 70;
    /** Altura (acima do chão da arena) do salão profundo e do fim do trecho sinuoso: acima do teto (chão + 21). */
    public static final int MEANDER_ABOVE_FLOOR = 27;
    /** Distância horizontal do centro da arena até a ponta do túnel que encosta na porta. */
    static final int DOOR_TUNNEL_END = 27;
    static final int DOOR_APPROACH = 34;
    static final int DEEP_HALL_DISTANCE = 66;
    /** Folga de rocha entre um túnel do trecho sinuoso e o chão da superfície (ou do fundo do mar). */
    static final int ROOF_COVER = 6;
    static final int MAX_MEANDER_STEPS = 60;

    private ArenaCaveLayout() {
    }

    /** O traçado pronto, com os pontos que os testes conferem. */
    public record Plan(List<StructurePiece> pieces, BlockPos arenaCenter, int arenaFloor, Direction door,
            CavePieces.Arena arena, List<CavePieces.Tunnel> tunnels, List<CavePieces.Tunnel> finalDescent) {
    }

    public static int arenaFloorFor(int entranceY, int minBuildHeight) {
        int floor = Math.min(DEFAULT_ARENA_FLOOR, entranceY - MIN_DEPTH_BELOW_ENTRANCE);
        return Math.max(floor, minBuildHeight + 8);
    }

    /**
     * @param centerX,centerZ centro do chunk de início (o ponto do anel)
     * @param entrance        o bloco de chão onde abre a boca da caverna
     * @param ground          altura do chão sólido (sem água) numa coluna, para o túnel não aflorar nem furar o mar
     */
    public static Plan plan(RandomSource random, int centerX, int centerZ, BlockPos entrance, int arenaFloor,
            IntBinaryOperator ground) {
        List<StructurePiece> pieces = new ArrayList<>();
        List<CavePieces.Tunnel> tunnels = new ArrayList<>();

        // A arena: perto do centro, porta voltada para um lado sorteado.
        Direction door = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int arenaX = centerX + random.nextInt(25) - 12;
        int arenaZ = centerZ + random.nextInt(25) - 12;
        BlockPos arenaCenter = new BlockPos(arenaX, arenaFloor + 1, arenaZ);
        double ax = arenaX + 0.5;
        double az = arenaZ + 0.5;
        double meanderFloor = arenaFloor + MEANDER_ABOVE_FLOOR;

        // 1. Rampa da entrada.
        double shaftAngle = random.nextDouble() * Math.PI * 2;
        Vec3 mouth = new Vec3(entrance.getX() + 0.5, entrance.getY() + 1.5, entrance.getZ() + 0.5);
        Vec3 ramp1 = mouth.add(Math.cos(shaftAngle) * 5, -8, Math.sin(shaftAngle) * 5);
        Vec3 ramp2 = ramp1.add(Math.cos(shaftAngle + 0.5) * 7, -8, Math.sin(shaftAngle + 0.5) * 7);
        addTunnel(pieces, tunnels, mouth, ramp1, 2.6F);
        addTunnel(pieces, tunnels, ramp1, ramp2, 2.5F);
        // 2. A marca da superfície.
        pieces.add(new CavePieces.Entrance(entrance, (float) shaftAngle, random.nextInt()));

        // 3. Trecho sinuoso: gira em volta do centro, descendo até o nível do salão profundo.
        double angle = Math.atan2(ramp2.z - centerZ, ramp2.x - centerX);
        double spin = random.nextBoolean() ? 1 : -1;
        Vec3 current = ramp2;
        int step = 0;
        while (current.y > meanderFloor + 0.01 && step < MAX_MEANDER_STEPS) {
            step++;
            angle += spin * (0.55 + random.nextDouble() * 0.5);
            double radius = 22 + random.nextDouble() * 36;
            double nx = centerX + 0.5 + Math.cos(angle) * radius;
            double nz = centerZ + 0.5 + Math.sin(angle) * radius;
            double horizontal = Math.sqrt(Mth.square(nx - current.x) + Mth.square(nz - current.z));
            double drop = Math.max(4, horizontal * (0.35 + random.nextDouble() * 0.25));
            double ny = Math.max(meanderFloor, current.y - drop);
            float tunnelRadius = 2.2F + random.nextFloat();
            // Fica sempre debaixo de rocha: no ponto novo e no meio do trecho.
            double cover = tunnelRadius + ROOF_COVER;
            ny = Math.min(ny, ground.applyAsInt(Mth.floor(nx), Mth.floor(nz)) - cover);
            double mx = (nx + current.x) / 2;
            double mz = (nz + current.z) / 2;
            double midCeiling = ground.applyAsInt(Mth.floor(mx), Mth.floor(mz)) - cover;
            if ((ny + current.y) / 2 > midCeiling) {
                ny = Math.min(ny, 2 * midCeiling - current.y);
            }
            ny = Math.max(ny, meanderFloor);
            Vec3 next = new Vec3(nx, ny, nz);
            addTunnel(pieces, tunnels, current, next, tunnelRadius);
            if (step % 3 == 0 || random.nextInt(4) == 0) {
                pieces.add(new CavePieces.Room(BlockPos.containing(next), 5 + random.nextInt(4), 3 + random.nextInt(2),
                        5 + random.nextInt(4)));
            }
            current = next;
        }

        // 4. Salão profundo, do lado da porta, acima do nível do teto da arena.
        Vec3 hall = new Vec3(ax + door.getStepX() * DEEP_HALL_DISTANCE, meanderFloor,
                az + door.getStepZ() * DEEP_HALL_DISTANCE);
        addTunnel(pieces, tunnels, current, hall, 2.8F);
        pieces.add(new CavePieces.Room(BlockPos.containing(hall), 10, 5, 10));

        // 5. Descida final até a porta, sempre do lado de fora da parede.
        List<CavePieces.Tunnel> finalDescent = new ArrayList<>();
        Vec3 approach = new Vec3(ax + door.getStepX() * DOOR_APPROACH, arenaFloor + 3.5,
                az + door.getStepZ() * DOOR_APPROACH);
        Vec3 doorEnd = new Vec3(ax + door.getStepX() * DOOR_TUNNEL_END, arenaFloor + 3.0,
                az + door.getStepZ() * DOOR_TUNNEL_END);
        finalDescent.add(addTunnel(pieces, tunnels, hall, approach, 2.6F));
        finalDescent.add(addTunnel(pieces, tunnels, approach, doorEnd, 2.4F));

        // 6. A arena, por último.
        CavePieces.Arena arena = new CavePieces.Arena(arenaX, arenaFloor, arenaZ, door);
        pieces.add(arena);
        return new Plan(pieces, arenaCenter, arenaFloor, door, arena, tunnels, finalDescent);
    }

    private static CavePieces.Tunnel addTunnel(List<StructurePiece> pieces, List<CavePieces.Tunnel> tunnels, Vec3 from,
            Vec3 to, float radius) {
        CavePieces.Tunnel tunnel = new CavePieces.Tunnel(from, to, radius);
        pieces.add(tunnel);
        tunnels.add(tunnel);
        return tunnel;
    }
}
