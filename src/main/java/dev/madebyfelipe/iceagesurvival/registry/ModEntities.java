package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.entity.PrehistoricCreature;
import dev.madebyfelipe.iceagesurvival.entity.TestCreature;
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

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TEST_CREATURE.get(), PrehistoricCreature.createBaseAttributes().build());
    }
}
