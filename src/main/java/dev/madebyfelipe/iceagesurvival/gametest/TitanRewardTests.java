package dev.madebyfelipe.iceagesurvival.gametest;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.effect.BleedingEffect;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import dev.madebyfelipe.iceagesurvival.item.StasisProjectorItem;
import dev.madebyfelipe.iceagesurvival.item.TitanSerumItem;
import dev.madebyfelipe.iceagesurvival.registry.ModEntities;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** As recompensas do Titanovenator: o soro (mais ataque, mais vida e a mordida que sangra) e o projetor de êxtase. */
@GameTestHolder(IceAgeSurvival.MODID)
@PrefixGameTestTemplate(false)
public class TitanRewardTests {
    private static final String EMPTY = "empty";

    private static LandCreature tamedSmilodon(GameTestHelper helper, Player owner) {
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 2, 2, 2);
        smilodon.tame(owner);
        smilodon.setAffinity(PrehistoricCreature.MAX_AFFINITY);
        return smilodon;
    }

    private static ItemStack serumIn(Player player) {
        ItemStack stack = new ItemStack(ModItems.TITAN_SERUM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    /** Na criatura da própria pessoa: ataque e vida sobem um quarto, a vida enche e a dose sai da mão. */
    @GameTest(template = EMPTY, batch = "titan_reward")
    public static void theSerumRaisesAttackAndHealthOfYourCreature(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamedSmilodon(helper, owner);
        double attack = smilodon.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double health = smilodon.getAttributeValue(Attributes.MAX_HEALTH);
        smilodon.setHealth(1.0F);
        ItemStack stack = serumIn(owner);

        stack.interactLivingEntity(owner, smilodon, InteractionHand.MAIN_HAND);

        helper.assertTrue(TitanSerumItem.has(smilodon), "a criatura não ficou marcada com o soro");
        helper.assertTrue(Math.abs(smilodon.getAttributeValue(Attributes.ATTACK_DAMAGE)
                - attack * (1 + TitanSerumItem.ATTACK_BONUS)) < 0.01, "ataque: " + attack + " → "
                + smilodon.getAttributeValue(Attributes.ATTACK_DAMAGE));
        double expectedHealth = Math.min(1024.0, health * (1 + TitanSerumItem.HEALTH_BONUS));
        helper.assertTrue(Math.abs(smilodon.getMaxHealth() - expectedHealth) < 0.01,
                "vida máxima: " + health + " → " + smilodon.getMaxHealth());
        helper.assertTrue(smilodon.getHealth() == smilodon.getMaxHealth(), "a vida não encheu: " + smilodon.getHealth());
        helper.assertTrue(stack.getCount() == 1, "a dose não foi gasta: " + stack.getCount());
        helper.succeed();
    }

    /** Uma dose por criatura; criatura de outra pessoa (ou selvagem) recusa, sem gastar o soro. */
    @GameTest(template = EMPTY, batch = "titan_reward")
    public static void theSerumIsOncePerCreatureAndOnlyForYours(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        Player stranger = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamedSmilodon(helper, owner);
        ItemStack stack = serumIn(owner);
        stack.interactLivingEntity(owner, smilodon, InteractionHand.MAIN_HAND);
        double attack = smilodon.getAttributeValue(Attributes.ATTACK_DAMAGE);
        stack.interactLivingEntity(owner, smilodon, InteractionHand.MAIN_HAND);
        helper.assertTrue(stack.getCount() == 1, "a segunda dose foi gasta: " + stack.getCount());
        helper.assertTrue(smilodon.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack, "a segunda dose somou");

        ItemStack other = serumIn(stranger);
        LandCreature wild = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 5, 2, 5);
        other.interactLivingEntity(stranger, smilodon, InteractionHand.MAIN_HAND);
        other.interactLivingEntity(stranger, wild, InteractionHand.MAIN_HAND);
        helper.assertTrue(other.getCount() == 2, "o soro pegou em criatura que não é da pessoa");
        helper.assertFalse(TitanSerumItem.has(wild), "o soro pegou numa selvagem");
        helper.succeed();
    }

    /** Quem tomou o soro morde e faz sangrar; antes do soro, a mesma mordida não sangra. */
    @GameTest(template = EMPTY, batch = "titan_reward")
    public static void theSerumGivesTheBleedingBite(GameTestHelper helper) {
        Player owner = helper.makeMockSurvivalPlayer();
        LandCreature smilodon = tamedSmilodon(helper, owner);
        Pig before = helper.spawnWithNoFreeWill(EntityType.PIG, 4, 2, 2);
        smilodon.doHurtTarget(before);
        helper.assertFalse(BleedingEffect.isBleeding(before), "sem o soro, o dente-de-sabre já fazia sangrar");

        serumIn(owner).interactLivingEntity(owner, smilodon, InteractionHand.MAIN_HAND);
        Pig after = helper.spawnWithNoFreeWill(EntityType.PIG, 4, 2, 4);
        after.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        after.setHealth(1000.0F);
        helper.runAfterDelay(12, () -> {
            smilodon.doHurtTarget(after);
            helper.assertTrue(BleedingEffect.isBleeding(after), "a mordida com o soro não fez sangrar");
            helper.succeed();
        });
    }

    /** Congelada pelo projetor: sem IA, invulnerável; passado o tempo, volta ao normal sozinha. */
    @GameTest(template = EMPTY, batch = "titan_reward", timeoutTicks = 80)
    public static void theProjectorFreezesAndThenReleases(GameTestHelper helper) {
        LandCreature smilodon = helper.spawn(ModEntities.SMILODON.get(), new BlockPos(2, 2, 2));
        StasisProjectorItem.freeze(smilodon, 20);
        helper.assertTrue(smilodon.isNoAi() && smilodon.isInvulnerable(), "não congelou");
        float health = smilodon.getHealth();
        smilodon.hurt(helper.getLevel().damageSources().generic(), 10.0F);
        helper.assertTrue(smilodon.getHealth() == health, "congelada levou dano");
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(StasisProjectorItem.frozen(smilodon), "a marca do congelamento ficou");
            helper.assertFalse(smilodon.isNoAi() || smilodon.isInvulnerable(), "não descongelou");
            helper.succeed();
        });
    }

    /** O projetor não pega em pessoas nem no Titanovenator. */
    @GameTest(template = EMPTY, batch = "titan_reward")
    public static void theProjectorSparesPeopleAndTheTitan(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        LandCreature boss = helper.spawnWithNoFreeWill(ModEntities.TITANOVENATOR.get(), 4, 2, 4);
        helper.assertTrue(boss instanceof TitanovenatorBoss, "o tipo titanovenator não cria o boss");
        LandCreature smilodon = helper.spawnWithNoFreeWill(ModEntities.SMILODON.get(), 2, 2, 2);
        helper.assertFalse(StasisProjectorItem.freezable(player), "pessoa congelável");
        helper.assertFalse(StasisProjectorItem.freezable(boss), "o Titanovenator congelável");
        helper.assertTrue(StasisProjectorItem.freezable(smilodon), "criatura comum não congelável");
        boss.discard();
        helper.succeed();
    }
}
