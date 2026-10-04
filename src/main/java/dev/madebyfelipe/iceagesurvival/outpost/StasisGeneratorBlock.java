package dev.madebyfelipe.iceagesurvival.outpost;

import net.minecraft.world.level.block.Block;

/**
 * Gerador do campo de êxtase da base. Enquanto houver um de pé perto do {@link ContainmentCoreBlock núcleo}, o campo
 * segura o que está dentro dele; derrubar todos é o único jeito de libertar (e de ferir) o que está preso. Duro de
 * quebrar e sem drop.
 */
public class StasisGeneratorBlock extends Block {
    public StasisGeneratorBlock(Properties properties) {
        super(properties);
    }
}
