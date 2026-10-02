package dev.madebyfelipe.iceagesurvival.temperature;

import dev.madebyfelipe.iceagesurvival.config.ServerConfig;
import dev.madebyfelipe.iceagesurvival.core.temperature.ColdReading;
import dev.madebyfelipe.iceagesurvival.core.temperature.ColdTuning;
import dev.madebyfelipe.iceagesurvival.core.temperature.Coldness;
import dev.madebyfelipe.iceagesurvival.registry.ModItems;
import dev.madebyfelipe.iceagesurvival.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.species.BodyProfile;
import dev.madebyfelipe.iceagesurvival.species.Species;

/** Mede o frio a partir do bioma, do céu, da roupa e das fontes de calor por perto. */
public final class EnvironmentColdSource implements ColdSource {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @Override
    public double severity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        boolean sheltered = !level.canSeeSky(pos);
        ColdReading reading = new ColdReading(
                biomeTemperature(level, pos),
                pos.getY() - level.getChunkSource().getGenerator().getSeaLevel(),
                level.isNight(),
                level.isRaining() && !sheltered,
                sheltered,
                heatProximity(level, pos, ServerConfig.COLD_HEAT_RADIUS.get()),
                insulation(player) + bodyHeat(player),
                player.isInWaterOrRain());
        return Coldness.severity(reading, tuning());
    }

    /**
     * A temperatura do bioma; num mundo do TerraFirmaCraft, a do clima dele (todo bioma do TFC diz 0,5 e o frio
     * de verdade vem da posição).
     */
    private static double biomeTemperature(ServerLevel level, BlockPos pos) {
        return dev.madebyfelipe.iceagesurvival.compat.tfc.TfcCompat.biomeTemperature(level, pos)
                .orElseGet(() -> level.getBiome(pos).value().getBaseTemperature());
    }

    private static ColdTuning tuning() {
        return new ColdTuning(
                ServerConfig.COLD_ALTITUDE_DROP_PER_BLOCK.get(),
                ServerConfig.COLD_NIGHT_DROP.get(),
                ServerConfig.COLD_STORM_DROP.get(),
                ServerConfig.COLD_SHELTER_WARMTH.get(),
                ServerConfig.COLD_HEAT_WARMTH.get(),
                ServerConfig.COLD_WET_DROP.get());
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

    /** Maior alcance de calor do corpo que uma espécie pode declarar ({@code body.body_heat_radius}). */
    private static final double MAX_BODY_HEAT_RADIUS = 16.0;

    /**
     * Calor do corpo de criaturas (o Elasmotério): montado, o calor inteiro da montaria; a pé, o da
     * criatura mais quente por perto, caindo com a distância. Não soma várias — encostar em três
     * não aquece três vezes.
     */
    public static double bodyHeat(LivingEntity player) {
        double best = 0.0;
        if (player.getVehicle() instanceof PrehistoricCreature mount) {
            best = mount.species().flatMap(Species::body).map(BodyProfile::bodyHeat).orElse(0.0);
        }
        var nearby = player.level().getEntitiesOfClass(PrehistoricCreature.class,
                player.getBoundingBox().inflate(MAX_BODY_HEAT_RADIUS), PrehistoricCreature::isAlive);
        for (PrehistoricCreature creature : nearby) {
            BodyProfile body = creature.species().flatMap(Species::body).orElse(null);
            if (body == null || body.bodyHeat() <= 0.0) {
                continue;
            }
            double distance = Math.sqrt(distanceSqr(creature.getBoundingBox(), player.position()));
            best = Math.max(best, Coldness.bodyHeat(body.bodyHeat(), distance, body.bodyHeatRadius()));
        }
        return best;
    }

    private static double distanceSqr(AABB box, Vec3 point) {
        double dx = Math.max(Math.max(box.minX - point.x, point.x - box.maxX), 0.0);
        double dy = Math.max(Math.max(box.minY - point.y, point.y - box.maxY), 0.0);
        double dz = Math.max(Math.max(box.minZ - point.z, point.z - box.maxZ), 0.0);
        return dx * dx + dy * dy + dz * dz;
    }

    /** Soma do isolamento do couro vanilla e das roupas de pena e de pele do mod. */
    public static double insulation(LivingEntity player) {
        double total = 0.0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += insulationValue(player.getItemBySlot(slot).getItem());
        }
        return total;
    }

    private static double insulationValue(Item item) {
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE
                || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) {
            return 0.2;
        }
        if (item == ModItems.FUR_HELMET.get() || item == ModItems.FUR_CHESTPLATE.get()
                || item == ModItems.FUR_LEGGINGS.get() || item == ModItems.FUR_BOOTS.get()) {
            return 0.35;
        }
        // Pena: completa (1,0) segura o frio do dia nos biomas nevados, mas não o da noite (§15). O
        // casaco cobre o tronco e sozinho já vale o dobro de cada uma das outras peças.
        if (item == ModItems.FEATHER_CHESTPLATE.get()) {
            return 0.4;
        }
        if (item == ModItems.FEATHER_HELMET.get() || item == ModItems.FEATHER_LEGGINGS.get()
                || item == ModItems.FEATHER_BOOTS.get()) {
            return 0.2;
        }
        return 0.0;
    }
}
