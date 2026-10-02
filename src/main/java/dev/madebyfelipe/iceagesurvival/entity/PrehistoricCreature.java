package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.command.Movement;
import dev.madebyfelipe.iceagesurvival.core.command.Obedience;
import dev.madebyfelipe.iceagesurvival.core.command.Stance;
import dev.madebyfelipe.iceagesurvival.core.genetics.Genetics;
import dev.madebyfelipe.iceagesurvival.core.genetics.Genome;
import dev.madebyfelipe.iceagesurvival.genetics.GenomeNbt;
import dev.madebyfelipe.iceagesurvival.item.CreatureEggItem;
import dev.madebyfelipe.iceagesurvival.species.BreedingProfile;
import dev.madebyfelipe.iceagesurvival.core.ecology.TargetPriority;
import dev.madebyfelipe.iceagesurvival.entity.ai.HuntGoal;
import dev.madebyfelipe.iceagesurvival.core.spawn.DangerZones;
import dev.madebyfelipe.iceagesurvival.core.spawn.SpeciesSpacing;
import dev.madebyfelipe.iceagesurvival.world.CreatureLocator;
import dev.madebyfelipe.iceagesurvival.world.GroupSpacing;
import dev.madebyfelipe.iceagesurvival.core.stats.Stat;
import dev.madebyfelipe.iceagesurvival.core.stats.StatPoints;
import dev.madebyfelipe.iceagesurvival.core.stats.StatProfile;
import dev.madebyfelipe.iceagesurvival.core.stats.WildLevels;
import dev.madebyfelipe.iceagesurvival.core.taming.TamingRules;
import dev.madebyfelipe.iceagesurvival.core.taming.TamingSession;
import dev.madebyfelipe.iceagesurvival.entity.ai.OrderGoals;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.BehaviorProfile;
import dev.madebyfelipe.iceagesurvival.species.BodyProfile;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import dev.madebyfelipe.iceagesurvival.species.SoundProfile;
import dev.madebyfelipe.iceagesurvival.species.SpawnProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import dev.madebyfelipe.iceagesurvival.species.StorageProfile;
import dev.madebyfelipe.iceagesurvival.species.WarinessProfile;
import dev.madebyfelipe.iceagesurvival.species.FamilyProfile;
import dev.madebyfelipe.iceagesurvival.core.mount.FlightModel;
import dev.madebyfelipe.iceagesurvival.core.mount.FlightStamina;
import dev.madebyfelipe.iceagesurvival.core.ecology.Hunger;
import dev.madebyfelipe.iceagesurvival.core.ecology.Perception;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials;
import dev.madebyfelipe.iceagesurvival.core.ecology.Stress;
import dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse;
import dev.madebyfelipe.iceagesurvival.species.EcologyProfile;
import dev.madebyfelipe.iceagesurvival.core.mount.MountedReach;
import dev.madebyfelipe.iceagesurvival.species.TamingProfile;
import dev.madebyfelipe.iceagesurvival.menu.CreatureStorageMenu;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

/**
 * Base de toda criatura do mod. Guarda os pontos de atributo do indivíduo e os
 * aplica sobre a curva da {@link Species} correspondente ao tipo da entidade,
 * e conduz o ciclo torpor → inconsciente → alimentação → domesticada.
 *
 * <p>Dono e estado domesticado vêm de {@link TamableAnimal}. O controle da montaria
 * reaproveita o modelo de veículo do vanilla ({@code travelRidden}): o cliente de quem
 * monta simula o movimento e o servidor valida, como num cavalo.
 */
public abstract class PrehistoricCreature extends TamableAnimal {
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TORPOR_FRACTION =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_UNCONSCIOUS =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    /** Desmaiada, com quem a derrubou, e sem nada que coma no inventário: a domesticação parou. */
    private static final EntityDataAccessor<Boolean> DATA_NEEDS_TAMING_FOOD =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TAMING_PROGRESS =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_MOVEMENT =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STANCE =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_FEMALE =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MATING =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SADDLED =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    /** Fôlego de voo restante, de 0 a 1 (o cliente de quem monta precisa dele para limitar a subida). */
    private static final EntityDataAccessor<Float> DATA_FLIGHT_STAMINA =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.FLOAT);
    /** Domesticada que morreu: o corpo fica no chão com o inventário e o implante. */
    private static final EntityDataAccessor<Boolean> DATA_CORPSE =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.BOOLEAN);
    /** Estresse, de 0 a 100: o painel sob a mira mostra o humor. */
    private static final EntityDataAccessor<Float> DATA_STRESS =
            SynchedEntityData.defineId(PrehistoricCreature.class, EntityDataSerializers.FLOAT);
    private static final String TAG_STRESS = "Stress";
    private static final String TAG_TICKS_SINCE_MEAL = "TicksSinceMeal";
    /** Intervalo da atualização do estresse (volta ao repouso). */
    private static final int STRESS_INTERVAL = 20;
    /** Por quanto tempo a presa se sabe caçada depois do último aviso. */
    private static final int HUNTED_MEMORY_TICKS = 100;
    /** Quanto tempo quem perdeu a disputa foge do rival. */
    private static final int YIELD_TICKS = 200;
    /** Numa briga entre rivais, quem cai abaixo desta fração da vida desiste e foge. */
    private static final float RIVAL_YIELD_HEALTH = 0.5F;

    private static final String TAG_STAT_POINTS = "StatPoints";
    private static final String TAG_TORPOR = "Torpor";
    private static final String TAG_UNCONSCIOUS = "Unconscious";
    private static final String TAG_TAMING = "Taming";
    private static final String TAG_TAMING_FOOD = "Food";
    private static final String TAG_TAMING_QUALITY = "Quality";
    private static final String TAG_TAMING_DAMAGE = "Damage";
    private static final String TAG_NEXT_FEED_TIME = "NextFeedTime";
    private static final String TAG_AFFINITY = "Affinity";
    private static final String TAG_HOME = "Home";
    private static final String TAG_GROUP = "Group";
    /** Raio em que se procura o bando: longe assim, o membro ainda volta para junto dos outros. */
    public static final double GROUP_RANGE = 64.0;
    /** Bando sem {@code group_max} na espécie (lobo desligado, criatura de teste). */
    private static final int DEFAULT_GROUP_MAX = 6;
    /** Ordem única de antes dos assobios; só lida, para converter mundos antigos. */
    private static final String TAG_LEGACY_ORDER = "Order";
    private static final String TAG_MOVEMENT = "Movement";
    private static final String TAG_STANCE = "Stance";
    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_TAMER = "Tamer";
    private static final String TAG_SADDLED = "Saddled";
    private static final String TAG_MUTATIONS = "Mutations";
    private static final String TAG_HEALTH_GENE = "HealthGene";
    private static final String TAG_FEMALE = "Female";
    private static final String TAG_FLIGHT_STAMINA = "FlightStamina";
    private static final String TAG_CORPSE = "Corpse";
    private static final String TAG_CORPSE_TICKS = "CorpseTicks";
    private static final String TAG_MATING = "Mating";
    private static final String TAG_NEXT_MATING = "NextMating";
    private static final String TAG_GESTATION_END = "GestationEnd";
    private static final String TAG_GESTATION_CHILD = "GestationChild";
    private static final String TAG_ZONE_CHECKED = "ZoneChecked";
    private static final String TAG_SPACING_CHECKED = "SpacingChecked";
    /** Quem do bando está a até este raio come junto e reparte a presa. */
    private static final double MEAL_SHARE_RADIUS = 16.0;
    /** Mesmo a menor presa mata um pouco da fome. */
    private static final double MIN_MEAL_FRACTION = 0.1;
    /** A cada quanto a domesticada grava onde está, para o menu "localizar criatura", em ticks. */
    private static final int LOCATOR_UPDATE_INTERVAL = 100;
    /** A cada quanto um indivíduo selvagem renova a marca do grupo dele, em ticks. */
    private static final int SPACING_REPORT_INTERVAL = 600;
    /** Queda máxima, em blocos/tick, de uma espécie voadora fora do voo: ela plana. */
    private static final double GLIDE_FALL_SPEED = 0.2;
    private static final String TAG_TERRITORY = "Territory";
    /** Por quanto tempo um animal que encarou este predador ainda conta como ameaça para ele. */
    private static final int INTIMIDATION_TICKS = 60;

    /** Segundos juntos, com o acasalamento ligado, até a fêmea conceber. */
    public static final int MATING_SECONDS = 10;
    /** Distância máxima entre os dois para cruzar. */
    public static final double MATING_RADIUS = 8.0;
    /** Tamanho do filhote em relação ao adulto (o vanilla usa 0,5). */
    private static final float BABY_SCALE = 0.4F;

    private static final int TORPOR_UPDATE_INTERVAL_TICKS = 20;
    /** Afinidade inicial de uma domesticação com eficiência de 100%. */
    private static final float MAX_INITIAL_AFFINITY = 50.0F;
    public static final float MAX_AFFINITY = 100.0F;
    /** Uma criatura recém-domesticada segue o dono e o defende. */
    private static final Movement DEFAULT_MOVEMENT = Movement.FOLLOW;
    private static final Stance DEFAULT_STANCE = Stance.DEFEND;
    /** Espera entre duas alimentações de uma criatura já domesticada. */
    private static final int TAMED_FEED_INTERVAL_TICKS = 30 * 20;
    /** Afinidade ganha ao dar o alimento preferido a uma criatura domesticada. */
    private static final float AFFINITY_PER_FEED = 5.0F;
    /**
     * Cura por porção de comida, em fração da vida máxima por ponto de {@code value} do alimento: a
     * melhor comida da espécie (valor 40) cura 20%. Com valor fixo em pontos, a carne mal fazia
     * cócegas num T-Rex de 220 de vida.
     */
    private static final double HEAL_FRACTION_PER_FOOD_VALUE = 0.005;
    private static final double MIN_HEAL_FRACTION = 0.03;
    private static final double MAX_HEAL_FRACTION = 0.25;
    /** Ferida, aceita comida da mão a cada meio segundo, para não gastar a pilha num clique segurado. */
    private static final int HEAL_FEED_COOLDOWN_TICKS = 10;
    /** Ferida e domesticada, come sozinha do próprio inventário a cada 5 s. */
    private static final int SELF_HEAL_INTERVAL_TICKS = 100;
    private long nextHealTime;
    /** Uma criatura domesticada larga o alvo que se afastar mais que isto. */
    private static final double TAMED_TARGET_LEASH = 40.0;
    /** Uma fileira de baú. */
    public static final int INVENTORY_SIZE = 9;
    private static final double INVENTORY_REACH = 8.0;
    /** Item que sela uma criatura montável. */
    public static final net.minecraft.world.item.Item SADDLE_ITEM = Items.SADDLE;
    /** Alcance da mordida de quem monta, a partir da caixa de colisão da criatura. */
    /** Alcance mínimo do golpe montado; o de cada montaria é {@link #riddenReach()}. */
    public static final double RIDDEN_ATTACK_REACH = MountedReach.BASE_REACH;
    /** Quantos blocos à frente do corpo a mordida de quem monta quebra. */
    /** Ticks entre dois ataques de quem monta. */
    private static final int RIDDEN_ATTACK_COOLDOWN = 20;
    /** Recuo da ré de quem monta, como no cavalo: andar para trás é bem mais lento. */
    private static final float RIDDEN_BACKWARD_FACTOR = 0.25F;
    /** Fator lateral de quem monta, como no cavalo. */
    private static final float RIDDEN_STRAFE_FACTOR = 0.5F;

    private StatPoints statPoints = StatPoints.NONE;
    /** Mutações por atributo herdadas; com os pontos e o gene, formam o {@link #genome()}. */
    private int[] mutations = new int[Stat.values().length];
    private boolean healthGene;
    private boolean statsRolled;
    private double torpor;
    private TamingSession tamingSession = new TamingSession();
    private long nextRiderAttackTime;
    /** Durante o golpe de quem monta: a animação e o som já saíram, {@link #doHurtTarget} não repete. */
    private boolean attackSwung;
    /** Game time a partir do qual a criatura aceita comer de novo. */
    private long nextFeedTime;
    /** Segundos seguidos perto de um parceiro com o acasalamento ligado. */
    private int matingProgress;
    /** Game time a partir do qual a fêmea pode cruzar de novo. */
    private long nextMatingTime;
    /** Game time do parto; 0 = não está prenhe. */
    private long gestationEnd;
    @Nullable
    private Genome gestationChild;
    private float affinity;
    /** Centro do território: onde a criatura entrou no mundo pela primeira vez. */
    @Nullable
    private BlockPos homePos;
    /**
     * Bando desta criatura selvagem. Quem nasce junto (geração do terreno, reposição, família) sai no
     * mesmo bando; a sem bando (mundo antigo, ovo, comando) é adotada pelo bando da espécie mais perto
     * que tenha vaga. Domesticada, sai do bando. Dois bandos não se fundem.
     */
    @Nullable
    private UUID groupId;
    /** Adotando um bando agora: trava contra reentrada (a adoção conta membros de outros bandos). */
    private boolean adopting;
    /** Intruso que esta criatura está expulsando do território ({@code TerritoryGoal}). */
    @Nullable
    private LivingEntity territorialFoe;
    /** Quem derrubou a criatura: só essa pessoa mexe no inventário e é ela quem fica com a criatura. */
    @Nullable
    private UUID tamerUUID;
    private boolean breaksLeaves;
    /** Espreitando o alvo em vez de persegui-lo (só no servidor). */
    private boolean stalking;
    /** Até quando a presa sabe que esta criatura a espreita (viu, ou o bando deu o bote). */
    private long stalkBlownUntil = Long.MIN_VALUE;
    @Nullable
    private Species perceptionSpecies;
    private double perceptionRadius;
    /** Ameaça avisada por outro da manada, para o {@code WaryGoal} pegar; expira sozinha. */
    @Nullable
    private LivingEntity noticedThreat;
    private long noticedThreatUntil;
    /** Predador que acabou de comer: não caça até este tick. Não é salvo — dura minutos. */
    /** Momento da última refeição (tempo do jogo); {@code Long.MIN_VALUE} = ainda não sorteado. */
    private long lastMealTime = Long.MIN_VALUE;
    private double stress;
    private long huntedUntil;
    private int huntedBy = 1;
    private long huntStartTime = -1;
    private long huntFailedUntil;
    @Nullable
    private LivingEntity yieldingFrom;
    private long yieldUntil;
    /** Quem está encarando, blefando ou investindo contra esta criatura agora (só no servidor). */
    @Nullable
    private LivingEntity intimidator;
    private long intimidatedUntil;
    /**
     * Já conferida contra a zona de perigo da espécie. A geração do terreno roda antes de o spawn do
     * mundo estar decidido, e mundos de versões anteriores têm fauna que hoje não nasceria ali.
     */
    private boolean zoneChecked;
    /**
     * Já conferida contra o espaçamento entre grupos da espécie. Como {@link #zoneChecked}: só o que
     * veio da geração do terreno ou de um mundo de antes da regra fica para conferir.
     */
    private boolean spacingChecked;
    /** Voando por conta própria, sem montaria ({@link #setWildFlying}); só no servidor. */
    private boolean wildFlight;
    /** Raio de território próprio deste indivíduo (o apex inicial); 0 = o da espécie. */
    private int territoryOverride;
    private float plowHardness;
    /** Carga do pulo enviada pelo cliente de quem monta, de 0 a 1. */
    /** Se o impulso do pulo já foi aplicado e a criatura ainda não voltou ao chão. */
    /** Teclas de quem monta, lidas no cliente dele: o movimento da montaria é simulado lá (D18). */
    private boolean riderJumpHeld;
    private boolean riderJumpWasHeld;
    private boolean riderBoost;
    /** Velocidade escalar do voo montado, em blocos/tick; só no cliente de quem monta. */
    private double flightSpeed;
    /** Rumo e inclinação da trajetória no voo montado: seguem o olhar com curva limitada. */
    private float flightYaw;
    private float flightPitch;
    /** Só no cliente: a inclinação do corpo em voo (graus, + sobe o bico), seguindo a trajetória. */
    private float bodyFlightPitch;
    private float prevBodyFlightPitch;
    /** Emboscada (Smilodon): ticks de arrancada que restam, e se o próximo golpe agarra. */
    private int ambushTicks;
    private boolean grabReady;
    /** Bicada (ave-terrível): ticks de recuo depois de acertar, e de quem se afasta. */
    private int retreatTicks;
    @Nullable
    private LivingEntity retreatFrom;
    private static final java.util.UUID AMBUSH_SPEED_MODIFIER =
            java.util.UUID.fromString("6d2b7c1a-3a8e-4c55-9b77-2a7d3e9c1f10");
    /** O desafio em curso, se um caçador trouxe a cabeça de outro da espécie (só apex). */
    @Nullable
    private dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel duel;
    /** Ticks que o bico ainda fica travado depois de bater num escudo (Kelenken). */
    private int beakStuckTicks;
    /** Há quanto tempo é corpo, em ticks. */
    private int corpseTicks;
    /** Vinte minutos: depois disso o corpo some e o que sobrou cai no chão. */
    public static final int CORPSE_TICKS = 20 * 60 * 20;
    /** Esgotou o fôlego de voo: só decola de novo com {@link #TAKEOFF_STAMINA}. */
    private boolean flightExhausted;
    /** Fôlego mínimo para decolar depois de esgotar. */
    public static final float TAKEOFF_STAMINA = 0.3F;
    /** Segundos pousada para encher o fôlego de voo do zero. */
    private static final double FLIGHT_STAMINA_REFILL_SECONDS = 10.0;
    /** Inclinação máxima do corpo no voo, em graus. */
    private static final float MAX_BODY_FLIGHT_PITCH = 45.0F;
    /** Ticks sem quadro novo (montaria fora da tela) até voltar ao assento dos dados. */
    private static final int ANIMATED_SEAT_MAX_AGE = 5;
    /**
     * Posição do osso {@code rider_pos} já animado, relativa à origem da entidade, gravada pelo
     * renderer a cada quadro (só no cliente). Na animação de voo o corpo se inclina; sem isto o
     * assento fica parado e o modelo invade a frente de quem monta.
     */
    @Nullable
    private Vec3 animatedSeat;
    private int animatedSeatTick;
    private final SimpleContainer inventory;

    protected PrehistoricCreature(EntityType<? extends PrehistoricCreature> type, Level level) {
        super(type, level);
        int slots = species().flatMap(Species::storage).map(StorageProfile::slots).orElse(INVENTORY_SIZE);
        inventory = new SimpleContainer(slots) {
            @Override
            public boolean stillValid(Player player) {
                return canAccessInventory(player)
                        && player.distanceToSqr(PrehistoricCreature.this) <= INVENTORY_REACH * INVENTORY_REACH;
            }
        };
    }

    /**
     * Regra de spawn natural: em chão firme e na superfície (inclusive sobre neve e gelo, e sob
     * copas de árvore), nunca dentro de cavernas. Usa o mapa de altura, que não depende de luz.
     */
    public static boolean checkSurfaceSpawnRules(EntityType<? extends PrehistoricCreature> type,
                                                 ServerLevelAccessor level, MobSpawnType reason,
                                                 BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        if (pos.getY() < surface || !level.getBlockState(below).isValidSpawn(level, below, type)) {
            return false;
        }
        // Zonas de perigo: cada espécie só nasce a partir de uma distância do spawn do mundo.
        int minDistance = Species.of(level.registryAccess(), type).flatMap(Species::spawn)
                .map(SpawnProfile::minDistance).orElse(0);
        if (minDistance > 0 && !DangerZones.allowed(distanceFromWorldSpawn(level.getLevel(), pos), minDistance)) {
            return false;
        }
        // Por último, porque marca o lugar do grupo novo: uma manada de Brontossauro a cada 300 blocos.
        return GroupSpacing.permitsSpawn(level.getLevel(), type, pos);
    }

    /**
     * Conta para o espaçamento entre grupos ({@link GroupSpacing}): selvagem e sem dono. Presa ao mundo
     * (nome, comando, teste) não conta — exceto o apex inicial, que tem território próprio e precisa
     * afastar os T-Rex naturais.
     */
    public boolean countsForGroupSpacing() {
        return !isTame() && tamerUUID == null && (!isPersistenceRequired() || territoryOverride > 0);
    }

    /**
     * O {@code Animal} só aceita nascer onde o chão é grama ou há luz; à noite, sobre neve, isso
     * recusava toda a reposição e boa parte do spawn da geração do terreno. Quem decide o chão é
     * {@link #checkSurfaceSpawnRules}, que já roda antes, pelo {@code SpawnPlacements}.
     */
    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, MobSpawnType reason) {
        return true;
    }

    /**
     * Selvagem, solta, e mais perto do spawn do que a espécie nasce hoje: sobra da geração do terreno
     * com o spawn provisório ou de um mundo de versão anterior. Domesticada, com dono ou presa ao
     * mundo (nome, comando, teste) fica.
     */
    private boolean outsideDangerZone() {
        if (isTame() || tamerUUID != null || isPersistenceRequired() || isVehicle()
                || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        int minDistance = species().flatMap(Species::spawn).map(SpawnProfile::minDistance).orElse(0);
        return minDistance > 0 && !DangerZones.allowed(distanceFromWorldSpawn(serverLevel, blockPosition()), minDistance);
    }

    /** Marca como já conferida contra a zona de perigo (criaturas postas de propósito). */
    public void markZoneChecked() {
        zoneChecked = true;
    }

    /** Território próprio, no lugar do raio da espécie; centrado onde a criatura está. */
    public void setTerritory(BlockPos home, int radius) {
        homePos = home;
        territoryOverride = Math.max(0, radius);
        applyBehavior();
    }

    /** Distância horizontal até o spawn do mundo. */
    public static double distanceFromWorldSpawn(ServerLevel level, BlockPos pos) {
        BlockPos spawn = level.getSharedSpawnPos();
        return DangerZones.horizontalDistance(pos.getX(), pos.getZ(), spawn.getX(), spawn.getZ());
    }

    /**
     * O bando de quem nasce junto, repassado pelo vanilla de um membro ao seguinte. Herda o dado do
     * {@code AgeableMob} porque o vanilla converte para ele; sem filhote sorteado (os filhotes do mod
     * vêm da família).
     */
    public static final class Pack extends AgeableMob.AgeableMobGroupData {
        private final EntityType<?> type;
        private final UUID id;

        public Pack(EntityType<?> type, UUID id) {
            super(false);
            this.type = type;
            this.id = id;
        }

        public EntityType<?> type() {
            return type;
        }

        public UUID id() {
            return id;
        }
    }

    /** Marca os parentes criados por {@link #spawnFamily}, para eles não criarem família também. */
    private static final class FamilyMember implements SpawnGroupData {
        private static final FamilyMember INSTANCE = new FamilyMember();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        // Só a geração do terreno fica para conferir: o spawn do mundo ainda pode mudar.
        zoneChecked = reason != MobSpawnType.CHUNK_GENERATION;
        spacingChecked = zoneChecked;
        if (spawnData == FamilyMember.INSTANCE) {
            return spawnData;
        }
        // O vanilla repassa o mesmo dado a todo o grupo que nasce junto: é o que faz deles um bando.
        boolean natural = reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION;
        Pack pack = natural && spawnData instanceof Pack existing && existing.type() == getType() ? existing : null;
        if (natural) {
            if (pack == null) {
                pack = new Pack(getType(), UUID.randomUUID());
            }
            groupId = pack.id();
        }
        super.finalizeSpawn(level, difficulty, reason, pack, dataTag);
        if (natural && !isBaby()) {
            species().flatMap(Species::spawn).flatMap(SpawnProfile::family)
                    .ifPresent(family -> spawnFamily(level, difficulty, family));
        }
        return pack != null ? pack : spawnData;
    }

    /**
     * Espécie solitária às vezes nasce em família: com {@code calf_chance}, um filhote junto; com
     * filhote, {@code mate_chance} de o outro adulto também estar. Os parentes são selvagens.
     */
    private void spawnFamily(ServerLevelAccessor level, DifficultyInstance difficulty, FamilyProfile family) {
        RandomSource random = level.getRandom();
        if (random.nextDouble() >= family.calfChance()) {
            if (random.nextDouble() < family.pairChance()) {
                spawnMate(level, difficulty);
            }
            return;
        }
        PrehistoricCreature calf = spawnRelative(level, difficulty);
        if (calf != null) {
            int maturation = calf.breedingProfile().map(BreedingProfile::maturationSeconds).orElse(1200);
            // Filhote já crescido em parte: entre um quarto e três quartos do caminho.
            calf.setAge(-(int) (maturation * 20 * (0.25 + 0.5 * random.nextDouble())));
            calf.refreshDimensions();
        }
        if (random.nextDouble() < family.mateChance()) {
            spawnRelative(level, difficulty);
        }
    }

    /**
     * O parceiro do outro sexo, ao lado. Os atributos dos dois são sorteados aqui, antes de entrar no
     * mundo: na geração do terreno a entidade só é carregada depois, e o sorteio de lá trocaria o sexo.
     */
    private void spawnMate(ServerLevelAccessor level, DifficultyInstance difficulty) {
        if (!statsRolled) {
            rollWildStats();
        }
        if (!(getType().create(level.getLevel()) instanceof PrehistoricCreature mate)) {
            return;
        }
        Vec3 spot = familySpot(level);
        mate.moveTo(spot.x, spot.y, spot.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mate.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, FamilyMember.INSTANCE, null);
        mate.groupId = groupId;
        mate.rollWildStats();
        mate.setFemale(!isFemale());
        level.addFreshEntity(mate);
    }

    /**
     * Onde o parente nasce: ao lado, a uma largura de corpo, num chunk que o mundo aceita agora. Na geração do
     * terreno só o chunk do meio recebe entidades — um parente no vizinho derrubava o servidor (visto com o TFC e as
     * criaturas grandes) —, então tenta outras direções e, sem nenhuma, nasce no mesmo ponto.
     */
    private Vec3 familySpot(ServerLevelAccessor level) {
        double offset = getBbWidth() + 0.5;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            double x = getX() + Math.cos(angle) * offset;
            double z = getZ() + Math.sin(angle) * offset;
            if (level.hasChunk(net.minecraft.core.SectionPos.blockToSectionCoord(x),
                    net.minecraft.core.SectionPos.blockToSectionCoord(z))) {
                return new Vec3(x, getY(), z);
            }
        }
        return position();
    }

    @Nullable
    private PrehistoricCreature spawnRelative(ServerLevelAccessor level, DifficultyInstance difficulty) {
        if (!(getType().create(level.getLevel()) instanceof PrehistoricCreature relative)) {
            return null;
        }
        Vec3 spot = familySpot(level);
        relative.moveTo(spot.x, spot.y, spot.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        relative.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, FamilyMember.INSTANCE, null);
        relative.groupId = groupId;
        level.addFreshEntity(relative);
        return relative;
    }

    /** Atributos que toda criatura precisa ter registrados para os stats serem aplicados. */
    public static AttributeSupplier.Builder createBaseAttributes() {
        return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LEVEL, 1);
        entityData.define(DATA_TORPOR_FRACTION, 0.0F);
        entityData.define(DATA_UNCONSCIOUS, false);
        entityData.define(DATA_TAMING_PROGRESS, 0.0F);
        entityData.define(DATA_NEEDS_TAMING_FOOD, false);
        entityData.define(DATA_MOVEMENT, (byte) DEFAULT_MOVEMENT.ordinal());
        entityData.define(DATA_STANCE, (byte) DEFAULT_STANCE.ordinal());
        entityData.define(DATA_SADDLED, false);
        entityData.define(DATA_FEMALE, false);
        entityData.define(DATA_MATING, false);
        entityData.define(DATA_FLYING, false);
        entityData.define(DATA_FLIGHT_STAMINA, 1.0F);
        entityData.define(DATA_CORPSE, false);
        entityData.define(DATA_STRESS, 0.0F);
    }

    public Optional<Species> species() {
        return Species.of(level().registryAccess(), getType());
    }

    public Optional<BehaviorProfile> behavior() {
        return species().flatMap(Species::behavior);
    }

    private Optional<TamingProfile> tamingProfile() {
        return species().flatMap(Species::taming);
    }

    /** Montaria da espécie; vazio se a espécie não pode ser montada. */
    public Optional<MountProfile> mountProfile() {
        return species().flatMap(Species::mount);
    }

    public boolean isFlightMount() {
        return mountProfile().map(MountProfile::flying).orElse(false);
    }

    public boolean isFlying() {
        return entityData.get(DATA_FLYING);
    }

    /** Teclas de pulo e de impulso (correr) de quem monta, a cada tick, no cliente dele. */
    public void setRiderInput(boolean jump, boolean boost) {
        riderJumpHeld = jump;
        riderBoost = boost;
    }

    /**
     * Liga ou desliga o voo montado. No cliente de quem monta, é a física de voo que decide; o
     * servidor recebe o estado ({@code FlightInputPayload}) só para tirar a gravidade e não tratar a
     * montaria como "flutuando" — a posição já vem do cliente, como a de qualquer veículo.
     */
    public void setFlying(boolean flying) {
        boolean allowed = flying && isFlightMount() && isRideReady() && isVehicle() && !isUnconscious();
        wildFlight = false;
        entityData.set(DATA_FLYING, allowed);
        setNoGravity(allowed);
        if (!allowed) {
            flightSpeed = 0.0;
        }
    }

    /**
     * Pode voar por conta própria ({@code WildFlightGoal}): espécie voadora, selvagem, acordada, adulta,
     * solta e sem ninguém em cima.
     */
    public boolean canFlyWild() {
        return isFlightMount() && !isTame() && !isVehicle() && !isUnconscious() && !isBaby() && !isLeashed()
                && !isPassenger();
    }

    /** Liga ou desliga o voo selvagem; o estado é o mesmo do voo montado, e com ele a animação. */
    public void setWildFlying(boolean flying) {
        boolean allowed = flying && canFlyWild();
        wildFlight = allowed;
        entityData.set(DATA_FLYING, allowed);
        setNoGravity(allowed);
    }

    /** Chamado pelo renderer com a posição do osso {@code rider_pos} neste quadro. */
    public void setAnimatedSeat(Vec3 offset) {
        animatedSeat = offset;
        animatedSeatTick = tickCount;
    }

    // ---- Sons ----

    private Optional<SoundProfile> soundProfile() {
        return species().flatMap(Species::sounds);
    }

    @Nullable
    private SoundEvent speciesSound(Function<SoundProfile, Optional<ResourceLocation>> which) {
        return soundProfile().flatMap(which).map(SoundProfile::event).orElse(null);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        // Inconsciente não faz barulho.
        return isUnconscious() ? null : speciesSound(SoundProfile::ambient);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return speciesSound(SoundProfile::hurt);
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return speciesSound(SoundProfile::death);
    }

    @Override
    protected float getSoundVolume() {
        return soundProfile().map(SoundProfile::volume).orElse(1.0F);
    }

    /** O golpe em si, acerte ou não: toca o som de ataque. As subclasses somam a animação. */
    protected void swingAttack() {
        SoundEvent attack = speciesSound(SoundProfile::attack);
        if (attack != null) {
            playSound(attack, getSoundVolume(), getVoicePitch());
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isBeakStuck() || duel != null && duel.roaring()) {
            return false;
        }
        setStalking(false);
        if (!attackSwung) {
            swingAttack();
        }
        boolean shielded = huntSpecial() == BehaviorProfile.HuntSpecial.BEAK_STRIKE && target instanceof Player player
                && player.isBlocking() && player.isDamageSourceBlocked(damageSources().mobAttack(this));
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            hornToss(living);
            applyHuntSpecial(living);
        } else if (shielded) {
            beakStuck();
        }
        return hit;
    }

    /**
     * A bicada que bate num escudo erguido: o bico, fundido ao crânio, trava — a ave fica parada e sem atacar por
     * {@link HuntSpecials#BEAK_STUCK_TICKS}. A fraqueza da Kelenken: quem defende no tempo certo ganha a janela.
     */
    private void beakStuck() {
        beakStuckTicks = HuntSpecials.BEAK_STUCK_TICKS;
        retreatTicks = 0;
        retreatFrom = null;
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        playSound(SoundEvents.ANVIL_LAND, 0.6F, 1.6F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getEyeY(), getZ(), 12,
                    0.4, 0.3, 0.4, 0.1);
        }
    }

    /** Bico travado depois de bater num escudo: parada, sem atacar. */
    public boolean isBeakStuck() {
        return beakStuckTicks > 0;
    }

    private BehaviorProfile.HuntSpecial huntSpecial() {
        return behavior().map(BehaviorProfile::huntSpecial).orElse(BehaviorProfile.HuntSpecial.NONE);
    }

    /**
     * O bote: a emboscada arranca (+50% de velocidade por 4 s) e deixa o próximo golpe agarrar; o salto pula
     * sobre a presa a média distância ({@link HuntSpecials}).
     */
    public void onPounce(LivingEntity target) {
        if (level().isClientSide || isTame()) {
            return;
        }
        switch (huntSpecial()) {
            case AMBUSH -> {
                ambushTicks = HuntSpecials.AMBUSH_TICKS;
                grabReady = true;
                AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(AMBUSH_SPEED_MODIFIER);
                    speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            AMBUSH_SPEED_MODIFIER, "ambush", HuntSpecials.AMBUSH_SPEED_BONUS,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            case PACK_LEAP -> {
                if (onGround() && HuntSpecials.inLeapRange(distanceTo(target))) {
                    double[] leap = HuntSpecials.leapVelocity(target.getX() - getX(), target.getZ() - getZ());
                    setDeltaMovement(leap[0], leap[1], leap[2]);
                    hasImpulse = true;
                }
            }
            default -> {
            }
        }
    }

    /** O que o golpe faz a mais: a emboscada agarra; a bicada fura a armadura e manda recuar. */
    private void applyHuntSpecial(LivingEntity target) {
        switch (huntSpecial()) {
            case AMBUSH -> {
                if (grabReady) {
                    grabReady = false;
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, HuntSpecials.GRAB_TICKS,
                            HuntSpecials.GRAB_AMPLIFIER), this);
                }
            }
            case BEAK_STRIKE -> {
                double pierce = HuntSpecials.beakPierce(getAttributeValue(Attributes.ATTACK_DAMAGE));
                if (pierce > 0.0 && target.isAlive()) {
                    // O mesmo golpe: sem a janela de invulnerabilidade, a parte que atravessa a armadura.
                    target.invulnerableTime = 0;
                    target.hurt(damageSources().magic(), (float) pierce);
                }
                retreatTicks = HuntSpecials.RETREAT_TICKS;
                retreatFrom = target;
            }
            default -> {
            }
        }
    }

    /** Bicada: depois de acertar, recua de {@link #retreatTarget()} por um instante. */
    public boolean isRetreatingAfterStrike() {
        return retreatTicks > 0 && retreatFrom != null && retreatFrom.isAlive();
    }

    @Nullable
    public LivingEntity retreatTarget() {
        return retreatFrom;
    }

    public boolean isAmbushing() {
        return ambushTicks > 0;
    }

    private void tickHuntSpecials() {
        if (beakStuckTicks > 0) {
            beakStuckTicks--;
            getNavigation().stop();
        }
        if (retreatTicks > 0 && --retreatTicks == 0) {
            retreatFrom = null;
        }
        if (ambushTicks > 0 && --ambushTicks == 0) {
            grabReady = false;
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(AMBUSH_SPEED_MODIFIER);
            }
        }
    }

    /** Golpe de chifre/cabeça das espécies cautelosas: empurra e joga para o alto ({@code wariness.knockback/lift}). */
    private void hornToss(LivingEntity target) {
        WarinessProfile profile = wariness().orElse(null);
        if (profile == null || profile.knockback() <= 0.0 && profile.lift() <= 0.0 || isTame()) {
            return;
        }
        Vec3 push = target.position().subtract(position()).multiply(1, 0, 1);
        push = push.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, getYRot()) : push.normalize();
        double resistance = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        target.push(push.x * profile.knockback() * resistance, profile.lift() * resistance,
                push.z * profile.knockback() * resistance);
        target.hurtMarked = true;
    }

    // ---- Bando ----

    @Nullable
    public UUID groupId() {
        ensureGroup();
        return groupId;
    }

    /** Selvagem sem bando no servidor: é adotada já, na primeira vez que alguém pergunta pelo bando dela. */
    private void ensureGroup() {
        if (groupId == null && !adopting && !isTame() && !level().isClientSide && isAlive()) {
            adopting = true;
            try {
                adoptGroup();
            } finally {
                adopting = false;
            }
        }
    }

    /** Mesmo bando pelo id já gravado, sem disparar adoção: é o que a contagem de membros usa. */
    private boolean sameGroupId(PrehistoricCreature other) {
        return other != this && other.getType() == getType() && groupId != null && groupId.equals(other.groupId)
                && !isTame() && !other.isTame();
    }

    /** Para testes e comandos: põe a criatura num bando. */
    public void setGroupId(@Nullable UUID id) {
        groupId = id;
    }

    /** Do mesmo bando: mesma espécie, selvagens os dois, mesmo id de bando. */
    public boolean sameGroup(LivingEntity other) {
        if (other == this || !(other instanceof PrehistoricCreature creature) || creature.getType() != getType()
                || isTame() || creature.isTame()) {
            return false;
        }
        ensureGroup();
        creature.ensureGroup();
        return groupId != null && groupId.equals(creature.groupId);
    }

    /** Tamanho máximo do bando: o {@code group_max} da espécie. */
    public int groupMax() {
        return species().flatMap(Species::spawn).map(SpawnProfile::groupMax).orElse(DEFAULT_GROUP_MAX);
    }

    /** Membros do bando carregados no raio, sem contar esta. */
    public List<PrehistoricCreature> groupMembers(double radius) {
        ensureGroup();
        if (groupId == null || isTame()) {
            return List.of();
        }
        return level().getEntitiesOfClass(PrehistoricCreature.class, getBoundingBox().inflate(radius),
                other -> sameGroupId(other) && other.isAlive());
    }

    /**
     * Selvagem sem bando: entra no bando da espécie mais perto que ainda tenha vaga, ou funda um. Roda
     * no primeiro tick e de tempos em tempos, para o que veio de mundo antigo, de ovo ou de comando.
     */
    private void adoptGroup() {
        if (groupId != null || isTame() || !(level() instanceof ServerLevel)) {
            return;
        }
        int herdRadius = behavior().map(BehaviorProfile::herdRadius).orElse(0);
        if (isBaby()) {
            // Filhote é da família: entra no bando do adulto da espécie mais perto, mesmo acima do limite.
            PrehistoricCreature parent = level().getEntitiesOfClass(PrehistoricCreature.class,
                            getBoundingBox().inflate(16.0), other -> other != this && other.getType() == getType()
                                    && !other.isTame() && !other.isBaby() && other.isAlive())
                    .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (parent != null && parent.groupId() != null) {
                groupId = parent.groupId;
                return;
            }
        }
        if (herdRadius > 0) {
            double reach = Math.max(herdRadius * 2.0, 16.0);
            PrehistoricCreature best = null;
            double bestDistance = Double.MAX_VALUE;
            for (PrehistoricCreature other : level().getEntitiesOfClass(PrehistoricCreature.class,
                    getBoundingBox().inflate(reach), other -> other != this && other.getType() == getType()
                            && !other.isTame() && other.groupId != null && other.isAlive())) {
                double distance = distanceToSqr(other);
                if (distance < bestDistance && other.groupMembers(GROUP_RANGE).size() + 1 < other.groupMax()) {
                    best = other;
                    bestDistance = distance;
                }
            }
            if (best != null) {
                groupId = best.groupId;
                if (best.lastMealTime != Long.MIN_VALUE) {
                    lastMealTime = best.lastMealTime;
                }
                return;
            }
        }
        groupId = UUID.randomUUID();
    }

    // ---- Território ----

    /**
     * Raio de defesa em volta desta criatura: quem entra é intruso. Manada: o raio dela mais 6; solitária:
     * quatro vezes a largura do corpo mais 4. Sempre entre 8 e 24 blocos.
     */
    public double defendRadius() {
        int herdRadius = behavior().map(BehaviorProfile::herdRadius).orElse(0);
        double radius = herdRadius > 0 ? herdRadius + 6.0 : getBbWidth() * 4.0 + 4.0;
        return Mth.clamp(radius, 8.0, 24.0);
    }

    /** Começa a expulsar o intruso. */
    public void defendTerritoryAgainst(LivingEntity intruder) {
        territorialFoe = intruder;
        setTarget(intruder);
    }

    /** O intruso que está expulsando, se ainda for o alvo. */
    @Nullable
    public LivingEntity territorialFoe() {
        if (territorialFoe != null && (getTarget() != territorialFoe || !territorialFoe.isAlive())) {
            territorialFoe = null;
        }
        return territorialFoe;
    }

    /** Guerra entre bandos: os membros sem alvo entram na briga contra o inimigo. */
    public void rallyGroupAgainst(LivingEntity enemy) {
        for (PrehistoricCreature member : groupMembers(GROUP_RANGE)) {
            if (member.getTarget() == null && !member.isBaby() && !member.isUnconscious()
                    && member.yieldingFrom() == null) {
                member.defendTerritoryAgainst(enemy);
            }
        }
    }

    // ---- Prioridade de alvo ----

    /** Por quanto tempo depois de ser ferida pelo jogador conta como provocada, em ticks. */
    private static final int PROVOKED_TICKS = 100;

    /**
     * Mira primeiro a ameaça maior ou mais imediata ({@link TargetPriority}): com o jogador como alvo e
     * uma criatura (ou golem) mais perigosa atacando esta ou o bando, ou um rival de território, troca
     * para ela. Caçando o jogador sem ter sido provocada, troca por uma presa melhor para o bando, se
     * houver ({@link HuntGoal#bestNonPlayerPrey}).
     */
    private void reprioritizeTarget() {
        if (!(getTarget() instanceof Player player) || isTame() || isUnconscious()) {
            return;
        }
        boolean provoked = getLastHurtByMob() == player && tickCount - getLastHurtByMobTimestamp() < PROVOKED_TICKS;
        double playerDanger = TargetPriority.danger(sizeRatioOf(player), 1, provoked, distanceTo(player));
        LivingEntity threat = null;
        double threatDanger = playerDanger;
        double range = Math.max(behavior().map(BehaviorProfile::aggroRadius).orElse(16.0), 16.0);
        for (Mob other : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(range),
                other -> other != this && other.isAlive() && !sameGroup(other) && threatensUs(other))) {
            int group = other instanceof PrehistoricCreature creature ? creature.fightingGroup() : 1;
            double danger = TargetPriority.danger(sizeRatioOf(other), group, true, distanceTo(other));
            if (TargetPriority.outranks(danger, threatDanger)) {
                threat = other;
                threatDanger = danger;
            }
        }
        if (threat != null) {
            if (isHunting()) {
                endHunt();
            }
            setTarget(threat);
            return;
        }
        if (!provoked && isHunting()) {
            LivingEntity prey = HuntGoal.bestPrey(this);
            if (prey != null && !(prey instanceof Player)) {
                setTarget(prey);
                rallyPack(prey);
                if (prey instanceof PrehistoricCreature hunted) {
                    hunted.onHunted(this, fightingGroup());
                }
            }
        }
    }

    /** Está atacando esta criatura ou alguém do bando, ou é rival de território dela. */
    private boolean threatensUs(Mob other) {
        LivingEntity target = other.getTarget();
        if (target == this || target != null && sameGroup(target)) {
            return true;
        }
        return other instanceof PrehistoricCreature creature
                && (creature.territorialFoe() == this || territorialFoe() == creature);
    }

    // ---- Ecologia ----

    public Optional<WarinessProfile> wariness() {
        return behavior().flatMap(BehaviorProfile::wariness);
    }

    /** Gesto de ameaça: o som de alerta. As subclasses com animação somam o gesto. */
    public void threatDisplay() {
        playAlert();
    }

    /** Outro da manada viu uma ameaça: esta também passa a encará-la. */
    public void noticeThreat(LivingEntity threat) {
        noticedThreat = threat;
        noticedThreatUntil = level().getGameTime() + 40;
    }

    @Nullable
    public LivingEntity takeNoticedThreat() {
        LivingEntity threat = noticedThreat;
        noticedThreat = null;
        return threat != null && threat.isAlive() && level().getGameTime() <= noticedThreatUntil ? threat : null;
    }

    /**
     * Outro animal está encarando, blefando ou investindo contra esta criatura: ela passa a vê-lo
     * como ameaça enquanto durar o confronto, mesmo que ele não ataque — e fica sabendo na hora.
     */
    public void intimidatedBy(LivingEntity other) {
        if (intimidator != other || level().getGameTime() > intimidatedUntil) {
            noticeThreat(other);
        }
        intimidator = other;
        intimidatedUntil = level().getGameTime() + INTIMIDATION_TICKS;
    }

    /** Se {@code other} confrontou esta criatura há pouco. */
    public boolean isIntimidatedBy(LivingEntity other) {
        return other == intimidator && other.isAlive() && level().getGameTime() <= intimidatedUntil;
    }

    /** O alerta corre pela manada: os do mesmo tipo, selvagens e sem alvo, no raio da manada, encaram também. */
    public void alertHerdToThreat(LivingEntity threat) {
        int herdRadius = behavior().map(BehaviorProfile::herdRadius).orElse(0);
        double radius = Math.max(herdRadius, 8);
        for (PrehistoricCreature other : level().getEntitiesOfClass(PrehistoricCreature.class,
                getBoundingBox().inflate(radius), candidate -> sameGroup(candidate)
                        && candidate.getTarget() == null && !candidate.isUnconscious())) {
            other.noticeThreat(threat);
        }
    }

    /** Se há filhote selvagem da espécie a até {@code radius} blocos: a mãe fica bem mais brava. */
    public boolean hasCalfNearby(double radius) {
        if (isBaby() || radius <= 0) {
            return false;
        }
        return !level().getEntitiesOfClass(PrehistoricCreature.class, getBoundingBox().inflate(radius),
                other -> sameGroup(other) && other.isBaby() && other.isAlive()).isEmpty();
    }

    /** Filhote ferido: os adultos selvagens da espécie por perto vão para cima de quem o feriu. */
    private void callParents(LivingEntity attacker) {
        double radius = wariness().map(WarinessProfile::calfRadius).orElse(16.0);
        for (PrehistoricCreature adult : level().getEntitiesOfClass(PrehistoricCreature.class,
                getBoundingBox().inflate(Math.max(radius, 8.0)), other -> sameGroup(other)
                        && !other.isBaby() && !other.isUnconscious())) {
            adult.setTarget(attacker);
        }
    }

    /** Predador recém-alimentado: não procura presa. */
    public boolean isSated() {
        return hungerDrive() == Hunger.Drive.SATED;
    }

    public EcologyProfile ecology() {
        return behavior().map(BehaviorProfile::ecology).orElse(EcologyProfile.DEFAULT);
    }

    /** Ticks desde a última refeição; o primeiro valor é sorteado, para as caçadas não sincronizarem. */
    public long ticksSinceMeal() {
        if (lastMealTime == Long.MIN_VALUE) {
            // O sorteio sai do id do bando: quem nasce junto tem a mesma fome e caça junto.
            double roll = groupId != null ? new java.util.Random(groupId.getLeastSignificantBits()).nextDouble()
                    : getRandom().nextDouble();
            lastMealTime = level().getGameTime() - Hunger.spawnTicksSinceMeal(ecology().hungerSeconds(), roll);
        }
        return level().getGameTime() - lastMealTime;
    }

    /** Fome do predador: saciado, oportunista ou caçando ({@link Hunger}). */
    public Hunger.Drive hungerDrive() {
        int sated = behavior().map(BehaviorProfile::satedSeconds).orElse(BehaviorProfile.PASSIVE.satedSeconds());
        return Hunger.drive(ticksSinceMeal(), sated, ecology().hungerSeconds());
    }

    /** Fome máxima ({@code /ias hunger}): caça agora, sem a espera de uma caçada que falhou. */
    public void starve() {
        setTicksSinceMeal(ecology().hungerSeconds() * 40L);
        huntFailedUntil = 0;
    }

    /** Para testes e comandos: fome como se a última refeição tivesse sido há tanto tempo. */
    public void setTicksSinceMeal(long ticks) {
        lastMealTime = level().getGameTime() - ticks;
    }

    // ---- Estresse ----

    public double stress() {
        return level().isClientSide ? entityData.get(DATA_STRESS) : stress;
    }

    public Stress.Mood mood() {
        return Stress.mood(stress());
    }

    public void addStress(Stress.Event event) {
        addStress(event, 1.0);
    }

    /** Domesticada, o termômetro fica parado: o que importa a ela é a obediência. */
    public void addStress(Stress.Event event, double scale) {
        if (level().isClientSide || isTame()) {
            return;
        }
        setStress(Stress.apply(stress, event, ecology().nervousness(), scale));
    }

    public void setStress(double value) {
        stress = Math.max(0.0, Math.min(Stress.MAX, value));
        entityData.set(DATA_STRESS, (float) stress);
    }

    private void tickStress() {
        boolean comforted = isSated() && behavior().flatMap(BehaviorProfile::prey).isPresent()
                || !isIsolated() && behavior().map(BehaviorProfile::herdRadius).orElse(0) > 0;
        setStress(Stress.decay(stress, STRESS_INTERVAL / 20.0, comforted));
    }

    /** O mesmo evento nos da manada por perto (selvagens, da espécie). */
    private void stressHerd(Stress.Event event) {
        double radius = Math.max(behavior().map(BehaviorProfile::herdRadius).orElse(0), 12);
        for (PrehistoricCreature other : level().getEntitiesOfClass(PrehistoricCreature.class,
                getBoundingBox().inflate(radius), this::sameGroup)) {
            other.addStress(event);
        }
    }

    // ---- Caçada ----

    /**
     * Um predador escolheu esta criatura como presa: ela e a manada se sabem caçadas (a manada
     * dispara, ver {@link dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse}).
     *
     * @param pack quantos predadores vêm juntos
     */
    public void onHunted(LivingEntity predator, int pack) {
        markHunted(pack);
        addStress(Stress.Event.HUNTED);
        noticeThreat(predator);
        double radius = Math.max(behavior().map(BehaviorProfile::herdRadius).orElse(0), 12);
        for (PrehistoricCreature other : level().getEntitiesOfClass(PrehistoricCreature.class,
                getBoundingBox().inflate(radius), o -> sameGroup(o) && !o.isUnconscious())) {
            other.markHunted(pack);
            other.addStress(Stress.Event.HUNTED, 0.6);
            other.noticeThreat(predator);
        }
    }

    /** A caçada continua: renova o aviso na presa e na manada, sem somar estresse de novo. */
    public void stillHunted(LivingEntity predator, int pack) {
        markHunted(pack);
        noticeThreat(predator);
        double radius = Math.max(behavior().map(BehaviorProfile::herdRadius).orElse(0), 12);
        for (PrehistoricCreature other : level().getEntitiesOfClass(PrehistoricCreature.class,
                getBoundingBox().inflate(radius), o -> sameGroup(o) && !o.isUnconscious())) {
            other.markHunted(pack);
            other.noticeThreat(predator);
        }
    }

    private void markHunted(int pack) {
        huntedUntil = level().getGameTime() + HUNTED_MEMORY_TICKS;
        huntedBy = Math.max(1, pack);
    }

    public boolean isHunted() {
        return level().getGameTime() < huntedUntil;
    }

    /** Quantos predadores caçam esta manada agora (1 se nenhum). */
    public int huntingPack() {
        return isHunted() ? huntedBy : 1;
    }

    /** Começou a perseguir uma presa. */
    public void beginHunt() {
        huntStartTime = level().getGameTime();
    }

    /** Já deu o bote nesta caçada: a presa pode saber que é caçada. Quem espreita começa escondido. */
    private boolean huntRevealed;

    public boolean huntRevealed() {
        return huntRevealed;
    }

    public void setHuntRevealed(boolean revealed) {
        huntRevealed = revealed;
    }

    /** Perseguindo presa (não um jogador, não um rival). */
    public boolean isHunting() {
        return huntStartTime >= 0 && getTarget() != null;
    }

    /** Há quantos ticks a perseguição começou. */
    public long huntTicks() {
        return huntStartTime < 0 ? 0 : level().getGameTime() - huntStartTime;
    }

    /** A presa escapou: frustração e um tempo antes de tentar de novo. */
    public void huntFailed() {
        huntStartTime = -1;
        huntFailedUntil = level().getGameTime() + 600;
        addStress(Stress.Event.HUNT_FAILED);
    }

    public void endHunt() {
        huntStartTime = -1;
    }

    public boolean recentlyFailedHunt() {
        return level().getGameTime() < huntFailedUntil;
    }

    // ---- Rivais ----

    /** Macho adulto selvagem da mesma espécie, rival por acesso a fêmeas. */
    public boolean isRival(LivingEntity other) {
        return other != this && other instanceof PrehistoricCreature creature && !creature.isTame()
                && !creature.isBaby() && !creature.isFemale() && !isFemale() && other.getType() == getType();
    }

    /** Força para disputa: vida × ataque, com o bando somando. */
    public double dominance() {
        double attack = getAttribute(Attributes.ATTACK_DAMAGE) != null ? getAttributeValue(Attributes.ATTACK_DAMAGE) : 1.0;
        return getHealth() * Math.max(1.0, attack);
    }

    /** Perdeu a disputa: larga o alvo e foge do rival por um tempo. */
    public void yieldTo(LivingEntity rival) {
        yieldingFrom = rival;
        yieldUntil = level().getGameTime() + YIELD_TICKS;
        setTarget(null);
        addStress(Stress.Event.YIELDED);
    }

    /**
     * Um rival mais forte veio tirar satisfação. Calmo, cede e vai embora; estressado (encurralado,
     * ferido, com fome), às vezes revida.
     */
    public void challengedBy(PrehistoricCreature challenger) {
        if (!isRival(challenger)) {
            return;
        }
        addStress(Stress.Event.RIVAL_SEEN, 5.0);
        boolean fightsBack = mood().atLeast(Stress.Mood.STRESSED) && getRandom().nextDouble() < 0.5
                || dominance() >= challenger.dominance();
        if (fightsBack) {
            setTarget(challenger);
        } else {
            yieldTo(challenger);
        }
    }

    /** Fugindo de um rival que ganhou a disputa; nulo se não. */
    @Nullable
    public LivingEntity yieldingFrom() {
        if (yieldingFrom != null && (!yieldingFrom.isAlive() || level().getGameTime() >= yieldUntil)) {
            yieldingFrom = null;
        }
        return yieldingFrom;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        Optional<BehaviorProfile> behavior = behavior();
        boolean playerMeal = victim instanceof Player && behavior.map(profile -> profile.prey().isPresent()).orElse(false);
        boolean speciesPrey = behavior.flatMap(BehaviorProfile::prey).map(victim.getType()::is).orElse(false);
        if (!isTame() && behavior.isPresent() && (speciesPrey || playerMeal)) {
            // Comeu: recupera vida e passa um tempo sem caçar, perto de onde abateu. A saciedade é do bando e
            // vem do tamanho da presa dividido por quem come: um dodô repartido por três Alossauros quase não
            // mata a fome. O jogador conta como refeição inteira: quem o matou não ataca de novo sem ser provocado.
            List<PrehistoricCreature> eaters = groupMembers(MEAL_SHARE_RADIUS);
            double fraction = victim instanceof Player ? 1.0
                    : Mth.clamp(sizeRatioOf(victim) / (1 + eaters.size()), MIN_MEAL_FRACTION, 1.0);
            long hungerTicks = ecology().hungerSeconds() * 20L;
            ticksSinceMeal();
            lastMealTime = Math.max(lastMealTime, level.getGameTime() - Math.round((1.0 - fraction) * hungerTicks));
            for (PrehistoricCreature member : groupMembers(GROUP_RANGE)) {
                member.lastMealTime = Math.max(member.lastMealTime, lastMealTime);
                if (member.getTarget() == victim || member.isHunting()) {
                    member.setTarget(null);
                    member.endHunt();
                }
            }
            huntStartTime = -1;
            heal((float) (getMaxHealth() * 0.25 * fraction));
            setTarget(null);
            addStress(Stress.Event.FED);
        }
        return result;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        boolean acquired = target != null && getTarget() == null;
        super.setTarget(target);
        // Quem espreita fica quieto: o alerta sai no bote (StalkGoal).
        boolean stalker = stalks();
        if (acquired && !level().isClientSide && getTarget() == target && (isTame() || !stalker)) {
            playAlert();
        }
    }

    /** Som de alerta da espécie (o rugido), se ela tiver um. */
    public void playAlert() {
        SoundEvent alert = speciesSound(SoundProfile::alert);
        if (alert != null) {
            playSound(alert, getSoundVolume(), getVoicePitch());
        }
    }

    // ---- Atributos ----

    public StatPoints statPoints() {
        return statPoints;
    }

    /** O que a criatura passa aos filhotes: pontos, mutações e o gene de vida. Só no servidor. */
    public Genome genome() {
        return new Genome(statPoints, mutations, healthGene);
    }

    /** Aplica um genoma inteiro (filhote recém-nascido, testes). */
    public void setGenome(Genome genome) {
        mutations = genome.mutations();
        healthGene = genome.healthGene();
        species().ifPresentOrElse(species -> setStatPoints(genome.points(), species), () -> {
            statPoints = genome.points();
            statsRolled = true;
        });
    }

    /** Nível do indivíduo; disponível também no cliente. */
    public int creatureLevel() {
        return entityData.get(DATA_LEVEL);
    }

    /** Torpor necessário para derrubar este indivíduo. */
    public double maxTorpor() {
        return species().map(species -> species.stats().value(Stat.TORPOR, statPoints)).orElse(0.0);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (level().isClientSide) {
            return;
        }
        if (!statsRolled) {
            rollWildStats();
        }
        if (homePos == null) {
            homePos = blockPosition();
        }
        applyBehavior();
    }

    /** Volta a valer o território da espécie (depois de rondar com fome). */
    public void restoreTerritory() {
        applyBehavior();
    }

    /** Aplica percepção e território da espécie. Criaturas domesticadas não têm território. */
    private void applyBehavior() {
        Optional<BehaviorProfile> behavior = behavior();
        // O alcance do caminho cobre a caçada e a disputa entre machos da mesma espécie.
        behavior.ifPresent(profile -> setBase(Attributes.FOLLOW_RANGE, Math.max(profile.aggroRadius(),
                Math.max(profile.prey().isPresent() ? huntRadius() : 0.0,
                        profile.ecology().rivalRadius()))));
        int territory = territoryOverride > 0 ? territoryOverride
                : behavior.map(BehaviorProfile::territoryRadius).orElse(0);
        if (!isTame() && territory > 0 && homePos != null) {
            restrictTo(homePos, territory);
        } else {
            clearRestriction();
        }
        // Pavor de água (Smilodon selvagem): o caminho nunca passa pela água e foge da beira.
        boolean dread = fearsWater();
        setPathfindingMalus(BlockPathTypes.WATER, dread ? -1.0F : BlockPathTypes.WATER.getMalus());
        setPathfindingMalus(BlockPathTypes.WATER_BORDER, dread ? WATER_BORDER_DREAD : BlockPathTypes.WATER_BORDER.getMalus());
    }

    /** Penalidade da beira d'água para quem tem pavor dela: contorna lagos de longe. */
    private static final float WATER_BORDER_DREAD = 16.0F;

    /** Selvagem com pavor de água ({@code behavior.fears_water}): não entra, não segue presa nela e, se cair, sai. */
    public boolean fearsWater() {
        return !isTame() && behavior().map(BehaviorProfile::fearsWater).orElse(false);
    }

    @Override
    public void setTame(boolean tame) {
        super.setTame(tame);
        if (!level().isClientSide) {
            applyBehavior();
        }
    }

    private RandomGenerator randomGenerator() {
        return random::nextLong;
    }

    private void rollWildStats() {
        Optional<Species> species = species();
        if (species.isEmpty()) {
            IceAgeSurvival.LOGGER.warn("Sem definição de espécie para {}; atributos padrão mantidos",
                    EntityType.getKey(getType()));
            return;
        }
        RandomGenerator generator = randomGenerator();
        // Mais longe do spawn, criaturas de nível mais alto.
        int step = ServerConfig.WILD_LEVEL_STEP.get();
        int cap = level() instanceof ServerLevel serverLevel
                ? DangerZones.levelCap(ServerConfig.MAX_WILD_LEVEL.get(), step,
                        distanceFromWorldSpawn(serverLevel, blockPosition()), ServerConfig.FULL_DANGER_DISTANCE.get())
                : ServerConfig.MAX_WILD_LEVEL.get();
        int level = WildLevels.roll(cap, step, generator);
        healthGene = generator.nextDouble() < ServerConfig.WILD_HEALTH_GENE_CHANCE.get();
        setFemale(generator.nextBoolean());
        setStatPoints(StatPoints.rollWild(level, generator), species.get());
        setHealth(getMaxHealth());
    }

    private void setStatPoints(StatPoints points, Species species) {
        statPoints = points;
        statsRolled = true;
        entityData.set(DATA_LEVEL, points.level());

        StatProfile profile = species.stats();
        setBase(Attributes.MAX_HEALTH, profile.value(Stat.HEALTH, points));
        setBase(Attributes.ATTACK_DAMAGE, profile.value(Stat.ATTACK, points));
        setBase(Attributes.MOVEMENT_SPEED, profile.value(Stat.SPEED, points) * genome().speedMultiplier());
        setBase(Attributes.ARMOR, profile.value(Stat.ARMOR, points));

        BodyProfile body = species.body().orElse(BodyProfile.DEFAULT);
        setBase(Attributes.KNOCKBACK_RESISTANCE, body.knockbackResistance());
        setMaxUpStep((float) body.stepHeight());
        breaksLeaves = body.breaksLeaves();
        plowHardness = body.plowHardness();
        if (breaksLeaves || plowHardness > 0.0F) {
            // Sem isto o pathfinder contorna copas que a criatura consegue atravessar.
            setPathfindingMalus(BlockPathTypes.LEAVES, 0.0F);
        }
    }

    private void setBase(Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // ---- Torpor ----

    public double torpor() {
        return torpor;
    }

    /** De 0 a 1; disponível também no cliente. */
    public float torporFraction() {
        return entityData.get(DATA_TORPOR_FRACTION);
    }

    /** Disponível também no cliente. */
    public boolean isUnconscious() {
        return entityData.get(DATA_UNCONSCIOUS) || isCorpse();
    }

    public void addTorpor(double amount) {
        addTorpor(amount, null);
    }

    /**
     * Aplica torpor vindo de um tranquilizante. Criaturas domesticadas são imunes.
     *
     * @param source quem aplicou; se este torpor derrubar a criatura, ela passa a ser dessa pessoa para domesticar
     */
    public void addTorpor(double amount, @Nullable Player source) {
        Optional<TamingProfile> profile = tamingProfile();
        if (level().isClientSide || amount <= 0 || isTame() || profile.isEmpty()) {
            return;
        }
        boolean wasConscious = !isUnconscious();
        setTorpor(torpor + amount * profile.get().torporMultiplier());
        if (wasConscious && isUnconscious() && source != null) {
            tamerUUID = source.getUUID();
        }
    }

    /** Define o torpor diretamente, derrubando ou acordando a criatura conforme o caso. */
    public void setTorpor(double value) {
        double max = maxTorpor();
        torpor = Mth.clamp(value, 0.0, max);
        entityData.set(DATA_TORPOR_FRACTION, max > 0 ? (float) (torpor / max) : 0.0F);
        if (!isUnconscious() && max > 0 && torpor >= max) {
            knockOut();
        } else if (entityData.get(DATA_UNCONSCIOUS) && torpor <= 0) {
            wakeUp();
        }
    }

    private void knockOut() {
        entityData.set(DATA_UNCONSCIOUS, true);
        ejectPassengers();
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
    }

    private void wakeUp() {
        entityData.set(DATA_UNCONSCIOUS, false);
        tamingSession.reset();
        entityData.set(DATA_TAMING_PROGRESS, 0.0F);
        entityData.set(DATA_NEEDS_TAMING_FOOD, false);
        tamerUUID = null;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isUnconscious() || isBeakStuck();
    }

    // Inconsciente, a criatura não sai do lugar: nem empurrada, nem por recuo de golpe.
    @Override
    public boolean isPushable() {
        return !isUnconscious() && super.isPushable();
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!isUnconscious()) {
            super.knockback(strength, x, z);
        }
    }

    @Override
    public void tick() {
        if (isFlightMount()) {
            // Só o servidor decide o fim do voo: no cliente o voo selvagem não é conhecido ({@code wildFlight}
            // é do servidor), e desligar ali o voo de um Pteranodonte sem ninguém em cima o fazia "andar no ar".
            if (!level().isClientSide) {
                if (wildFlight) {
                    if (!canFlyWild()) {
                        setWildFlying(false);
                    }
                } else if (isFlying() && (!isVehicle() || isUnconscious() || !isRideReady())) {
                    setFlying(false);
                }
            }
            setNoGravity(isFlying());
            if (isFlying()) {
                resetFallDistance();
            } else if (!onGround() && !isInWater() && getDeltaMovement().y < -GLIDE_FALL_SPEED) {
                // Sem voar (desmaiada, laçada, largada no ar), plana até o chão em vez de despencar.
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x, -GLIDE_FALL_SPEED, movement.z);
                resetFallDistance();
            }
        } else {
            setNoGravity(false);
        }
        super.tick();
        if (level().isClientSide && isFlightMount()) {
            updateBodyFlightPitch();
        }
        if (isFlightMount()) {
            if (level().isClientSide) {
                updateFlightExhaustion();
            } else {
                tickFlightStamina();
            }
        }
        if (!level().isClientSide && isCorpse()) {
            tickCorpse();
            return;
        }
        if (!level().isClientSide) {
            tickHuntSpecials();
            if (isApex() && !isTame()) {
                tickApex();
            }
        }
        if (!level().isClientSide && !zoneChecked) {
            zoneChecked = true;
            if (outsideDangerZone()) {
                discard();
                return;
            }
        }
        if (!level().isClientSide && groupId == null && !isTame() && (tickCount == 1 || tickCount % 100 == 0)) {
            adoptGroup();
        }
        if (!level().isClientSide && tickCount % 20 == 0 && getType().is(ModTags.DISABLED)
                && !isTame() && tamerUUID == null && !isPersistenceRequired() && !isVehicle()) {
            // Espécie desligada (o lobo-terrível): a selvagem que sobrou de antes some.
            discard();
            return;
        }
        if (level() instanceof ServerLevel serverLevel && countsForGroupSpacing()
                && (!spacingChecked || (tickCount + getId()) % SPACING_REPORT_INTERVAL == 0)) {
            // Mundo antigo: outro grupo da espécie já ocupa a região, e este sobra. Depois da primeira
            // conferência, só renova a marca — uma manada migrando pode passar perto de outra.
            boolean removable = !spacingChecked && !isPersistenceRequired() && !isVehicle();
            spacingChecked = true;
            if (GroupSpacing.report(serverLevel, getType(), blockPosition(), !removable)
                    == SpeciesSpacing.Verdict.TOO_CLOSE && removable) {
                discard();
                return;
            }
        }
        if (level().isClientSide || tickCount % TORPOR_UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        if (isTame() && tickCount % LOCATOR_UPDATE_INTERVAL == 0) {
            CreatureLocator.update(this);
        }
        if (torpor > 0) {
            double decayPerSecond = tamingProfile().map(TamingProfile::torporDecayPerSecond).orElse(0.0);
            setTorpor(torpor - decayPerSecond * TORPOR_UPDATE_INTERVAL_TICKS / 20.0);
        }
        if (isUnconscious() && !isTame()) {
            eatFromInventory();
        }
        reprioritizeTarget();
        if (isTame() && !isUnconscious() && tickCount % SELF_HEAL_INTERVAL_TICKS == 0) {
            eatToHeal();
        }
        LivingEntity target = getTarget();
        if (isTame() && target != null
                && (!target.isAlive() || distanceToSqr(target) > TAMED_TARGET_LEASH * TAMED_TARGET_LEASH)) {
            setTarget(null);
        } else if (target != null && !canAttack(target)) {
            setTarget(null);
        }
        tickBreeding();
    }

    // ---- Reprodução ----

    public boolean isFemale() {
        return entityData.get(DATA_FEMALE);
    }

    public void setFemale(boolean female) {
        entityData.set(DATA_FEMALE, female);
    }

    /**
     * Cliente: o bico aponta para onde o voo vai — para cima subindo, para baixo mergulhando. Calculado do
     * próprio movimento, vale para o voo selvagem (que o cliente não conhece) e para o montado.
     */
    private void updateBodyFlightPitch() {
        prevBodyFlightPitch = bodyFlightPitch;
        float target = 0.0F;
        if (isFlying() && !onGround()) {
            double dy = getY() - yo;
            double horizontal = Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
            if (Math.abs(dy) + horizontal > 0.01) {
                target = Mth.clamp((float) Math.toDegrees(Math.atan2(dy, horizontal)),
                        -MAX_BODY_FLIGHT_PITCH, MAX_BODY_FLIGHT_PITCH);
            }
        }
        bodyFlightPitch += (target - bodyFlightPitch) * 0.2F;
    }

    /** A inclinação do corpo em voo neste quadro, em graus (+ sobe o bico). */
    public float bodyFlightPitch(float partialTick) {
        return Mth.lerp(partialTick, prevBodyFlightPitch, bodyFlightPitch);
    }

    // ---- Apex: tributo e desafio ----

    /** T-Rex e Espinossauro: só se domam vencendo o desafio ({@link dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel}). */
    public boolean isApex() {
        return getType().is(dev.madebyfelipe.iceagesurvival.registry.ModTags.APEX);
    }

    /** A cabeça-troféu desta espécie ({@code <espécie>_head}), ou ar se não houver. */
    public static net.minecraft.world.item.Item trophyFor(net.minecraft.world.entity.EntityType<?> type) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(id.getNamespace(), id.getPath() + "_head"));
    }

    private boolean isTrophyOf(ItemStack stack) {
        net.minecraft.world.item.Item trophy = trophyFor(getType());
        return trophy != null && trophy != Items.AIR && stack.is(trophy);
    }

    /**
     * O tributo: com a cabeça de outro da espécie na mão, o apex reconhece um caçador hábil — não o caça nem o
     * ataca; encara e ruge.
     */
    public boolean respectsTribute(Player player) {
        return isApex() && !isTame() && duel == null
                && (isTrophyOf(player.getMainHandItem()) || isTrophyOf(player.getOffhandItem()));
    }

    public boolean isDueling() {
        return duel != null;
    }

    private void startDuel(Player player, InteractionHand hand, ItemStack trophy) {
        if (!player.getAbilities().instabuild) {
            trophy.shrink(1);
        }
        duel = new dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel(player.getUUID());
        setTarget(null);
        getNavigation().stop();
        playAlert();
        threatDisplay();
        player.sendSystemMessage(Component.translatable("iceagesurvival.apex.challenge", getDisplayName(),
                dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.MAX_CREATURES));
    }

    /** Juiz do desafio: quem bate fora das regras cancela o ritual. */
    private void refereeDuel(DamageSource source) {
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        java.util.UUID player = attacker instanceof Player p ? p.getUUID() : null;
        java.util.UUID creature = attacker != null && !(attacker instanceof Player) ? attacker.getUUID() : null;
        java.util.UUID owner = attacker instanceof net.minecraft.world.entity.OwnableEntity pet ? pet.getOwnerUUID() : null;
        if (duel.hitBy(player, creature, owner) == dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.Verdict.CANCEL) {
            cancelDuel("iceagesurvival.apex.interfered");
        }
    }

    private void cancelDuel(String reasonKey) {
        if (duel == null) {
            return;
        }
        Player challenger = level().getPlayerByUUID(duel.challenger());
        duel = null;
        if (challenger != null) {
            challenger.sendSystemMessage(Component.translatable(reasonKey, getDisplayName()));
        }
    }

    /** Vencido: não morre, cai desmaiado no nome do desafiante, que o doma com carne como qualquer outro. */
    private void winDuel() {
        java.util.UUID challenger = duel.challenger();
        duel = null;
        setHealth(Math.max(1.0F, getMaxHealth() * 0.1F));
        setTarget(null);
        setTorpor(maxTorpor());
        tamerUUID = challenger;
        calmAttackers();
        Player player = level().getPlayerByUUID(challenger);
        if (player != null) {
            player.sendSystemMessage(Component.translatable("iceagesurvival.apex.won", getDisplayName()));
        }
    }

    private void tickApex() {
        if (duel != null) {
            Player challenger = level().getPlayerByUUID(duel.challenger());
            boolean wasRoaring = duel.roaring();
            if (duel.tickRoar()) {
                // Aceito o tributo: ruge para o desafiante se afastar e deixar as criaturas dele lutarem.
                setTarget(null);
                getNavigation().stop();
                if (challenger != null) {
                    getLookControl().setLookAt(challenger, 30.0F, 30.0F);
                }
                if (tickCount % ROAR_INTERVAL == 0) {
                    playAlert();
                    threatDisplay();
                }
            }
            boolean roarEnded = wasRoaring && !duel.roaring();
            if (!roarEnded && tickCount % 20 != 0) {
                return;
            }
            if (challenger == null || !challenger.isAlive()
                    || challenger.distanceTo(this) > dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.MAX_DISTANCE) {
                cancelDuel("iceagesurvival.apex.abandoned");
            } else if (!duel.roaring()) {
                chooseDuelFoe(challenger);
            }
            return;
        }
        // Tributo à vista: encara quem traz a cabeça, ruge de vez em quando, e não o ataca.
        Player bearer = level().getNearestPlayer(getX(), getY(), getZ(), TRIBUTE_SIGHT,
                candidate -> candidate instanceof Player p && respectsTribute(p));
        if (bearer != null) {
            getLookControl().setLookAt(bearer, 10.0F, 10.0F);
            if (getTarget() == bearer) {
                setTarget(null);
            }
            if (tickCount % 100 == 0) {
                playAlert();
            }
        }
    }

    /** De quanto em quanto tempo o apex ruge durante a janela do tributo. */
    private static final int ROAR_INTERVAL = 30;
    /** Até onde o apex procura as criaturas do desafiante. */
    private static final double DUEL_FOE_RADIUS = 24.0;

    /**
     * Depois do rugido: as criaturas do desafiante primeiro (fica na que já está brigando); o desafiante só se não
     * tiver nenhuma por perto ou se acabou de bater no apex.
     */
    private void chooseDuelFoe(Player challenger) {
        java.util.UUID owner = duel.challenger();
        java.util.function.Predicate<LivingEntity> ally = other -> other.isAlive()
                && other instanceof net.minecraft.world.entity.OwnableEntity pet && owner.equals(pet.getOwnerUUID())
                && !(other instanceof PrehistoricCreature creature && creature.isUnconscious());
        boolean provoked = getLastHurtByMob() == challenger
                && tickCount - getLastHurtByMobTimestamp() < dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.PROVOKED_TICKS;
        LivingEntity current = getTarget();
        LivingEntity creature = current != null && ally.test(current) ? current
                : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(DUEL_FOE_RADIUS), ally).stream()
                        .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        LivingEntity foe = dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.foe(creature != null, provoked)
                == dev.madebyfelipe.iceagesurvival.core.ecology.ApexDuel.Foe.CREATURES ? creature : challenger;
        if (current != foe) {
            setTarget(foe);
        }
    }

    /** De quão longe o apex nota a cabeça na mão do jogador. */
    private static final double TRIBUTE_SIGHT = 24.0;

    // ---- Corpo e implante ----

    public boolean isCorpse() {
        return entityData.get(DATA_CORPSE);
    }

    /** O corpo não é inimigo de ninguém: os selvagens largam a domesticada caída. */
    @Override
    public boolean canBeSeenAsEnemy() {
        return !isCorpse() && super.canBeSeenAsEnemy();
    }

    /**
     * Quem não se ataca: durante o rugido do apex, ninguém; a domesticada não bate na criatura desmaiada que o dono
     * está domando (o apex vencido no desafio, ou a derrubada a dardo).
     */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (duel != null && duel.roaring()) {
            return false;
        }
        if (isTame() && target instanceof PrehistoricCreature downed && downed.isUnconscious()
                && downed.tamerUUID != null && downed.tamerUUID.equals(getOwnerUUID())) {
            return false;
        }
        if (fearsWater() && target.isInWater()) {
            return false;
        }
        return super.canAttack(target);
    }

    /** Quem está atacando esta criatura larga o alvo (o apex vencido, a domesticada que virou corpo). */
    private void calmAttackers() {
        for (net.minecraft.world.entity.Mob mob : level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                getBoundingBox().inflate(CALM_RADIUS), mob -> mob.getTarget() == this)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    private static final double CALM_RADIUS = 48.0;

    /**
     * A domesticada não some ao morrer: fica caída por {@link #CORPSE_TICKS} com o inventário e o implante (a
     * criatura inteira, para a mesa de reviver). Sem espaço no inventário, o implante cai ao lado.
     */
    private void becomeCorpse(DamageSource source) {
        ejectPassengers();
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        setHealth(1.0F);
        calmAttackers();
        ItemStack implant = dev.madebyfelipe.iceagesurvival.item.ImplantItem.of(this, ModItems.IMPLANT.get());
        entityData.set(DATA_CORPSE, true);
        corpseTicks = 0;
        ItemStack left = inventory.addItem(implant);
        if (!left.isEmpty()) {
            spawnAtLocation(left);
        }
        if (getOwner() instanceof Player owner) {
            owner.sendSystemMessage(Component.translatable("iceagesurvival.corpse.fallen", getDisplayName(),
                    source.getLocalizedDeathMessage(this)));
        }
    }

    /** Os dados que o implante guarda: a criatura inteira, menos o que fica no corpo e o que é do lugar. */
    public CompoundTag implantData() {
        CompoundTag tag = saveWithoutId(new CompoundTag());
        for (String key : new String[] {TAG_INVENTORY, TAG_CORPSE, TAG_CORPSE_TICKS, TAG_HOME, "UUID", "Pos", "Motion",
                "Rotation", "Passengers", "Leash", "Health", "DeathTime", "HurtTime", "HurtByTimestamp"}) {
            tag.remove(key);
        }
        tag.putBoolean(TAG_SADDLED, false);
        return tag;
    }

    private void tickCorpse() {
        corpseTicks++;
        // Some no fim do prazo, ou quando já tiraram tudo (e o implante).
        boolean emptied = corpseTicks > 40 && corpseTicks % 20 == 0 && inventory.isEmpty() && !isSaddled();
        if (corpseTicks >= CORPSE_TICKS || emptied) {
            if (isSaddled()) {
                spawnAtLocation(new ItemStack(SADDLE_ITEM));
            }
            Containers.dropContents(level(), this, inventory);
            discard();
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isCorpse() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    // ---- Fôlego de voo ----

    /** Fôlego de voo máximo, em segundos (atributo {@code flight_stamina}; 0 = não voa). */
    public double maxFlightStamina() {
        return species().map(species -> species.stats().value(Stat.FLIGHT_STAMINA, statPoints)).orElse(0.0);
    }

    /** Fôlego de voo restante, de 0 a 1. */
    public float flightStaminaFraction() {
        return entityData.get(DATA_FLIGHT_STAMINA);
    }

    public void setFlightStaminaFraction(float fraction) {
        entityData.set(DATA_FLIGHT_STAMINA, Mth.clamp(fraction, 0.0F, 1.0F));
        updateFlightExhaustion();
    }

    /** Esgotada: não bate as asas, só plana, e não decola até recuperar {@link #TAKEOFF_STAMINA}. */
    public boolean isFlightExhausted() {
        return flightExhausted;
    }

    /** Nos dois lados (o cliente de quem monta decide a decolagem): esgota no zero, recupera em 30%. */
    private void updateFlightExhaustion() {
        flightExhausted = FlightStamina.exhausted(flightExhausted, flightStaminaFraction(), TAKEOFF_STAMINA);
    }

    /**
     * Servidor: voando gasta (subindo o dobro, planando em descida um quinto), pousada recarrega. O valor vai
     * sincronizado: o cliente de quem monta limita a subida com ele.
     */
    private void tickFlightStamina() {
        double max = maxFlightStamina();
        if (max <= 0.0) {
            return;
        }
        float fraction = flightStaminaFraction();
        if (isFlying() && !onGround()) {
            double dy = getY() - yo;
            double rate = FlightStamina.drainRate(dy);
            fraction -= (float) (rate / 20.0 / max);
            if (fraction <= 0.0F) {
                fraction = 0.0F;
                if (wildFlight) {
                    // O selvagem esgotado não voa mais: plana até o chão.
                    setWildFlying(false);
                }
            }
        } else if (onGround() || isInWater()) {
            fraction += (float) (1.0 / (FLIGHT_STAMINA_REFILL_SECONDS * 20.0));
        }
        setFlightStaminaFraction(fraction);
    }

    /** ♀ ou ♂. */
    public static String sexSymbol(boolean female) {
        return female ? "♀" : "♂";
    }

    /** Com nome dado pelo jogador, o sexo aparece ao lado dele (na plaquinha e nas mensagens). */
    @Override
    public Component getDisplayName() {
        Component name = super.getDisplayName();
        if (!hasCustomName()) {
            return name;
        }
        return name.copy().append(Component.literal(" " + sexSymbol(isFemale()))
                .withStyle(style -> style.withColor(isFemale() ? 0xFF7BC4 : 0x5BA8FF)));
    }

    /** Se o dono ligou o acasalamento; disponível também no cliente. */
    public boolean isMatingEnabled() {
        return entityData.get(DATA_MATING);
    }

    public void setMatingEnabled(boolean enabled) {
        entityData.set(DATA_MATING, enabled);
        if (!enabled) {
            matingProgress = 0;
        }
    }

    public boolean isPregnant() {
        return gestationEnd > 0;
    }

    /** Fração da gestação cumprida, de 0 a 1; −1 se não estiver prenhe. Só no servidor. */
    public float gestationProgress() {
        if (!isPregnant()) {
            return -1.0F;
        }
        int total = breedingProfile().map(BreedingProfile::incubationSeconds).orElse(1) * 20;
        return Mth.clamp(1.0F - (gestationEnd - level().getGameTime()) / (float) total, 0.0F, 1.0F);
    }

    /** Fração do crescimento cumprida, de 0 a 1 (1 = adulto). Só no servidor. */
    public float maturationProgress() {
        if (!isBaby()) {
            return 1.0F;
        }
        int total = breedingProfile().map(BreedingProfile::maturationSeconds).orElse(1) * 20;
        return Mth.clamp(1.0F + getAge() / (float) total, 0.0F, 1.0F);
    }

    public Optional<BreedingProfile> breedingProfile() {
        return species().flatMap(Species::breeding);
    }

    /**
     * Uma vez por segundo: a fêmea com o acasalamento ligado, perto de um macho da mesma espécie
     * e do mesmo dono também com ele ligado, soma um segundo; em {@link #MATING_SECONDS} concebe.
     */
    private void tickBreeding() {
        if (isPregnant()) {
            if (level().getGameTime() >= gestationEnd) {
                giveBirth();
            }
            return;
        }
        Optional<BreedingProfile> breeding = breedingProfile();
        PrehistoricCreature partner = breeding.isPresent() && canMate() && isFemale()
                && level().getGameTime() >= nextMatingTime ? findMate() : null;
        if (partner == null) {
            matingProgress = 0;
            return;
        }
        matingProgress++;
        level().broadcastEntityEvent(this, (byte) 18);
        level().broadcastEntityEvent(partner, (byte) 18);
        if (matingProgress >= MATING_SECONDS) {
            conceive(partner, breeding.get());
        }
    }

    private boolean canMate() {
        return isMatingEnabled() && isTame() && !isBaby() && !isUnconscious() && isAlive();
    }

    @Nullable
    private PrehistoricCreature findMate() {
        return level().getEntitiesOfClass(PrehistoricCreature.class, getBoundingBox().inflate(MATING_RADIUS),
                        other -> other != this && other.getType() == getType() && !other.isFemale() && other.canMate()
                                && java.util.Objects.equals(other.getOwnerUUID(), getOwnerUUID()))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private void conceive(PrehistoricCreature father, BreedingProfile breeding) {
        Genome child = Genetics.inherit(genome(), father.genome(), randomGenerator(), mutationTuning());
        long now = level().getGameTime();
        matingProgress = 0;
        nextMatingTime = now + breeding.cooldownSeconds() * 20L;
        Player owner = getOwnerUUID() == null ? null : level().getPlayerByUUID(getOwnerUUID());
        if (breeding.offspring() == BreedingProfile.Offspring.EGG) {
            spawnAtLocation(CreatureEggItem.create(getType(), child, getOwnerUUID()));
            playSound(SoundEvents.TURTLE_LAY_EGG, 1.0F, 1.0F);
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("iceagesurvival.breeding.egg", getName()), true);
            }
        } else {
            gestationEnd = now + breeding.incubationSeconds() * 20L;
            gestationChild = child;
            if (owner != null) {
                owner.displayClientMessage(Component.translatable("iceagesurvival.breeding.pregnant", getName()), true);
            }
        }
    }

    private void giveBirth() {
        Genome child = gestationChild;
        gestationEnd = 0;
        gestationChild = null;
        if (child != null && level() instanceof ServerLevel serverLevel) {
            spawnOffspring(serverLevel, getType(), child, getOwnerUUID(), position());
        }
    }

    public static Genetics.Tuning mutationTuning() {
        return new Genetics.Tuning(ServerConfig.MUTATION_CHANCE.get(), ServerConfig.MUTATION_ATTEMPTS.get(),
                ServerConfig.HEALTH_GENE_CHANCE.get());
    }

    /**
     * Põe um filhote no mundo: com o genoma dado, sexo sorteado, domesticado pelo dono (se
     * houver), seguindo e passivo. Usado pelo parto e pela incubadora.
     */
    @Nullable
    public static PrehistoricCreature spawnOffspring(ServerLevel level, EntityType<?> type, Genome genome,
                                                     @Nullable UUID owner, Vec3 pos) {
        if (!(type.create(level) instanceof PrehistoricCreature baby)) {
            return null;
        }
        baby.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        baby.setGenome(genome);
        baby.setFemale(level.random.nextBoolean());
        baby.setHealth(baby.getMaxHealth());
        int maturation = baby.breedingProfile().map(BreedingProfile::maturationSeconds).orElse(1200);
        baby.setAge(-maturation * 20);
        baby.refreshDimensions();
        if (owner != null) {
            baby.setTame(true);
            baby.setOwnerUUID(owner);
            baby.affinity = MAX_INITIAL_AFFINITY;
        }
        baby.setMovement(Movement.FOLLOW);
        baby.setStance(Stance.PASSIVE);
        level.addFreshEntity(baby);
        level.broadcastEntityEvent(baby, (byte) 18);
        return baby;
    }

    public float getAgeScale() {
        return isBaby() ? BABY_SCALE : 1.0F;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getAgeScale());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % STRESS_INTERVAL == 0) {
            tickStress();
        }
        if (level().isClientSide || !horizontalCollision || isUnconscious()
                || !ForgeEventFactory.getMobGriefingEvent(level(), this)) {
            return;
        }
        if (plowHardness > 0.0F) {
            plowThroughTheWay();
        } else if (breaksLeaves) {
            breakLeavesInTheWay();
        }
    }

    public boolean isStalking() {
        return stalking;
    }

    public void setStalking(boolean stalking) {
        this.stalking = stalking;
    }

    /** Carnívoro que espreita antes do bote ({@code hunt_style} {@code stalk}, o padrão de quem tem presa). */
    public boolean stalks() {
        return behavior().map(b -> b.prey().isPresent() && b.huntStyle() == BehaviorProfile.HuntStyle.STALK)
                .orElse(false);
    }

    /** A espreita acabou: a presa viu esta criatura, ou o bando já deu o bote. */
    public void blowStalk() {
        stalkBlownUntil = level().getGameTime() + 40;
    }

    public boolean stalkBlown() {
        return level().getGameTime() <= stalkBlownUntil;
    }

    /**
     * O bote: o bando inteiro que espreita a mesma presa larga a espreita junto.
     */
    public void signalPounce(LivingEntity prey) {
        double radius = Math.max(behavior().map(BehaviorProfile::herdRadius).orElse(0), 16) * 2.0;
        for (PrehistoricCreature other : level().getEntitiesOfClass(PrehistoricCreature.class,
                getBoundingBox().inflate(radius), o -> o != this && sameGroup(o) && o.getTarget() == prey)) {
            other.blowStalk();
        }
    }

    /**
     * O faro de caça que vale ({@link Perception}): o {@code hunt_radius} da espécie, nunca abaixo do maior
     * alerta das presas da dieta + a folga. Calculado de novo quando os dados da espécie recarregam.
     */
    public double huntRadius() {
        Species species = species().orElse(null);
        if (species == null) {
            return ecology().huntRadius();
        }
        if (species != perceptionSpecies) {
            perceptionSpecies = species;
            perceptionRadius = Perception.huntRadius(ecology().huntRadius(), largestPreyAlert());
        }
        return perceptionRadius;
    }

    /** O maior raio de alerta (ou de filhote) entre as espécies do mod que esta caça; 0 se nenhuma. */
    public double largestPreyAlert() {
        var preyTag = behavior().flatMap(BehaviorProfile::prey).orElse(null);
        var registry = level().registryAccess().registry(Species.REGISTRY_KEY).orElse(null);
        if (preyTag == null || registry == null) {
            return 0.0;
        }
        double largest = 0.0;
        for (var entry : registry.entrySet()) {
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(entry.getKey().location());
            if (type.isEmpty() || !type.get().is(preyTag) || type.get() == getType()) {
                continue;
            }
            var wariness = entry.getValue().behavior().flatMap(BehaviorProfile::wariness);
            if (wariness.isPresent()) {
                largest = Math.max(largest, Math.max(wariness.get().alertRadius(), wariness.get().calfRadius()));
            }
        }
        return largest;
    }

    /** Se atravessa a vegetação da superfície quebrando. */
    public boolean plows() {
        return plowHardness > 0.0F;
    }

    /**
     * Quebra, à frente do corpo, os blocos da tag {@code plowable} com dureza até o limite da
     * espécie. Só vegetação e neve: o chão e as encostas ficam, e a criatura sobe por eles.
     */
    private void plowThroughTheWay() {
        Vec3 motion = getDeltaMovement().multiply(1.0, 0.0, 1.0);
        Vec3 forward = motion.lengthSqr() > 1.0E-4 ? motion.normalize() : Vec3.directionFromRotation(0.0F, getYRot());
        AABB box = getBoundingBox().expandTowards(forward.scale(0.8)).inflate(0.1, 0.0, 0.1);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY + 0.01, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = level().getBlockState(pos);
            if (!state.is(ModTags.PLOWABLE) || state.hasBlockEntity()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level(), pos);
            if (hardness >= 0.0F && hardness <= plowHardness
                    && ForgeEventFactory.onEntityDestroyBlock(this, pos, state)) {
                level().destroyBlock(pos, true, this);
            }
        }
    }

    private void breakLeavesInTheWay() {
        AABB box = getBoundingBox().inflate(0.2);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (level().getBlockState(pos).getBlock() instanceof LeavesBlock) {
                level().destroyBlock(pos, true, this);
            }
        }
    }

    // ---- Manada ----

    /**
     * Defesa em grupo: faz as criaturas selvagens da mesma espécie por perto, que ainda não
     * tenham alvo, atacarem quem feriu esta. Uma varredura por agressão sofrida.
     */
    /**
     * Caça em bando: os da mesma espécie por perto, selvagens e sem alvo, partem atrás da presa
     * que esta escolheu. Só para espécies de manada (lobos, raptores, alossauros).
     */
    public void rallyPack(@Nullable LivingEntity prey) {
        int herdRadius = behavior().map(BehaviorProfile::herdRadius).orElse(0);
        if (prey == null || isTame() || herdRadius <= 0) {
            return;
        }
        for (PrehistoricCreature other : level().getEntitiesOfClass(
                PrehistoricCreature.class, getBoundingBox().inflate(herdRadius),
                candidate -> sameGroup(candidate)
                        && !candidate.isBaby() && !candidate.isUnconscious() && candidate.getTarget() == null)) {
            other.setTarget(prey);
            other.beginHunt();
        }
    }

    /**
     * Se esta criatura de manada está desgarrada: nenhum outro da espécie, acordado, no raio da
     * manada. Criaturas solitárias estão sempre isoladas.
     */
    /**
     * Quantos da espécie enfrentam algo juntos aqui, contando esta: a manada com defesa em grupo e o
     * bando de caçadores. Solitárias contam 1. É a mesma conta para os dois lados de um confronto
     * ({@link dev.madebyfelipe.iceagesurvival.core.ecology.ThreatResponse}) e para a caçada.
     */
    public int fightingGroup() {
        BehaviorProfile behavior = behavior().orElse(null);
        if (behavior == null || behavior.herdRadius() <= 0
                || !behavior.groupDefense() && behavior.prey().isEmpty()) {
            return 1;
        }
        double radius = Math.max(behavior.herdRadius(), ThreatResponse.ALLY_RADIUS);
        return 1 + level().getEntitiesOfClass(PrehistoricCreature.class, getBoundingBox().inflate(radius),
                other -> sameGroup(other) && !other.isBaby() && !other.isUnconscious()).size();
    }

    /** Porte de {@code other} em relação a esta criatura ({@link ThreatResponse#sizeRatio}). */
    public double sizeRatioOf(LivingEntity other) {
        return ThreatResponse.sizeRatio(getBbWidth(), getBbHeight(), other.getBbWidth(), other.getBbHeight());
    }

    public boolean isIsolated() {
        int herdRadius = behavior().map(BehaviorProfile::herdRadius).orElse(0);
        if (herdRadius <= 0) {
            return true;
        }
        return level().getEntitiesOfClass(PrehistoricCreature.class, getBoundingBox().inflate(herdRadius),
                other -> sameGroup(other) && other.isAlive() && !other.isUnconscious())
                .isEmpty();
    }

    public void alertHerd(@Nullable LivingEntity attacker) {
        Optional<BehaviorProfile> behavior = behavior();
        if (attacker == null || isTame() || behavior.isEmpty() || !behavior.get().groupDefense()) {
            return;
        }
        double range = Math.max(behavior.get().herdRadius(), behavior.get().aggroRadius());
        for (PrehistoricCreature other : level().getEntitiesOfClass(
                PrehistoricCreature.class, getBoundingBox().inflate(range),
                candidate -> sameGroup(candidate)
                        && !candidate.isUnconscious() && candidate.getTarget() == null)) {
            other.setTarget(attacker);
        }
    }

    // ---- Inventário ----

    public SimpleContainer inventory() {
        return inventory;
    }

    @Nullable
    public UUID tamerUUID() {
        return tamerUUID;
    }

    /**
     * Domesticada: só o dono. Selvagem: só enquanto inconsciente, e só quem a derrubou
     * (ou qualquer um, se ninguém a derrubou).
     */
    public boolean canAccessInventory(Player player) {
        if (!isAlive()) {
            return false;
        }
        if (isTame()) {
            return isOwner(player);
        }
        return isUnconscious() && (tamerUUID == null || tamerUUID.equals(player.getUUID()));
    }

    private void openInventory(Player player) {
        if (!canAccessInventory(player)) {
            player.displayClientMessage(Component.translatable("iceagesurvival.taming.not_yours"), true);
            return;
        }
        if (!isTame() && tamerUUID == null) {
            tamerUUID = player.getUUID();
        }
        int rows = Mth.clamp((inventory.getContainerSize() + 8) / 9, 1, 6);
        int pages = Math.max(1, (inventory.getContainerSize() + 53) / 54);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                        (containerId, playerInventory, opener) ->
                                new CreatureStorageMenu(containerId, playerInventory, this),
                        getDisplayName()),
                buffer -> {
                    buffer.writeVarInt(getId());
                    buffer.writeVarInt(inventory.getContainerSize());
                    buffer.writeVarInt(rows);
                    buffer.writeVarInt(pages);
                    buffer.writeBoolean(canBeSaddled());
                });
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (isSaddled()) {
            spawnAtLocation(new ItemStack(SADDLE_ITEM));
        }
        Containers.dropContents(level(), this, inventory);
    }

    // ---- Comandos ----

    /** Se o jogador é o dono. Compara pelo UUID, então vale mesmo com o dono fora do mundo. */
    public boolean isOwner(Player player) {
        return isTame() && player.getUUID().equals(getOwnerUUID());
    }

    /** Seguir ou ficar; disponível também no cliente. Só tem efeito em criaturas domesticadas. */
    public Movement movement() {
        Movement[] all = Movement.values();
        int index = entityData.get(DATA_MOVEMENT);
        return index >= 0 && index < all.length ? all[index] : DEFAULT_MOVEMENT;
    }

    /** Postura de luta; disponível também no cliente. Só tem efeito em criaturas domesticadas. */
    public Stance stance() {
        Stance[] all = Stance.values();
        int index = entityData.get(DATA_STANCE);
        return index >= 0 && index < all.length ? all[index] : DEFAULT_STANCE;
    }

    public void setMovement(Movement movement) {
        entityData.set(DATA_MOVEMENT, (byte) movement.ordinal());
        setOrderedToSit(movement == Movement.STAY);
        if (movement == Movement.STAY) {
            getNavigation().stop();
        }
    }

    public void setStance(Stance stance) {
        entityData.set(DATA_STANCE, (byte) stance.ordinal());
        if (!stance.fightsBack()) {
            setTarget(null);
        }
    }

    /** Sorteia se a criatura obedece a um comando, conforme a afinidade. */
    public boolean rollObedience() {
        return Obedience.obeys(affinity, MAX_AFFINITY, ServerConfig.MIN_OBEDIENCE.get(), randomGenerator());
    }

    public void setAffinity(float value) {
        affinity = Mth.clamp(value, 0.0F, MAX_AFFINITY);
    }

    /**
     * Registra os goals que fazem uma criatura domesticada cumprir ordens. Chamar em
     * {@code registerGoals}; as prioridades dos goals de movimento ficam por conta da espécie.
     */
    protected void addOrderGoals(int stayPriority, int followPriority, double followSpeed) {
        goalSelector.addGoal(stayPriority, new OrderGoals.Stay(this));
        goalSelector.addGoal(followPriority, new OrderGoals.Follow(this, followSpeed, 8.0F, 3.0F));
        targetSelector.addGoal(1, new OrderGoals.DefendOwner(this));
        targetSelector.addGoal(2, new OrderGoals.AssistOwner(this));
        targetSelector.addGoal(3, new OrderGoals.Retaliate(this));
    }

    // ---- Montaria ----

    /** Se a espécie aceita sela; disponível também no cliente (vem dos dados sincronizados). */
    public boolean canBeSaddled() {
        return mountProfile().map(MountProfile::requiresSaddle).orElse(false) && !isBaby();
    }

    /** Se a espécie é montável por um adulto (com ou sem sela). */
    public boolean isMountable() {
        return mountProfile().isPresent() && !isBaby();
    }

    /** Pronta para levar alguém: selada, ou de uma espécie que se monta sem sela. */
    public boolean isRideReady() {
        return isSaddled() || mountProfile().map(mount -> !mount.requiresSaddle()).orElse(false) && !isBaby();
    }

    /** Disponível também no cliente. */
    public boolean isSaddled() {
        return entityData.get(DATA_SADDLED);
    }

    public void setSaddled(boolean saddled) {
        entityData.set(DATA_SADDLED, saddled);
        if (!saddled) {
            ejectPassengers();
        }
    }

    /**
     * Se este jogador pode montar agora. Exige ser o dono, a criatura selada, acordada e com
     * afinidade suficiente — uma criatura que acabou de ser domesticada ainda não deixa montar.
     */
    public boolean canBeRiddenBy(Player player) {
        Optional<MountProfile> mount = mountProfile();
        return mount.isPresent()
                && isRideReady()
                && isAlive()
                && !isUnconscious()
                && isOwner(player)
                && affinity >= mount.get().minAffinity();
    }

    /**
     * Coloca o jogador na criatura, se ela aceitar. Como no cavalo, quem monta de verdade é
     * o servidor; no cliente a chamada só confirma que a interação valeu.
     */
    public boolean ride(Player player) {
        if (!canBeRiddenBy(player) || isVehicle()) {
            return false;
        }
        if (level().isClientSide) {
            return true;
        }
        // Montar solta uma criatura que estava mandada ficar.
        if (movement() == Movement.STAY) {
            setMovement(Movement.FOLLOW);
        }
        getNavigation().stop();
        setTarget(null);
        player.setYRot(getYRot());
        player.setXRot(getXRot());
        return player.startRiding(this);
    }

    /**
     * Ataque de quem monta. A mordida sempre acontece — recarga, animação, som e, para as
     * espécies com {@code break_hardness}, os blocos à frente — e só depois se procura quem
     * morder: o alvo mirado, se estiver ao alcance, ou a primeira criatura na área da
     * mordida. Nunca o próprio dono nem as criaturas dele; o servidor confere tudo porque o
     * cliente só diz em quem mirou.
     *
     * @param target em quem quem monta mirou; nulo se a mira não pegou ninguém
     * @return se a mordida aconteceu (não se acertou alguém)
     */
    public boolean attackAsMount(Player rider, @Nullable LivingEntity target) {
        if (getControllingPassenger() != rider || !isAlive() || isUnconscious()
                || level().getGameTime() < nextRiderAttackTime) {
            return false;
        }
        nextRiderAttackTime = level().getGameTime() + RIDDEN_ATTACK_COOLDOWN;
        swingAttack();
        if (rider instanceof ServerPlayer serverRider) {
            breakBlocksInBite(serverRider);
        }
        LivingEntity victim = canBiteAsMount(rider, target, true) ? target : level()
                .getEntitiesOfClass(LivingEntity.class, biteArea(), candidate -> canBiteAsMount(rider, candidate, false))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (victim != null) {
            attackSwung = true;
            try {
                doHurtTarget(victim);
            } finally {
                attackSwung = false;
            }
        }
        return true;
    }

    /**
     * @param aimed o alvo veio da mira de quem monta: vale em qualquer direção ao alcance; sem
     *              mira, só o que estiver no cone à frente
     */
    private boolean canBiteAsMount(Player rider, @Nullable LivingEntity target, boolean aimed) {
        if (target == null || target == this || target == rider || !target.isAlive() || hasPassenger(target)) {
            return false;
        }
        if (target instanceof net.minecraft.world.entity.OwnableEntity ownable && rider.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        if (target instanceof Player other && !rider.canHarmPlayer(other)) {
            return false;
        }
        if (!getBoundingBox().inflate(riddenReach()).intersects(target.getBoundingBox())) {
            return false;
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 to = target.position().subtract(position());
        return aimed || MountedReach.inBiteCone(facing.x, facing.z, to.x, to.z);
    }

    /** Alcance do golpe montado, a partir da borda do corpo: cresce com o tamanho da montaria. */
    public double riddenReach() {
        return MountedReach.reach(getBbWidth());
    }

    /** Em volta do corpo pelo alcance da mordida, descendo um pouco: pega o bicho baixo à frente. */
    private AABB biteArea() {
        double reach = riddenReach();
        return getBoundingBox().inflate(reach, 0.0, reach).expandTowards(0.0, -1.5, 0.0);
    }

    /**
     * Quebra os blocos na frente do corpo, do chão em que pisa até o topo da cabeça, com
     * dureza até o limite da espécie e, se ela tiver {@code break_blocks}, só os daquela tag.
     * Os blocos dropam como se quebrados à mão. Respeita {@code mobGriefing}, a proteção do spawn e
     * os eventos de quebra de bloco (mods de proteção de terreno), como se fosse quem monta
     * quebrando, e nunca quebra bloco com inventário.
     */
    private void breakBlocksInBite(ServerPlayer rider) {
        MountProfile mount = mountProfile().orElse(null);
        if (mount == null || mount.breakHardness() <= 0.0F || !ForgeEventFactory.getMobGriefingEvent(level(), this)) {
            return;
        }
        float maxHardness = mount.breakHardness();
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        double halfWidth = getBbWidth() / 2.0;
        double sideReach = halfWidth + MountedReach.BREAK_SIDE_MARGIN;
        int minY = Mth.floor(getY() + 0.01);
        int maxY = Mth.floor(getY() + getBbHeight() - 0.01);
        java.util.Set<BlockPos> bitten = new java.util.LinkedHashSet<>();
        for (double depth = halfWidth + 0.5; depth <= halfWidth + MountedReach.breakDepth(getBbWidth()); depth += 0.5) {
            for (double lateral = -sideReach; lateral <= sideReach + 1.0E-3; lateral += 0.5) {
                Vec3 column = position().add(forward.scale(depth)).add(side.scale(lateral));
                for (int y = minY; y <= maxY; y++) {
                    bitten.add(BlockPos.containing(column.x, y, column.z));
                }
            }
        }
        for (BlockPos pos : bitten) {
            var state = level().getBlockState(pos);
            float hardness = state.getDestroySpeed(level(), pos);
            if (state.isAir() || hardness < 0.0F || hardness > maxHardness || state.hasBlockEntity()
                    || mount.breakBlocks().isPresent() && !state.is(mount.breakBlocks().get())
                    || !level().mayInteract(rider, pos)
                    || rider.connection.connection.channel() != null
                            && ForgeHooks.onBlockBreakEvent(level(), rider.gameMode.getGameModeForPlayer(), rider, pos) < 0) {
                continue;
            }
            level().destroyBlock(pos, true, this);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (isRideReady() && !isUnconscious() && getFirstPassenger() instanceof Player player && isOwner(player)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (isFlying()) {
            // No voo a velocidade é posta inteira por tickFlight; o travel do vanilla só a aplica.
            return Vec3.ZERO;
        }
        float strafe = player.xxa * RIDDEN_STRAFE_FACTOR;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= RIDDEN_BACKWARD_FACTOR;
        }
        return new Vec3(strafe, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        double multiplier = mountProfile().map(MountProfile::speedMultiplier).orElse(1.0);
        return (float) (getAttributeValue(Attributes.MOVEMENT_SPEED) * multiplier);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (!isFlying()) {
            // A criatura aponta para onde quem monta olha; o passo do pescoço é metade, como no cavalo.
            setRot(player.getYRot(), player.getXRot() * 0.5F);
        }
        // Em voo o rumo é do tickFlight, no cliente de quem monta; o servidor recebe a rotação com a posição.
        yRotO = yBodyRot = yHeadRot = getYRot();
        getNavigation().stop();
        if (!isControlledByLocalInstance()) {
            return;
        }
        boolean jumpPressed = riderJumpHeld && !riderJumpWasHeld;
        riderJumpWasHeld = riderJumpHeld;
        if (isFlightMount()) {
            tickFlight(player, jumpPressed);
        } else if (jumpPressed && onGround()) {
            riderJump();
        }
    }

    /**
     * Pulo na hora, sem a barra de carga do cavalo. A altura e o avanço vêm da espécie: as grandes
     * não pulam ({@code jump_strength} 0) e o Smilodon dá um bote longo e baixo.
     */
    private void riderJump() {
        MountProfile mount = mountProfile().orElse(null);
        if (mount == null || mount.jumpStrength() <= 0.0) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(movement.x + forward.x * mount.jumpForward(),
                mount.jumpStrength() * getBlockJumpFactor(),
                movement.z + forward.z * mount.jumpForward());
        hasImpulse = true;
        ForgeHooks.onLivingJump(this);
    }

    /** Voo montado, no cliente de quem monta ({@link FlightModel}). */
    private void tickFlight(Player player, boolean jumpPressed) {
        MountProfile mount = mountProfile().orElse(MountProfile.DEFAULT);
        double maxSpeed = mount.flightSpeed();
        FlightModel.Tuning tuning = FlightModel.Tuning.forMaxSpeed(maxSpeed, mount.flightTurnRate());
        if (!isFlying()) {
            if (jumpPressed && isRideReady() && !isFlightExhausted()) {
                setFlying(true);
                flightSpeed = maxSpeed * FlightModel.TAKEOFF_SPEED_FRACTION;
                flightYaw = getYRot();
                flightPitch = 0.0F;
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x, FlightModel.TAKEOFF_LIFT, movement.z);
            }
            return;
        }
        boolean exhausted = flightStaminaFraction() <= 0.0F;
        // Sem fôlego, não bate as asas nem dá impulso: só plana, sem subir.
        FlightModel.Input input = new FlightModel.Input(player.zza, player.xxa,
                riderJumpHeld && !exhausted, riderBoost && !exhausted);
        flightYaw = FlightModel.nextYaw(flightYaw, player.getYRot(), flightSpeed, tuning);
        flightPitch = FlightModel.nextPitch(flightPitch, exhausted ? Math.max(player.getXRot(), 12.0F) : player.getXRot(),
                tuning);
        flightSpeed = FlightModel.nextSpeed(flightSpeed, flightPitch, input, tuning);
        FlightModel.Velocity velocity = FlightModel.velocity(flightSpeed, flightYaw, flightPitch, input, tuning);
        if (onGround() && velocity.y() < 0) {
            // Rasante: encostou no chão rápido demais para pousar; desliza em vez de afundar.
            velocity = new FlightModel.Velocity(velocity.x(), 0.0, velocity.z());
        }
        setDeltaMovement(velocity.x(), velocity.y(), velocity.z());
        setRot(flightYaw, flightPitch);
        yRotO = yBodyRot = yHeadRot = flightYaw;
        resetFallDistance();
        if (FlightModel.shouldLand(onGround(), riderJumpHeld, player.getXRot(), flightSpeed, tuning)) {
            setFlying(false);
        }
    }

    /** Sem ninguém no controle, a montaria aérea não fica parada no ar. */
    @Override
    protected void removePassenger(net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (isFlying() && !isVehicle()) {
            setFlying(false);
        }
    }

    /**
     * No cliente de uma montaria voadora, quem monta vai no osso {@code rider_pos} animado, como no
     * Revival. Nas outras, e no servidor, na altura dos dados, adiantado por {@code seat_forward}.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (!hasPassenger(passenger)) {
            return;
        }
        if (level().isClientSide && isFlightMount() && animatedSeat != null
                && tickCount - animatedSeatTick <= ANIMATED_SEAT_MAX_AGE) {
            move.accept(passenger, getX() + animatedSeat.x,
                    getY() + animatedSeat.y + passenger.getMyRidingOffset(), getZ() + animatedSeat.z);
            return;
        }
        double forward = mountProfile().map(MountProfile::seatForward).orElse(0.0);
        Vec3 ahead = Vec3.directionFromRotation(0.0F, yBodyRot).scale(forward);
        move.accept(passenger, getX() + ahead.x,
                getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset(), getZ() + ahead.z);
    }

    /** Onde quem monta se senta. Fica nos dados da espécie, junto do resto do corpo. */
    @Override
    public double getPassengersRidingOffset() {
        return mountProfile().map(mount -> mount.seatHeight(getBbHeight())).orElse((double) getBbHeight());
    }

    @Override
    public boolean isPushedByFluid() {
        // Com alguém montado, a correnteza não arrasta a criatura para fora do controle de quem monta.
        return !isVehicle() && super.isPushedByFluid();
    }

    // ---- Domesticação ----

    /** Se há no inventário algo que a espécie come para ser domesticada. */
    private boolean hasTamingFood(TamingProfile profile) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (profile.foodFor(inventory.getItem(slot)).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** A domesticação está parada por falta de comida no inventário; disponível também no cliente. */
    public boolean needsTamingFood() {
        return entityData.get(DATA_NEEDS_TAMING_FOOD);
    }

    /** De 0 a 1; disponível também no cliente. */
    public float tamingProgress() {
        return entityData.get(DATA_TAMING_PROGRESS);
    }

    /** De 0 a {@link #MAX_AFFINITY}; só no servidor. */
    public float affinity() {
        return affinity;
    }

    private double requiredFood(TamingProfile profile) {
        return TamingRules.requiredFood(profile.requiredFood(), profile.requiredFoodPerLevel(), statPoints.level());
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && duel != null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            winDuel();
            return;
        }
        if (!level().isClientSide && isTame() && !isCorpse() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            becomeCorpse(source);
            return;
        }
        if (!level().isClientSide && !isTame()) {
            stressHerd(Stress.Event.HERD_KILLED);
        }
        super.die(source);
        if (!level().isClientSide && isDeadOrDying()) {
            CreatureLocator.forget(this);
        }
    }

    /**
     * Saindo do mundo carregado (chunk descarregado, troca de dimensão), a domesticada grava onde ficou
     * para o menu "localizar criatura"; removida de vez, sai da lista.
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && isTame()) {
            if (reason == RemovalReason.DISCARDED) {
                CreatureLocator.forget(this);
            } else if (reason != RemovalReason.KILLED) {
                CreatureLocator.update(this);
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && duel != null) {
            refereeDuel(source);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isUnconscious() && !isTame()) {
            tamingSession.recordDamage(amount / getMaxHealth());
        }
        if (hurt && !level().isClientSide && !isTame()) {
            addStress(Stress.Event.HURT);
            stressHerd(Stress.Event.HERD_HURT);
            if (isAlive() && !isUnconscious() && source.getEntity() instanceof LivingEntity attacker
                    && !(attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                setTarget(attacker);
            }
            // Briga de rivais não é até a morte: quem fica fraco desiste e vai embora.
            if (source.getEntity() instanceof PrehistoricCreature rival && isRival(rival)
                    && getHealth() < getMaxHealth() * RIVAL_YIELD_HEALTH) {
                yieldTo(rival);
                if (rival.getTarget() == this) {
                    rival.setTarget(null);
                }
            }
        }
        if (hurt && !level().isClientSide && isBaby() && !isTame() && source.getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            callParents(attacker);
        }
        return hurt;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isApex() && !isTame() && !isUnconscious() && duel == null && isTrophyOf(player.getItemInHand(hand))) {
            if (!level().isClientSide) {
                startDuel(player, hand, player.getItemInHand(hand));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isCorpse()) {
            // O corpo só se abre: o inventário e o implante.
            if (!level().isClientSide && canAccessInventory(player)) {
                openInventory(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.STIMULANT.get()) && torpor > 0) {
            if (!level().isClientSide) {
                usePlayerItem(player, hand, held);
                setTorpor(torpor - ServerConfig.STIMULANT_TORPOR.get());
                playSound(SoundEvents.GENERIC_DRINK, 0.8F, 1.2F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isUnconscious() && !isTame()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.NARCOTIC.get())) {
                if (!level().isClientSide) {
                    usePlayerItem(player, hand, stack);
                    addTorpor(ServerConfig.NARCOTIC_TORPOR.get(), player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            // A comida vai no inventário; a criatura come sozinha no ritmo dela.
            if (!level().isClientSide) {
                openInventory(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isUnconscious() && isOwner(player)) {
            if (player.isSecondaryUseActive()) {
                if (!level().isClientSide) {
                    openInventory(player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(SADDLE_ITEM) && canBeSaddled() && !isSaddled()) {
                if (!level().isClientSide) {
                    usePlayerItem(player, hand, stack);
                    setSaddled(true);
                    playSound(SoundEvents.HORSE_SADDLE, 0.5F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            Optional<TamingProfile.Food> food = tamingProfile().flatMap(p -> p.foodFor(stack));
            // Alimentar tem preferência; passada a fome, a mesma mão cheia de carne monta. Ferida, sempre
            // aceita comida: é como se cura.
            if (food.isPresent() && (level().getGameTime() >= nextFeedTime || getHealth() < getMaxHealth())) {
                if (!level().isClientSide) {
                    feedTamed(player, hand, stack, food.get());
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (isMountable()) {
                if (ride(player)) {
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
                if (!level().isClientSide) {
                    player.displayClientMessage(mountRefusal(), true);
                }
                return InteractionResult.sidedSuccess(false);
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Por que a criatura não deixou montar, para dizer a quem tentou. */
    private Component mountRefusal() {
        if (!isRideReady()) {
            return Component.translatable("iceagesurvival.mount.needs_saddle", getName());
        }
        float required = mountProfile().map(MountProfile::minAffinity).orElse(0.0F);
        if (affinity < required) {
            return Component.translatable("iceagesurvival.mount.needs_affinity", getName(), (int) required);
        }
        return Component.translatable("iceagesurvival.mount.occupied", getName());
    }

    /** Alimentar uma criatura domesticada cura e aumenta a afinidade; é opcional, não manutenção. */
    private void feedTamed(Player player, InteractionHand hand, ItemStack stack, TamingProfile.Food food) {
        long now = level().getGameTime();
        boolean hurt = getHealth() < getMaxHealth();
        if (!hurt && now < nextFeedTime) {
            player.displayClientMessage(Component.translatable("iceagesurvival.taming.not_hungry"), true);
            return;
        }
        if (hurt && now < nextHealTime) {
            return;
        }
        usePlayerItem(player, hand, stack);
        heal(healAmount(food));
        nextHealTime = now + HEAL_FEED_COOLDOWN_TICKS;
        // A afinidade sobe só no ritmo normal das refeições: curar não vira atalho para ela.
        if (now >= nextFeedTime) {
            setAffinity(affinity + (float) (AFFINITY_PER_FEED * food.quality()));
            nextFeedTime = now + TAMED_FEED_INTERVAL_TICKS;
        }
        playSound(SoundEvents.GENERIC_EAT, 0.8F, 0.8F + getRandom().nextFloat() * 0.4F);
        level().broadcastEntityEvent(this, (byte) 7);
    }

    /** Quanto uma porção cura: fração da vida máxima proporcional ao valor do alimento. */
    public float healAmount(TamingProfile.Food food) {
        double fraction = Mth.clamp(food.value() * HEAL_FRACTION_PER_FOOD_VALUE, MIN_HEAL_FRACTION, MAX_HEAL_FRACTION);
        return (float) (getMaxHealth() * fraction);
    }

    /** Domesticada e ferida: come uma porção do próprio inventário, a melhor que tiver. */
    private void eatToHeal() {
        if (getHealth() >= getMaxHealth()) {
            return;
        }
        Optional<TamingProfile> profile = tamingProfile();
        if (profile.isEmpty()) {
            return;
        }
        int bestSlot = -1;
        TamingProfile.Food best = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            Optional<TamingProfile.Food> food = profile.get().foodFor(inventory.getItem(slot));
            if (food.isPresent() && (best == null || food.get().value() > best.value())) {
                bestSlot = slot;
                best = food.get();
            }
        }
        if (best == null) {
            return;
        }
        inventory.removeItem(bestSlot, 1);
        heal(healAmount(best));
        playSound(SoundEvents.GENERIC_EAT, 0.6F, 0.8F + getRandom().nextFloat() * 0.4F);
    }

    /** Inconsciente, come uma unidade do melhor alimento que houver no inventário, respeitando o intervalo. */
    private void eatFromInventory() {
        Optional<TamingProfile> profile = tamingProfile();
        entityData.set(DATA_NEEDS_TAMING_FOOD, profile.isPresent() && tamerUUID != null && !hasTamingFood(profile.get()));
        long now = level().getGameTime();
        if (profile.isEmpty() || tamerUUID == null || now < nextFeedTime) {
            return;
        }
        int bestSlot = -1;
        TamingProfile.Food best = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            Optional<TamingProfile.Food> food = profile.get().foodFor(inventory.getItem(slot));
            if (food.isPresent() && (best == null || food.get().quality() > best.quality())) {
                bestSlot = slot;
                best = food.get();
            }
        }
        if (best == null) {
            return;
        }
        inventory.removeItem(bestSlot, 1);
        tamingSession.feed(best.value(), best.quality());
        nextFeedTime = now + profile.get().feedIntervalSeconds() * 20L;

        double required = requiredFood(profile.get());
        if (tamingSession.isComplete(required)) {
            completeTaming(tamerUUID);
        } else {
            entityData.set(DATA_TAMING_PROGRESS, (float) tamingSession.progress(required));
        }
    }

    /**
     * Domestica na hora, com a eficiência e os níveis bônus de uma domesticação perfeita e
     * afinidade cheia. Só para os comandos de teste — o caminho de jogo é torpor e alimento.
     */
    public void debugTame(Player owner) {
        if (level().isClientSide) {
            return;
        }
        tamingSession = new TamingSession();
        tamingProfile().ifPresent(profile -> tamingSession.feed(requiredFood(profile), 1.0));
        completeTaming(owner.getUUID());
        setAffinity(MAX_AFFINITY);
    }

    /**
     * Refaz os pontos de atributo da criatura num nível dado, como se ela tivesse nascido
     * nele. Usado pelos comandos de teste. (Não é {@code setLevel}, que no {@code Entity}
     * do vanilla troca o mundo da entidade.)
     */
    public void setCreatureLevel(int newLevel) {
        if (level().isClientSide) {
            return;
        }
        species().ifPresent(species -> setStatPoints(
                StatPoints.rollWild(newLevel, randomGenerator(), species.stats().scalableStats()), species));
        setHealth(getMaxHealth());
        setTorpor(Math.min(torpor, maxTorpor()));
    }

    private void completeTaming(UUID owner) {
        groupId = null;
        double effectiveness = tamingSession.effectiveness();
        int bonus = TamingRules.bonusPoints(
                statPoints.level(), ServerConfig.TAMING_BONUS_LEVEL_FRACTION.get(), effectiveness);
        species().ifPresent(species -> setStatPoints(
                statPoints.addRandom(bonus, randomGenerator(), species.stats().scalableStats()), species));
        affinity = (float) (effectiveness * MAX_INITIAL_AFFINITY);

        setMovement(DEFAULT_MOVEMENT);
        setStance(DEFAULT_STANCE);
        Player player = level().getPlayerByUUID(owner);
        if (player != null) {
            tame(player);
        } else {
            // O dono pode ter saído do servidor enquanto a criatura comia.
            setTame(true);
            setOwnerUUID(owner);
        }
        setTorpor(0);
        level().broadcastEntityEvent(this, (byte) 7); // corações de domesticação do TamableAnimal
        CreatureLocator.update(this);
    }

    // ---- Persistência ----

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (statsRolled) {
            CompoundTag points = new CompoundTag();
            for (Stat stat : Stat.values()) {
                points.putInt(stat.id(), statPoints.get(stat));
            }
            compound.put(TAG_STAT_POINTS, points);
        }
        compound.putDouble(TAG_TORPOR, torpor);
        compound.putDouble(TAG_STRESS, stress);
        if (lastMealTime != Long.MIN_VALUE) {
            compound.putLong(TAG_TICKS_SINCE_MEAL, ticksSinceMeal());
        }
        compound.putBoolean(TAG_UNCONSCIOUS, isUnconscious());
        CompoundTag taming = new CompoundTag();
        taming.putDouble(TAG_TAMING_FOOD, tamingSession.foodValue());
        taming.putDouble(TAG_TAMING_QUALITY, tamingSession.qualityWeighted());
        taming.putDouble(TAG_TAMING_DAMAGE, tamingSession.damageFraction());
        compound.put(TAG_TAMING, taming);
        compound.putLong(TAG_NEXT_FEED_TIME, nextFeedTime);
        compound.putFloat(TAG_AFFINITY, affinity);
        compound.putString(TAG_MOVEMENT, movement().id());
        compound.putString(TAG_STANCE, stance().id());
        if (homePos != null) {
            compound.put(TAG_HOME, NbtUtils.writeBlockPos(homePos));
        }
        if (groupId != null) {
            compound.putUUID(TAG_GROUP, groupId);
        }
        compound.put(TAG_INVENTORY, inventory.createTag());
        if (tamerUUID != null) {
            compound.putUUID(TAG_TAMER, tamerUUID);
        }
        compound.putBoolean(TAG_SADDLED, isSaddled());
        CompoundTag mutationTag = new CompoundTag();
        for (Stat stat : Stat.values()) {
            if (mutations[stat.ordinal()] > 0) {
                mutationTag.putInt(stat.id(), mutations[stat.ordinal()]);
            }
        }
        compound.put(TAG_MUTATIONS, mutationTag);
        compound.putBoolean(TAG_HEALTH_GENE, healthGene);
        compound.putBoolean(TAG_FEMALE, isFemale());
        compound.putFloat(TAG_FLIGHT_STAMINA, flightStaminaFraction());
        if (isCorpse()) {
            compound.putBoolean(TAG_CORPSE, true);
            compound.putInt(TAG_CORPSE_TICKS, corpseTicks);
        }
        compound.putBoolean(TAG_MATING, isMatingEnabled());
        compound.putLong(TAG_NEXT_MATING, nextMatingTime);
        compound.putBoolean(TAG_ZONE_CHECKED, zoneChecked);
        compound.putBoolean(TAG_SPACING_CHECKED, spacingChecked);
        if (territoryOverride > 0) {
            compound.putInt(TAG_TERRITORY, territoryOverride);
        }
        if (gestationChild != null) {
            compound.putLong(TAG_GESTATION_END, gestationEnd);
            compound.put(TAG_GESTATION_CHILD, GenomeNbt.write(gestationChild));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        Optional<Species> species = species();
        // Antes dos pontos: a velocidade depende das mutações.
        CompoundTag mutationTag = compound.getCompound(TAG_MUTATIONS);
        for (Stat stat : Stat.values()) {
            mutations[stat.ordinal()] = Math.max(0, mutationTag.getInt(stat.id()));
        }
        healthGene = compound.getBoolean(TAG_HEALTH_GENE);
        setStress(compound.getDouble(TAG_STRESS));
        if (compound.contains(TAG_TICKS_SINCE_MEAL)) {
            lastMealTime = level().getGameTime() - compound.getLong(TAG_TICKS_SINCE_MEAL);
        }
        // Criaturas de antes da reprodução não tinham sexo: sorteia uma vez.
        setFemale(compound.contains(TAG_FEMALE) ? compound.getBoolean(TAG_FEMALE) : random.nextBoolean());
        setFlightStaminaFraction(compound.contains(TAG_FLIGHT_STAMINA) ? compound.getFloat(TAG_FLIGHT_STAMINA) : 1.0F);
        entityData.set(DATA_CORPSE, compound.getBoolean(TAG_CORPSE));
        corpseTicks = compound.getInt(TAG_CORPSE_TICKS);
        entityData.set(DATA_MATING, compound.getBoolean(TAG_MATING));
        nextMatingTime = compound.getLong(TAG_NEXT_MATING);
        zoneChecked = compound.getBoolean(TAG_ZONE_CHECKED);
        spacingChecked = compound.getBoolean(TAG_SPACING_CHECKED);
        territoryOverride = compound.getInt(TAG_TERRITORY);
        if (compound.contains(TAG_GESTATION_CHILD, Tag.TAG_COMPOUND)) {
            gestationEnd = compound.getLong(TAG_GESTATION_END);
            gestationChild = GenomeNbt.read(compound.getCompound(TAG_GESTATION_CHILD));
        }
        if (compound.contains(TAG_STAT_POINTS, Tag.TAG_COMPOUND)) {
            CompoundTag saved = compound.getCompound(TAG_STAT_POINTS);
            StatPoints points = StatPoints.NONE;
            for (Stat stat : Stat.values()) {
                points = points.with(stat, Math.max(0, saved.getInt(stat.id())));
            }
            if (species.isPresent()) {
                // A vida atual já foi lida pelo super; reaplicar os atributos não a altera.
                float health = getHealth();
                setStatPoints(points, species.get());
                setHealth(health);
            } else {
                statPoints = points;
                statsRolled = true;
            }
        }

        CompoundTag taming = compound.getCompound(TAG_TAMING);
        tamingSession = new TamingSession(
                taming.getDouble(TAG_TAMING_FOOD), taming.getDouble(TAG_TAMING_QUALITY), taming.getDouble(TAG_TAMING_DAMAGE));
        nextFeedTime = compound.getLong(TAG_NEXT_FEED_TIME);
        affinity = Mth.clamp(compound.getFloat(TAG_AFFINITY), 0.0F, MAX_AFFINITY);
        groupId = compound.hasUUID(TAG_GROUP) ? compound.getUUID(TAG_GROUP) : null;
        homePos = compound.contains(TAG_HOME, Tag.TAG_COMPOUND)
                ? NbtUtils.readBlockPos(compound.getCompound(TAG_HOME))
                : null;
        inventory.fromTag(compound.getList(TAG_INVENTORY, Tag.TAG_COMPOUND));
        tamerUUID = compound.hasUUID(TAG_TAMER) ? compound.getUUID(TAG_TAMER) : null;
        entityData.set(DATA_SADDLED, compound.getBoolean(TAG_SADDLED));
        Movement movement = Movement.byId(compound.getString(TAG_MOVEMENT), DEFAULT_MOVEMENT);
        Stance stance = Stance.byId(compound.getString(TAG_STANCE), DEFAULT_STANCE);
        if (!compound.contains(TAG_STANCE) && compound.contains(TAG_LEGACY_ORDER)) {
            switch (compound.getString(TAG_LEGACY_ORDER)) {
                case "stay" -> {
                    movement = Movement.STAY;
                    stance = Stance.PASSIVE;
                }
                case "follow" -> stance = Stance.NEUTRAL;
                case "flee" -> stance = Stance.FLEE;
                default -> { }
            }
        }
        entityData.set(DATA_MOVEMENT, (byte) movement.ordinal());
        entityData.set(DATA_STANCE, (byte) stance.ordinal());

        double max = maxTorpor();
        torpor = Mth.clamp(compound.getDouble(TAG_TORPOR), 0.0, max);
        entityData.set(DATA_TORPOR_FRACTION, max > 0 ? (float) (torpor / max) : 0.0F);
        entityData.set(DATA_UNCONSCIOUS, compound.getBoolean(TAG_UNCONSCIOUS) && torpor > 0);
        entityData.set(DATA_TAMING_PROGRESS, species.flatMap(Species::taming)
                .map(profile -> (float) tamingSession.progress(requiredFood(profile)))
                .orElse(0.0F));
    }

    // A reprodução vanilla (alimentar dois adultos) não se aplica; o mod tem sistema próprio.
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }
}
