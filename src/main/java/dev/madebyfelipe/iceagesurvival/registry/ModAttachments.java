package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import dev.madebyfelipe.iceagesurvival.temperature.ColdState;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Dados do mod anexados a entidades que não são nossas. Criaturas do mod guardam o estado no
 * próprio NBT (D8); o jogador é de terceiros, e é para isso que os attachments existem.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, IceAgeSurvival.MODID);

    /** Frio acumulado pelo jogador. Não acompanha a morte: renascer aquece. */
    public static final Supplier<AttachmentType<ColdState>> COLD = ATTACHMENT_TYPES.register(
            "cold",
            () -> AttachmentType.builder(ColdState::new)
                    .serialize(ColdState.CODEC, state -> state.exposure() > 0)
                    .build());

    private ModAttachments() {
    }
}
