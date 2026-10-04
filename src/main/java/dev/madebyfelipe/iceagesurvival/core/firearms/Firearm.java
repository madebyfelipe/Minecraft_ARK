package dev.madebyfelipe.iceagesurvival.core.firearms;

/**
 * As armas de fogo no estilo do Dino Crisis 2 (D58). Lógica pura, sem Minecraft: o item pergunta aqui o dano, o ritmo,
 * o pente e a munição; a conversão para a vida das criaturas fica em {@link Ballistics}.
 *
 * <p>O dano é o do DC2 na dificuldade Normal, contra o raptor (que não tem resistência): pistola 400, escopeta 600 de
 * perto e 400 de longe, submetralhadora 200, metralhadora pesada 800, canhão sólido 1500 e rifle antitanque 1600 (dados
 * de dano levantados pela comunidade no GameFAQs e do guia oficial da Famitsu; os atributos Ataque/Velocidade/Alcance
 * entre parênteses são os do guia e guiaram o ritmo e o alcance daqui).
 *
 * <p>Ritmo, pente, recarga e alcance não existem no DC2 (lá não há pente): números propostos, ajustáveis aqui.
 */
public enum Firearm {
    /** Pistola da Regina (Ataque 20, Velocidade 80, Alcance 30): semiautomática, rápida, alcance curto. */
    HANDGUN("handgun", AmmoFamily.LIGHT_ROUNDS, 400, 400, 0.0, 40.0, 5, false, 15, 30, 1, 0.5, true, true, false),
    /** Escopeta do Dylan (30/70/70): o cone de chumbo pega mais de um bicho; 600 de perto, 400 de longe. */
    SHOTGUN("shotgun", AmmoFamily.SHELLS, 400, 600, 5.0, 24.0, 16, false, 8, 50, 8, 5.0, true, true, false),
    /** Submetralhadora da Regina (20/90/80): automática, fraca por tiro, atira correndo. */
    SUBMACHINE_GUN("submachine_gun", AmmoFamily.LIGHT_ROUNDS, 200, 200, 0.0, 48.0, 2, true, 50, 45, 1, 2.2, true,
            true, false),
    /** Metralhadora pesada da Regina (60/90/40): automática e forte, anda devagar atirando. */
    HEAVY_MACHINE_GUN("heavy_machine_gun", AmmoFamily.LIGHT_ROUNDS, 800, 800, 0.0, 40.0, 3, true, 100, 80, 1, 1.6,
            false, true, false),
    /** Canhão sólido do Dylan (70/60/50): lança a esfera que destrói células por vibração. */
    SOLID_CANNON("solid_cannon", AmmoFamily.SOLID_CELLS, 1500, 1500, 0.0, 48.0, 20, false, 6, 50, 1, 0.0, false, true,
            false),
    /**
     * Rifle antitanque do Dylan (90/30/70): atravessa tudo o que está na linha. Não provoca a fúria (no DC2 ele, os
     * foguetes e os mísseis matam sem enfurecer), mas o bicho já enfurecido segura o tiro dele também.
     */
    ANTI_TANK_RIFLE("anti_tank_rifle", AmmoFamily.HEAVY_ROUNDS, 1600, 1600, 0.0, 96.0, 40, false, 5, 60, 1, 0.0, false,
            false, true);

    private final String id;
    private final AmmoFamily ammo;
    private final double damage;
    private final double closeDamage;
    private final double closeRange;
    private final double range;
    private final int interval;
    private final boolean automatic;
    private final int magazine;
    private final int reloadTicks;
    private final int pellets;
    private final double spread;
    private final boolean light;
    private final boolean buildsRage;
    private final boolean piercing;

    /**
     * @param damage      dano do DC2 por tiro (de longe, na escopeta)
     * @param closeDamage dano do DC2 a até {@code closeRange} blocos
     * @param range       alcance, em blocos
     * @param interval    ticks entre um tiro e o próximo
     * @param automatic   segurar o botão atira sem parar
     * @param magazine    tiros no pente
     * @param reloadTicks ticks de recarga
     * @param pellets     raios por tiro (o chumbo da escopeta); quem é atingido por qualquer um leva o tiro inteiro
     * @param spread      desvio de cada raio, em graus
     * @param light       arma leve: o couro do Alossauro segura 80% dela ({@link Ballistics#ARMORED_LIGHT_FACTOR})
     * @param buildsRage  cada acerto conta para a fúria ({@link Rage})
     * @param piercing    o tiro atravessa as criaturas e segue até o primeiro bloco
     */
    Firearm(String id, AmmoFamily ammo, double damage, double closeDamage, double closeRange, double range,
            int interval, boolean automatic, int magazine, int reloadTicks, int pellets, double spread, boolean light,
            boolean buildsRage, boolean piercing) {
        this.id = id;
        this.ammo = ammo;
        this.damage = damage;
        this.closeDamage = closeDamage;
        this.closeRange = closeRange;
        this.range = range;
        this.interval = interval;
        this.automatic = automatic;
        this.magazine = magazine;
        this.reloadTicks = reloadTicks;
        this.pellets = pellets;
        this.spread = spread;
        this.light = light;
        this.buildsRage = buildsRage;
        this.piercing = piercing;
    }

    /** O nome do item ({@code iceagesurvival:<id>}). */
    public String id() {
        return id;
    }

    public AmmoFamily ammo() {
        return ammo;
    }

    /** Dano do DC2 de um tiro que acerta a esta distância, antes de qualquer resistência. */
    public double dc2Damage(double distance) {
        return distance <= closeRange ? closeDamage : damage;
    }

    public double range() {
        return range;
    }

    public int interval() {
        return interval;
    }

    public boolean automatic() {
        return automatic;
    }

    public int magazine() {
        return magazine;
    }

    public int reloadTicks() {
        return reloadTicks;
    }

    public int pellets() {
        return pellets;
    }

    public double spread() {
        return spread;
    }

    public boolean light() {
        return light;
    }

    public boolean buildsRage() {
        return buildsRage;
    }

    public boolean piercing() {
        return piercing;
    }
}
