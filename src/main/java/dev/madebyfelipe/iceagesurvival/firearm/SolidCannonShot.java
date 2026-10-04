package dev.madebyfelipe.iceagesurvival.firearm;

import dev.madebyfelipe.iceagesurvival.core.firearms.Firearm;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import java.util.Optional;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.joml.Vector3f;

/**
 * A esfera do canhão sólido (D58): voa reta, sem cair, e estoura no primeiro bloco ou criatura com a vibração que
 * destrói as células — o tiro de 1500 do DC2 ({@link FirearmHits}). Some ao fim do alcance do canhão. Passa pelas
 * criaturas domesticadas de quem atirou e pela montaria dele.
 */
public class SolidCannonShot extends Projectile {
    /** Blocos por tick: rápida o bastante para acertar quem corre, lenta o bastante para se ver a esfera. */
    public static final double SPEED = 1.5;
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.35F, 0.95F, 0.9F), 1.2F);

    private Vec3 origin = Vec3.ZERO;

    public SolidCannonShot(EntityType<? extends SolidCannonShot> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SolidCannonShot(Level level, Player shooter, Vec3 muzzle, Vec3 direction) {
        this(Firearms.SOLID_CANNON_SHOT.get(), level);
        setOwner(shooter);
        origin = shooter.getEyePosition();
        setPos(muzzle.x, muzzle.y - getBbHeight() / 2.0, muzzle.z);
        setDeltaMovement(direction.normalize().scale(SPEED));
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            HitResult hit = sweep(position(), position().add(motion));
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                if (isRemoved()) {
                    return;
                }
            }
            if (tickCount * SPEED > Firearm.SOLID_CANNON.range()) {
                discard();
                return;
            }
        } else {
            for (int i = 0; i < 2; i++) {
                double back = i * 0.5;
                level().addParticle(TRAIL, getX() - motion.x * back, getY() + getBbHeight() / 2 - motion.y * back,
                        getZ() - motion.z * back, 0.0, 0.0, 0.0);
            }
        }
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    /**
     * O que a esfera pega neste passo: o primeiro bloco sólido e, antes dele, o corpo vivo mais próximo (as partes do
     * More Hitboxes contam como o corpo). A varredura é própria, como a do tiro instantâneo: começar o passo já
     * dentro da caixa também conta (a do vanilla, {@code ProjectileUtil}, só conta quem a esfera cruza de fora).
     */
    private HitResult sweep(Vec3 from, Vec3 to) {
        BlockHitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : to;
        Entity best = null;
        Vec3 at = null;
        double nearest = Double.MAX_VALUE;
        AABB reach = getBoundingBox().expandTowards(end.subtract(from)).inflate(1.0);
        for (Entity candidate : level().getEntities(this, reach, entity -> !entity.isSpectator()
                && (entity instanceof LivingEntity || entity instanceof PartEntity<?>))) {
            if (!canHitEntity(candidate)) {
                continue;
            }
            AABB box = candidate.getBoundingBox().inflate(getBbWidth() / 2.0);
            Optional<Vec3> crossing = box.contains(from) ? Optional.of(from) : box.clip(from, end);
            if (crossing.isPresent() && from.distanceToSqr(crossing.get()) < nearest) {
                nearest = from.distanceToSqr(crossing.get());
                best = candidate;
                at = crossing.get();
            }
        }
        return best != null ? new EntityHitResult(best, at) : block;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity root = entity instanceof PartEntity<?> part ? part.getParent() : entity;
        if (!(root instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        Entity owner = getOwner();
        if (owner != null) {
            UUID ownerId = owner.getUUID();
            return root != owner && root.getRootVehicle() != owner.getRootVehicle()
                    && !(root instanceof OwnableEntity pet && ownerId.equals(pet.getOwnerUUID()));
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        Entity root = hit instanceof PartEntity<?> part ? part.getParent() : hit;
        if (root instanceof LivingEntity target && level() instanceof ServerLevel server) {
            FirearmHits.hit(server, getOwner(), target, Firearm.SOLID_CANNON, origin.distanceTo(result.getLocation()),
                    position());
        }
        burst(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        burst(result.getLocation());
    }

    /** O estouro: a onda de vibração (faíscas e o anel ciano) e o som. */
    private void burst(Vec3 at) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.4);
            server.sendParticles(TRAIL, at.x, at.y, at.z, 30, 0.7, 0.7, 0.7, 0.0);
            server.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            server.playSound(null, at.x, at.y, at.z, Firearms.SOLID_IMPACT.get(), SoundSource.PLAYERS, 2.0F,
                    0.9F + random.nextFloat() * 0.2F);
        }
        discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("OriginX", origin.x);
        tag.putDouble("OriginY", origin.y);
        tag.putDouble("OriginZ", origin.z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        origin = new Vec3(tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
    }
}
