package dev.sbwdronejammer.server;

import dev.sbwdronejammer.config.JammerConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

public final class DroneImpactHandler {
    public static void afterMove(Entity drone) {
        if (!(drone.level() instanceof ServerLevel level)
                || !DroneJamState.isFalling(drone)
                || !DroneJamState.isArmed(drone)
                || DroneJamState.impactProcessed(drone)
                || DroneJamState.fallTicks(drone) < JammerConfig.IMPACT_DELAY_TICKS.get()) {
            return;
        }

        boolean collision = drone.onGround() || drone.verticalCollision;
        boolean belowWorld = drone.getY() < level.getMinBuildHeight() - 16;
        boolean timedOut = DroneJamState.fallTicks(drone) >= JammerConfig.MAX_FALL_TICKS.get();
        if (!collision && !belowWorld && !timedOut) {
            return;
        }
        if (!DroneJamState.markImpactProcessed(drone.getPersistentData())) {
            return;
        }

        ServerPlayer attacker = DroneJamState.jammerOwner(drone)
                .map(level.getServer().getPlayerList()::getPlayer)
                .orElse(null);
        DamageSource crashSource = attacker == null
                ? drone.damageSources().genericKill()
                : new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.GENERIC_KILL), attacker);
        float damage = JammerConfig.ALWAYS_DESTROY_ON_IMPACT.get()
                ? Float.MAX_VALUE
                : Math.max(20.0F, (float) (-drone.getDeltaMovement().y * 100.0));
        drone.hurt(crashSource, damage);
    }

    private DroneImpactHandler() {
    }
}
