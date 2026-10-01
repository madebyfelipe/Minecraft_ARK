package dev.madebyfelipe.iceagesurvival.core.spawn;

import java.util.List;

/**
 * Espaçamento entre grupos de uma espécie, sem Minecraft: "uma manada de Brontossauro a cada 300
 * blocos". Cada grupo selvagem deixa uma marca ({@link Claim}) que acompanha onde ele está; um grupo
 * novo só nasce a mais de {@code spacing} blocos de toda marca.
 *
 * <p>A marca e não uma contagem de entidades porque a geração do terreno e a reposição não enxergam o
 * que está em chunk descarregado: duas manadas a 250 blocos uma da outra nunca estão carregadas juntas.
 */
public final class SpeciesSpacing {
    /**
     * Raio do "mesmo grupo": quem nasce ou aparece até aqui de uma marca é da manada dela. Cobre o
     * espalhamento de uma manada (o líder chama de volta quem passa de 18 blocos) com folga.
     */
    public static final double GROUP_RADIUS = 64.0;
    /** Marca de grupo que não foi visto por este tempo, com o lugar carregado, é apagada. */
    public static final long GRACE_TICKS = 1200L;

    private SpeciesSpacing() {
    }

    /** Onde um grupo foi visto pela última vez e quando. */
    public record Claim(int x, int z, long lastSeen) {
        double distanceSqr(double px, double pz) {
            double dx = px - x;
            double dz = pz - z;
            return dx * dx + dz * dz;
        }
    }

    public enum Verdict {
        /** Nenhum grupo no raio: pode nascer um grupo novo (e marcar o lugar). */
        NEW_GROUP,
        /** Perto da marca de um grupo: é membro dele. */
        SAME_GROUP,
        /** Outro grupo a menos de {@code spacing} blocos. */
        TOO_CLOSE
    }

    /** O que um indivíduo em (x, z) é, diante das marcas que já existem. */
    public static Verdict judge(List<Claim> claims, double x, double z, int spacing) {
        if (spacing <= 0) {
            return Verdict.NEW_GROUP;
        }
        int nearest = nearest(claims, x, z);
        if (nearest < 0) {
            return Verdict.NEW_GROUP;
        }
        double distanceSqr = claims.get(nearest).distanceSqr(x, z);
        if (distanceSqr <= GROUP_RADIUS * GROUP_RADIUS) {
            return Verdict.SAME_GROUP;
        }
        return distanceSqr < (double) spacing * spacing ? Verdict.TOO_CLOSE : Verdict.NEW_GROUP;
    }

    /** Índice da marca mais próxima, ou -1 se não há nenhuma. */
    public static int nearest(List<Claim> claims, double x, double z) {
        int best = -1;
        double bestSqr = Double.MAX_VALUE;
        for (int index = 0; index < claims.size(); index++) {
            double distanceSqr = claims.get(index).distanceSqr(x, z);
            if (distanceSqr < bestSqr) {
                bestSqr = distanceSqr;
                best = index;
            }
        }
        return best;
    }

    /** Marca a apagar: o lugar está carregado, ninguém do grupo está lá e passou o prazo. */
    public static boolean expired(Claim claim, long now) {
        return now - claim.lastSeen() > GRACE_TICKS;
    }
}
