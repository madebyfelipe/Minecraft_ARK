package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.effect.VenomEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, IceAgeSurvival.MODID);

    /** Peçonha da Megalania. */
    public static final RegistryObject<MobEffect> VENOM = EFFECTS.register("venom", VenomEffect::new);
    /** Sangramento da mordida cortante do Giganotosaurus (D47). */
    public static final RegistryObject<MobEffect> BLEEDING = EFFECTS.register("bleeding",
            dev.madebyfelipe.iceagesurvival.effect.BleedingEffect::new);

    /** Perna quebrada pela clava do Anquilossauro: lenta e sem pular ({@code core/ecology/TailClub}). */
    public static final RegistryObject<MobEffect> BROKEN_LEG = EFFECTS.register("broken_leg",
            dev.madebyfelipe.iceagesurvival.effect.BrokenLegEffect::new);

    private ModEffects() {
    }
}
