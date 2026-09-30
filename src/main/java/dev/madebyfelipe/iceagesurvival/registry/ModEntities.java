package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.Smilodon;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
import dev.madebyfelipe.iceagesurvival.entity.TranqArrow;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IceAgeSurvival.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TestCreature>> TEST_CREATURE =
            ENTITY_TYPES.register("test_creature", () -> EntityType.Builder.of(TestCreature::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .build("test_creature"));

    public static final DeferredHolder<EntityType<?>, EntityType<Smilodon>> SMILODON =
            ENTITY_TYPES.register("smilodon", () -> EntityType.Builder.of(Smilodon::new, MobCategory.CREATURE)
                    .sized(1.4F, 1.8F)
                    .clientTrackingRange(10)
                    .build("smilodon"));

    public static final DeferredHolder<EntityType<?>, EntityType<TranqArrow>> TRANQ_ARROW =
            ENTITY_TYPES.register("tranq_arrow", () -> EntityType.Builder.<TranqArrow>of(TranqArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.13F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("tranq_arrow"));

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TEST_CREATURE.get(), PrehistoricCreature.createBaseAttributes().build());
        event.put(SMILODON.get(), PrehistoricCreature.createBaseAttributes().build());
    }
}
