package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.LandCreature;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IceAgeSurvival.MODID);

    private static final List<RegistryObject<EntityType<LandCreature>>> LAND_CREATURE_LIST = new ArrayList<>();
    /** Todas as espécies terrestres, para registrar atributos, renderers e regras de spawn de uma vez. */
    public static final List<RegistryObject<EntityType<LandCreature>>> LAND_CREATURES =
            Collections.unmodifiableList(LAND_CREATURE_LIST);

    // Espécie nova: uma linha aqui (id e caixa de colisão), um ovo em ModItems, o JSON em
    // data/.../species/ e os assets gerados por tools/gen_<especie>.py.
    public static final RegistryObject<EntityType<LandCreature>> SMILODON = landCreature("smilodon", 1.3F, 2.3F);
    public static final RegistryObject<EntityType<LandCreature>> MAMMOTH = landCreature("mammoth", 3.0F, 4.65F);
    public static final RegistryObject<EntityType<LandCreature>> DIRE_WOLF = landCreature("dire_wolf", 0.8F, 1.2F);
    public static final RegistryObject<EntityType<LandCreature>> TYRANNOSAURUS = landCreature("tyrannosaurus", 2.7F, 5.4F);
    public static final RegistryObject<EntityType<LandCreature>> VELOCIRAPTOR = landCreature("velociraptor", 0.7F, 1.1F);
    public static final RegistryObject<EntityType<LandCreature>> UTAHRAPTOR = landCreature("utahraptor", 1.2F, 2.3F);
    public static final RegistryObject<EntityType<LandCreature>> SPINOSAURUS = landCreature("spinosaurus", 2.7F, 5.6F);
    public static final RegistryObject<EntityType<LandCreature>> ALLOSAURUS = landCreature("allosaurus", 1.8F, 3.4F);
    public static final RegistryObject<EntityType<LandCreature>> BRONTOSAURUS = landCreature("brontosaurus", 4.0F, 8.0F);
    public static final RegistryObject<EntityType<LandCreature>> STEGOSAURUS = landCreature("stegosaurus", 2.2F, 3.2F);
    public static final RegistryObject<EntityType<LandCreature>> PTERANODON = landCreature("pteranodon", 1.6F, 2.0F);
    public static final RegistryObject<EntityType<LandCreature>> DIREBEAR = landCreature("direbear", 2.0F, 3.0F);
    public static final RegistryObject<EntityType<LandCreature>> DODO = landCreature("dodo", 0.7F, 0.9F);
    public static final RegistryObject<EntityType<LandCreature>> GALLIMIMUS = landCreature("gallimimus", 1.2F, 2.3F);
    public static final RegistryObject<EntityType<LandCreature>> TRICERATOPS = landCreature("triceratops", 2.4F, 2.8F);
    public static final RegistryObject<EntityType<LandCreature>> ELASMOTHERIUM = landCreature("elasmotherium", 1.8F, 2.4F);

    public static final RegistryObject<EntityType<TestCreature>> TEST_CREATURE =
            ENTITY_TYPES.register("test_creature", () -> EntityType.Builder.of(TestCreature::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .build("test_creature"));

    public static final RegistryObject<EntityType<TranqArrow>> TRANQ_ARROW =
            ENTITY_TYPES.register("tranq_arrow", () -> EntityType.Builder.<TranqArrow>of(TranqArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("tranq_arrow"));

    private ModEntities() {
    }

    private static RegistryObject<EntityType<LandCreature>> landCreature(String id, float width, float height) {
        RegistryObject<EntityType<LandCreature>> holder = ENTITY_TYPES.register(id,
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

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        for (var creature : LAND_CREATURES) {
            event.register(creature.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    PrehistoricCreature::checkSurfaceSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}
