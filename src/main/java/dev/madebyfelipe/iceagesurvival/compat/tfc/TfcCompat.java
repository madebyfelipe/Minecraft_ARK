package dev.madebyfelipe.iceagesurvival.compat.tfc;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.world.BiomeWeights;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * Integração opcional com o TerraFirmaCraft. Sem o TFC instalado, tudo aqui responde "não" e nada do TFC é
 * carregado. Com ele:
 * <ul>
 *   <li>o datapack embutido {@code datapacks/tfc_compat} troca o preset Era do Gelo pelo gerador do TFC, frio
 *   (≈ −10 °C constantes);</li>
 *   <li>o frio do jogador lê o clima do TFC, porque todo bioma dele diz 0,5 de temperatura;</li>
 *   <li>no mundo padrão do TFC, os biomas primitivos pesam o dobro no sorteio ({@code ChooseBiomesMixin}).</li>
 * </ul>
 */
public final class TfcCompat {
    public static final String MODID = "tfc";
    /** Quanto pesam os biomas primitivos no mundo padrão do TFC (decisão do Felipe: o dobro). */
    public static final int PRIMITIVE_WEIGHT = 2;
    private static final String PACK_ID = "iceagesurvival_tfc_compat";
    private static final String PACK_PATH = "datapacks/tfc_compat";

    /** Se o TFC está instalado; nulo até a lista de mods existir. */
    private static volatile Boolean loaded;
    private static volatile boolean primitiveWeighting;
    private static volatile Set<Integer> primitiveBiomes;
    private static final Map<int[], int[]> WEIGHTED = new ConcurrentHashMap<>();

    private TfcCompat() {
    }

    public static boolean isLoaded() {
        Boolean known = loaded;
        if (known != null) {
            return known;
        }
        ModList mods = ModList.get();
        if (mods != null) {
            loaded = mods.isLoaded(MODID);
            return loaded;
        }
        // Cedo demais para o ModList (mixins): a lista de carregamento já sabe.
        return LoadingModList.get() != null && LoadingModList.get().getModFileById(MODID) != null;
    }

    public static boolean isTfcGenerator(ChunkGenerator generator) {
        return isLoaded() && TfcBridge.isTfcGenerator(generator);
    }

    /** O Overworld veio do preset Era do Gelo com o gerador do TFC. */
    public static boolean isIceAgeGenerator(ChunkGenerator generator) {
        return isLoaded() && TfcBridge.isIceAgeGenerator(generator);
    }

    /** A temperatura do clima do TFC, na escala de bioma do vanilla; vazia fora de um mundo TFC. */
    public static OptionalDouble biomeTemperature(ServerLevel level, BlockPos pos) {
        if (!isTfcGenerator(level.getChunkSource().getGenerator())) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(TfcBridge.vanillaTemperature(level, pos));
    }

    /** Registra o datapack de compatibilidade, obrigatório e sempre ligado, só com o TFC. */
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !isLoaded()) {
            return;
        }
        Path path = ModList.get().getModFileById(IceAgeSurvival.MODID).getFile().findResource(PACK_PATH);
        Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Ice Age Survival + TerraFirmaCraft"), true,
                id -> new PathPackResources(id, path, true), PackType.SERVER_DATA, Pack.Position.TOP,
                PackSource.BUILT_IN);
        if (pack == null) {
            IceAgeSurvival.LOGGER.error("Datapack de compatibilidade com o TFC não encontrado em {}", path);
            return;
        }
        event.addRepositorySource(consumer -> consumer.accept(pack));
    }

    /**
     * Carregado o Overworld (antes de gerar o primeiro chunk): os primitivos pesam mais só no mundo padrão do TFC —
     * no preset Era do Gelo, não.
     */
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        primitiveWeighting = isTfcGenerator(generator) && !isIceAgeGenerator(generator);
    }

    /** Chamado pelo mixin a cada sorteio de bioma do TFC. */
    public static int[] weighChoices(int[] choices) {
        if (!primitiveWeighting) {
            return choices;
        }
        Set<Integer> primitive = primitiveBiomes;
        if (primitive == null) {
            primitive = TfcBridge.primitiveBiomes();
            primitiveBiomes = primitive;
        }
        Set<Integer> favored = primitive;
        return WEIGHTED.computeIfAbsent(choices,
                original -> BiomeWeights.weighted(original, favored::contains, PRIMITIVE_WEIGHT));
    }
}
