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
    public static void furInsulatesMoreThanLeatherAndIronNotAtAll(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
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
