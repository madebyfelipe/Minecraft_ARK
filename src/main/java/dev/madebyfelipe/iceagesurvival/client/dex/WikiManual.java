package dev.madebyfelipe.iceagesurvival.client.dex;

import com.mojang.logging.LogUtils;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.wiki.Manual;
import dev.madebyfelipe.iceagesurvival.core.wiki.ManualParser;
import java.io.Reader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * O manual do Analisador ({@code assets/iceagesurvival/wiki/manual.json}), lido como recurso do cliente: recarrega
 * com os pacotes de recursos (F3+T). Sem o arquivo ou com ele quebrado, o terminal mostra o manual vazio.
 */
public final class WikiManual extends SimplePreparableReloadListener<Manual> {
    public static final WikiManual INSTANCE = new WikiManual();
    private static final ResourceLocation FILE = IceAgeSurvival.id("wiki/manual.json");
    private static final Logger LOGGER = LogUtils.getLogger();

    private Manual manual = Manual.EMPTY;

    private WikiManual() {
    }

    public static Manual get() {
        return INSTANCE.manual;
    }

    @Override
    protected Manual prepare(ResourceManager resources, ProfilerFiller profiler) {
        return resources.getResource(FILE).map(resource -> {
            try (Reader reader = resource.openAsReader()) {
                return ManualParser.parse(reader);
            } catch (Exception exception) {
                LOGGER.error("Manual do Analisador ilegível: {}", FILE, exception);
                return Manual.EMPTY;
            }
        }).orElseGet(() -> {
            LOGGER.warn("Manual do Analisador não encontrado: {}", FILE);
            return Manual.EMPTY;
        });
    }

    @Override
    protected void apply(Manual prepared, ResourceManager resources, ProfilerFiller profiler) {
        manual = prepared;
    }
}
