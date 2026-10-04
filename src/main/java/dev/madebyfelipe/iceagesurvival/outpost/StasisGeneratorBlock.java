package dev.madebyfelipe.iceagesurvival.outpost;

import net.minecraft.world.level.block.Block;

/**
 * Emissor do campo de êxtase da base (o id continua {@code stasis_generator}, das bases já geradas): projeta o campo
 * do {@link ContainmentCoreBlock núcleo}, que o cliente desenha com um feixe saindo de cada emissor. Indestrutível e
 * sem drop; quem desliga o campo é o {@link ContainmentConsoleBlock console}. Se todos sumirem mesmo assim (modo
 * criativo), o campo cai.
 */
public class StasisGeneratorBlock extends Block {
    public StasisGeneratorBlock(Properties properties) {
        super(properties);
    }
}
