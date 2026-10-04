package dev.madebyfelipe.iceagesurvival.client.dex;

import dev.madebyfelipe.iceagesurvival.network.DinoFilePayload;
import dev.madebyfelipe.iceagesurvival.network.ScanResultPayload;
import dev.madebyfelipe.iceagesurvival.network.TerminalReadPayload;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/**
 * A DINO FILE do jogador no cliente: as espécies registradas (do servidor) e a última leitura de cada uma, para a
 * ANÁLISE DO INDIVÍDUO, e quantos registros militares ele já recuperou. Terminado um scan, abre o terminal na ficha da
 * espécie; lido um terminal militar com registro novo, na aba REGISTROS.
 */
public final class DinoFileClient {
    private static final Set<String> REGISTERED = new LinkedHashSet<>();
    private static final Map<String, ScanResultPayload> LAST_SCANS = new HashMap<>();
    private static final Map<String, Integer> RECORDS = new HashMap<>();

    private DinoFileClient() {
    }

    public static void receive(DinoFilePayload payload) {
        REGISTERED.clear();
        payload.registered().forEach(species -> REGISTERED.add(species.toString()));
        RECORDS.clear();
        RECORDS.putAll(payload.records());
    }

    /**
     * Leu um terminal. Registro novo que o manual tem: abre a aba REGISTROS nele. Senão, só um aviso na barra: este
     * terminal já foi lido, ou não há mais registros a recuperar.
     */
    public static void receiveTerminal(TerminalReadPayload payload) {
        RECORDS.put(payload.series(), payload.records());
        int total = WikiManual.get().records(payload.series()).size();
        if (payload.newRecord() && payload.records() <= total) {
            AnalyzerScreen.openOnRecord(payload.series(), payload.records() - 1);
            return;
        }
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(payload.newRecord()
                    ? "iceagesurvival.analyzer.terminal.empty" : "iceagesurvival.analyzer.terminal.already_read"), true);
        }
    }

    /** Quantos registros da série estão destravados (os primeiros dela no manual, na ordem). */
    public static int records(String series) {
        return RECORDS.getOrDefault(series, 0);
    }

    public static void receiveScan(ScanResultPayload payload) {
        String species = payload.species().toString();
        REGISTERED.add(species);
        LAST_SCANS.put(species, payload);
        AnalyzerScreen.openOnScan(species, payload.newEntry());
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        REGISTERED.clear();
        LAST_SCANS.clear();
        RECORDS.clear();
    }

    public static boolean isRegistered(String species) {
        return REGISTERED.contains(species);
    }

    public static Optional<ScanResultPayload> lastScan(String species) {
        return Optional.ofNullable(LAST_SCANS.get(species));
    }
}
