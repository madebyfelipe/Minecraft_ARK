package dev.madebyfelipe.iceagesurvival.client.tabula;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Referências aos modelos Tabula, poses e texturas que o Jurassic Reborn fornece em runtime, no espírito do
 * {@link dev.madebyfelipe.iceagesurvival.entity.CreatureAppearance} (que é do Revival). Nada disso é copiado
 * para o projeto: tudo é lido do namespace {@code jurassicreborn} do mod instalado.
 *
 * <p>Uma espécie nova do Jurassic Reborn é uma linha em {@link #SPECIES}: o id da nossa entidade, o nome da espécie
 * lá, a fase de crescimento cujo modelo usar, a variante de cor, as partes de pescoço e cabeça (de trás para a
 * frente) que acompanham o olhar, as partes que respiram e quanto o corpo anda numa volta da animação de andar.
 *
 * @param species      nome da espécie no Jurassic Reborn ({@code models/entities/<species>/...})
 * @param stage        fase de crescimento do modelo ({@code adult}); os filhotes usam o mesmo, em escala menor
 * @param variant      variante da textura ({@code green} é a natural)
 * @param headChain    nomes dos cubos do pescoço à cabeça, que somam o olhar
 * @param breathing    nomes dos cubos do tronco que oscilam na respiração
 * @param strideBlocks quanto o corpo avança numa volta de WALKING/RUNNING, em blocos, com o modelo na escala 1;
 *                     acerta o ritmo da passada pela velocidade (0 = ritmo fixo do arquivo)
 */
public record JurassicRebornAppearance(
        String species,
        String stage,
        String variant,
        List<String> headChain,
        List<String> breathing,
        float strideBlocks) {
    public static final String NAMESPACE = "jurassicreborn";

    /**
     * O Baryonyx: na passada de WALKING o pé de apoio recua 36 px em 60% da volta, então o corpo anda ~60 px
     * (3,75 blocos) por volta na escala 1. RUNNING usa as mesmas poses, só mais rápidas.
     */
    private static final Map<String, JurassicRebornAppearance> SPECIES = Map.of(
            "baryonyx", new JurassicRebornAppearance("baryonyx", "adult", "green",
                    List.of("Neck1", "Neck2", "Neck3", "Neck4", "Neck5", "Neck6", "Head"),
                    List.of("Body 2", "Body 3"),
                    3.75F),
            // O Giganotosaurus: a pele "sand" (areia), a natural das três do Jurassic Reborn. A
            // perna é ~2× a do Baryonyx (85 px contra 40), então a passada é ~2× maior.
            "giganotosaurus", new JurassicRebornAppearance("giganotosaurus", "adult", "sand",
                    List.of("Neck", "Neck2", "bone", "Head"),
                    List.of("Chest1_r1", "Shoulders_r1"),
                    7.5F));

    /** Aparência do Jurassic Reborn da nossa entidade, se ela usar um modelo de lá. */
    public static Optional<JurassicRebornAppearance> forEntity(ResourceLocation entityId) {
        return Optional.ofNullable(SPECIES.get(entityId.getPath()));
    }

    /** Ids (caminho) das nossas entidades desenhadas com modelos do Jurassic Reborn. */
    public static Iterable<String> entityPaths() {
        return SPECIES.keySet();
    }

    private String folder() {
        return "models/entities/" + species + "/" + stage + "/";
    }

    /** O JSON de poses da fase, ex. {@code models/entities/baryonyx/adult/baryonyx_adult.json}. */
    public ResourceLocation posesResource() {
        return resource(folder() + species + "_" + stage + ".json");
    }

    /** Uma pose ({@code .tbl}) pelo nome que aparece no JSON de poses. */
    public ResourceLocation poseResource(String pose) {
        return resource(folder() + pose + ".tbl");
    }

    public ResourceLocation textureResource(boolean female) {
        return resource(texturePrefix(female) + "_" + variant + ".png");
    }

    /** Pálpebras fechadas, desenhadas por cima da pele quando a criatura dorme, desmaia, morre ou pisca. */
    public ResourceLocation eyelidResource(boolean female) {
        return resource(texturePrefix(female) + "_eyelid.png");
    }

    private String texturePrefix(boolean female) {
        return "textures/entities/" + species + "/" + species + "_" + (female ? "female" : "male") + "_" + stage;
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
