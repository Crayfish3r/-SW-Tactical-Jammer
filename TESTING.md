# Integration testing

Run automated unit and deterministic load coverage without starting a Minecraft client:

```powershell
.\gradlew.bat clean build jammerLoadTest
```

## SuperbWarfare 0.8.9 final

The full pass requires the external SuperbWarfare runtime JAR; it is intentionally not vendored.
Put the 0.8.9 final JAR in `run/mods/`, then run `.\gradlew.bat runServer --args "--nogui"`.
Accept the generated EULA, restart, and verify the server reaches `Done` without loading client classes.

Test drones at 2–3, 30, and 100+ blocks; horizontal motion; immediate jammer shutdown;
controller/jammer logout; two overlapping jammers; dimension and chunk unload/reload; server restart;
ordinary and kamikaze payloads; and a drone already on the ground. Check both
`affectFriendlyDrones` settings. Start tracking an already falling drone to verify one-shot state replay.

Impact must flow through SuperbWarfare `hurt()` and `DroneEntity.destroy()`, the linked monitor must
keep `Using=false`, and logs must contain no injector, duplicate-impact, or client-classloading errors.
