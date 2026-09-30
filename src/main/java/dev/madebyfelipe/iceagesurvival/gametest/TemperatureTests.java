package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.registry.ModAttachments;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.temperature.ColdExposure;
import dev.madebyfelipe.iceagesurvival.temperature.ColdState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
    public static void insulatingArmorIsLeatherOnly(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.LEATHER_CHESTPLATE).is(ModTags.INSULATING_ARMOR),
                "peitoral de couro fora da tag");
        helper.assertTrue(new ItemStack(Items.LEATHER_BOOTS).is(ModTags.INSULATING_ARMOR),
                "botas de couro fora da tag");
        helper.assertTrue(!new ItemStack(Items.IRON_CHESTPLATE).is(ModTags.INSULATING_ARMOR),
                "peitoral de ferro dentro da tag");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void coldStateStartsAtZeroAndStaysInRange(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ColdState state = player.getData(ModAttachments.COLD);
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
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getData(ModAttachments.COLD).setExposure(0.75);

        Player reloaded = helper.makeMockPlayer(GameType.SURVIVAL);
        reloaded.load(player.saveWithoutId(new CompoundTag()));

        double exposure = reloaded.getData(ModAttachments.COLD).exposure();
        helper.assertTrue(exposure == 0.75, "frio acumulado não sobreviveu ao salvamento: " + exposure);
        helper.succeed();
    }

    /** O jogador mock do gametest é criativo, e no criativo o frio não conta. */
    @GameTest(template = EMPTY)
    public static void creativePlayersAreLeftAlone(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getData(ModAttachments.COLD).setExposure(0.8);

        ColdExposure.onPlayerTick(new PlayerTickEvent.Post(player));

        helper.assertTrue(player.getData(ModAttachments.COLD).exposure() == 0.0,
                "frio acumulado sobreviveu ao modo criativo");
        helper.assertTrue(player.getTicksFrozen() == 0, "jogador criativo ficou congelado");
        helper.succeed();
    }
}
