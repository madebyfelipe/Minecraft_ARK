package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.command.CreatureCommands;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cápsula criogênica: guarda uma criatura domesticada do jogador e a solta em outro lugar, para levar o animal
 * longe. Usada na criatura (do dono, a até o alcance dos comandos; vale desmaiada), a congela inteira — identidade,
 * genoma, pontos, sela, inventário. Usada num bloco, a solta ali, se o corpo couber. Reutilizável: ao soltar, volta
 * a ficar vazia. Congelada, o tempo não passa para a criatura. O menu da tecla O faz o mesmo sem tirar a cápsula do
 * inventário.
 */
public class CryoCapsuleItem extends Item {
    private static final String TAG_CREATURE = "Creature";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_LEVEL = "Level";
    private static final String TAG_NAME = "Name";

    public CryoCapsuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Se a cápsula guarda uma criatura. */
    public static boolean holdsCreature(ItemStack stack) {
        return stack.getItem() instanceof CryoCapsuleItem && stack.hasTag() && stack.getTag().contains(TAG_CREATURE)
                && stack.getTag().contains(TAG_TYPE);
    }

    /** Se é uma cápsula vazia. */
    public static boolean isEmptyCapsule(ItemStack stack) {
        return stack.getItem() instanceof CryoCapsuleItem && !holdsCreature(stack);
    }

    /** Nome da criatura guardada (vazio se não houver). */
    public static String creatureName(ItemStack stack) {
        return holdsCreature(stack) ? stack.getTag().getString(TAG_NAME) : "";
    }

    /** Nível da criatura guardada (0 se não houver). */
    public static int creatureLevel(ItemStack stack) {
        return holdsCreature(stack) ? stack.getTag().getInt(TAG_LEVEL) : 0;
    }

    /** Espécie guardada, para o nome traduzido. */
    public static Optional<ResourceLocation> creatureType(ItemStack stack) {
        return holdsCreature(stack) ? Optional.ofNullable(ResourceLocation.tryParse(stack.getTag().getString(TAG_TYPE)))
                : Optional.empty();
    }

    /** Congela a criatura na cápsula vazia e a tira do mundo. Só no servidor. */
    public static boolean freeze(Player player, ItemStack capsule, PrehistoricCreature creature) {
        if (!isEmptyCapsule(capsule) || !creature.canBeFrozenBy(player)) {
            return false;
        }
        CompoundTag tag = capsule.getOrCreateTag();
        tag.put(TAG_CREATURE, creature.freezeData());
        tag.putString(TAG_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()).toString());
        tag.putInt(TAG_LEVEL, creature.creatureLevel());
        tag.putString(TAG_NAME, creature.getName().getString());
        creature.level().playSound(null, creature.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.NEUTRAL,
                1.0F, 0.6F);
        creature.discard();
        player.displayClientMessage(Component.translatable("iceagesurvival.cryo.frozen", tag.getString(TAG_NAME)), true);
        return true;
    }

    /** Solta a criatura guardada na posição dada; a cápsula só esvazia se ela entrou no mundo. Só no servidor. */
    public static boolean release(ServerLevel level, Player player, ItemStack capsule, Vec3 pos) {
        if (!holdsCreature(capsule)) {
            return false;
        }
        CompoundTag tag = capsule.getTag();
        Optional<EntityType<?>> type = EntityType.byString(tag.getString(TAG_TYPE));
        PrehistoricCreature creature = type.isEmpty() ? null
                : PrehistoricCreature.thaw(level, type.get(), tag.getCompound(TAG_CREATURE), pos);
        if (creature == null) {
            player.displayClientMessage(Component.translatable("iceagesurvival.cryo.no_room"), true);
            return false;
        }
        for (String key : new String[] {TAG_CREATURE, TAG_TYPE, TAG_LEVEL, TAG_NAME}) {
            tag.remove(key);
        }
        if (tag.isEmpty()) {
            capsule.setTag(null);
        }
        level.playSound(null, creature.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("iceagesurvival.cryo.released", creature.getName()), true);
        return true;
    }

    /** Pelo menu: solta à frente do jogador, no chão, a uma distância que deixa o corpo livre dele. */
    public static boolean releaseInFront(ServerLevel level, Player player, ItemStack capsule) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Optional<EntityType<?>> type = holdsCreature(capsule)
                ? EntityType.byString(capsule.getTag().getString(TAG_TYPE)) : Optional.empty();
        double distance = 1.5 + type.map(entityType -> entityType.getWidth() / 2.0).orElse(1.0);
        Vec3 ahead = player.position().add(forward.scale(distance));
        BlockPos column = BlockPos.containing(ahead);
        // Procura o chão perto da altura dos pés: de um bloco acima até três abaixo.
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos feet = column.offset(0, dy, 0);
            if (!level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
                return release(level, player, capsule, new Vec3(ahead.x, feet.getY(), ahead.z));
            }
        }
        player.displayClientMessage(Component.translatable("iceagesurvival.cryo.no_room"), true);
        return false;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!(target instanceof PrehistoricCreature creature) || holdsCreature(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!creature.canBeFrozenBy(player)) {
            player.displayClientMessage(Component.translatable("iceagesurvival.cryo.not_yours"), true);
            return InteractionResult.FAIL;
        }
        // No criativo o stack recebido é uma cópia; o que vale é o da mão.
        return freeze(player, player.getItemInHand(hand), creature) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!holdsCreature(stack) || player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spot = Vec3.atBottomCenterOf(pos);
        if (spot.distanceToSqr(player.position()) > CreatureCommands.COMMAND_RANGE * CreatureCommands.COMMAND_RANGE) {
            return InteractionResult.FAIL;
        }
        return release(level, player, stack, spot) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!holdsCreature(stack)) {
            tooltip.add(Component.translatable("item.iceagesurvival.cryo_capsule.empty").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.iceagesurvival.cryo_capsule.hint_empty")
                    .withStyle(ChatFormatting.DARK_AQUA));
            return;
        }
        Component species = creatureType(stack)
                .<Component>map(type -> Component.translatable("entity." + type.getNamespace() + "." + type.getPath()))
                .orElse(Component.literal("?"));
        tooltip.add(Component.literal(creatureName(stack)).withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.iceagesurvival.implant.creature", species, creatureLevel(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceagesurvival.cryo_capsule.hint_full")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return holdsCreature(stack);
    }
}
