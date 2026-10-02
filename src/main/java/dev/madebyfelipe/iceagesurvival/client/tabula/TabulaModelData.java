package dev.madebyfelipe.iceagesurvival.client.tabula;

import java.util.List;

/** Um modelo Tabula lido: tamanho da textura e os cubos raiz (cada um com a sua árvore). */
public record TabulaModelData(int textureWidth, int textureHeight, List<TabulaCube> roots) {
}
