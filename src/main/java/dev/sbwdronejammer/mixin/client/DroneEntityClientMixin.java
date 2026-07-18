package dev.sbwdronejammer.mixin.client;

import dev.sbwdronejammer.client.ClientDroneJamState;
import dev.sbwdronejammer.logic.FallPhysics;
import dev.sbwdronejammer.mixin.VehicleControlAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.DroneEntity", remap = false)
public abstract class DroneEntityClientMixin {
    @Unique
    private Vec3 sbwdronejammer$clientIncomingMotion;

    @Inject(method = "travel", at = @At("HEAD"), remap = false, require = 1)
    private void sbwdronejammer$disableClientControls(CallbackInfo callbackInfo) {
        Entity drone = (Entity) (Object) this;
        ClientDroneJamState.State state = ClientDroneJamState.state(drone);
        if (state == null) {
            return;
        }
        sbwdronejammer$clientIncomingMotion = drone.getDeltaMovement();
        VehicleControlAccess controls = (VehicleControlAccess) (Object) this;
        controls.sbwdronejammer$setLeftInputDown(false);
        controls.sbwdronejammer$setRightInputDown(false);
        controls.sbwdronejammer$setForwardInputDown(false);
        controls.sbwdronejammer$setBackInputDown(false);
        controls.sbwdronejammer$setUpInputDown(false);
        controls.sbwdronejammer$setDownInputDown(false);
        controls.sbwdronejammer$setPower(0.0F);
    }

    @Inject(method = "travel", at = @At("TAIL"), remap = false, require = 1)
    private void sbwdronejammer$applyClientFallingMotion(CallbackInfo callbackInfo) {
        Entity drone = (Entity) (Object) this;
        ClientDroneJamState.State state = ClientDroneJamState.state(drone);
        Vec3 incoming = sbwdronejammer$clientIncomingMotion;
        if (state == null || incoming == null) {
            sbwdronejammer$clientIncomingMotion = null;
            return;
        }
        ((VehicleControlAccess) (Object) this).sbwdronejammer$setPower(0.0F);
        FallPhysics.Motion motion = FallPhysics.next(
                new FallPhysics.Motion(incoming.x, incoming.y, incoming.z),
                state.fallTicks(), state.parameters()
        );
        drone.setDeltaMovement(motion.x(), motion.y(), motion.z());
        ClientDroneJamState.advance(drone);
        sbwdronejammer$clientIncomingMotion = null;
    }
}
