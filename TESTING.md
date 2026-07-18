# Integration testing

## Automated verification

Run the unit tests, exact production-JAR mixin contract check, reobfuscated build, and deterministic
load test on Windows:

```powershell
.\gradlew.bat clean build verifySbwMixinContract jammerLoadTest --console=plain
```

The Unix-wrapper equivalent is:

```bash
./gradlew clean build verifySbwMixinContract jammerLoadTest --console=plain
```

`verifySbwMixinContract` obtains exactly
`superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar`, verifies SHA-256
`6eece0df9c927bddee3bd373fe90a2bbcba7157e9408ee0f94d13e04dc638395`, and checks target classes,
method descriptors, declaring classes, `@Invoker` declarations, and the absence of inherited
`DroneEntity` method shadows. Use `-PsbwProductionJar=C:\path\to\the.jar` for an explicit local copy.

A successful Java compilation alone is not sufficient: Mixin resolves `@Shadow`, `@Invoker`, and
injections while transforming the production target classes. Always run the runtime smoke tests too.

## Runtime Mixin smoke tests

Place the exact SuperbWarfare JAR and its runtime dependencies in `run/mods/`:

- Curios Forge 5.14.1+1.20.1
- GeckoLib Forge 4.4.6
- Kotlin for Forge 4.11.0

Launch the production-namespace client smoke test:

```powershell
.\gradlew.bat productionClientSmoke --console=plain
```

`productionClientSmoke` stages the reobfuscated Tactical Jammer JAR and exact runtime dependencies under
`build/production-client-smoke/mods`, switches the ForgeGradle launch target from named userdev to
the production/SRG `forgeclient` target, and does not add the main source set through `MOD_CLASSES`.

Wait until the title screen is available. The log must pass `CONSTRUCT` and must not contain
`InvalidMixinException`, `MixinApplyError`, `InjectionError`, `NoClassDefFoundError`, or
`ClassNotFoundException`. This run transforms both `VehicleEntity` and `DroneEntity` and applies the
client `DroneEntityClientMixin`.

Run the dedicated-server smoke test separately:

```powershell
.\gradlew.bat runServer --console=plain
```

Without accepting a newly generated EULA, reaching the EULA gate verifies early server-side class
transformation. For a full server test, accept the EULA yourself, restart, and verify that the server
reaches `Done` without loading any `dev.sbwdronejammer.mixin.client` or `net.minecraft.client` class
on behalf of Tactical Jammer.

## In-game matrix

Test drones at 2–3, 30, and 100+ blocks; horizontal motion; immediate jammer shutdown;
controller/jammer logout; two overlapping jammers; dimension and chunk unload/reload; server restart;
ordinary and kamikaze payloads; and a drone already on the ground. Check both
`affectFriendlyDrones` settings. Start tracking an already falling drone to verify one-shot state replay.

Impact must flow through SuperbWarfare `hurt()` and `DroneEntity.destroy()`, the linked monitor must
keep `Using=false`, and logs must contain no injector, duplicate-impact, or client-classloading errors.
