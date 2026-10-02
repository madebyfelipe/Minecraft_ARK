package dev.madebyfelipe.iceagesurvival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.madebyfelipe.iceagesurvival.item.GunKinematics;
import dev.madebyfelipe.iceagesurvival.item.TranqGunItem;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Animação do rifle e da besta de dardos, no lugar do braço que balançava.
 *
 * <ul>
 *   <li>Terceira pessoa: os dois braços na pose de mira da besta carregada, com um coice que ergue os braços no tiro
 *   (o próprio jogador e, pelo traçante, quem atira de rifle por perto).</li>
 *   <li>Primeira pessoa: o coice (sobe e recua) e, durante a recarga, a arma abaixa e gira um pouco; volta a subir
 *   quando está pronta — é o sinal de que dá para atirar de novo. Curvas em {@link GunKinematics}.</li>
 * </ul>
 */
public final class GunClientExtensions implements IClientItemExtensions {
    public static final GunClientExtensions INSTANCE = new GunClientExtensions();

    /** Pose de mira com coice: a da besta carregada, com os braços erguidos pelo coice do tiro. */
    private static final HumanoidModel.ArmPose AIM = HumanoidModel.ArmPose.create("ICEAGESURVIVAL_GUN_AIM", true,
            GunClientExtensions::aim);

    /** Quanto o coice ergue os braços em terceira pessoa (radianos no auge). */
    private static final float THIRD_PERSON_KICK = 0.35F;
    /** Coice da primeira pessoa no auge: recuo, subida (blocos) e cano para cima (graus). */
    private static final float KICK_BACK = 0.10F;
    private static final float KICK_UP = 0.04F;
    private static final float KICK_PITCH = 14.0F;
    /** Arma abaixada na recarga: descida (blocos), cano para baixo e giro para dentro (graus). */
    private static final float RELOAD_DROP = 0.10F;
    private static final float RELOAD_PITCH = 18.0F;
    private static final float RELOAD_ROLL = 22.0F;
    /** Disparos mais antigos que isto são esquecidos: o coice já acabou. */
    private static final double SHOT_MEMORY_TICKS = 20.0;

    /** Tick (do mundo do cliente) do último disparo de cada entidade, para o coice. Lista curta, limpa sozinha. */
    private static final Int2DoubleOpenHashMap LAST_SHOT = new Int2DoubleOpenHashMap();
    /** Último disparo do jogador local por mão, para o coice da primeira pessoa. */
    private static final double[] LOCAL_SHOT = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};

    private GunClientExtensions() {
    }

    /** O jogador local atirou (chamado de {@link TranqGunItem#use} no cliente). */
    public static void onLocalShot(Player player, InteractionHand hand) {
        double now = now(0.0F);
        LOCAL_SHOT[hand.ordinal()] = now;
        remember(player.getId(), now);
    }

    /** Outro jogador atirou de rifle (soube-se pelo traçante). */
    public static void onRemoteShot(int shooterId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getId() == shooterId) {
            return; // o próprio jogador já começou o coice quando clicou
        }
        remember(shooterId, now(0.0F));
    }

    /** Ao sair do mundo: nada de disparo antigo no próximo. */
    public static void clear() {
        LAST_SHOT.clear();
        LOCAL_SHOT[0] = Double.NEGATIVE_INFINITY;
        LOCAL_SHOT[1] = Double.NEGATIVE_INFINITY;
    }

    private static void remember(int entityId, double now) {
        LAST_SHOT.int2DoubleEntrySet().removeIf(entry -> now - entry.getDoubleValue() > SHOT_MEMORY_TICKS);
        LAST_SHOT.put(entityId, now);
    }

    private static double now(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0.0 : minecraft.level.getGameTime() + partialTick;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return entity.isUsingItem() ? null : AIM;
    }

    /** A pose da besta carregada, mais o coice. {@code arm} é o braço que segura a arma. */
    private static void aim(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, right);
        double shot = LAST_SHOT.getOrDefault(entity.getId(), Double.NEGATIVE_INFINITY);
        float kick = (float) GunKinematics.kick(now(Minecraft.getInstance().getPartialTick()) - shot);
        if (kick > 0.0F) {
            model.rightArm.xRot -= kick * THIRD_PERSON_KICK;
            model.leftArm.xRot -= kick * THIRD_PERSON_KICK;
        }
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        int reload = stack.getItem() instanceof TranqGunItem gun ? gun.reloadTicks() : 0;
        float cooldown = player.getCooldowns().getCooldownPercent(stack.getItem(), partialTick);
        // Na recarga ignora o "re-equipar" que o vanilla dispara a cada uso: quem mexe a arma é a animação daqui.
        float equip = cooldown > 0.0F ? 0.0F : equipProcess;

        // O mesmo que o vanilla faz com um item comum (golpe com o botão esquerdo e posição na mão).
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

        // Coice e recarga, girando em torno da mão.
        InteractionHand hand = (arm == player.getMainArm()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        float kick = (float) GunKinematics.kick(now(partialTick) - LOCAL_SHOT[hand.ordinal()]);
        float lowered = cooldown > 0.0F ? (float) GunKinematics.lowered((1.0F - cooldown) * reload, reload) : 0.0F;
        pose.translate(0.0F, kick * KICK_UP - lowered * RELOAD_DROP, kick * KICK_BACK);
        pose.mulPose(Axis.XP.rotationDegrees(kick * KICK_PITCH - lowered * RELOAD_PITCH));
        pose.mulPose(Axis.ZP.rotationDegrees(side * lowered * RELOAD_ROLL));
        return true;
    }
}
