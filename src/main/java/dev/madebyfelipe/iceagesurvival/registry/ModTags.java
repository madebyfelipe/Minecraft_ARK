package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tags do mod referenciadas em código. As tags de espécie vêm dos JSONs e não passam por aqui. */
public final class ModTags {
    /** Blocos que aquecem um jogador por perto. */
    /** Vegetação e neve que os grandes animais atravessam quebrando ({@code body.plow_hardness}). */
    public static final TagKey<Block> PLOWABLE =
            TagKey.create(Registries.BLOCK, IceAgeSurvival.id("plowable"));
    public static final TagKey<Block> HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, IceAgeSurvival.id("heat_sources"));

    /** Peças de armadura que protegem do frio. */
    public static final TagKey<Item> INSULATING_ARMOR =
            TagKey.create(Registries.ITEM, IceAgeSurvival.id("insulating_armor"));

    private ModTags() {
    }
}
