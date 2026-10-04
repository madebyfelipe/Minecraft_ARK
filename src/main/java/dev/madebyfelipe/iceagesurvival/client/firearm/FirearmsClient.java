package dev.madebyfelipe.iceagesurvival.client.firearm;

import com.mojang.blaze3d.platform.InputConstants;
import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.firearm.FirearmItem;
import dev.madebyfelipe.iceagesurvival.firearm.Firearms;
import dev.madebyfelipe.iceagesurvival.item.GunKinematics;
import dev.madebyfelipe.iceagesurvival.network.FirearmReloadPayload;
import dev.madebyfelipe.iceagesurvival.network.FirearmShotPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import dev.madebyfelipe.iceagesurvival.network.ReloadRequestPayload;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;

/**
 * O lado do cliente das armas de fogo (D58): a tecla de recarga, o contador de munição, o coice (na mão e na câmera),
 * a animação do tiro e da recarga nos modelos, os traçantes e a esfera do canhão. Chamado por
 * {@code IceAgeSurvivalClient#init}.
 */
public final class FirearmsClient {
    /** B: o R é do Analisador (D56). */
    private static final KeyMapping RELOAD = new KeyMapping("key.iceagesurvival.reload", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, "key.categories.iceagesurvival");

    /** Coice na primeira pessoa, por arma: recuo e subida (blocos) e cano para cima (graus); e o chute da câmera. */
    public record Recoil(float back, float up, float pitch, float camera, float thirdPerson) {
    }

    /** Disparos mais antigos que isto são esquecidos: o coice já acabou. */
    private static final double SHOT_MEMORY_TICKS = 20.0;
    /** Último tiro de cada entidade (tick do mundo do cliente), para o coice em terceira pessoa. */
    private static final Int2DoubleOpenHashMap LAST_SHOT = new Int2DoubleOpenHashMap();
    /** Último tiro do jogador local por mão. */
    private static final double[] LOCAL_SHOT = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
    /** Recarga em curso de cada entidade: quando começou, quanto dura e em que mão. */
    private static final Int2ObjectOpenHashMap<Reload> RELOADS = new Int2ObjectOpenHashMap<>();

    private record Reload(double start, int ticks, boolean mainHand) {
    }

    private FirearmsClient() {
    }

    public static void init(IEventBus modEventBus) {
        FirearmItem.setClientShotHook(FirearmsClient::onLocalShot);
        FirearmShotPayload.setClientHandler(FirearmsClient::onShot);
        FirearmReloadPayload.setClientHandler(FirearmsClient::onReload);
        modEventBus.addListener(FirearmsClient::registerKeys);
        modEventBus.addListener(FirearmsClient::registerOverlays);
        modEventBus.addListener(FirearmsClient::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(FirearmsClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(FirearmsClient::onMovementInput);
        MinecraftForge.EVENT_BUS.addListener(FirearmsClient::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(FirearmTracers::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(FirearmTracers::onRenderLevel);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "firearm_ammo", FirearmsClient::renderAmmo);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Firearms.SOLID_CANNON_SHOT.get(), SolidCannonShotRenderer::new);
    }

    public static Recoil recoil(Firearm gun) {
        return switch (gun) {
            case HANDGUN -> new Recoil(0.08F, 0.03F, 12.0F, 1.6F, 0.30F);
            case SHOTGUN -> new Recoil(0.16F, 0.05F, 20.0F, 4.5F, 0.50F);
            case SUBMACHINE_GUN -> new Recoil(0.04F, 0.01F, 4.0F, 0.7F, 0.12F);
            case HEAVY_MACHINE_GUN -> new Recoil(0.06F, 0.015F, 5.0F, 1.0F, 0.16F);
            case SOLID_CANNON -> new Recoil(0.14F, 0.05F, 14.0F, 3.0F, 0.40F);
            case ANTI_TANK_RIFLE -> new Recoil(0.26F, 0.08F, 26.0F, 8.0F, 0.70F);
        };
    }

    // ---- tiros ----

    /** O jogador local atirou: coice, chute da câmera e a animação, sem esperar o servidor. */
    private static void onLocalShot(Player player, InteractionHand hand, ItemStack stack, Firearm gun) {
        double now = now(0.0F);
        LOCAL_SHOT[hand.ordinal()] = now;
        remember(player.getId(), now);
        Recoil recoil = recoil(gun);
        player.setXRot(player.getXRot() - recoil.camera());
        player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * recoil.camera() * 0.5F);
        trigger(player, stack, "fire");
    }

    /** Chegou um tiro: traçantes e, se foi outro jogador, o coice e a animação na arma dele. */
    private static void onShot(FirearmShotPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        boolean self = minecraft.player != null && minecraft.player.getId() == payload.shooterId();
        FirearmTracers.receive(payload, self);
        if (!self) {
            remember(payload.shooterId(), now(0.0F));
            Entity shooter = minecraft.level.getEntity(payload.shooterId());
            if (shooter instanceof LivingEntity living) {
                trigger(living, living.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND), "fire");
            }
        }
    }

    /** Alguém começou a recarregar: a animação na arma e, para o próprio jogador, a inclinação da primeira pessoa. */
    private static void onReload(FirearmReloadPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        RELOADS.put(payload.shooterId(), new Reload(now(0.0F), payload.gun().reloadTicks(), payload.mainHand()));
        Entity shooter = minecraft.level.getEntity(payload.shooterId());
        if (shooter instanceof LivingEntity living) {
            trigger(living, living.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND), "reload");
        }
    }

    private static void trigger(LivingEntity holder, ItemStack stack, String animation) {
        if (stack.getItem() instanceof FirearmItem item) {
            long id = GeoItem.getId(stack);
            if (id != Long.MAX_VALUE) {
                item.triggerAnim(holder, id, FirearmItem.CONTROLLER, animation);
            }
        }
    }

    private static void remember(int entityId, double now) {
        LAST_SHOT.int2DoubleEntrySet().removeIf(entry -> now - entry.getDoubleValue() > SHOT_MEMORY_TICKS);
        LAST_SHOT.put(entityId, now);
    }

    private static double now(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0.0 : minecraft.level.getGameTime() + partialTick;
    }

    /** O coice dos braços em terceira pessoa, em radianos. */
    static float thirdPersonKick(LivingEntity entity) {
        double shot = LAST_SHOT.getOrDefault(entity.getId(), Double.NEGATIVE_INFINITY);
        double kick = GunKinematics.kick(now(Minecraft.getInstance().getPartialTick()) - shot);
        if (kick <= 0.0) {
            return 0.0F;
        }
        Firearm gun = entity.getMainHandItem().getItem() instanceof FirearmItem item ? item.gun()
                : entity.getOffhandItem().getItem() instanceof FirearmItem item ? item.gun() : Firearm.HANDGUN;
        return (float) kick * recoil(gun).thirdPerson();
    }

    /** O coice da primeira pessoa nesta mão, de 0 a 1. */
    static double firstPersonKick(InteractionHand hand, float partialTick) {
        return GunKinematics.kick(now(partialTick) - LOCAL_SHOT[hand.ordinal()]);
    }

    /** Fração da recarga do jogador nesta mão (0 a 1), ou −1 se não está recarregando. */
    static double reloadProgress(Player player, InteractionHand hand, float partialTick) {
        Reload reload = RELOADS.get(player.getId());
        if (reload == null || reload.mainHand() != (hand == InteractionHand.MAIN_HAND)) {
            return -1.0;
        }
        double progress = (now(partialTick) - reload.start()) / reload.ticks();
        return progress >= 1.0 || progress < 0.0 ? -1.0 : progress;
    }

    // ---- tecla, movimento e HUD ----

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        double now = now(0.0F);
        RELOADS.int2ObjectEntrySet().removeIf(entry -> now - entry.getValue().start() > entry.getValue().ticks());
        while (RELOAD.consumeClick()) {
            LocalPlayer player = minecraft.player;
            if (player != null && minecraft.screen == null && holding(player) != null) {
                ModPayloads.sendToServer(new ReloadRequestPayload());
            }
        }
    }

    /**
     * Segurando o gatilho da automática o vanilla anda a 20% (como quem puxa o arco). A submetralhadora atira correndo
     * e a metralhadora pesada andando (DC2): devolve a velocidade inteira e a metade, nessa ordem.
     */
    private static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.isUsingItem() || player.isPassenger()
                || !(player.getUseItem().getItem() instanceof FirearmItem item)) {
            return;
        }
        float factor = switch (item.gun()) {
            case SUBMACHINE_GUN -> 5.0F;
            case HEAVY_MACHINE_GUN -> 2.5F;
            default -> 1.0F;
        };
        event.getInput().forwardImpulse *= factor;
        event.getInput().leftImpulse *= factor;
    }

    /** A arma de fogo na mão (a principal primeiro), ou {@code null}. */
    private static ItemStack holding(Player player) {
        if (player.getMainHandItem().getItem() instanceof FirearmItem) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().getItem() instanceof FirearmItem ? player.getOffhandItem() : null;
    }

    /** Pente e reserva, à direita da barra de itens: "12/15 · 48", ou "recarregando". */
    private static void renderAmmo(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        ItemStack stack = holding(player);
        if (stack == null || !(stack.getItem() instanceof FirearmItem item)) {
            return;
        }
        Firearm gun = item.gun();
        int rounds = FirearmItem.rounds(stack);
        int reserve = player.getAbilities().instabuild ? -1
                : player.getInventory().countItem(Firearms.ammo(gun.ammo()));
        boolean reloading = reloadProgress(player, player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND, partialTick) >= 0.0;
        Component text = reloading ? Component.translatable("hud.iceagesurvival.firearm.reloading")
                : Component.translatable("hud.iceagesurvival.firearm.ammo", rounds, gun.magazine(),
                        reserve < 0 ? "∞" : String.valueOf(reserve));
        Font font = minecraft.font;
        int x = width / 2 + 98;
        int y = height - 19;
        int color = reloading ? 0x9FD8E8 : rounds == 0 ? 0xE0402A : rounds * 4 <= gun.magazine() ? 0xF2B036 : 0xEDE6D6;
        graphics.renderItem(stack, x, y - 4);
        graphics.drawString(font, text, x + 18, y, color, true);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LAST_SHOT.clear();
        RELOADS.clear();
        LOCAL_SHOT[0] = Double.NEGATIVE_INFINITY;
        LOCAL_SHOT[1] = Double.NEGATIVE_INFINITY;
        FirearmTracers.clear();
    }
}
