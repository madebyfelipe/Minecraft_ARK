package dev.madebyfelipe.iceagesurvival.client.tabula;

/**
 * Um passo de uma animação por poses: a pose ({@code .tbl} sem extensão) e quantos ticks leva para chegar nela a
 * partir da anterior. Na primeira pose de um laço, é o tempo de voltar da última para ela.
 */
public record PoseStep(String pose, float ticks) {
}
