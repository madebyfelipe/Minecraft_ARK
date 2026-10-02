package dev.madebyfelipe.iceagesurvival.defense;

import dev.madebyfelipe.iceagesurvival.entity.CreatureAction;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.animatable.GeoEntity;

/**
 * O gigante selvagem ({@link DefenseBlocks#WALL_BREAKERS}) com o caminho até o alvo fechado por muro ou portão de
 * madeira golpeia o bloco que está entre ele e o alvo, um golpe por segundo, até abrir passagem
 * ({@link DefenseDamage#HITS_TO_BREAK} golpes por bloco). Pedra não cede: diante dela o golpe não sai.
 */
public class BreakDefenseGoal extends Goal {
    /** Um golpe por segundo, como a mordida. */
    private static final int HIT_INTERVAL = 20;
    /** De quanto em quanto tempo procura o bloco que fecha o caminho. */
    private static final int SCAN_INTERVAL = 10;
    /** Até onde, à frente da borda do corpo, procura o bloco. */
    private static final double SCAN_DEPTH = 2.0;
    /** O fim do caminho mais longe que isto do alvo = caminho fechado. */
    private static final double PATH_END_TOLERANCE_SQR = 4.0;
    /** Desiste depois disto sem derrubar (o alvo saiu de vista, mudou de lado). */
    private static final int GIVE_UP_TICKS = 20 * 20;

    private final PrehistoricCreature creature;
    @Nullable
    private BlockPos block;
    private int nextScan;
    private int nextHit;
    private int ticks;

    public BreakDefenseGoal(PrehistoricCreature creature) {
        this.creature = creature;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (creature.isTame() || creature.isVehicle() || !DefenseDamage.isBreaker(creature) || --nextScan > 0) {
            return false;
        }
        nextScan = SCAN_INTERVAL;
        LivingEntity target = creature.getTarget();
        if (target == null || !target.isAlive() || !ForgeEventFactory.getMobGriefingEvent(creature.level(), creature)) {
            return false;
        }
        // Primeiro a busca barata à frente; o caminho só se calcula com um muro ali.
        block = blockingDefense(target);
        if (block != null && reachable(target)) {
            block = null;
        }
        return block != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = creature.getTarget();
        return block != null && ticks < GIVE_UP_TICKS && target != null && target.isAlive() && !creature.isTame()
                && DefenseDamage.yields(creature.level().getBlockState(block));
    }

    @Override
    public void start() {
        ticks = 0;
        nextHit = HIT_INTERVAL / 2;
        creature.getNavigation().stop();
    }

    @Override
    public void stop() {
        block = null;
        nextScan = SCAN_INTERVAL;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (block == null) {
            return;
        }
        ticks++;
        Vec3 center = Vec3.atCenterOf(block);
        creature.getLookControl().setLookAt(center.x, center.y, center.z, 30.0F, 30.0F);
        if (!inReach(block)) {
            creature.getMoveControl().setWantedPosition(center.x, creature.getY(), center.z, 1.0);
            return;
        }
        creature.getNavigation().stop();
        if (--nextHit > 0) {
            return;
        }
        nextHit = HIT_INTERVAL;
        // A animação do ataque, como a mordida (o swingAttack da criatura é protegido).
        creature.startAction(CreatureAction.ATTACK, 20);
        if (creature instanceof GeoEntity geo) {
            geo.triggerAnim("attack", "attack");
        }
        creature.swing(InteractionHand.MAIN_HAND);
        DefenseDamage.hit(creature, block, creature.level().getBlockState(block));
    }

    /** Se o bloco está ao alcance do golpe: até um bloco e meio da borda do corpo. */
    private boolean inReach(BlockPos pos) {
        return creature.getBoundingBox().inflate(1.5).intersects(new AABB(pos));
    }

    /** O caminho atual (ou um novo) chega ao alvo. */
    private boolean reachable(LivingEntity target) {
        Path path = creature.getNavigation().getPath();
        if (path == null || path.isDone()) {
            path = creature.getNavigation().createPath(target, 0);
        }
        if (path == null) {
            return false;
        }
        Node end = path.getEndNode();
        return end != null && target.distanceToSqr(end.x + 0.5, end.y, end.z + 0.5) <= PATH_END_TOLERANCE_SQR;
    }

    /**
     * O muro ou portão de madeira mais perto à frente do corpo, na direção do alvo, da altura dos pés até a da cabeça.
     * Pedra na frente: nada (não cede, e o gigante não golpeia à toa).
     */
    @Nullable
    private BlockPos blockingDefense(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(creature.position()).multiply(1.0, 0.0, 1.0);
        if (toTarget.lengthSqr() < 1.0E-4) {
            return null;
        }
        Vec3 forward = toTarget.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        double halfWidth = creature.getBbWidth() / 2.0;
        int minY = Mth.floor(creature.getY() + 0.01);
        int maxY = Mth.floor(creature.getY() + Math.min(creature.getBbHeight(), 3.0));
        for (double depth = halfWidth + 0.5; depth <= halfWidth + SCAN_DEPTH; depth += 0.5) {
            for (double lateral = -halfWidth; lateral <= halfWidth + 1.0E-3; lateral += 0.5) {
                Vec3 column = creature.position().add(forward.scale(depth)).add(side.scale(lateral));
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = BlockPos.containing(column.x, y, column.z);
                    if (DefenseDamage.yields(creature.level().getBlockState(pos))) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
