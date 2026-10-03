package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.core.mount.MountedReach;
import dev.madebyfelipe.iceagesurvival.defense.DefenseBlocks;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * O que a criatura quebra no caminho: ao esbarrar ({@link #bumpThrough}) e na mordida de quem monta ({@link #bite}).
 */
public final class BlockBreaking {
    private BlockBreaking() {
    }

    /**
     * Esbarrou em algo andando: atravessa a vegetação que a espécie consegue quebrar ({@code body.plow_hardness})
     * ou, sem isso, só as folhas ({@code body.breaks_leaves}).
     */
    public static void bumpThrough(PrehistoricCreature creature, float plowHardness, boolean breaksLeaves) {
        if (plowHardness > 0.0F) {
            plow(creature, plowHardness);
        } else if (breaksLeaves) {
            breakLeaves(creature);
        }
    }

    /** Se o pathfinder deve tratar copa como caminho livre: a criatura a atravessa quebrando. */
    public static boolean walksThroughLeaves(PrehistoricCreature creature, float plowHardness, boolean breaksLeaves) {
        return breaksLeaves || plowHardness > 0.0F;
    }

    /**
     * Quebra, à frente do corpo, os blocos da tag {@code plowable} com dureza até o limite da
     * espécie. Só vegetação e neve: o chão e as encostas ficam, e a criatura sobe por eles.
     */
    private static void plow(PrehistoricCreature creature, float plowHardness) {
        Vec3 motion = creature.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        Vec3 forward = motion.lengthSqr() > 1.0E-4 ? motion.normalize()
                : Vec3.directionFromRotation(0.0F, creature.getYRot());
        AABB box = creature.getBoundingBox().expandTowards(forward.scale(0.8)).inflate(0.1, 0.0, 0.1);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY + 0.01, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = creature.level().getBlockState(pos);
            if (!state.is(ModTags.PLOWABLE) || state.hasBlockEntity()) {
                continue;
            }
            float hardness = state.getDestroySpeed(creature.level(), pos);
            if (hardness >= 0.0F && hardness <= plowHardness
                    && ForgeEventFactory.onEntityDestroyBlock(creature, pos, state)) {
                creature.level().destroyBlock(pos, true, creature);
            }
        }
    }

    private static void breakLeaves(PrehistoricCreature creature) {
        AABB box = creature.getBoundingBox().inflate(0.2);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (creature.level().getBlockState(pos).getBlock() instanceof LeavesBlock) {
                creature.level().destroyBlock(pos, true, creature);
            }
        }
    }

    /**
     * Quebra os blocos na frente do corpo, do chão em que pisa até o topo da cabeça, com
     * dureza até o limite da espécie e, se ela tiver {@code break_blocks}, só os daquela tag.
     * Os blocos dropam como se quebrados à mão. Respeita {@code mobGriefing}, a proteção do spawn e
     * os eventos de quebra de bloco (mods de proteção de terreno), como se fosse quem monta
     * quebrando, e nunca quebra bloco com inventário.
     */
    public static void bite(PrehistoricCreature creature, ServerPlayer rider) {
        MountProfile mount = creature.mountProfile().orElse(null);
        if (mount == null || mount.breakHardness() <= 0.0F
                || !ForgeEventFactory.getMobGriefingEvent(creature.level(), creature)) {
            return;
        }
        float maxHardness = mount.breakHardness();
        Vec3 forward = Vec3.directionFromRotation(0.0F, creature.getYRot());
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        double halfWidth = creature.getBbWidth() / 2.0;
        double sideReach = halfWidth + MountedReach.BREAK_SIDE_MARGIN;
        int minY = Mth.floor(creature.getY() + 0.01);
        int maxY = Mth.floor(creature.getY() + creature.getBbHeight() - 0.01);
        java.util.Set<BlockPos> bitten = new java.util.LinkedHashSet<>();
        double maxDepth = halfWidth + MountedReach.breakDepth(creature.getBbWidth());
        for (double depth = halfWidth + 0.5; depth <= maxDepth; depth += 0.5) {
            for (double lateral = -sideReach; lateral <= sideReach + 1.0E-3; lateral += 0.5) {
                Vec3 column = creature.position().add(forward.scale(depth)).add(side.scale(lateral));
                for (int y = minY; y <= maxY; y++) {
                    bitten.add(BlockPos.containing(column.x, y, column.z));
                }
            }
        }
        for (BlockPos pos : bitten) {
            var state = creature.level().getBlockState(pos);
            if (DefenseBlocks.isDefense(state)) {
                // Defesa nunca quebra direto: o gigante conta um golpe na madeira; qualquer outra, nada.
                DefenseBlocks.biteHit(creature, rider, pos, state);
                continue;
            }
            float hardness = state.getDestroySpeed(creature.level(), pos);
            if (state.isAir() || hardness < 0.0F || hardness > maxHardness || state.hasBlockEntity()
                    || mount.breakBlocks().isPresent() && !state.is(mount.breakBlocks().get())
                    || !creature.level().mayInteract(rider, pos)
                    || rider.connection.connection.channel() != null
                            && ForgeHooks.onBlockBreakEvent(creature.level(),
                                    rider.gameMode.getGameModeForPlayer(), rider, pos) < 0) {
                continue;
            }
            creature.level().destroyBlock(pos, true, creature);
        }
    }
}
