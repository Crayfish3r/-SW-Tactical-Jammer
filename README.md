# SBW Drone Jammer

Minimal Forge 1.20.1 addon for Superb Warfare. It adds one handheld item:
`sbwdronejammer:drone_jammer`.

## MVP behaviour

- Right-click toggles the jammer on or off.
- An active jammer works from the player's inventory or off-hand.
- Default radius is 32 blocks.
- Nearby `superbwarfare:drone` entities are shown on a client radar.
- Radar contacts and effective range are supplied by the server; client config cannot extend detection.
- A linked Superb Warfare monitor cannot control a drone while that drone is in range.
- Monitor control is restored when the drone leaves the interference area.
- Friendly drones can be included or ignored in the common Forge config.

## Configuration

After the first launch, edit:

`config/sbwdronejammer-common.toml`

```toml
[droneJammer]
range = 32
affectFriendlyDrones = true
```

## Build

Requires Java 17:

```powershell
.\gradlew.bat clean build
```

Output: `build/libs/tactical_jammer.jar`.

Install the resulting JAR on both the dedicated server and every client. Superb
Warfare and all of its normal dependencies are still required.
