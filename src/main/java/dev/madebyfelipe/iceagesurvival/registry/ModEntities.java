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
    public static final RegistryObject<EntityType<LandCreature>> VELOCIRAPTOR = landCreature("velociraptor", 0.6F, 0.9F);
    public static final RegistryObject<EntityType<LandCreature>> UTAHRAPTOR = landCreature("utahraptor", 1.2F, 2.3F);
    public static final RegistryObject<EntityType<LandCreature>> SPINOSAURUS = landCreature("spinosaurus", 2.7F, 5.6F);
    public static final RegistryObject<EntityType<LandCreature>> ALLOSAURUS = landCreature("allosaurus", 1.8F, 3.4F);
    public static final RegistryObject<EntityType<LandCreature>> BRONTOSAURUS = landCreature("brontosaurus", 4.0F, 8.0F);
    public static final RegistryObject<EntityType<LandCreature>> STEGOSAURUS = landCreature("stegosaurus", 4.35F, 5.45F);
    public static final RegistryObject<EntityType<LandCreature>> PTERANODON = landCreature("pteranodon", 1.6F, 2.0F);
    public static final RegistryObject<EntityType<LandCreature>> DIREBEAR = landCreature("direbear", 2.0F, 3.0F);
    public static final RegistryObject<EntityType<LandCreature>> DODO = landCreature("dodo", 0.7F, 0.9F);
    public static final RegistryObject<EntityType<LandCreature>> GALLIMIMUS = landCreature("gallimimus", 1.2F, 2.3F);
    public static final RegistryObject<EntityType<LandCreature>> TRICERATOPS = landCreature("triceratops", 5.2F, 5.5F);
    public static final RegistryObject<EntityType<LandCreature>> ELASMOTHERIUM = landCreature("elasmotherium", 1.8F, 2.4F);
    /** Kelenken, a maior ave-terrível: 2,5–3 m de altura. */
    public static final RegistryObject<EntityType<LandCreature>> KELENKEN = landCreature("kelenken", 1.8F, 4.0F);
    /** Ornitholestes, pequeno terópode do sub-bosque: ~2 m de comprimento e 15 kg, do porte do Velociraptor. */
    public static final RegistryObject<EntityType<LandCreature>> ORNITHOLESTES = landCreature("ornitholestes", 0.7F, 1.1F);
    /**
     * Baryonyx, espinossaurídeo pescador do Cretáceo Inferior: 7,5–10 m, 1,2–2 t, quadril a 2,5 m. Modelo, poses e sons
     * do Jurassic Reborn em runtime.
     */
    public static final RegistryObject<EntityType<LandCreature>> BARYONYX = landCreature("baryonyx", 1.8F, 2.7F);
    /**
     * Quetzalcoatlus, o maior pterossauro: 10–11 m de envergadura, ~220 kg, de pé da altura de uma girafa. Modelo e
     * animações do Revival em runtime, na escala 3,6. A caixa fica menor que o desenho: ela dá o porte das regras de
     * ecologia, e um porte maior que o do T-Rex não condiz com os ~220 kg do animal.
     */
    public static final RegistryObject<EntityType<LandCreature>> QUETZALCOATLUS =
            landCreature("quetzalcoatlus", 2.2F, 4.5F);
    /**
     * Megalania (Varanus priscus), o lagarto-monitor gigante do Pleistoceno australiano: 3,5–5,5 m, até ~600 kg, baixo
     * e comprido. Modelo, animações e sons do Revival em runtime.
     */
    public static final RegistryObject<EntityType<LandCreature>> MEGALANIA = landCreature("megalania", 1.5F, 1.1F);
    /**
     * Giganotosaurus, o boss da arena da caverna (D47): 12–13 m, ~8 t, um pouco mais comprido e mais leve que o T-Rex.
     * Modelo, poses e sons do Jurassic Reborn em runtime (na escala 1, ~14 blocos de comprimento e 4,5 de altura). Não
     * nasce sozinho: só o altar da arena o chama. Com 4,5 de altura não passa pela porta de 4 da arena.
     */
    public static final RegistryObject<EntityType<LandCreature>> GIGANOTOSAURUS = landCreature("giganotosaurus",
            3.0F, 4.5F, dev.madebyfelipe.iceagesurvival.entity.GiganotosaurusBoss::new);

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
        return landCreature(id, width, height, LandCreature::new);
    }

    /** Espécie com classe própria (o boss), ainda na lista das terrestres. */
    private static RegistryObject<EntityType<LandCreature>> landCreature(String id, float width, float height,
                                                                         EntityType.EntityFactory<LandCreature> factory) {
        RegistryObject<EntityType<LandCreature>> holder = ENTITY_TYPES.register(id,
                () -> EntityType.Builder.<LandCreature>of(factory, MobCategory.CREATURE)
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
