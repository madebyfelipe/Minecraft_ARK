package dev.madebyfelipe.iceagesurvival.mixin.tfc;

import dev.madebyfelipe.iceagesurvival.compat.tfc.TfcCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Peso dos biomas primitivos no mundo padrão do TerraFirmaCraft: o {@code ChooseBiomes} sorteia de listas fixas no
 * código dele, e aqui a lista entra no sorteio com os primitivos repetidos ({@link TfcCompat#weighChoices}). Só é
 * aplicado com o TFC instalado ({@code IceAgeMixinPlugin}); classe do TFC, sem ofuscação ({@code remap = false}).
 */
@Mixin(targets = "net.dries007.tfc.world.region.ChooseBiomes", remap = false)
public abstract class ChooseBiomesMixin {
    @ModifyVariable(method = "randomSeededFrom", at = @At("HEAD"), argsOnly = true, remap = false)
    private int[] iceagesurvival$weighPrimitiveBiomes(int[] choices) {
        return TfcCompat.weighChoices(choices);
    }
}
