package dev.sbwdronejammer.mixin.client;

import dev.sbwdronejammer.client.ClientDroneJamState;
import dev.sbwdronejammer.logic.FallPhysics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.DroneEntity", remap = false)
public abstract class DroneEntityClientMixin {
    @Unique
    private Vec3 sbwdronejammer$clientIncomingMotion;

    @Shadow(remap = false) public abstract void setPower(float value);
    @Shadow(remap = false) public abstract void setLeftInputDown(boolean value);
    @Shadow(remap = false) public abstract void setRightInputDown(boolean value);
    @Shadow(remap = false) public abstract void setForwardInputDown(boolean value);
    @Shadow(remap = false) public abstract void setBackInputDown(boolean value);
    @Shadow(remap = false) public abstract void setUpInputDown(boolean value);
    @Shadow(remap = false) public abstract void setDownInputDown(boolean value);

    @Inject(method = "travel", at = @At("HEAD"), remap = false, require = 1)
    private void sbwdronejammer$disableClientControls(CallbackInfo callbackInfo) {
        Entity drone = (Entity) (Object) this;
        ClientDroneJamState.State state = ClientDroneJamState.state(drone);
        if (state == null) {
            return;
        }
        sbwdronejammer$clientIncomingMotion = drone.getDeltaMovement();
        setLeftInputDown(false);
        setRightInputDown(false);
        setForwardInputDown(false);
        setBackInputDown(false);
        setUpInputDown(false);
        setDownInputDown(false);
        setPower(0.0F);
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
        setPower(0.0F);
        FallPhysics.Motion motion = FallPhysics.next(
                new FallPhysics.Motion(incoming.x, incoming.y, incoming.z),
                state.fallTicks(), state.parameters()
        );
        drone.setDeltaMovement(motion.x(), motion.y(), motion.z());
        ClientDroneJamState.advance(drone);
        sbwdronejammer$clientIncomingMotion = null;
    }
}
