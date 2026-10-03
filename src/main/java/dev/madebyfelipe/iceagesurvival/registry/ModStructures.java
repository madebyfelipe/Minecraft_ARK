package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.world.cave.ArenaCaveStructure;
import dev.madebyfelipe.iceagesurvival.world.cave.CavePieces;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Estruturas do mod: a caverna da arena (D47), gerada por código — sem NBT — a partir de túneis, salas, a entrada
 * marcada na superfície e a arena fechada com o altar.
 */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, IceAgeSurvival.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, IceAgeSurvival.MODID);

    public static final RegistryObject<StructureType<ArenaCaveStructure>> ARENA_CAVE =
            STRUCTURE_TYPES.register("arena_cave", () -> () -> ArenaCaveStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CAVE_TUNNEL = PIECE_TYPES.register("arena_cave_tunnel",
            () -> (StructurePieceType.ContextlessType) CavePieces.Tunnel::new);
    public static final RegistryObject<StructurePieceType> CAVE_ROOM = PIECE_TYPES.register("arena_cave_room",
            () -> (StructurePieceType.ContextlessType) CavePieces.Room::new);
    public static final RegistryObject<StructurePieceType> CAVE_ENTRANCE = PIECE_TYPES.register("arena_cave_entrance",
            () -> (StructurePieceType.ContextlessType) CavePieces.Entrance::new);
    public static final RegistryObject<StructurePieceType> CAVE_ARENA = PIECE_TYPES.register("arena_cave_arena",
            () -> (StructurePieceType.ContextlessType) CavePieces.Arena::new);

    /** A estrutura em JSON ({@code data/iceagesurvival/worldgen/structure/arena_cave.json}). */
    public static final ResourceKey<Structure> ARENA_CAVE_STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, IceAgeSurvival.id("arena_cave"));
    /** O conjunto em anéis concêntricos que espalha as cavernas pelo mundo. */
    public static final ResourceKey<StructureSet> ARENA_CAVE_SET =
            ResourceKey.create(Registries.STRUCTURE_SET, IceAgeSurvival.id("arena_caves"));
    /** O que o rastreador procura com {@code findNearestMapStructure}. */
    public static final TagKey<Structure> ARENA_CAVES = TagKey.create(Registries.STRUCTURE, IceAgeSurvival.id("arena_cave"));

    private ModStructures() {
    }

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        PIECE_TYPES.register(bus);
    }
}
