package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.item.SignalReceiverItem;
import dev.madebyfelipe.iceagesurvival.network.BaseSignalPayload;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** O Receptor de Sinal: onde se acha (o saque dos postos e o baú de comando do complexo), a mão e a mensagem do alvo. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class SignalReceiverTests {
    private static final String EMPTY = "empty";

    private static int receivers(GameTestHelper helper, ResourceLocation id, int rolls) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().getLootData().getLootTable(id);
        helper.assertTrue(table != LootTable.EMPTY, "falta a tabela de saque " + id);
        int found = 0;
        for (int roll = 0; roll < rolls; roll++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
                    Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO))).create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.is(ModItems.SIGNAL_RECEIVER.get())) {
                    found++;
                }
            }
        }
        return found;
    }

    @GameTest(template = EMPTY)
    public static void outpostChestsSometimesHaveAReceiver(GameTestHelper helper) {
        int found = receivers(helper, IceAgeSurvival.id("chests/military_outpost"), 400);
        // 30% de 400: longe de zero e de tudo.
        helper.assertTrue(found > 70 && found < 170, "receptores em 400 baús: " + found + " (esperado ~120)");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theComplexCommandChestAlwaysHasAReceiver(GameTestHelper helper) {
        int found = receivers(helper, IceAgeSurvival.id("chests/military_outpost_command"), 50);
        helper.assertTrue(found == 50, "o baú de comando deu receptor em " + found + " de 50");
        StructureTemplate template = helper.getLevel().getStructureManager()
                .get(IceAgeSurvival.id("military_outpost_complex")).orElse(null);
        helper.assertTrue(template != null, "falta o template do complexo");
        List<StructureTemplate.StructureBlockInfo> command = template.filterBlocks(BlockPos.ZERO,
                new StructurePlaceSettings(), Blocks.CHEST).stream()
                .filter(chest -> chest.nbt() != null
                        && "iceagesurvival:chests/military_outpost_command".equals(chest.nbt().getString("LootTable")))
                .toList();
        helper.assertTrue(command.size() == 1, "o complexo devia ter 1 baú de comando, tem " + command.size());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void receiverWorksInEitherHand(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        helper.assertFalse(SignalReceiverItem.isHolding(player), "de mãos vazias não está segurando o receptor");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SIGNAL_RECEIVER.get()));
        helper.assertTrue(SignalReceiverItem.isHolding(player), "na mão esquerda o receptor devia contar");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getTag(SignalReceiverItem.BASE).map(tag -> tag.size() == 1).orElse(false),
                "a tag de estrutura da base devia ter só a base");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void baseSignalPayloadSurvivesEncodeAndDecode(GameTestHelper helper) {
        for (Optional<GlobalPos> target : List.of(Optional.<GlobalPos>empty(),
                Optional.of(GlobalPos.of(Level.OVERWORLD, new BlockPos(1200, 70, -830))))) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                BaseSignalPayload.encode(new BaseSignalPayload(target), buf);
                BaseSignalPayload decoded = BaseSignalPayload.decode(buf);
                helper.assertTrue(decoded.base().equals(target), "voltou " + decoded.base() + ", foi " + target);
            } finally {
                buf.release();
            }
        }
        helper.succeed();
    }
}
