package dev.madebyfelipe.iceagesurvival.core.firearms;

/**
 * Converte o dano do Dino Crisis 2 para a vida das criaturas do mod, de modo que cada arma precise do mesmo número de
 * tiros que no DC2 (D58). Sem classes do Minecraft.
 *
 * <p>Cada espécie diz, no bloco {@code firearm} do JSON, a vida que o bicho equivalente tem no DC2 (Normal) e se tem o
 * couro que segura as armas leves. O tiro tira da criatura a mesma <i>fração</i> da vida base da espécie que tiraria
 * do bicho do DC2: {@code dano = dano DC2 × vida base ÷ vida DC2}. Na vida base do nível 1 o número de tiros é o do
 * DC2; um indivíduo de nível alto, com mais vida, aguenta mais, mas só pela raiz da vida a mais
 * ({@link Target#levelFactor()}): com 4× a vida base, o dobro dos tiros.
 *
 * <p>Equivalentes do DC2 usados (guia oficial, Normal): Compsognathus/Oviraptor nível 1 ~400, raptor marrom/verde
 * ~800, Oviraptor nível 2 ~1000, raptor vermelho ~1600, Inostrancevia 2300 (couro), Alossauro 5000 (couro). O T-Rex e
 * o Giganotossauro não morrem no DC2; aqui o porte do Rex é proposto como o dobro do Alossauro (10 000, couro).
 */
public final class Ballistics {
    /** O couro do Alossauro segura 80% das armas leves: a pistola faz 80 nele no DC2, em vez de 400. */
    public static final double ARMORED_LIGHT_FACTOR = 0.2;
    /** Enfurecido, o bicho segura 75% de qualquer tiro (DC2). */
    public static final double RAGE_FACTOR = 0.25;
    /**
     * Para quem não tem o bloco {@code firearm} (jogadores, mobs vanilla): a escala do raptor, 800 do DC2 para 16 de
     * vida — a pistola tira 8, a metralhadora pesada 16, o antitanque 32.
     */
    public static final double GENERIC_DC2_PER_HEALTH = 50.0;

    private Ballistics() {
    }

    /**
     * Quem leva o tiro.
     *
     * @param baseHealth vida base da espécie, no nível 1
     * @param dc2Health  vida do bicho equivalente no DC2
     * @param armored    tem o couro que segura as armas leves
     * @param health     vida máxima real do indivíduo (nível, mutações, soro)
     */
    public record Target(double baseHealth, double dc2Health, boolean armored, double health) {
        /** Indivíduo de nível 1, com a vida base. */
        public Target(double baseHealth, double dc2Health, boolean armored) {
            this(baseHealth, dc2Health, armored, baseHealth);
        }

        /** Sem bloco {@code firearm}: escala do raptor, sem couro. */
        public static Target generic() {
            return new Target(1.0, GENERIC_DC2_PER_HEALTH, false);
        }

        /**
         * Quanto o tiro cresce com a vida do indivíduo: a raiz de vida real ÷ vida base. Um bicho com 4× a vida base
         * leva tiros 2× mais fortes e aguenta 2× mais tiros que no DC2, em vez de 4× (no nível alto as armas ficavam
         * fracas demais: cinco tiros de pistola num dodô).
         */
        public double levelFactor() {
            return baseHealth > 0 && health > baseHealth ? Math.sqrt(health / baseHealth) : 1.0;
        }
    }

    /**
     * O dano de um tiro, já na vida do mod.
     *
     * @param distance distância do atirador ao ponto atingido, em blocos
     * @param enraged  o alvo está enfurecido ({@link Rage})
     */
    public static double damage(Firearm gun, double distance, Target target, boolean enraged) {
        return dc2Damage(gun, distance, target.armored(), enraged) * target.baseHealth() / target.dc2Health()
                * target.levelFactor();
    }

    /** O dano que o tiro faria no DC2, com o couro e a fúria. */
    public static double dc2Damage(Firearm gun, double distance, boolean armored, boolean enraged) {
        double dc2 = gun.dc2Damage(distance);
        if (armored && gun.light()) {
            dc2 *= ARMORED_LIGHT_FACTOR;
        }
        if (enraged) {
            dc2 *= RAGE_FACTOR;
        }
        return dc2;
    }

    /** Quantos tiros matam o bicho do DC2 desta vida, sem fúria. */
    public static int hitsToKill(Firearm gun, double distance, double dc2Health, boolean armored) {
        return (int) Math.ceil(dc2Health / dc2Damage(gun, distance, armored, false) - 1e-9);
    }
}
