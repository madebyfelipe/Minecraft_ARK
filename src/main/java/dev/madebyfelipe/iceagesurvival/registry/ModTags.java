package dev.madebyfelipe.iceagesurvival.registry;

import dev.madebyfelipe.iceagesurvival.IceAgeSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Tags do mod referenciadas em código. As tags de espécie vêm dos JSONs e não passam por aqui. */
public final class ModTags {
    /** Vegetação e neve que os grandes animais atravessam quebrando ({@code body.plow_hardness}). */
    public static final TagKey<Block> PLOWABLE =
            TagKey.create(Registries.BLOCK, IceAgeSurvival.id("plowable"));

    /** Blocos que aquecem um jogador por perto. */
    public static final TagKey<Block> HEAT_SOURCES =
            TagKey.create(Registries.BLOCK, IceAgeSurvival.id("heat_sources"));

    /**
     * Espécies desligadas: não nascem, não têm ovo na aba criativa e a selvagem que existir num mundo
     * some ao carregar. A entidade segue registrada para mundos antigos e as domesticadas ficam.
     */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> DISABLED =
            TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("disabled"));

    /** Apex: só se doma vencendo o desafio, depois de trazer a cabeça de outro da espécie. */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> APEX =
            TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("apex"));

    /** Esconderijo da camuflagem: folhas, mato, arbustos, neve fofa ({@code behavior.habits.camouflage}). */
    public static final TagKey<Block> UNDERGROWTH =
            TagKey.create(Registries.BLOCK, IceAgeSurvival.id("undergrowth"));

    /** Carne crua que o necrófago come do chão ({@code behavior.habits.scavenges}). */
    public static final TagKey<net.minecraft.world.item.Item> CARRION =
            TagKey.create(Registries.ITEM, IceAgeSurvival.id("carrion"));

    /** Predadores: o sentinela avisa deles, e o necrófago não chega perto da carne com um deles ali. */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> PREDATORS =
            TagKey.create(Registries.ENTITY_TYPE, IceAgeSurvival.id("predators"));

    private ModTags() {
    }
}
