package dev.madebyfelipe.iceagesurvival.entity;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Teto da vida máxima. O vanilla corta {@code generic.max_health} em 1024, e com isso as criaturas grandes de nível
 * alto empatavam todas no mesmo valor (o Triceratops alcançava o Brontossauro). Aqui o teto sobe para {@link #MAX}.
 *
 * <p>Sem mixin (o projeto não gera refmap): o campo final {@code maxValue} do {@link RangedAttribute} é trocado por
 * reflexão, uma vez, na construção do mod.
 */
public final class HealthCap {
    public static final double MAX = 100_000.0;
    /** {@code RangedAttribute.maxValue} em SRG. */
    private static final String MAX_VALUE_FIELD = "f_22308_";

    private HealthCap() {
    }

    public static void raise() {
        if (!(Attributes.MAX_HEALTH instanceof RangedAttribute attribute)) {
            return;
        }
        try {
            Field field = ObfuscationReflectionHelper.findField(RangedAttribute.class, MAX_VALUE_FIELD);
            field.setDouble(attribute, Math.max(MAX, attribute.getMaxValue()));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LogUtils.getLogger().warn("Não foi possível subir o teto de vida máxima; fica o do vanilla", exception);
        }
    }
}
