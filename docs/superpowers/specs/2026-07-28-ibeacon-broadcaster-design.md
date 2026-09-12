# iBeacon Broadcaster — design

Date: 2026-07-28

## Goal

An Android app that continuously broadcasts a single iBeacon advertisement with a
user-supplied identity (UUID / major / minor), survives aggressive OEM process
killers, and restarts itself after reboot or app update.

## Scope

In scope:

- One active beacon at a time: UUID, major, minor, measured power (RSSI @ 1 m).
- Advertising rate chosen from the three Android presets (no custom duty cycle).
- Transmit power level selection.
- Foreground service that holds the advertisement up as long as the user wants it.
- Keep-alive stack: boot autostart, watchdogs, wake lock, battery-optimisation
  and vendor autostart helpers.

Out of scope (explicitly dropped during brainstorming):

- Duty cycling (broadcast N seconds / pause M seconds).
- Multiple simultaneous beacons.
- Beacon scanning / ranging.
- Saved beacon profiles.

## Advertising rate

Android does not accept an advertising interval in milliseconds. The rate comes
from `AdvertiseSettings.ADVERTISE_MODE_*`:

| Preset | Approx. interval | Trade-off |
|---|---|---|
| `LOW_LATENCY` | ~100 ms | best discoverability, highest drain |
| `BALANCED` | ~250 ms | middle ground |
| `LOW_POWER` | ~1000 ms | cheapest, slowest to be found |

The UI exposes exactly these three, plus the transmit power level
(`ADVERTISE_TX_POWER_ULTRA_LOW / LOW / MEDIUM / HIGH`), which controls range
rather than rate.

## Packet format

iBeacon is a manufacturer-specific AD structure, company ID `0x004C` (Apple),
25 bytes of payload:

```
02 15 | uuid[16] | major[2] | minor[2] | measuredPower[1]
```

`0x02` = iBeacon type, `0x15` = 21 remaining bytes. Major, minor and the UUID
are big-endian; measured power is a signed byte (typically -59). The company ID
itself is passed separately to `AdvertiseData.addManufacturerData(0x004C, …)`,
so the app builds only the 23 bytes after it.

No third-party beacon library: the payload is small enough to build directly,
and dropping the dependency keeps full control over the bytes on the air.

## Architecture

| Component | Responsibility |
|---|---|
| `IBeaconPayload` | pure function `BeaconConfig` → `ByteArray`; no Android deps |
| `BeaconConfig` | data model + validation (UUID text, 0..65535 ranges, power) |
| `SettingsRepository` | device-protected prefs: persisted config + `shouldRun` flag |
| `BeaconAdvertiser` | wraps `BluetoothLeAdvertiser`; start/stop, error mapping |
| `BeaconService` | foreground service, owns the advertiser, reacts to BT state |
| `BootReceiver` | `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` |
| `WatchdogWorker` | WorkManager, every 15 min: flag set but service dead → start |
| `WatchdogAlarm` | `setExactAndAllowWhileIdle` every ~5 min, re-arms itself |
| `KeepAliveHelper` | battery-optimisation request + vendor autostart intents |
| `MainActivity` + Compose screen | configuration, start/stop, status, checklist |

Settings live in device-protected storage rather than DataStore's default
location: boot receivers run before the user unlocks the phone, and
credential-protected storage is unreadable that early.

`shouldRun` is the single source of truth for "the user wants the beacon on".
Every restart path reads that flag; nothing restarts a beacon the user stopped
deliberately. A start request that arrives while the correct advertisement is
already on the air is a no-op, so the burst of `LOCKED_BOOT_COMPLETED` +
`BOOT_COMPLETED` + watchdogs does not flap the radio.

## Keep-alive strategy

1. **Foreground service**, type `connectedDevice`, ongoing non-dismissable
   notification with a Stop action.
2. **`START_STICKY`** plus `onTaskRemoved` → schedule an immediate restart, so
   swiping the app away does not kill the beacon.
3. **Partial `WakeLock`** held while advertising, so a dozing CPU does not stall
   the service's own bookkeeping.
4. **Two independent watchdogs** — WorkManager (15 min floor, survives process
   death, restored after reboot by the framework) and an exact alarm
   (`setExactAndAllowWhileIdle`, ~5 min, pierces Doze). Either one alone has a
   blind spot; together they cover each other.
5. **Boot / update receivers** to come back after reboot and after the app is
   reinstalled or updated.
6. **Bluetooth adapter state receiver**: when the user toggles BT off and on,
   the advertisement is re-raised automatically.
7. **User-facing checklist** in the UI: runtime permissions, battery
   optimisation exemption, vendor autostart screen (Xiaomi, Huawei, Oppo, Vivo,
   Samsung). These vendor settings cannot be granted programmatically, so the
   app takes the user to the right screen instead of silently hoping.

## Permissions

`BLUETOOTH_ADVERTISE` (API 31+), `BLUETOOTH` + `BLUETOOTH_ADMIN` (≤30),
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`,
`POST_NOTIFICATIONS` (API 33+), `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.

## Testing

- JVM unit tests for `IBeaconPayload` (exact byte layout, endianness, signed
  measured power) and `BeaconConfig` validation.
- `./gradlew test` and `./gradlew assembleDebug` must pass.
- BLE advertising cannot be verified on an emulator — the emulator has no BLE
  peripheral role. Real-device verification is the user's step; the app surfaces
  advertiser callbacks (including `ADVERTISE_FAILED_*` codes) in the UI so a
  failure on device is diagnosable.

## Known limits

- Devices without `isMultipleAdvertisementSupported` / peripheral-mode support
  cannot advertise at all; the app detects this and says so instead of failing
  silently.
- No non-root technique guarantees survival on MIUI/HarmonyOS without the user
  granting autostart manually.
- WorkManager's minimum periodic interval is 15 minutes; the exact alarm covers
  the gap.

## Stack

Kotlin, Jetpack Compose (Material 3), minSdk 26, compileSdk/targetSdk 35,
AGP 8.7, Gradle 8.10.2, JDK 21.
