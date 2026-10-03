package dev.madebyfelipe.iceagesurvival.entity;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.core.mount.MountedReach;
import dev.madebyfelipe.iceagesurvival.defense.DefenseBlock;
import dev.madebyfelipe.iceagesurvival.defense.DefenseBlocks;
import dev.madebyfelipe.iceagesurvival.defense.DefenseDamage;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import dev.madebyfelipe.iceagesurvival.species.BodyProfile;
import dev.madebyfelipe.iceagesurvival.species.MountProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * O que a fauna quebra (D44), por espécie em {@code body.breaks}:
 * <ul>
 *   <li>{@code none} — nada (raptores, Ornitholestes, dodô, pterossauros, Megalania; o padrão);</li>
 *   <li>{@code plants} — folhas e plantas, ao esbarrar (Smilodon, urso-terrível, Galimimo, Kelenken);</li>
 *   <li>{@code wood} — também madeira, a golpes, só para alcançar o alvo: muro e portão nossos em
 *   {@link #DEFENSE_HITS} golpes ({@link #DEFENSE_HITS_GIANT} nos gigantes), madeira vanilla (tag
 *   {@link #CREATURE_BREAKABLE_WOOD}: tronco, tábua, cerca, porteira, porta, alçapão, escada e laje) em
 *   {@link #WOOD_HITS} ({@link #WOOD_HITS_GIANT}).</li>
 * </ul>
 * Os <b>gigantes</b> ({@code body.giant}: T-Rex, Espinossauro, Brontossauro, Tricerátopo, Mamute, Estegossauro)
 * golpeiam com menos golpes e derrubam o tronco esbarrando. Pedra, nenhuma selvagem. Filhote não quebra madeira nem
 * tronco. Tudo respeita {@code mobGriefing}; a mordida de quem monta segue os mesmos golpes na madeira e passa pelo
 * evento de quebra como o jogador que monta (o resto do terreno segue {@code mount.break_hardness}, como antes).
 */
public final class BlockBreaking {
    /** Madeira vanilla que cede aos golpes de quem tem {@code body.breaks: wood}. */
    public static final TagKey<Block> CREATURE_BREAKABLE_WOOD =
            TagKey.create(Registries.BLOCK, IceAgeSurvival.id("creature_breakable_wood"));

    /** Golpes para derrubar um bloco (ou um portão inteiro) de muro ou portão de madeira nosso. */
    public static final int DEFENSE_HITS = 6;
    public static final int DEFENSE_HITS_GIANT = 3;
    /** Golpes para derrubar um bloco de madeira vanilla. */
    public static final int WOOD_HITS = 3;
    public static final int WOOD_HITS_GIANT = 2;

    private BlockBreaking() {
    }

    // ---- Quem quebra o quê ----

    /** O corpo da espécie da criatura; o padrão (não quebra nada) para qualquer outro mob. */
    public static BodyProfile body(Mob mob) {
        if (mob instanceof PrehistoricCreature creature) {
            return creature.species().flatMap(Species::body).orElse(BodyProfile.DEFAULT);
        }
        return BodyProfile.DEFAULT;
    }

    /** Se a criatura derruba madeira a golpes: {@code body.breaks: wood} e adulta. */
    public static boolean breaksWood(Mob mob) {
        return !mob.isBaby() && body(mob).breakLevel() == BodyProfile.Breaks.WOOD;
    }

    /** Se é um dos gigantes ({@code body.giant}): menos golpes na madeira, tronco cai ao esbarrar. */
    public static boolean isGiant(Mob mob) {
        return body(mob).giant();
    }

    /** Se o bloco cede a golpes: muro ou portão de madeira nosso, ou madeira vanilla da tag. */
    public static boolean isWood(BlockState state) {
        if (state.getBlock() instanceof DefenseBlock defense) {
            return defense.yieldsToGiants();
        }
        return state.is(CREATURE_BREAKABLE_WOOD) && !state.hasBlockEntity();
    }

    /** Golpes que este bloco de madeira aguenta de um gigante ou de outra criatura. */
    public static int hitsToBreak(BlockState state, boolean giant) {
        if (DefenseBlocks.isDefense(state)) {
            return giant ? DEFENSE_HITS_GIANT : DEFENSE_HITS;
        }
        return giant ? WOOD_HITS_GIANT : WOOD_HITS;
    }

    // ---- Esbarrão ----

    /**
     * Esbarrou em algo andando: atravessa a vegetação que a espécie consegue quebrar ({@code body.plow_hardness})
     * ou, sem isso, só as folhas ({@code body.breaks}: {@code plants} ou {@code wood}). Tronco, só gigante adulto.
     */
    public static void bumpThrough(PrehistoricCreature creature, float plowHardness, boolean breaksLeaves) {
        if (!ForgeEventFactory.getMobGriefingEvent(creature.level(), creature)) {
            return;
        }
        if (plowHardness > 0.0F) {
            plow(creature, plowHardness, creature.isBaby() || !isGiant(creature));
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
    private static void plow(PrehistoricCreature creature, float plowHardness, boolean sparesLogs) {
        Vec3 motion = creature.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        Vec3 forward = motion.lengthSqr() > 1.0E-4 ? motion.normalize()
                : Vec3.directionFromRotation(0.0F, creature.getYRot());
        AABB box = creature.getBoundingBox().expandTowards(forward.scale(0.8)).inflate(0.1, 0.0, 0.1);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY + 0.01, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = creature.level().getBlockState(pos);
            if (!state.is(ModTags.PLOWABLE) || state.hasBlockEntity() || sparesLogs && state.is(BlockTags.LOGS)) {
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
            var state = creature.level().getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock && ForgeEventFactory.onEntityDestroyBlock(creature, pos, state)) {
                creature.level().destroyBlock(pos, true, creature);
            }
        }
    }

    // ---- Mordida de quem monta ----

    /**
     * Os blocos na frente do corpo, do chão em que pisa até o topo da cabeça. Madeira (muro, portão, tábua, cerca,
     * tronco…) leva um golpe, se a montaria derruba madeira ({@code body.breaks: wood}), e cai no último golpe como
     * a da selvagem. O resto quebra direto se a espécie tem {@code mount.break_hardness}, com dureza até esse limite
     * e, se ela tiver {@code break_blocks}, só os daquela tag. As outras defesas (pedra, armadilhas), nunca. Os blocos dropam
     * como se quebrados à mão, ou minerados com o {@code mount.break_tool} (o Anquilossauro). Respeita {@code mobGriefing}, a proteção do spawn e o evento de quebra de bloco
     * (mods de proteção de terreno) como se fosse quem monta quebrando, e nunca quebra bloco com inventário.
     */
    public static void bite(PrehistoricCreature creature, ServerPlayer rider) {
        if (!ForgeEventFactory.getMobGriefingEvent(creature.level(), creature)) {
            return;
        }
        MountProfile mount = creature.mountProfile().orElse(null);
        float maxHardness = mount == null ? 0.0F : mount.breakHardness();
        boolean strikesWood = breaksWood(creature);
        if (maxHardness <= 0.0F && !strikesWood) {
            return;
        }
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
            if (state.isAir()) {
                continue;
            }
            if (isWood(state)) {
                // Madeira nunca quebra direto: conta um golpe (o mesmo de quando selvagem).
                if (strikesWood && riderMayBreak(creature, rider, pos)) {
                    DefenseDamage.hit(creature, pos, state);
                }
                continue;
            }
            if (DefenseBlocks.isDefense(state) || maxHardness <= 0.0F) {
                continue;
            }
            float hardness = state.getDestroySpeed(creature.level(), pos);
            if (hardness < 0.0F || hardness > maxHardness || state.hasBlockEntity()
                    || mount.breakBlocks().isPresent() && !state.is(mount.breakBlocks().get())
                    || !riderMayBreak(creature, rider, pos)) {
                continue;
            }
            if (mount.breakTool().isPresent()) {
                // Dropa como se minerado com a ferramenta: a pedra do Anquilossauro dá pedregulho, o minério dá o minério.
                Block.dropResources(state, creature.level(), pos, null, rider, new ItemStack(mount.breakTool().get()));
                creature.level().destroyBlock(pos, false, creature);
            } else {
                creature.level().destroyBlock(pos, true, creature);
            }
        }
    }

    /**
     * A proteção do spawn e o {@code BlockEvent.BreakEvent} como o jogador que monta (mods de proteção de terreno).
     * O jogador falso dos testes, sem canal de rede, não dispara o evento (o cancelamento mandaria pacote a ele).
     */
    private static boolean riderMayBreak(PrehistoricCreature creature, ServerPlayer rider, BlockPos pos) {
        return creature.level().mayInteract(rider, pos)
                && (rider.connection.connection.channel() == null
                        || ForgeHooks.onBlockBreakEvent(creature.level(), rider.gameMode.getGameModeForPlayer(),
                                rider, pos) >= 0);
    }
}
