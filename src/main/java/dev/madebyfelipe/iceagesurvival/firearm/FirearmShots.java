package dev.madebyfelipe.iceagesurvival.firearm;

import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import dev.madebyfelipe.iceagesurvival.item.TranqRifleItem;
import dev.madebyfelipe.iceagesurvival.network.FirearmReloadPayload;
import dev.madebyfelipe.iceagesurvival.network.FirearmShotPayload;
import dev.madebyfelipe.iceagesurvival.network.ModPayloads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/**
 * O tiro e a recarga no servidor (D58).
 *
 * <p>As armas de bala são instantâneas (hitscan), como o rifle tranquilizante: um raio do olho na direção do olhar
 * (com o desvio da arma), parado no primeiro bloco sólido. A escopeta solta {@link Firearm#pellets()} raios, e quem é
 * atingido por qualquer um deles leva o tiro inteiro uma vez (o cone do DC2 pega mais de um bicho). O rifle antitanque
 * atravessa todas as criaturas na linha. O canhão sólido lança a esfera ({@link SolidCannonShot}).
 *
 * <p>O tiro passa pelas criaturas domesticadas do próprio atirador e pela montaria dele, sem feri-las.
 */
public final class FirearmShots {
    /** Recuo do antitanque: "tão forte que é preciso parar para atirar". */
    private static final double ANTI_TANK_RECOIL = 0.45;
    /** Espera depois do clique seco, para o aviso não repetir a cada tick. */
    private static final int EMPTY_COOLDOWN = 10;

    private FirearmShots() {
    }

    /** Um trecho do tiro: até onde foi e o que atingiu no fim. */
    public record Segment(Vec3 end, FirearmShotPayload.Impact impact) {
    }

    /** O que um raio pegou: as criaturas atingidas (com a distância) e onde ele parou. */
    private record Trace(Vec3 end, List<Map.Entry<LivingEntity, Double>> targets, boolean hitBlock) {
    }

    public static void fire(ServerLevel level, Player player, InteractionHand hand, Firearm gun) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 muzzle = TranqRifleItem.muzzle(player, hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Firearms.SHOT_SOUNDS.get(gun).get(),
                SoundSource.PLAYERS, volume(gun), 0.95F + level.getRandom().nextFloat() * 0.1F);

        if (gun == Firearm.SOLID_CANNON) {
            SolidCannonShot shot = new SolidCannonShot(level, player, muzzle, look);
            level.addFreshEntity(shot);
            ModPayloads.sendToTrackingAndSelf(player, new FirearmShotPayload(player.getId(), gun,
                    hand == InteractionHand.MAIN_HAND, muzzle, List.of()));
            return;
        }

        Map<LivingEntity, Double> struck = new LinkedHashMap<>();
        List<Segment> segments = new ArrayList<>();
        RandomSource random = level.getRandom();
        for (int pellet = 0; pellet < gun.pellets(); pellet++) {
            Vec3 direction = deviate(look, gun.spread(), random);
            Trace trace = trace(level, player, eye, direction, gun.range(), gun.piercing());
            for (Map.Entry<LivingEntity, Double> target : trace.targets()) {
                struck.merge(target.getKey(), target.getValue(), Math::min);
            }
            FirearmShotPayload.Impact impact = !trace.targets().isEmpty() && !gun.piercing()
                    ? FirearmShotPayload.Impact.ENTITY
                    : trace.hitBlock() ? FirearmShotPayload.Impact.BLOCK : FirearmShotPayload.Impact.NONE;
            segments.add(new Segment(trace.end(), impact));
        }
        for (Map.Entry<LivingEntity, Double> target : struck.entrySet()) {
            FirearmHits.hit(level, player, target.getKey(), gun, target.getValue(), eye);
        }
        if (gun == Firearm.ANTI_TANK_RIFLE) {
            player.push(-look.x * ANTI_TANK_RECOIL, Math.max(0.0, -look.y * ANTI_TANK_RECOIL) * 0.5,
                    -look.z * ANTI_TANK_RECOIL);
            player.hurtMarked = true;
        }
        ModPayloads.sendToTrackingAndSelf(player, new FirearmShotPayload(player.getId(), gun,
                hand == InteractionHand.MAIN_HAND, muzzle, segments));
    }

    private static float volume(Firearm gun) {
        return switch (gun) {
            case SUBMACHINE_GUN -> 1.2F;
            case HANDGUN -> 1.6F;
            case SHOTGUN, SOLID_CANNON -> 2.4F;
            case HEAVY_MACHINE_GUN -> 2.8F;
            case ANTI_TANK_RIFLE -> 4.0F;
        };
    }

    /** A direção do olhar com um desvio aleatório de até {@code degrees} (o cone da arma). */
    public static Vec3 deviate(Vec3 look, double degrees, RandomSource random) {
        if (degrees <= 0.0) {
            return look;
        }
        // Um ponto no disco da base do cone, uniforme na área: raio pela raiz.
        double radius = Math.tan(Math.toRadians(degrees)) * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Mth.TWO_PI;
        Vec3 side = Math.abs(look.y) > 0.99 ? new Vec3(1, 0, 0) : look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(look).normalize();
        return look.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius)).normalize();
    }

    /**
     * O raio de um tiro: para no primeiro bloco sólido e pega as criaturas vivas antes dele (só a mais próxima, se a
     * arma não atravessa). Partes do More Hitboxes contam como o corpo. Fica de fora quem atira, a pilha de montaria
     * dele e as criaturas domesticadas dele.
     */
    private static Trace trace(Level level, Player shooter, Vec3 eye, Vec3 direction, double range, boolean piercing) {
        Vec3 far = eye.add(direction.scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                shooter));
        boolean hitBlock = block.getType() != HitResult.Type.MISS;
        Vec3 end = hitBlock ? block.getLocation() : far;

        Entity ownStack = shooter.getRootVehicle();
        UUID owner = shooter.getUUID();
        Map<LivingEntity, Double> found = new LinkedHashMap<>();
        Predicate<Entity> candidates = entity -> !entity.isSpectator()
                && (entity instanceof LivingEntity || entity instanceof PartEntity<?>);
        for (Entity candidate : level.getEntities(shooter, new AABB(eye, end).inflate(1.0), candidates)) {
            LivingEntity body = body(candidate);
            if (body == null || body == shooter || !body.isAlive() || !candidate.isPickable()
                    || body.getRootVehicle() == ownStack
                    || body instanceof OwnableEntity pet && owner.equals(pet.getOwnerUUID())) {
                continue;
            }
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            Optional<Vec3> crossing = box.contains(eye) ? Optional.of(eye) : box.clip(eye, end);
            if (crossing.isPresent()) {
                found.merge(body, eye.distanceTo(crossing.get()), Math::min);
            }
        }
        List<Map.Entry<LivingEntity, Double>> targets = new ArrayList<>(found.entrySet());
        targets.sort(Comparator.comparingDouble(Map.Entry::getValue));
        if (!piercing && targets.size() > 1) {
            targets = targets.subList(0, 1);
        }
        if (!piercing && !targets.isEmpty()) {
            end = eye.add(direction.scale(targets.get(0).getValue()));
        }
        return new Trace(end, targets, hitBlock);
    }

    @Nullable
    private static LivingEntity body(Entity entity) {
        Entity root = entity instanceof PartEntity<?> part ? part.getParent() : entity;
        return root instanceof LivingEntity living ? living : null;
    }

    /**
     * Completa o pente da arma nesta mão com a munição do inventário (no criativo, de graça). Sem munição, o clique
     * seco e o aviso. Durante a recarga a arma espera ({@link Firearm#reloadTicks()}).
     */
    public static void reload(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof FirearmItem item && !player.getCooldowns().isOnCooldown(item)) {
            refill(player, hand, stack, item);
        }
    }

    /**
     * A recarga logo depois do último tiro do pente: não espera o intervalo do tiro, que a espera da recarga substitui.
     */
    public static void reloadAfterLastShot(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof FirearmItem item) {
            refill(player, hand, stack, item);
        }
    }

    private static void refill(Player player, InteractionHand hand, ItemStack stack, FirearmItem item) {
        Firearm gun = item.gun();
        int rounds = FirearmItem.rounds(stack);
        int need = gun.magazine() - rounds;
        if (need <= 0) {
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        Item ammo = Firearms.ammo(gun.ammo());
        int load = creative ? need : take(player, ammo, need);
        Level level = player.level();
        if (load <= 0) {
            player.getCooldowns().addCooldown(item, EMPTY_COOLDOWN);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), Firearms.EMPTY.get(),
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable("item.iceagesurvival.firearm.no_ammo",
                    ammo.getDescription()), true);
            return;
        }
        FirearmItem.setRounds(stack, rounds + load);
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }
        player.getCooldowns().addCooldown(item, gun.reloadTicks());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Firearms.RELOAD_SOUNDS.get(gun).get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        ModPayloads.sendToTrackingAndSelf(player, new FirearmReloadPayload(player.getId(), gun,
                hand == InteractionHand.MAIN_HAND));
    }

    /** A tecla de recarga: a arma da mão principal, ou a da outra mão. */
    public static void reloadHeld(Player player) {
        InteractionHand hand = player.getMainHandItem().getItem() instanceof FirearmItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        reload(player, hand);
    }

    /** Tira até {@code count} da munição do inventário; devolve quanto tirou. */
    private static int take(Player player, Item ammo, int count) {
        int taken = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && taken < count; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ammo)) {
                int used = Math.min(count - taken, stack.getCount());
                stack.shrink(used);
                taken += used;
            }
        }
        return taken;
    }
}
