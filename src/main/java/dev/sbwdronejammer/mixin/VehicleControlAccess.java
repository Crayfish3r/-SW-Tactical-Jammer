package dev.sbwdronejammer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity", remap = false)
public interface VehicleControlAccess {
    @Invoker(value = "setPower", remap = false)
    void sbwdronejammer$setPower(float value);

    @Invoker(value = "getPower", remap = false)
    float sbwdronejammer$getPower();

    @Invoker(value = "setLeftInputDown", remap = false)
    void sbwdronejammer$setLeftInputDown(boolean value);

    @Invoker(value = "setRightInputDown", remap = false)
    void sbwdronejammer$setRightInputDown(boolean value);

    @Invoker(value = "setForwardInputDown", remap = false)
    void sbwdronejammer$setForwardInputDown(boolean value);

    @Invoker(value = "setBackInputDown", remap = false)
    void sbwdronejammer$setBackInputDown(boolean value);

    @Invoker(value = "setUpInputDown", remap = false)
    void sbwdronejammer$setUpInputDown(boolean value);

    @Invoker(value = "setDownInputDown", remap = false)
    void sbwdronejammer$setDownInputDown(boolean value);
}
