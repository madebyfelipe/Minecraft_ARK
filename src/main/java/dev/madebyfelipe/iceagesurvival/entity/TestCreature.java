package dev.madebyfelipe.iceagesurvival.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Criatura provisória, sem assets próprios, usada para validar o framework
 * antes de existir uma espécie real. Remover quando o Smilodon a substituir.
 */
public class TestCreature extends PrehistoricCreature {
    public TestCreature(EntityType<? extends TestCreature> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
    }
}
