package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.logging.LogUtils;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;

/**
 * Roda só parte da suíte no servidor de GameTest: {@code ./gradlew runGameTestServer -Ptests=Base,Outpost} mantém só os
 * testes das classes {@code BaseTests} e {@code OutpostTests} (o sufixo {@code Tests} é opcional). Sem a propriedade,
 * roda tudo. Serve para iterar; antes de integrar, a suíte inteira continua obrigatória.
 *
 * <p>O Forge registra e agrupa os testes em lotes no {@code Main}, sem gancho no meio; por isso o filtro refaz os
 * lotes do {@link GameTestServer} antes do primeiro tick, quando o servidor anuncia que vai começar.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public final class GameTestFilter {
    /** Propriedade de sistema que o {@code build.gradle} preenche com {@code -Ptests}. */
    public static final String PROPERTY = "iceagesurvival.gametests";
    private static final String PACKAGE = GameTestFilter.class.getPackageName() + ".";
    private static final Logger LOGGER = LogUtils.getLogger();

    private GameTestFilter() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        String filter = System.getProperty(PROPERTY, "").trim();
        if (filter.isEmpty() || !(event.getServer() instanceof GameTestServer server)) {
            return;
        }
        Set<String> wanted = testNames(filter);
        List<GameTestBatch> batches = ObfuscationReflectionHelper.getPrivateValue(GameTestServer.class, server,
                "f_177587_");
        List<GameTestBatch> kept = new ArrayList<>();
        int tests = 0;
        for (GameTestBatch batch : batches) {
            List<TestFunction> functions = batch.getTestFunctions().stream()
                    .filter(function -> wanted.contains(function.getTestName().toLowerCase(Locale.ROOT))).toList();
            if (!functions.isEmpty()) {
                kept.add(new GameTestBatch(batch.getName(), functions, batch::runBeforeBatchFunction,
                        batch::runAfterBatchFunction));
                tests += functions.size();
            }
        }
        batches.clear();
        batches.addAll(kept);
        LOGGER.info("Filtro de GameTests '{}': {} testes em {} lotes", filter, tests, kept.size());
    }

    /** Nomes dos testes das classes pedidas, como o registro os chama: o método em minúsculas, com ou sem a classe. */
    private static Set<String> testNames(String filter) {
        Set<String> names = new HashSet<>();
        for (String token : filter.split(",")) {
            String simple = token.trim();
            if (simple.isEmpty()) {
                continue;
            }
            if (!simple.endsWith("Tests")) {
                simple += "Tests";
            }
            Class<?> type;
            try {
                type = Class.forName(PACKAGE + simple);
            } catch (ClassNotFoundException missing) {
                throw new IllegalArgumentException("Filtro de GameTests: classe " + PACKAGE + simple + " não existe");
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GameTest.class)) {
                    String name = method.getName().toLowerCase(Locale.ROOT);
                    names.add(name);
                    names.add(simple.toLowerCase(Locale.ROOT) + "." + name);
                }
            }
        }
        return names;
    }
}
