package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.registry.ModAttachments;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.temperature.EnvironmentColdSource;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.temperature.ColdExposure;
import dev.madebyfelipe.iceagesurvival.temperature.ColdState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.level.block.Blocks;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * O que dá para conferir sem um bioma frio: as tags, o anexo no jogador e as regras do condutor.
 * A curva de frio em si é testada em JUnit ({@code ColdnessTest}); sentir o frio num mundo de
 * verdade continua sendo teste manual.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class TemperatureTests {
    private static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public static void heatSourceTagCoversFiresAndLava(GameTestHelper helper) {
        helper.assertTrue(Blocks.CAMPFIRE.defaultBlockState().is(ModTags.HEAT_SOURCES), "fogueira fora da tag");
        helper.assertTrue(Blocks.LAVA.defaultBlockState().is(ModTags.HEAT_SOURCES), "lava fora da tag");
        helper.assertTrue(Blocks.TORCH.defaultBlockState().is(ModTags.HEAT_SOURCES), "tocha fora da tag");
        helper.assertTrue(!Blocks.STONE.defaultBlockState().is(ModTags.HEAT_SOURCES), "pedra dentro da tag");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void furInsulatesMoreThanLeatherAndIronNotAtAll(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        helper.assertTrue(EnvironmentColdSource.insulation(player) == 0.0, "nu e isolado");
        dress(player, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
        double leather = EnvironmentColdSource.insulation(player);
        dress(player, ModItems.FUR_HELMET.get(), ModItems.FUR_CHESTPLATE.get(), ModItems.FUR_LEGGINGS.get(),
                ModItems.FUR_BOOTS.get());
        double fur = EnvironmentColdSource.insulation(player);
        dress(player, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        double iron = EnvironmentColdSource.insulation(player);
        helper.assertTrue(Math.abs(leather - 0.8) < 1e-6, "couro completo isola " + leather);
        helper.assertTrue(fur > leather, "pele não isola mais que couro: " + fur);
        helper.assertTrue(iron == 0.0, "ferro isola " + iron);
        helper.succeed();
    }

    private static void dress(Player player, net.minecraft.world.item.Item... pieces) {
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            player.setItemSlot(slots[i], new ItemStack(pieces[i]));
        }
    }

    @GameTest(template = EMPTY)
    public static void iceAgeAnimalsDropPelts(GameTestHelper helper) {
        var mammoth = helper.spawnWithNoFreeWill(dev.madebyfelipe.iceagesurvival.registry.ModEntities.MAMMOTH.get(), 1, 2, 1);
        mammoth.kill();
        helper.runAfterDelay(2, () -> {
            helper.assertItemEntityPresent(ModItems.PELT.get(), new net.minecraft.core.BlockPos(1, 2, 1), 3.0);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void coldStateStartsAtZeroAndStaysInRange(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        ColdState state = ModAttachments.coldState(player);
        helper.assertTrue(state.exposure() == 0.0 && !state.isFrozen(), "jogador nasce com frio acumulado");

        state.setExposure(5.0);
        helper.assertTrue(state.exposure() == 1.0 && state.isFrozen(), "frio passou de 1");
        state.setExposure(-5.0);
        helper.assertTrue(state.exposure() == 0.0 && !state.isFrozen(), "frio passou de 0");
        helper.succeed();
    }

    /** O frio acumulado é salvo com o jogador: quem sai congelando volta congelando. */
    @GameTest(template = EMPTY)
    public static void coldStateSurvivesSaveAndLoad(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        ModAttachments.coldState(player).setExposure(0.75);

        Player reloaded = helper.makeMockSurvivalPlayer();
        reloaded.load(player.saveWithoutId(new CompoundTag()));

        double exposure = ModAttachments.coldState(reloaded).exposure();
        helper.assertTrue(exposure == 0.75, "frio acumulado não sobreviveu ao salvamento: " + exposure);
        helper.succeed();
    }

    /** O jogador mock do gametest é criativo, e no criativo o frio não conta. */
    @GameTest(template = EMPTY)
    public static void creativePlayersAreLeftAlone(GameTestHelper helper) {
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(java.util.UUID.randomUUID(), "test-creative"));
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(GameType.CREATIVE);
        ModAttachments.coldState(player).setExposure(0.8);

        ColdExposure.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

        helper.assertTrue(ModAttachments.coldState(player).exposure() == 0.0,
                "frio acumulado sobreviveu ao modo criativo");
        helper.assertTrue(player.getTicksFrozen() == 0, "jogador criativo ficou congelado");
        helper.succeed();
    }

    /**
     * Ao renascer, o corpo antigo perde as capabilities e ainda recebe um tick; isso derrubava o
     * servidor ("Player cold-state capability is not attached").
     */
    @GameTest(template = EMPTY)
    public static void removedPlayerTickDoesNotCrash(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(java.util.UUID.randomUUID(), "test-respawn"));
        player.invalidateCaps();
        helper.assertTrue(ModAttachments.findColdState(player).isEmpty(), "capability seguiu válida");

        ColdExposure.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        helper.succeed();
    }

    /** Voltar do End recria o jogador sem morte: o frio acumulado vai junto. */
    @GameTest(template = EMPTY)
    public static void coldStateFollowsPortalClone(GameTestHelper helper) {
        ServerPlayer original = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(java.util.UUID.randomUUID(), "test-portal"));
        ModAttachments.coldState(original).setExposure(0.6);
        original.invalidateCaps();
        ServerPlayer clone = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                original.getGameProfile());

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone, original, false));

        double exposure = ModAttachments.coldState(clone).exposure();
        helper.assertTrue(exposure == 0.6, "frio não acompanhou o jogador pelo portal: " + exposure);
        helper.succeed();
    }

    /** Mundo normal (o do GameTest não é o preset): sem frio, nem para quem está em sobrevivência. */
    @GameTest(template = EMPTY)
    public static void noColdOutsideTheIceAgeMode(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        // O valor fica gravado no arquivo do mundo: um teste interrompido no meio deixava ON para o próximo.
        var config = dev.madebyfelipe.iceagesurvival.config.ServerConfig.ICE_AGE_MODE;
        config.set(dev.madebyfelipe.iceagesurvival.world.IceAgeMode.Setting.AUTO);
        try {
            helper.assertTrue(!dev.madebyfelipe.iceagesurvival.world.IceAgeMode.isIceAgePreset(server),
                    "o mundo do GameTest não é Era do Gelo");
            helper.assertTrue(!dev.madebyfelipe.iceagesurvival.world.IceAgeMode.isActive(server), "frio ligado em mundo normal");
            ServerPlayer player = PredatorTests.survivalPlayer(helper);
            ModAttachments.coldState(player).setExposure(0.8);
            ColdExposure.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            helper.assertTrue(ModAttachments.coldState(player).exposure() == 0.0, "frio acumulado em mundo normal");

            // Forçado na config, vale em qualquer mundo.
            config.set(dev.madebyfelipe.iceagesurvival.world.IceAgeMode.Setting.ON);
            helper.assertTrue(dev.madebyfelipe.iceagesurvival.world.IceAgeMode.isActive(server), "iceAgeMode=ON não ligou o frio");
        } finally {
            config.set(dev.madebyfelipe.iceagesurvival.world.IceAgeMode.Setting.AUTO);
        }
        helper.succeed();
    }

    /** O preset Era do Gelo é reconhecido pela fonte de biomas própria. */
    @GameTest(template = EMPTY)
    public static void iceAgePresetIsRecognisedByItsBiomeSource(GameTestHelper helper) {
        var normal = helper.getLevel().getChunkSource().getGenerator().getBiomeSource();
        helper.assertTrue(!dev.madebyfelipe.iceagesurvival.world.IceAgeMode.isIceAgeSource(normal), "fonte normal");
        var iceAge = new dev.madebyfelipe.iceagesurvival.world.RemappedBiomeSource(normal, java.util.Map.of());
        helper.assertTrue(dev.madebyfelipe.iceagesurvival.world.IceAgeMode.isIceAgeSource(iceAge), "fonte do preset");
        helper.succeed();
    }
}
