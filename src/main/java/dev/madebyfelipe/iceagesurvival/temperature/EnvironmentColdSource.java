package dev.madebyfelipe.iceagesurvival.temperature;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.temperature.ColdReading;
import dev.madebyfelipe.iceagesurvival.core.temperature.ColdTuning;
import dev.madebyfelipe.iceagesurvival.core.temperature.Coldness;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;

/** Mede o frio a partir do bioma, do céu, da roupa e das fontes de calor por perto. */
final class EnvironmentColdSource implements ColdSource {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @Override
    public double severity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        boolean sheltered = !level.canSeeSky(pos);
        ColdReading reading = new ColdReading(
                level.getBiome(pos).value().getBaseTemperature(),
                pos.getY() - level.getChunkSource().getGenerator().getSeaLevel(),
                level.isNight(),
                level.isRaining() && !sheltered,
                sheltered,
                heatProximity(level, pos, ServerConfig.COLD_HEAT_RADIUS.get()),
                insulatingPieces(player));
        return Coldness.severity(reading, tuning());
    }

    private static ColdTuning tuning() {
        return new ColdTuning(
                ServerConfig.COLD_ALTITUDE_DROP_PER_BLOCK.get(),
                ServerConfig.COLD_NIGHT_DROP.get(),
                ServerConfig.COLD_STORM_DROP.get(),
                ServerConfig.COLD_SHELTER_WARMTH.get(),
                ServerConfig.COLD_HEAT_WARMTH.get(),
                ServerConfig.COLD_INSULATION_PER_ARMOR_PIECE.get());
    }

    /**
     * 1 em cima da fonte de calor mais próxima, caindo a 0 na borda do raio. Todas as fontes esquentam
     * igual: uma tocha vale uma fogueira, porque uma tag não carrega intensidade.
     */
    private static double heatProximity(BlockGetter level, BlockPos center, int radius) {
        int nearestSqr = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distanceSqr = dx * dx + dy * dy + dz * dz;
                    if (distanceSqr >= nearestSqr) {
                        continue;
                    }
                    cursor.setWithOffset(center, dx, dy, dz);
                    if (level.getBlockState(cursor).is(ModTags.HEAT_SOURCES)) {
                        nearestSqr = distanceSqr;
                    }
                }
            }
        }
        if (nearestSqr == Integer.MAX_VALUE) {
            return 0.0;
        }
        return Math.max(0.0, 1.0 - Math.sqrt(nearestSqr) / radius);
    }

    private static int insulatingPieces(LivingEntity player) {
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).is(ModTags.INSULATING_ARMOR)) {
                pieces++;
            }
        }
        return pieces;
    }
}
