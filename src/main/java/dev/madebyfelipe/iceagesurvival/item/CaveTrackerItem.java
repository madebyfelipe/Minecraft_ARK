package dev.madebyfelipe.iceagesurvival.item;

import dev.madebyfelipe.iceagesurvival.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Rastreador da caverna (D47): feito com os troféus de apex, é jogado como o Olho do Ender. Voa na direção da caverna
 * da arena mais próxima ({@code findNearestMapStructure} com a tag {@code #iceagesurvival:arena_cave}) e, como o olho
 * vanilla, volta como item 4 em 5 vezes. Jogar de novo mais adiante triangula a entrada.
 */
public class CaveTrackerItem extends Item {
    /** Raio de busca em chunks; os anéis concêntricos são achados por lista, então serve qualquer valor grande. */
    public static final int SEARCH_RADIUS_CHUNKS = 100;
    public static final String NO_CAVE_KEY = "message.iceagesurvival.cave_tracker.none";

    public CaveTrackerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            BlockPos target = server.findNearestMapStructure(ModStructures.ARENA_CAVES, player.blockPosition(),
                    SEARCH_RADIUS_CHUNKS, false);
            if (target == null) {
                player.displayClientMessage(
                        Component.translatableWithFallback(NO_CAVE_KEY, "Nenhuma caverna da arena neste mundo"), true);
                return InteractionResultHolder.fail(stack);
            }
            launch(server, player, stack, target);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.swing(hand, true);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Lança o olho vanilla carregando este item, com o som e o efeito do Olho do Ender. */
    public static EyeOfEnder launch(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
        EyeOfEnder eye = new EyeOfEnder(level, player.getX(), player.getY(0.5), player.getZ());
        eye.setItem(stack);
        eye.signalTo(target);
        level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
        level.addFreshEntity(eye);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        level.levelEvent(null, 1003, player.blockPosition(), 0);
        return eye;
    }
}
