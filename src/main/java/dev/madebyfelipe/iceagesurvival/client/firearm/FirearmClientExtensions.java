package dev.madebyfelipe.iceagesurvival.client.firearm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmItem;
import dev.madebyfelipe.iceagesurvival.item.GunKinematics;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * As armas de fogo na mão (D58): o modelo GeckoLib, a pose de mira com os dois braços (terceira pessoa) e, na primeira
 * pessoa, o coice por arma e a arma inclinada durante a recarga (a animação da arma tira e põe o pente).
 */
public final class FirearmClientExtensions implements IClientItemExtensions {
    /** Pose de mira: a da besta carregada, com os braços erguidos pelo coice do tiro. */
    private static final HumanoidModel.ArmPose AIM = HumanoidModel.ArmPose.create("ICEAGESURVIVAL_FIREARM_AIM", true,
            FirearmClientExtensions::aim);
    /** Inclinação da recarga: a arma vira de lado e sobe um pouco, mostrando o encaixe do pente. */
    private static final float RELOAD_ROLL = 28.0F;
    private static final float RELOAD_PITCH = 12.0F;
    private static final float RELOAD_LIFT = 0.04F;

    private final FirearmItem item;
    private FirearmRenderer renderer;

    /** Criada no construtor do item, antes da arma estar definida: tudo daqui lê a arma só na hora de desenhar. */
    public FirearmClientExtensions(FirearmItem item) {
        this.item = item;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new FirearmRenderer(item.gun());
        }
        return renderer;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return AIM;
    }

    private static void aim(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, arm == HumanoidArm.RIGHT);
        float kick = FirearmsClient.thirdPersonKick(entity);
        if (kick > 0.0F) {
            model.rightArm.xRot -= kick;
            model.leftArm.xRot -= kick;
        }
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        Firearm gun = item.gun();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        double reload = FirearmsClient.reloadProgress(player, hand, partialTick);
        float equip = player.getCooldowns().isOnCooldown(item) ? 0.0F : equipProcess;

        // O que o vanilla faz com um item comum (golpe com o botão esquerdo e a posição na mão).
        float swingRoot = Mth.sqrt(swingProcess);
        pose.translate(side * -0.4F * Mth.sin(swingRoot * Mth.PI), 0.2F * Mth.sin(swingRoot * Mth.TWO_PI),
                -0.2F * Mth.sin(swingProcess * Mth.PI));
        pose.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        float swingCurve = Mth.sin(swingProcess * swingProcess * Mth.PI);
        pose.mulPose(Axis.YP.rotationDegrees(side * (45.0F + swingCurve * -20.0F)));
        float swingArc = Mth.sin(swingRoot * Mth.PI);
        pose.mulPose(Axis.ZP.rotationDegrees(side * swingArc * -20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(swingArc * -80.0F));
        pose.mulPose(Axis.YP.rotationDegrees(side * -45.0F));

        // Coice: recuo, subida e cano para cima, na medida da arma.
        float kick = (float) FirearmsClient.firstPersonKick(hand, partialTick);
        FirearmsClient.Recoil recoil = FirearmsClient.recoil(gun);
        pose.translate(0.0F, kick * recoil.up(), kick * recoil.back());
        pose.mulPose(Axis.XP.rotationDegrees(kick * recoil.pitch()));
        // Recarga: a arma inclina e sobe enquanto a animação troca o pente.
        float tilt = reload >= 0.0 ? (float) GunKinematics.lowered(reload * gun.reloadTicks(), gun.reloadTicks()) : 0.0F;
        pose.translate(0.0F, tilt * RELOAD_LIFT, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(side * tilt * RELOAD_ROLL));
        pose.mulPose(Axis.XP.rotationDegrees(tilt * RELOAD_PITCH));
        return true;
    }
}
