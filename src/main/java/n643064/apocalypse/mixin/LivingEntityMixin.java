package n643064.apocalypse.mixin;

import n643064.apocalypse.Apocalypse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final double ATTACK_RANGE = 3.0;
    private static final double NOISY_TARGET_RANGE = 24.0;
    private static final double MOVING_TARGET_RANGE = 16.0;
    private double lastAlertTime = 0;
    private Entity lastAlertedTarget = null;

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof ZombieEntity)) return;
        
        if (!Apocalypse.config.zombie.takeFallDamage) {
            cir.setReturnValue(false);
            return;
        }

        if (fallDistance < Apocalypse.config.zombie.minFallDamageDistance) {
            cir.setReturnValue(false);
            return;
        }

        float damage = (fallDistance - 3.0F) * damageMultiplier * Apocalypse.config.zombie.fallDamageMultiplier;
        if (damage > 0.0F) {
            entity.damage(damageSource, damage);
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canSee(Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    private void onCanSee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof ZombieEntity) || !cir.getReturnValue()) return;
        
        // Ignore creative and spectator players
        if (target instanceof PlayerEntity player) {
            if (player.isCreative() || player.isSpectator()) {
                cir.setReturnValue(false);
                return;
            }
        }
        
        ZombieEntity zombie = (ZombieEntity) entity;
        double distance = zombie.distanceTo(target);
        
        // Quick check for attack range
        if (distance <= ATTACK_RANGE) {
            alertNearbyZombies(zombie, target);
            return;
        }

        // Cache world time and light level
        long timeOfDay = zombie.getWorld().getTimeOfDay() % 24000;
        int lightLevel = zombie.getWorld().getBaseLightLevel(zombie.getBlockPos(), 0);
        boolean isDay = timeOfDay >= 0 && timeOfDay < 12000;

        // Check visibility based on time and light
        if (isDay) {
            if (lightLevel > 10 && distance > 8.0) {
                cir.setReturnValue(false);
                return;
            }
            if (lightLevel > 5 && distance > 12.0) {
                cir.setReturnValue(false);
                return;
            }
        } else {
            if (lightLevel < 5 && distance > 16.0) {
                cir.setReturnValue(false);
                return;
            }
            if (lightLevel < 10 && distance > 20.0) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Check for noisy targets
        if (target instanceof LivingEntity livingTarget) {
            if (livingTarget.isSprinting() || livingTarget.isSwimming()) {
                if (distance <= NOISY_TARGET_RANGE) {
                    alertNearbyZombies(zombie, target);
                    return;
                }
            } else if (livingTarget.getVelocity().lengthSquared() > 0.01 && distance <= MOVING_TARGET_RANGE) {
                alertNearbyZombies(zombie, target);
                return;
            }
        }

        cir.setReturnValue(false);
    }

    private void alertNearbyZombies(ZombieEntity alertingZombie, Entity target) {
        if (!Apocalypse.config.zombie.enableAlert) return;
        
        // Ignore creative and spectator players for alerts
        if (target instanceof PlayerEntity player) {
            if (player.isCreative() || player.isSpectator()) {
                return;
            }
        }
        
        // Prevent alerting about the same target repeatedly
        if (lastAlertedTarget == target) {
            return;
        }
        
        double currentTime = alertingZombie.getWorld().getTime();
        if (currentTime - lastAlertTime < Apocalypse.config.zombie.alertCheckInterval) return;
        
        lastAlertTime = currentTime;
        lastAlertedTarget = target;
        
        // Only play sound if this is the first zombie to spot the target
        if (alertingZombie.getTarget() == null) {
            alertingZombie.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 
                Apocalypse.config.zombie.alertSoundVolume, 
                Apocalypse.config.zombie.alertSoundPitch);
        }

        if (!(target instanceof LivingEntity)) return;

        Box searchBox = alertingZombie.getBoundingBox().expand(
            Apocalypse.config.zombie.alertRange, 4, Apocalypse.config.zombie.alertRange);
        alertingZombie.getWorld().getEntitiesByClass(
            ZombieEntity.class,
            searchBox,
            zombie -> zombie != alertingZombie && !zombie.isDead() && zombie.getTarget() == null
        ).forEach(nearbyZombie -> nearbyZombie.setTarget((LivingEntity) target));
    }
} 