package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.client.item.TitanRewardRenderer;
import java.util.function.Consumer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.ecology.HuntSpecials;
import dev.madebyfelipe.iceagesurvival.effect.BleedingEffect;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TitanovenatorBoss;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Soro do Titanovenator: o sangue dele, que o corpo larga ao morrer. Clicado numa criatura domesticada da própria
 * pessoa (acordada, uma vez por criatura), dá para sempre {@link #ATTACK_BONUS} de ataque e {@link #HEALTH_BONUS} de
 * vida a mais (multiplicando o total; a vida continua presa ao teto de {@link dev.madebyfelipe.iceagesurvival.entity.HealthCap}) e a mordida que sangra: cada
 * golpe corpo a corpo dela soma um nível de sangramento, até três, como a do Giganotosaurus.
 *
 * <p>Não é genético: vem em modificadores de atributo (salvos com a criatura) e numa marca, nunca nos pontos, então
 * os filhotes não herdam. Números propostos, ajustáveis.
 */
@Mod.EventBusSubscriber(modid = IceAgeSurvival.MODID)
public class TitanSerumItem extends Item implements GeoItem {
    /**
     * Sem id do GeckoLib por pilha (o soro empilha): todas as ampolas dividem a mesma animação, o anel âmbar que corre
     * pelo sangue em laço.
     */
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.titan_serum.idle");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public static final double ATTACK_BONUS = 0.25;
    public static final double HEALTH_BONUS = 0.25;
    public static final String TAG = IceAgeSurvival.MODID + ".titan_serum";
    private static final UUID ATTACK_MODIFIER = UUID.fromString("0b1c7e52-4d1a-4f0e-9a61-3e2f8c7d5a10");
    private static final UUID HEALTH_MODIFIER = UUID.fromString("6f2e9d41-8b3c-4a57-b0d2-1c4e7f9a8b23");

    public TitanSerumItem(Properties properties) {
        super(properties);
    }

    /** O modelo 3D da mão, do chão e da moldura ({@code tools/gen_titan_rewards.py}); na GUI, o ícone plano. */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(TitanRewardRenderer.extensions("titan_serum"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /** Se a criatura já tomou o soro. */
    public static boolean has(LivingEntity creature) {
        return creature.getPersistentData().getBoolean(TAG);
    }

    /** Por que o soro não pega nesta criatura, ou {@code null} se pega. */
    @Nullable
    public static Component refusal(Player player, LivingEntity target) {
        if (!(target instanceof PrehistoricCreature creature) || target instanceof TitanovenatorBoss
                || !creature.isTame() || !creature.isOwner(player)) {
            return Component.translatable("item.iceagesurvival.titan_serum.not_yours");
        }
        if (creature.isUnconscious() || !creature.isAlive()) {
            return Component.translatable("item.iceagesurvival.titan_serum.asleep");
        }
        if (has(creature)) {
            return Component.translatable("item.iceagesurvival.titan_serum.already");
        }
        return null;
    }

    /** Aplica o soro: os dois modificadores, a marca e a vida cheia. Só no servidor. */
    public static void apply(LivingEntity creature) {
        creature.getPersistentData().putBoolean(TAG, true);
        addModifier(creature, Attributes.ATTACK_DAMAGE, ATTACK_MODIFIER, "titan_serum_attack", ATTACK_BONUS);
        addModifier(creature, Attributes.MAX_HEALTH, HEALTH_MODIFIER, "titan_serum_health", HEALTH_BONUS);
        creature.setHealth(creature.getMaxHealth());
        if (creature.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, creature.getX(), creature.getY(0.6), creature.getZ(),
                    16, creature.getBbWidth() / 2, creature.getBbHeight() / 4, creature.getBbWidth() / 2, 0.1);
            level.playSound(null, creature.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 1.0F, 0.7F);
        }
    }

    private static void addModifier(LivingEntity entity, Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, name, amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        Component refusal = refusal(player, target);
        if (refusal != null) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(refusal, true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if (!player.level().isClientSide) {
            apply(target);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("item.iceagesurvival.titan_serum.applied",
                    target.getDisplayName()), true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.titan_serum.tooltip").withStyle(ChatFormatting.GRAY));
    }

    /** A mordida de quem tomou o soro: um nível de sangramento por golpe corpo a corpo, até três. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity().level().isClientSide || source.is(DamageTypeTags.IS_PROJECTILE)
                || !source.is(DamageTypes.MOB_ATTACK)) {
            return;
        }
        if (source.getDirectEntity() instanceof PrehistoricCreature attacker && source.getEntity() == attacker
                && has(attacker)) {
            BleedingEffect.cut(event.getEntity(), attacker, HuntSpecials.BLEED_LEVELS_PER_BITE,
                    HuntSpecials.BLEED_LEVEL_CAP, HuntSpecials.BLEED_TICKS);
        }
    }
}
