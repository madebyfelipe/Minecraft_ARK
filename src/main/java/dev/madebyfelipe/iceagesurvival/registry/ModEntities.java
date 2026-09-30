package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IceAgeSurvival.MODID);

    private static final List<DeferredHolder<EntityType<?>, EntityType<LandCreature>>> LAND_CREATURE_LIST = new ArrayList<>();
    /** Todas as espécies terrestres, para registrar atributos, renderers e regras de spawn de uma vez. */
    public static final List<DeferredHolder<EntityType<?>, EntityType<LandCreature>>> LAND_CREATURES =
            Collections.unmodifiableList(LAND_CREATURE_LIST);

    // Espécie nova: uma linha aqui (id e caixa de colisão), um ovo em ModItems, o JSON em
    // data/.../species/ e os assets gerados por tools/gen_<especie>.py.
    public static final DeferredHolder<EntityType<?>, EntityType<LandCreature>> SMILODON = landCreature("smilodon", 1.3F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<LandCreature>> MAMMOTH = landCreature("mammoth", 2.0F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<LandCreature>> DIRE_WOLF = landCreature("dire_wolf", 0.8F, 1.2F);

    public static final DeferredHolder<EntityType<?>, EntityType<TestCreature>> TEST_CREATURE =
            ENTITY_TYPES.register("test_creature", () -> EntityType.Builder.of(TestCreature::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .build("test_creature"));

    public static final DeferredHolder<EntityType<?>, EntityType<TranqArrow>> TRANQ_ARROW =
            ENTITY_TYPES.register("tranq_arrow", () -> EntityType.Builder.<TranqArrow>of(TranqArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.13F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("tranq_arrow"));

    private ModEntities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<LandCreature>> landCreature(String id, float width, float height) {
        DeferredHolder<EntityType<?>, EntityType<LandCreature>> holder = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.<LandCreature>of(LandCreature::new, MobCategory.CREATURE)
                        .sized(width, height)
                        .clientTrackingRange(10)
                        .build(id));
        LAND_CREATURE_LIST.add(holder);
        return holder;
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TEST_CREATURE.get(), PrehistoricCreature.createBaseAttributes().build());
        for (var creature : LAND_CREATURES) {
            event.put(creature.get(), PrehistoricCreature.createBaseAttributes().build());
        }
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (var creature : LAND_CREATURES) {
            event.register(creature.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    PrehistoricCreature::checkSurfaceSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}
