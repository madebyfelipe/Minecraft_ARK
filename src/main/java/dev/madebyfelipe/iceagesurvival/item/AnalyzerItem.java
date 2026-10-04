package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.client.item.AnalyzerRenderer;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.ScanResultPayload;
import dev.madebyfelipe.iceagesurvival.network.TerminalReadPayload;
import dev.madebyfelipe.iceagesurvival.outpost.MilitaryTerminalBlockEntity;
import dev.madebyfelipe.iceagesurvival.outpost.Outposts;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.world.DinoFileData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.player.PlayerEvent;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Analisador: o aparelho de campo que todo jogador recebe ao entrar no mundo pela primeira vez. Mirando numa criatura
 * do mod a até {@link #RANGE} blocos e segurando o clique por {@link #SCAN_TICKS} ticks, escaneia: registra a
 * espécie na DINO FILE do jogador ({@link DinoFileData}) e manda a leitura do indivíduo ao cliente
 * ({@link ScanResultPayload}). Mirando num terminal militar dos postos, a mesma leitura destrava o próximo registro
 * militar de quem leu, uma vez por terminal ({@link TerminalReadPayload}). Perder a mira no meio cancela. Clicando no
 * ar, abre o terminal do aparelho (DINO FILE, NOTAS e REGISTROS).
 *
 * <p>Na mão, no chão e na moldura é um modelo 3D do GeckoLib ({@link AnalyzerRenderer}); na GUI, o ícone plano. A
 * animação {@code idle} (LED piscando devagar, varredura calma na tela) vira {@code scan} enquanto alguém segura o
 * clique escaneando com este aparelho.
 */
public class AnalyzerItem extends Item implements GeoItem {
    public static final double RANGE = 24.0;
    public static final int SCAN_TICKS = 30;
    /** Marca, nos dados persistentes do jogador, de que ele já recebeu o analisador inicial. */
    public static final String GIVEN_TAG = "iceagesurvival.analyzer_given";

    /** O cliente registra aqui a abertura do terminal (o item não pode tocar em classes de cliente). */
    private static Runnable terminalOpener = () -> { };
    /** Quem está escaneando o quê, no servidor. */
    private static final Map<UUID, Target> SCANNING = new HashMap<>();
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.analyzer.idle");
    private static final RawAnimation SCAN = RawAnimation.begin().thenLoop("animation.analyzer.scan");
    /** O cliente registra aqui quem diz se um aparelho está escaneando agora (o uso de quem o segura). */
    private static Predicate<ItemStack> scanningCheck = stack -> false;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    /** O alvo de um scan: uma criatura (pelo id da entidade) ou um terminal militar (pela posição). */
    private record Target(int entity, @Nullable BlockPos terminal) {
        static Target of(PrehistoricCreature creature) {
            return new Target(creature.getId(), null);
        }

        static Target of(BlockPos terminal) {
            return new Target(-1, terminal.immutable());
        }
    }

    public AnalyzerItem(Properties properties) {
        super(properties);
    }

    public static void setTerminalOpener(Runnable opener) {
        terminalOpener = opener;
    }

    public static void setScanningCheck(Predicate<ItemStack> check) {
        scanningCheck = check;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(AnalyzerRenderer.EXTENSIONS);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "analyzer", 3, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            return state.setAndContinue(stack != null && scanningCheck.test(stack) ? SCAN : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    /** Cada aparelho ganha um id do GeckoLib, para que um escaneando não anime os outros. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server) {
            GeoItem.getOrAssignId(stack, server);
        }
    }

    /** O id gravado no aparelho não deve fazer a mão abaixar e subir de novo. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceagesurvival.analyzer.tooltip.scan").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceagesurvival.analyzer.tooltip.open").withStyle(ChatFormatting.GRAY));
    }

    /** A criatura do mod na mira do jogador, sem bloco no meio, a até {@link #RANGE} blocos. */
    @Nullable
    public static PrehistoricCreature aimed(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB box = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                entity -> entity instanceof PrehistoricCreature creature && creature.isAlive(), eye.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof PrehistoricCreature creature ? creature : null;
    }

    /** O terminal militar na mira do jogador, o primeiro bloco a até {@link #RANGE} blocos; {@code null} se não é um. */
    @Nullable
    public static BlockPos aimedTerminal(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK
                && player.level().getBlockState(hit.getBlockPos()).is(Outposts.MILITARY_TERMINAL.get())
                ? hit.getBlockPos() : null;
    }

    /** O que está na mira agora: a criatura tem preferência; senão, um terminal. */
    @Nullable
    private static Target aimedTarget(Player player) {
        PrehistoricCreature creature = aimed(player);
        if (creature != null) {
            return Target.of(creature);
        }
        BlockPos terminal = aimedTerminal(player);
        return terminal == null ? null : Target.of(terminal);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Target target = aimedTarget(player);
        if (target == null) {
            if (level.isClientSide) {
                terminalOpener.run();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            SCANNING.put(player.getUUID(), target);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.4F, 1.8F);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return SCAN_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** No servidor, a cada tick: perdeu a mira, cancela; senão, um bipe que sobe de tom. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        Target target = SCANNING.get(player.getUUID());
        if (target == null || !target.equals(aimedTarget(player))) {
            SCANNING.remove(player.getUUID());
            player.stopUsingItem();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(),
                    SoundSource.PLAYERS, 0.6F, 0.5F);
            return;
        }
        int elapsed = SCAN_TICKS - remaining;
        if (elapsed % 6 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
                    SoundSource.PLAYERS, 0.35F, 1.0F + elapsed / (float) SCAN_TICKS);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            Target target = SCANNING.remove(player.getUUID());
            if (target != null && target.terminal() != null) {
                completeTerminal(player, target.terminal());
            } else if (target != null && level.getEntity(target.entity()) instanceof PrehistoricCreature creature
                    && creature.isAlive()) {
                complete(player, creature);
            }
        }
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide) {
            SCANNING.remove(entity.getUUID());
        }
    }

    /** Escaneou: registra a espécie e manda a leitura. */
    public static void complete(ServerPlayer player, PrehistoricCreature creature) {
        boolean newEntry = DinoFileData.register(player, BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()));
        ModPayloads.sendToPlayer(player, ScanResultPayload.of(player, creature, newEntry));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                newEntry ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS,
                newEntry ? 0.5F : 0.6F, newEntry ? 1.6F : 1.4F);
    }

    /**
     * Leu um terminal: na primeira leitura desta pessoa neste terminal, destrava o próximo registro militar dela.
     *
     * @return {@code true} se destravou um registro
     */
    public static boolean completeTerminal(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof MilitaryTerminalBlockEntity terminal)) {
            return false;
        }
        boolean fresh = terminal.markRead(player.getUUID());
        int records = fresh ? DinoFileData.unlockRecord(player) : DinoFileData.records(player);
        ModPayloads.sendToPlayer(player, new TerminalReadPayload(records, fresh));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                fresh ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS,
                0.5F, fresh ? 1.2F : 0.7F);
        return fresh;
    }

    /** Primeiro login: um analisador no inventário (uma vez por jogador, mesmo morrendo). E a DINO FILE ao cliente. */
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag persistent = player.getPersistentData();
        CompoundTag kept = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!kept.getBoolean(GIVEN_TAG)) {
            ItemStack analyzer = new ItemStack(ModItems.ANALYZER.get());
            if (!player.getInventory().add(analyzer)) {
                player.drop(analyzer, false);
            }
            kept.putBoolean(GIVEN_TAG, true);
            persistent.put(Player.PERSISTED_NBT_TAG, kept);
        }
        DinoFileData.sync(player);
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SCANNING.remove(event.getEntity().getUUID());
    }
}
