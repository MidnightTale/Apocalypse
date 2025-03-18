package n643064.apocalypse.mixin;

import n643064.apocalypse.Apocalypse;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity instanceof ZombieEntity) {
            if (!Apocalypse.config.zombie.takeFallDamage) {
                cir.setReturnValue(false);
                return;
            }

            // Check if fall distance is within the configured range
            if (fallDistance < Apocalypse.config.zombie.minFallDamageDistance) {
                cir.setReturnValue(false);
                return;
            }

            // Calculate the actual damage
            float damage = (fallDistance - 3.0F) * damageMultiplier * Apocalypse.config.zombie.fallDamageMultiplier;
            if (damage > 0.0F) {
                entity.damage(damageSource, damage);
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }
} 