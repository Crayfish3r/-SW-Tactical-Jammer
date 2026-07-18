package dev.sbwdronejammer.mixin;

import dev.sbwdronejammer.compat.DroneControllerAccess;
import dev.sbwdronejammer.logic.FallPhysics;
import dev.sbwdronejammer.server.DroneImpactHandler;
import dev.sbwdronejammer.server.DroneJamManager;
import dev.sbwdronejammer.server.DroneJamState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.DroneEntity", remap = false)
public abstract class DroneEntityMixin implements DroneControllerAccess {
    @Shadow(remap = false)
    @Final
    private static EntityDataAccessor<String> CONTROLLER = null;
    @Unique
    private Vec3 sbwdronejammer$incomingMotion;

    @Shadow(remap = false)
    public abstract void setPower(float value);

    @Shadow(remap = false)
    public abstract void setLeftInputDown(boolean value);

    @Shadow(remap = false)
    public abstract void setRightInputDown(boolean value);

    @Shadow(remap = false)
    public abstract void setForwardInputDown(boolean value);

    @Shadow(remap = false)
    public abstract void setBackInputDown(boolean value);

    @Shadow(remap = false)
    public abstract void setUpInputDown(boolean value);

    @Shadow(remap = false)
    public abstract void setDownInputDown(boolean value);

    @Override
    public Optional<UUID> sbwdronejammer$controllerId() {
        String value = ((Entity) (Object) this).getEntityData().get(CONTROLLER);
        if (value == null || value.length() != 36) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), remap = false, require = 1)
    private void sbwdronejammer$disableControls(CallbackInfo callbackInfo) {
        Entity drone = (Entity) (Object) this;
        if (drone.level().isClientSide || !DroneJamState.isFalling(drone)) {
            return;
        }
        DroneImpactHandler.afterMove(drone);
        sbwdronejammer$incomingMotion = drone.getDeltaMovement();
        setLeftInputDown(false);
        setRightInputDown(false);
        setForwardInputDown(false);
        setBackInputDown(false);
        setUpInputDown(false);
        setDownInputDown(false);
        setPower(0.0F);
    }

    @Inject(method = "travel", at = @At("TAIL"), remap = false, require = 1)
    private void sbwdronejammer$forceFallingMotion(CallbackInfo callbackInfo) {
        Entity drone = (Entity) (Object) this;
        Vec3 incoming = sbwdronejammer$incomingMotion;
        if (drone.level().isClientSide || incoming == null || !DroneJamState.isFalling(drone)) {
            sbwdronejammer$incomingMotion = null;
            return;
        }

        setPower(0.0F);
        FallPhysics.Motion motion = FallPhysics.next(
                new FallPhysics.Motion(incoming.x, incoming.y, incoming.z),
                DroneJamState.fallTicks(drone),
                DroneJamManager.serverParameters()
        );
        drone.setDeltaMovement(motion.x(), motion.y(), motion.z());
        DroneJamState.advanceFallTick(drone);
        sbwdronejammer$incomingMotion = null;
    }
}
