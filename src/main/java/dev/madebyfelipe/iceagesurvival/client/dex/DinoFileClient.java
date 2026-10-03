package dev.madebyfelipe.iceagesurvival.client.dex;

import dev.madebyfelipe.iceagesurvival.network.DinoFilePayload;
import dev.madebyfelipe.iceagesurvival.network.ScanResultPayload;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/**
 * A DINO FILE do jogador no cliente: as espécies registradas (do servidor) e a última leitura de cada uma, para a
 * ANÁLISE DO INDIVÍDUO. Terminado um scan, abre o terminal na ficha da espécie.
 */
public final class DinoFileClient {
    private static final Set<String> REGISTERED = new LinkedHashSet<>();
    private static final Map<String, ScanResultPayload> LAST_SCANS = new HashMap<>();

    private DinoFileClient() {
    }

    public static void receive(DinoFilePayload payload) {
        REGISTERED.clear();
        payload.registered().forEach(species -> REGISTERED.add(species.toString()));
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
    }

    public static boolean isRegistered(String species) {
        return REGISTERED.contains(species);
    }

    public static Optional<ScanResultPayload> lastScan(String species) {
        return Optional.ofNullable(LAST_SCANS.get(species));
    }
}
