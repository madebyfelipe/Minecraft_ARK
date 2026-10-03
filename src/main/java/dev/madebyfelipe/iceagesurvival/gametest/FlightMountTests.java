package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.menu.CreatureStorageMenu;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Montaria voadora em jogo: o voo montado não fere a montaria nem quem monta, e o Quetzalcoatlus desmaiado abre o
 * inventário pelas partes do corpo e come o peixe para ser domesticado, como o Pteranodonte.
 */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class FlightMountTests {
    private static final String ARENA = "arena";

    /**
     * O jogador recém-entrado é invulnerável por 60 ticks ({@code spawnInvulnerableTime}, que só o {@code tick} do
     * {@link ServerPlayer} desconta): sem gastar esse tempo, nenhum dano apareceria no teste.
     */
    private static void endSpawnProtection(ServerPlayer player) {
        for (int i = 0; i < 61; i++) {
            player.tick();
        }
    }

    /** Domesticada, selada, com afinidade cheia e montada pelo dono (um jogador de sobrevivência). */
    private static LandCreature ridden(GameTestHelper helper, EntityType<LandCreature> type, ServerPlayer owner,
                                       int x, int y, int z) {
        LandCreature mount = helper.spawnWithNoFreeWill(type, x, y, z);
        mount.tame(owner);
        mount.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        mount.setSaddled(true);
        endSpawnProtection(owner);
        owner.setPos(mount.position());
        helper.assertTrue(mount.ride(owner), "deveria montar");
        return mount;
    }

    /**
     * Saindo do voo no alto (pousou na copa de uma árvore e escorregou da borda, ou o fôlego acabou), a montaria plana
     * até o chão. O movimento dela vem do cliente de quem monta: no servidor a queda somava a altura inteira e, no chão,
     * o dano de queda ia para ela e para quem monta. Montaria voadora plana; não leva nem passa dano de queda.
     */
    @GameTest(template = ARENA, batch = "flight_mount_fall", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aRiddenFlyerComingDownTakesNoFallDamage(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        for (EntityType<LandCreature> type : java.util.List.of(ModEntities.QUETZALCOATLUS.get(),
                ModEntities.PTERANODON.get())) {
            ServerPlayer owner = PredatorTests.survivalPlayer(helper);
            LandCreature mount = ridden(helper, type, owner, type == ModEntities.PTERANODON.get() ? 4 : 14, 20, 12);
            float mountHealth = mount.getHealth();
            float riderHealth = owner.getHealth();
            // O que o servidor faz com o pacote de veículo do cliente (handleMoveVehicle): move a montaria, sem voo,
            // planando 0,2 bloco por vez até o chão.
            for (int step = 0; step < 200 && !mount.onGround(); step++) {
                mount.move(MoverType.PLAYER, new Vec3(0.0, -0.2, 0.0));
            }
            helper.assertTrue(mount.onGround(), type + " não chegou ao chão: " + mount.position());
            helper.assertTrue(mount.getHealth() == mountHealth, type + " levou dano de queda: " + mountHealth + " → "
                    + mount.getHealth());
            helper.assertTrue(owner.getHealth() == riderHealth, "quem monta o " + type + " levou dano de queda: "
                    + riderHealth + " → " + owner.getHealth());
            owner.stopRiding();
            owner.discard();
        }
        helper.succeed();
    }

    /**
     * Quem monta vai bem acima da caixa de colisão da montaria (o Quetzalcoatlus leva o assento a 6,3 de uma caixa de
     * 4,5): roçando a copa, a cabeça entra nos galhos. Montado numa montaria voadora, não sufoca.
     */
    @GameTest(template = ARENA, batch = "flight_mount_branches", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void theRiderOfAFlyerDoesNotSuffocateInTheBranches(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ServerPlayer owner = PredatorTests.survivalPlayer(helper);
        LandCreature quetzal = ridden(helper, ModEntities.QUETZALCOATLUS.get(), owner, 12, 1, 12);
        quetzal.positionRider(owner);
        // Um galho de tronco na altura da cabeça de quem monta, sem tocar a caixa da montaria.
        var head = net.minecraft.core.BlockPos.containing(owner.getEyePosition());
        helper.getLevel().setBlockAndUpdate(head, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
        helper.assertTrue(owner.isInWall(), "o galho deveria estar na cabeça de quem monta (a " + owner.getEyePosition()
                + ")");
        float riderHealth = owner.getHealth();
        float mountHealth = quetzal.getHealth();
        helper.onEachTick(() -> {
            quetzal.positionRider(owner);
            owner.doTick(); // o jogador falso não tem conexão que o faça viver o tick
        });
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(owner.getHealth() == riderHealth, "quem monta sufocou no galho: " + riderHealth + " → "
                    + owner.getHealth());
            helper.assertTrue(quetzal.getHealth() == mountHealth, "a montaria levou dano: " + quetzal.getHealth());
            helper.assertTrue(quetzal.hasPassenger(owner), "quem monta caiu");
            owner.stopRiding();
            owner.discard();
            helper.succeed();
        });
    }

    /**
     * O pedido do Felipe: Quetzalcoatlus selvagem derrubado pelo jogador, peixe cru no inventário — ele come, o peixe
     * some e a domesticação sobe.
     */
    @GameTest(template = ARENA, batch = "flight_mount_tame", timeoutTicks = 400,
            setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void aKnockedOutQuetzalcoatlusEatsRawFishToBeTamed(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ServerPlayer tamer = PredatorTests.survivalPlayer(helper);
        LandCreature quetzal = helper.spawn(ModEntities.QUETZALCOATLUS.get(), 12, 1, 12);
        quetzal.addTorpor(quetzal.maxTorpor(), tamer);
        helper.assertTrue(quetzal.isUnconscious(), "deveria desmaiar");
        quetzal.inventory().addItem(new ItemStack(Items.COD, 4));
        helper.succeedWhen(() -> {
            helper.assertTrue(quetzal.isUnconscious(), "acordou antes de comer");
            helper.assertTrue(quetzal.inventory().countItem(Items.COD) < 4, "não comeu o peixe (progresso "
                    + quetzal.tamingProgress() + ", falta comida " + quetzal.needsTamingFood() + ")");
            helper.assertTrue(quetzal.tamingProgress() > 0.0F, "comeu e a domesticação não subiu");
        });
    }

    /**
     * O clique em qualquer parte do corpo (More Hitboxes) chega ao Quetzalcoatlus: desmaiado, abre o inventário para
     * quem o derrubou.
     */
    @GameTest(template = ARENA, batch = "flight_mount_parts", setupTicks = HuntTests.CHUNK_SETUP_TICKS)
    public static void clickingAnyPartOpensTheKnockedOutQuetzalcoatlusInventory(GameTestHelper helper) {
        HuntTests.clearStrays(helper);
        ServerPlayer tamer = PredatorTests.survivalPlayer(helper);
        LandCreature quetzal = helper.spawn(ModEntities.QUETZALCOATLUS.get(), 12, 1, 12);
        quetzal.addTorpor(quetzal.maxTorpor(), tamer);
        // Mão vazia: o jogador novo entra com o Analisador, e o clique com ele escaneia em vez de abrir.
        tamer.getInventory().clearContent();
        helper.runAfterDelay(2, () -> {
            PartEntity<?>[] parts = quetzal.getParts();
            helper.assertTrue(parts != null && parts.length > 0, "o Quetzalcoatlus deveria ter partes");
            for (PartEntity<?> part : parts) {
                tamer.closeContainer();
                tamer.setPos(quetzal.position().add(3.0, 0.0, 0.0));
                var result = tamer.interactOn(part, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.consumesAction(), "o clique na parte " + part + " não pegou: " + result);
                helper.assertTrue(tamer.containerMenu instanceof CreatureStorageMenu,
                        "o clique na parte " + part + " não abriu o inventário");
            }
            tamer.closeContainer();
            tamer.discard();
            helper.succeed();
        });
    }
}
