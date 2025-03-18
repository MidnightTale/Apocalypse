package n643064.apocalypse.core.entity.goal;

import n643064.apocalypse.Apocalypse;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

public class ZombiePounceAtTargetGoal extends Goal {
    private final ZombieEntity mob;
    private LivingEntity target;
    private final float velocity;
    private Vec3d jumpTarget;
    private int cooldown = 0;
    private static final double MIN_DISTANCE = 2.0;
    private static final double MAX_DISTANCE = 12.0;
    private static final double JUMP_POWER = 0.6;
    private static final double FORWARD_POWER = 2.2;

    public ZombiePounceAtTargetGoal(ZombieEntity mob, float velocity) {
        this.mob = mob;
        this.velocity = velocity;
        this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE, Goal.Control.LOOK));
    }

    private Vec3d calculateJumpVelocity(Vec3d start, Vec3d target) {
        double dx = target.x - start.x;
        double dz = target.z - start.z;
        // Calculate horizontal distance
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        
        // Calculate direction vector
        double dirX = dx / horizontalDistance;
        double dirZ = dz / horizontalDistance;
        
        // Calculate jump power based on distance
        double jumpPower = JUMP_POWER;
        if (horizontalDistance > 6.0) {
            jumpPower *= 1.2;
        }
        
        // Calculate forward power
        double forwardPower = FORWARD_POWER * velocity;
        if (horizontalDistance > 8.0) {
            forwardPower *= 1.1;
        }
        
        // Apply velocity
        return new Vec3d(
            dirX * forwardPower,
            jumpPower,
            dirZ * forwardPower
        );
    }

    @Override
    public boolean canStart() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (this.mob.getDataTracker().get(Apocalypse.IS_DIGGING)) {
            return false;
        }
        
        this.target = this.mob.getTarget();
        if (this.target == null || !this.mob.isOnGround()) {
            return false;
        }

        double distance = this.mob.distanceTo(this.target);
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
            return false;
        }

        // Set jump target
        jumpTarget = target.getPos();

        // Random chance to pounce
        return this.mob.getRandom().nextInt(Goal.toGoalTicks(5)) == 0;
    }

    @Override
    public void start() {
        // Look at target
        this.mob.lookAtEntity(this.target, 30.0F, 30.0F);

        // Calculate and apply velocity
        Vec3d jumpVel = calculateJumpVelocity(mob.getPos(), jumpTarget);
        this.mob.setVelocity(jumpVel);

        // Play aggressive sound
        float pitch = 1.4F + mob.getRandom().nextFloat() * 0.2F;
        mob.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 1.8F, pitch);

        cooldown = 30;
        this.mob.getJumpControl().setActive();
    }

    @Override
    public void stop() {
        jumpTarget = null;
    }

    @Override
    public boolean shouldContinue() {
        return !this.mob.isOnGround();
    }
}