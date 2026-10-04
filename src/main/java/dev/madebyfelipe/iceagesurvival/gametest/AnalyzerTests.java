package dev.madebyfelipe.iceagesurvival.gametest;

import com.mojang.authlib.GameProfile;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.item.AnalyzerItem;
import dev.madebyfelipe.iceagesurvival.network.DinoFilePayload;
import dev.madebyfelipe.iceagesurvival.network.ScanResultPayload;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.world.DinoFileData;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O Analisador e a DINO FILE: a mira, o registro por jogador, o analisador do primeiro login, o clique que não abre
 * nada da criatura, a leitura do indivíduo e os pacotes. Escritos a partir da especificação, com jogadores de servidor
 * de verdade (entram pela lista de jogadores, como num login) que saem da lista no fim de cada teste.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class AnalyzerTests {
    private static final String EMPTY = "empty";
    private static final String ARENA = "arena";
    private static final ResourceLocation DODO = IceAgeSurvival.id("dodo");
    private static final ResourceLocation SMILODON = IceAgeSurvival.id("smilodon");
    private static final ResourceLocation MAMMOTH = IceAgeSurvival.id("mammoth");

    // ---- Item ----

    @GameTest(template = EMPTY)
    public static void analyzerIsOnePerSlotAndScansInThirtyTicks(GameTestHelper helper) {
        ItemStack analyzer = new ItemStack(ModItems.ANALYZER.get());
        helper.assertTrue(analyzer.getMaxStackSize() == 1, "o analisador devia ser 1 por espaço, é "
                + analyzer.getMaxStackSize());
        helper.assertTrue(AnalyzerItem.SCAN_TICKS == 30, "o scan devia levar 30 ticks, leva " + AnalyzerItem.SCAN_TICKS);
        helper.assertTrue(analyzer.getUseDuration() == AnalyzerItem.SCAN_TICKS,
                "segurar o uso devia durar o tempo do scan, dura " + analyzer.getUseDuration());
        helper.assertTrue(AnalyzerItem.RANGE == 24.0, "o alcance devia ser 24 blocos, é " + AnalyzerItem.RANGE);
        helper.succeed();
    }

    // ---- DINO FILE ----

    @GameTest(template = EMPTY)
    public static void dinoFileRegistersEachSpeciesOncePerPlayer(GameTestHelper helper) {
        ServerPlayer first = serverPlayer(helper, "analista-a");
        ServerPlayer second = serverPlayer(helper, "analista-b");
        try {
            helper.assertTrue(DinoFileData.registered(first).isEmpty(), "jogador novo já tem DINO FILE: "
                    + DinoFileData.registered(first));
            helper.assertTrue(DinoFileData.register(first, DODO), "o primeiro registro do dodô não contou como novo");
            helper.assertFalse(DinoFileData.register(first, DODO), "o dodô contou como novo duas vezes");
            helper.assertTrue(DinoFileData.register(first, SMILODON), "o smilodon não contou como novo");
            helper.assertFalse(DinoFileData.register(first, DODO), "o dodô voltou a contar como novo");
            helper.assertTrue(DinoFileData.registered(first).equals(List.of(DODO, SMILODON)),
                    "a DINO FILE devia ter dodô e smilodon, nessa ordem, uma vez cada: " + DinoFileData.registered(first));

            helper.assertTrue(DinoFileData.registered(second).isEmpty(),
                    "o registro de um jogador apareceu na DINO FILE de outro: " + DinoFileData.registered(second));
            helper.assertTrue(DinoFileData.register(second, SMILODON),
                    "o smilodon de outro jogador não contou como novo para este");
            helper.assertTrue(DinoFileData.register(second, MAMMOTH), "o mamute não contou como novo");
            helper.assertTrue(DinoFileData.registered(second).equals(List.of(SMILODON, MAMMOTH)),
                    "DINO FILE do segundo jogador: " + DinoFileData.registered(second));
            helper.assertTrue(DinoFileData.registered(first).equals(List.of(DODO, SMILODON)),
                    "o registro do segundo jogador mexeu na DINO FILE do primeiro: " + DinoFileData.registered(first));
        } finally {
            logOut(helper, first);
            logOut(helper, second);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void completingAScanRegistersTheSpecies(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        try {
            AnalyzerItem.complete(player, dodo);
            helper.assertTrue(DinoFileData.registered(player).equals(List.of(DODO)),
                    "escanear o dodô não o registrou: " + DinoFileData.registered(player));
            AnalyzerItem.complete(player, dodo);
            helper.assertTrue(DinoFileData.registered(player).equals(List.of(DODO)),
                    "escanear o dodô de novo duplicou o registro: " + DinoFileData.registered(player));
            AnalyzerItem.complete(player, smilodon);
            helper.assertTrue(DinoFileData.registered(player).equals(List.of(DODO, SMILODON)),
                    "o smilodon escaneado devia entrar depois do dodô: " + DinoFileData.registered(player));
        } finally {
            dodo.discard();
            smilodon.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    // ---- Analisador do primeiro login ----

    @GameTest(template = EMPTY)
    public static void firstLoginGivesOneAnalyzerOnlyOnce(GameTestHelper helper) {
        // Entrar pela lista de jogadores dispara o login de verdade: é o primeiro deste jogador.
        ServerPlayer player = serverPlayer(helper, "novato");
        try {
            helper.assertTrue(analyzersOf(player) == 1,
                    "o primeiro login devia dar exatamente 1 analisador, deu " + analyzersOf(player));
            CompoundTag kept = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            helper.assertTrue(kept.getBoolean(AnalyzerItem.GIVEN_TAG),
                    "a marca de analisador entregue devia ficar nos dados persistentes do jogador");

            AnalyzerItem.onLoggedIn(new PlayerEvent.PlayerLoggedInEvent(player));
            AnalyzerItem.onLoggedIn(new PlayerEvent.PlayerLoggedInEvent(player));
            helper.assertTrue(analyzersOf(player) == 1,
                    "os logins seguintes deram outro analisador: " + analyzersOf(player));

            player.getInventory().clearContent();
            AnalyzerItem.onLoggedIn(new PlayerEvent.PlayerLoggedInEvent(player));
            helper.assertTrue(analyzersOf(player) == 0,
                    "com o inventário vazio, um login seguinte deu outro analisador: " + analyzersOf(player));
        } finally {
            droppedAnalyzers(player).forEach(Entity::discard);
            logOut(helper, player);
        }
        helper.succeed();
    }

    // ---- Clique na criatura ----

    @GameTest(template = EMPTY)
    public static void clickingACreatureWithTheAnalyzerPassesToTheItem(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        LandCreature tamed = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        LandCreature knockedOut = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        try {
            tamed.tame(player);
            knockedOut.setTorpor(knockedOut.maxTorpor());
            helper.assertTrue(tamed.isOwner(player), "o smilodon não ficou do jogador");
            helper.assertTrue(knockedOut.isUnconscious(), "o dodô não desmaiou");
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANALYZER.get()));
            for (LandCreature creature : List.of(wild, tamed, knockedOut)) {
                String which = describe(creature);
                InteractionResult result = creature.mobInteract(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result == InteractionResult.PASS,
                        "com o analisador na mão, mobInteract devia passar o clique (" + which + "), deu " + result);
                result = creature.interact(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result == InteractionResult.PASS,
                        "com o analisador na mão, o clique devia seguir para o item (" + which + "), deu " + result);
            }
            helper.assertTrue(player.getMainHandItem().is(ModItems.ANALYZER.get())
                    && player.getMainHandItem().getCount() == 1, "o clique mexeu no analisador da mão");
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "o clique abriu uma tela da criatura: "
                    + player.containerMenu);
        } finally {
            wild.discard();
            tamed.discard();
            knockedOut.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    // ---- Leitura (ScanResultPayload.of) ----

    @GameTest(template = EMPTY)
    public static void scanOfAWildPredatorIsDangerous(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        LandCreature ornitholestes = helper.spawnWithNoFreeWill(ModEntities.ORNITHOLESTES.get(), 1, 2, 1);
        try {
            ScanResultPayload scan = ScanResultPayload.of(player, smilodon, true);
            helper.assertTrue(scan.state() == ScanResultPayload.State.WILD, "smilodon selvagem lido como " + scan.state());
            helper.assertTrue(scan.danger() == 2, "smilodon selvagem (agressivo e caçador) devia ter perigo 2, tem "
                    + scan.danger());
            helper.assertTrue(scan.species().equals(SMILODON), "espécie lida: " + scan.species());
            helper.assertTrue(scan.entityId() == smilodon.getId(), "a leitura aponta para outra entidade");
            helper.assertTrue(scan.newEntry(), "o registro novo não chegou na leitura");
            helper.assertTrue(scan.level() == smilodon.creatureLevel(), "nível lido " + scan.level() + ", real "
                    + smilodon.creatureLevel());
            helper.assertTrue(scan.female() == smilodon.isFemale(), "sexo lido errado");
            helper.assertTrue(scan.health() == smilodon.getHealth() && scan.maxHealth() == smilodon.getMaxHealth(),
                    "vida lida " + scan.health() + "/" + scan.maxHealth() + ", real " + smilodon.getHealth() + "/"
                            + smilodon.getMaxHealth());
            helper.assertTrue(scan.owner().isEmpty(), "selvagem com dono na leitura: " + scan.owner());
            helper.assertFalse(ScanResultPayload.of(player, smilodon, false).newEntry(),
                    "um scan repetido chegou como registro novo");

            ScanResultPayload hunter = ScanResultPayload.of(player, ornitholestes, false);
            helper.assertTrue(hunter.state() == ScanResultPayload.State.WILD,
                    "ornitholestes selvagem lido como " + hunter.state());
            helper.assertTrue(hunter.danger() == 2,
                    "ornitholestes caça (mesmo sem ser agressivo): devia ter perigo 2, tem " + hunter.danger());
        } finally {
            smilodon.discard();
            ornitholestes.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void scanOfACalmWildHerbivoreOnlyDefends(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        try {
            ScanResultPayload scan = ScanResultPayload.of(player, dodo, true);
            helper.assertTrue(scan.state() == ScanResultPayload.State.WILD, "dodô selvagem lido como " + scan.state());
            helper.assertTrue(scan.danger() == 1, "dodô selvagem (não caça nem é agressivo) devia ter perigo 1, tem "
                    + scan.danger());
            helper.assertTrue(scan.species().equals(DODO), "espécie lida: " + scan.species());
        } finally {
            dodo.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void scanOfYourTamedCreatureIsHarmlessAndYours(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        try {
            smilodon.tame(player);
            ScanResultPayload scan = ScanResultPayload.of(player, smilodon, false);
            helper.assertTrue(scan.state() == ScanResultPayload.State.YOURS,
                    "smilodon domesticado pelo jogador lido como " + scan.state());
            helper.assertTrue(scan.danger() == 0, "domesticado devia ter perigo 0 (mesmo sendo predador), tem "
                    + scan.danger());
            helper.assertTrue(scan.owner().equals(player.getName().getString()),
                    "dono lido: '" + scan.owner() + "'");
        } finally {
            smilodon.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void scanTellsOthersCreaturesAndUnconsciousOnes(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        ServerPlayer other = serverPlayer(helper, "outro-dono");
        LandCreature others = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        LandCreature knockedOut = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), 1, 2, 1);
        try {
            others.tame(other);
            ScanResultPayload scan = ScanResultPayload.of(player, others, false);
            helper.assertTrue(scan.state() == ScanResultPayload.State.OTHERS,
                    "dodô de outro jogador lido como " + scan.state());
            helper.assertTrue(scan.owner().equals("outro-dono"), "dono lido: '" + scan.owner() + "'");
            helper.assertTrue(scan.danger() == 0, "domesticado (de outro) devia ter perigo 0, tem " + scan.danger());

            knockedOut.setTorpor(knockedOut.maxTorpor());
            helper.assertTrue(knockedOut.isUnconscious(), "o dodô não desmaiou");
            ScanResultPayload unconscious = ScanResultPayload.of(player, knockedOut, false);
            helper.assertTrue(unconscious.state() == ScanResultPayload.State.UNCONSCIOUS,
                    "dodô selvagem desmaiado lido como " + unconscious.state());
            helper.assertTrue(unconscious.torpor() > 0.99F, "torpor lido do desmaiado: " + unconscious.torpor());
        } finally {
            others.discard();
            knockedOut.discard();
            logOut(helper, player);
            logOut(helper, other);
        }
        helper.succeed();
    }

    // ---- Pacotes ----

    @GameTest(template = EMPTY)
    public static void payloadsSurviveEncodeAndDecode(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 1, 2, 1);
        try {
            List<Object> messages = new ArrayList<>();
            messages.add(new DinoFilePayload(
                    List.of(DODO, SMILODON, ResourceLocation.fromNamespaceAndPath("minecraft", "wolf")),
                    java.util.Map.of("postos", 3, "base", 1)));
            messages.add(new DinoFilePayload(List.of(), java.util.Map.of()));
            messages.add(ScanResultPayload.of(player, smilodon, true));
            messages.add(new ScanResultPayload(1234, MAMMOTH, false, "Mamute-Lanoso", 60, true, 87.5F, 120.0F, 0.25F,
                    14.5F, 6.0F, ScanResultPayload.State.OTHERS, "outro-dono", "uneasy", 0));
            messages.add(new ScanResultPayload(7, DODO, true, "Dodô", 10, false, 0.0F, 10.0F, 1.0F, 0.0F, 0.0F,
                    ScanResultPayload.State.UNCONSCIOUS, "", "panicked", 1));

            for (Object message : messages) {
                Object back;
                if (message instanceof DinoFilePayload dinoFile) {
                    back = roundTrip(helper, dinoFile, DinoFilePayload::encode, DinoFilePayload::decode);
                } else {
                    back = roundTrip(helper, (ScanResultPayload) message, ScanResultPayload::encode,
                            ScanResultPayload::decode);
                }
                helper.assertTrue(message.equals(back), "ida e volta mudou o pacote: " + message + " → " + back);
            }
        } finally {
            smilodon.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    // ---- Mira ----

    @GameTest(template = ARENA)
    public static void aimFindsTheCreatureInFront(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), new Vec3(12.5, 0, 12.5));
        try {
            player.moveTo(helper.absoluteVec(new Vec3(3.5, 0, 12.5)));
            player.lookAt(EntityAnchorArgument.Anchor.EYES, dodo.getBoundingBox().getCenter());
            PrehistoricCreature aimed = AnalyzerItem.aimed(player);
            helper.assertTrue(aimed == dodo, "o dodô a 9 blocos, bem na mira, não foi achado: " + aimed);

            // Olhando para o céu: o raio não passa perto do dodô nem sai para a área de outro teste.
            player.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(3.5, 30, 13.5)));
            helper.assertTrue(AnalyzerItem.aimed(player) == null,
                    "olhando para cima, longe do dodô, achou " + AnalyzerItem.aimed(player));
        } finally {
            dodo.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aimDoesNotSeeThroughAWall(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        LandCreature dodo = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), new Vec3(12.5, 0, 12.5));
        List<BlockPos> wall = new ArrayList<>();
        for (int y = 0; y <= 3; y++) {
            for (int z = 10; z <= 15; z++) {
                wall.add(new BlockPos(8, y, z));
            }
        }
        try {
            player.moveTo(helper.absoluteVec(new Vec3(3.5, 0, 12.5)));
            player.lookAt(EntityAnchorArgument.Anchor.EYES, dodo.getBoundingBox().getCenter());
            wall.forEach(pos -> helper.setBlock(pos, Blocks.STONE));
            helper.assertTrue(AnalyzerItem.aimed(player) == null,
                    "achou o dodô através de uma parede de pedra: " + AnalyzerItem.aimed(player));

            wall.forEach(pos -> helper.setBlock(pos, Blocks.AIR));
            helper.assertTrue(AnalyzerItem.aimed(player) == dodo,
                    "sem a parede, o mesmo dodô na mira devia ser achado: " + AnalyzerItem.aimed(player));
        } finally {
            wall.forEach(pos -> helper.setBlock(pos, Blocks.AIR));
            dodo.discard();
            logOut(helper, player);
        }
        helper.succeed();
    }

    // Batch próprio: no lote padrão, um dodô de um teste vizinho às vezes ficava dentro do alcance medido.
    @GameTest(template = ARENA, batch = "analyzer_aim_range")
    public static void aimReachesOnly24Blocks(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper, "analista");
        player.moveTo(helper.absoluteVec(new Vec3(3.5, 0, 3.5)));
        // Na diagonal da arena: ~27 blocos (fora do alcance) e ~18 blocos (dentro).
        LandCreature far = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), new Vec3(22.5, 0, 22.5));
        LandCreature near = null;
        try {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, far.getBoundingBox().getCenter());
            double distance = player.getEyePosition().distanceTo(far.getBoundingBox().getCenter());
            helper.assertTrue(distance > AnalyzerItem.RANGE + 2, "montagem errada: dodô longe a " + distance);
            helper.assertTrue(AnalyzerItem.aimed(player) == null,
                    "achou um dodô a " + (int) distance + " blocos, além do alcance de 24");
            far.discard();

            near = helper.spawnWithNoFreeWill(ModEntities.DODO.get(), new Vec3(16.5, 0, 16.5));
            player.lookAt(EntityAnchorArgument.Anchor.EYES, near.getBoundingBox().getCenter());
            distance = player.getEyePosition().distanceTo(near.getBoundingBox().getCenter());
            helper.assertTrue(distance < AnalyzerItem.RANGE - 2, "montagem errada: dodô perto a " + distance);
            helper.assertTrue(AnalyzerItem.aimed(player) == near,
                    "não achou o dodô a " + (int) distance + " blocos, dentro do alcance: " + AnalyzerItem.aimed(player));
        } finally {
            far.discard();
            if (near != null) {
                near.discard();
            }
            logOut(helper, player);
        }
        helper.succeed();
    }

    // ---- Apoio ----

    /**
     * Um jogador de servidor em sobrevivência, com nome, que entra pela lista de jogadores (dispara o login de
     * verdade). Mesmo jeito de {@code PredatorTests.survivalPlayer}, mas com o nome dado.
     */
    private static ServerPlayer serverPlayer(GameTestHelper helper, String name) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name)) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void logOut(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    /** Analisadores com o jogador: no inventário e caídos perto dele (se o inventário estivesse cheio). */
    private static int analyzersOf(ServerPlayer player) {
        int count = player.getInventory().countItem(ModItems.ANALYZER.get());
        for (ItemEntity dropped : droppedAnalyzers(player)) {
            count += dropped.getItem().getCount();
        }
        return count;
    }

    private static List<ItemEntity> droppedAnalyzers(ServerPlayer player) {
        return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4.0),
                item -> item.isAlive() && item.getItem().is(ModItems.ANALYZER.get()));
    }

    private static String describe(PrehistoricCreature creature) {
        return creature.getType().getDescriptionId() + (creature.isTame() ? " domesticado"
                : creature.isUnconscious() ? " desmaiado" : " selvagem");
    }

    private static <T> T roundTrip(GameTestHelper helper, T message, BiConsumer<T, FriendlyByteBuf> encode,
            Function<FriendlyByteBuf, T> decode) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            encode.accept(message, buf);
            T back = decode.apply(buf);
            helper.assertTrue(buf.readableBytes() == 0, "sobraram " + buf.readableBytes() + " bytes depois de ler "
                    + message);
            return back;
        } finally {
            buf.release();
        }
    }
}
