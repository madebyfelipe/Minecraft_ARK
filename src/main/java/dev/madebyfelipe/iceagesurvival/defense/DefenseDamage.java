package dev.madebyfelipe.iceagesurvival.defense;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Golpes dos gigantes nos muros e portões de madeira: dano acumulado por bloco (no portão, pelo portão inteiro),
 * visível como rachaduras, e o bloco cai no {@link #HITS_TO_BREAK}-ésimo golpe. Fica em memória: um servidor
 * reiniciado começa com as defesas inteiras de novo.
 */
public final class DefenseDamage {
    /** Golpes de gigante para derrubar um bloco (ou um portão inteiro) de madeira. */
    public static final int HITS_TO_BREAK = 6;

    private static final Map<Level, Map<BlockPos, Damage>> DAMAGE = new WeakHashMap<>();

    private DefenseDamage() {
    }

    /** Golpes contados e quem deu o último, em que tick: duas partes do mesmo portão na mesma mordida são um golpe. */
    private static final class Damage {
        int hits;
        long lastTick = Long.MIN_VALUE;
        int lastHitter;
    }

    /** Se esta criatura derruba madeira: é um dos gigantes de {@link DefenseBlocks#WALL_BREAKERS}. */
    public static boolean isBreaker(Mob mob) {
        return mob.getType().is(DefenseBlocks.WALL_BREAKERS);
    }

    /** Se o bloco cede aos gigantes: muro ou portão de madeira. */
    public static boolean yields(BlockState state) {
        return state.getBlock() instanceof DefenseBlock defense && defense.yieldsToGiants();
    }

    /**
     * Um golpe de {@code mob} no bloco de defesa em {@code pos}. Só conta se a criatura é um gigante e o bloco é
     * de madeira; em qualquer outro caso não faz nada. Respeita o {@code mobGriefing} e os eventos de quebra.
     *
     * @return se o golpe contou
     */
    public static boolean hit(Mob mob, BlockPos pos, BlockState state) {
        Level level = mob.level();
        if (level.isClientSide || !isBreaker(mob) || !yields(state)
                || !ForgeEventFactory.getMobGriefingEvent(level, mob)) {
            return false;
        }
        BlockPos key = ((DefenseBlock) state.getBlock()).damageKey(level, pos, state).immutable();
        Damage damage = DAMAGE.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(key, k -> new Damage());
        long now = level.getGameTime();
        if (damage.lastTick == now && damage.lastHitter == mob.getId()) {
            return false;
        }
        damage.lastTick = now;
        damage.lastHitter = mob.getId();
        damage.hits++;
        level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 1.0F,
                0.8F + level.random.nextFloat() * 0.2F);
        if (damage.hits >= HITS_TO_BREAK) {
            if (ForgeEventFactory.onEntityDestroyBlock(mob, pos, state)) {
                forget(level, key);
                level.destroyBlock(pos, true, mob);
            }
            return true;
        }
        level.destroyBlockProgress(crackId(pos), pos, damage.hits * 10 / HITS_TO_BREAK);
        return true;
    }

    /** Golpes que o bloco (ou o portão dele) já levou. */
    public static int hits(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos key = state.getBlock() instanceof DefenseBlock defense ? defense.damageKey(level, pos, state) : pos;
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
