package dev.madebyfelipe.iceagesurvival.client.tabula;

import java.util.List;

/**
 * Um cubo de um modelo Tabula ({@code model.json} do {@code .tbl}, projVersion 4), na convenção do ModelRenderer
 * antigo do vanilla: {@code position} é o ponto de rotação (relativo ao do pai), {@code offset} é o canto da caixa
 * relativo a esse ponto, {@code rotation} em graus e {@code mcScale} é o quanto a caixa infla. A escala por cubo
 * do Tabula ({@code scale}) não entra: o Jurassic Reborn também a ignora.
 */
public record TabulaCube(
        String name,
        String identifier,
        float[] dimensions,
        float[] position,
        float[] offset,
        float[] rotation,
        int textureU,
        int textureV,
        boolean mirror,
        float inflate,
        boolean hidden,
        List<TabulaCube> children) {
}
