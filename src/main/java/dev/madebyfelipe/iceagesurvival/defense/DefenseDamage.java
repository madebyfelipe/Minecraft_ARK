package dev.madebyfelipe.iceagesurvival.defense;

import dev.madebyfelipe.iceagesurvival.entity.BlockBreaking;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Golpes da fauna na madeira (D44): dano acumulado por bloco (no portão, pelo portão inteiro; na porta, pela porta),
 * visível como rachaduras, e o bloco cai no último golpe ({@link BlockBreaking#hitsToBreak}: muro e portão nossos 6,
 * 3 nos gigantes; madeira vanilla 3, 2 nos gigantes). Quem golpeia é quem tem {@code body.breaks: wood}, adulto.
 * Fica em memória: um servidor reiniciado começa com a madeira inteira de novo.
 */
public final class DefenseDamage {

    private static final Map<Level, Map<BlockPos, Damage>> DAMAGE = new WeakHashMap<>();

    private DefenseDamage() {
    }

    /** Golpes contados e quem deu o último, em que tick: duas partes do mesmo portão na mesma mordida são um golpe. */
    private static final class Damage {
        int hits;
        long lastTick = Long.MIN_VALUE;
        int lastHitter;
        /** O bloco golpeado: se outro tomou o lugar (o jogador trocou a cerca), os golpes recomeçam. */
        Block block;
    }

    /** Se esta criatura derruba madeira a golpes ({@link BlockBreaking#breaksWood}). */
    public static boolean isBreaker(Mob mob) {
        return BlockBreaking.breaksWood(mob);
    }

    /** Se o bloco cede a golpes: muro ou portão de madeira nosso, ou madeira vanilla ({@link BlockBreaking#isWood}). */
    public static boolean yields(BlockState state) {
        return BlockBreaking.isWood(state);
    }

    /** A posição que guarda os golpes: a parte mestra no portão, a metade de baixo na porta, o próprio bloco no resto. */
    private static BlockPos damageKey(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DefenseBlock defense) {
            return defense.damageKey(level, pos, state);
        }
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /**
     * Um golpe de {@code mob} no bloco em {@code pos}. Só conta se a criatura derruba madeira e o bloco é de
     * madeira; em qualquer outro caso não faz nada. Respeita o {@code mobGriefing} e os eventos de quebra.
     *
     * @return se o golpe contou
     */
    public static boolean hit(Mob mob, BlockPos pos, BlockState state) {
        Level level = mob.level();
        if (level.isClientSide || !isBreaker(mob) || !yields(state)
                || !ForgeEventFactory.getMobGriefingEvent(level, mob)) {
            return false;
        }
        BlockPos key = damageKey(level, pos, state).immutable();
        Damage damage = DAMAGE.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(key, k -> new Damage());
        if (damage.block != state.getBlock()) {
            damage.hits = 0;
            damage.block = state.getBlock();
        }
        int needed = BlockBreaking.hitsToBreak(state, BlockBreaking.isGiant(mob));
        long now = level.getGameTime();
        if (damage.lastTick == now && damage.lastHitter == mob.getId()) {
            return false;
        }
        damage.lastTick = now;
        damage.lastHitter = mob.getId();
        damage.hits++;
        level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 1.0F,
                0.8F + level.random.nextFloat() * 0.2F);
        if (damage.hits >= needed) {
            if (ForgeEventFactory.onEntityDestroyBlock(mob, pos, state)) {
                forget(level, key);
                level.destroyBlock(pos, true, mob);
            }
            return true;
        }
        level.destroyBlockProgress(crackId(pos), pos, damage.hits * 10 / needed);
        return true;
    }

    /** Golpes que o bloco (ou o portão dele) já levou. */
    public static int hits(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos key = damageKey(level, pos, state);
        Map<BlockPos, Damage> byPos = DAMAGE.get(level);
        Damage damage = byPos == null ? null : byPos.get(key);
        return damage == null ? 0 : damage.hits;
    }

    /** O bloco saiu: esquece os golpes e apaga as rachaduras. */
    public static void forget(Level level, BlockPos key) {
        Map<BlockPos, Damage> byPos = DAMAGE.get(level);
        if (byPos != null && byPos.remove(key) != null && !level.isClientSide) {
            level.destroyBlockProgress(crackId(key), key, -1);
        }
    }

    /** Um id de "quem quebra" por posição, longe dos ids de entidade (que são positivos). */
    private static int crackId(BlockPos pos) {
        return -1 - (Long.hashCode(pos.asLong()) & 0x3FFFFFFF);
    }
}
