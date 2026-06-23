# Cardlock — Mifare NFC Access Control Terminal

An autonomous Android kiosk application for access control and attendance systems. The terminal reads **Mifare Classic** cards and wristbands, displays the cardholder's balance or status on a full-screen interface, and drives physical actuators (turnstiles, electronic lockers, relays) over the **Wiegand** interface.

The app is built for unattended 24/7 operation, is fully configurable on-site through a built-in web panel, and supports two independent card-reading channels at once: the device's built-in NFC module and an external industrial reader connected over a serial port.

## Features

- **Dual reading backends** — the same build works both with the Android NFC adapter (`MifareClassic` API) and with an external industrial reader over a serial port, selectable at runtime via settings.
- **Three decoding algorithms** — covers different card encoding schemes used across deployments, including a mode that derives the sector key directly from the card UID via an XOR scheme.
- **Access control output** — generates Wiegand 26/34 codes and controls relays through GPIO with sensor feedback, for direct integration with turnstiles and electronic lockers.
- **Kiosk lockdown** — runs as Device Owner with Lock Task Mode (COSU), auto-starts on boot, intercepts the HOME button, hides system bars and runs full-screen immersive.
- **On-site configuration** — branding, texts, colors, media and the field-reading map are all configured from an embedded web panel and persisted as JSON. No APK rebuild required.
- **Event journaling** — scan events with timestamp and wristband battery status are logged locally (Storage Access Framework) or uploaded to cloud storage by token.
- **Reliability layer** — 100 ms GPIO polling for instant actuator response and automatic serial-port reconnection every 5 minutes to survive long continuous runs.

## Architecture

The app uses a **hybrid architecture**. A native Android core handles hardware and system policies, while the entire user interface is a local web app rendered inside a `WebView` and driven by a JSON configuration.

```
┌─────────────────────────────────────────────────────────┐
│                     Android Core (Java)                   │
│                                                           │
│   MainActivity ── NFC ──── MifareClassic API              │
│        │          SP  ──── Serial port (JNI) + reader SDK │
│        │          System  Device Owner / Lock Task / GPIO │
│        │                                                  │
│   WebAppInterface  (JS  ↔  Java bridge)                   │
└────────────────────────────┬──────────────────────────────┘
                             │ evaluateJavascript / @JavascriptInterface
┌────────────────────────────┴──────────────────────────────┐
│              WebView UI  (HTML / CSS / jQuery)             │
│        configurable screens, admin panel, media           │
└───────────────────────────────────────────────────────────┘
```

The native core and the JavaScript layer communicate **both ways**: the UI calls device methods through a `@JavascriptInterface` bridge, and the core pushes rendered markup and variables back into the `WebView` via `evaluateJavascript`. This keeps the UI fully configurable without a heavy native view layer and without network access.

### Card reading

The reading layer is abstracted behind a single contract with two interchangeable backends (`deviceType`):

- `0` — Android `MifareClassic` adapter
- `1` — external reader over `/dev/ttyS7`

On top of that sit three decoding algorithms (`algo`):

| algo | Description |
|------|-------------|
| `0`  | Key-based sector authentication, value read as 16-bit integer or hex |
| `1`  | Sector key derived from the card UID via XOR, Wiegand output |
| `2`  | Flag-based read with wristband battery status and event journaling |

The reading map (sector, block, byte range, conversion mode and key) is a configurable structure serialized to JSON, so one terminal can serve balance cards, ID passes and telemetry wristbands by switching settings only.

### Serial protocol

The external reader is accessed through a native serial-port library (JNI, builds for `armeabi`, `armeabi-v7a`, `x86`) and a hardware SDK. On top of the raw stream the app implements:

- Frame parsing with **BCC checksum** integrity validation.
- A stepwise command sequence: field activation, anti-collision, card select, sector authentication and block read.
- A dedicated worker thread with a queue so I/O never blocks the UI and frames are not dropped during rapid taps.

## Project structure

```
app/src/main/
├── java/com/mifare/mifare/
│   ├── MainActivity.java        # Activity, WebView host, NFC lifecycle
│   ├── NFC.java                 # MifareClassic reading + decoding algorithms
│   ├── SP.java                  # Serial-port reader, protocol, GPIO/Wiegand
│   ├── System.java              # Device Owner, Lock Task, file/cloud I/O
│   ├── WebAppInterface.java     # JS → Java bridge
│   ├── Helper.java              # Java → JS bridge, shared state
│   ├── Set.java / MifareData.java  # Settings & card data models
│   └── ...                      # Boot receiver, admin receiver, splash
├── java/android_serialport_api/ # Serial port wrapper
├── jni/                         # Native serial port (C, NDK)
└── assets/wwwroot/              # WebView UI (HTML / CSS / jQuery)
```

## Tech stack

- **Android** (Java), `minSdk 24`, `targetSdk 34`
- **WebView** UI: HTML, CSS, jQuery, with a two-way JavaScript ↔ Java bridge
- **Hardware**: Android NFC API (Mifare Classic), native serial port via JNI/NDK, reader SDK, Wiegand 26/34, GPIO/relay control
- **System**: Device Owner, Device Policy Manager, Lock Task Mode (COSU), `BOOT_COMPLETED` autostart, overlay & immersive full-screen
- **Data**: Gson (JSON), SharedPreferences, Storage Access Framework, cloud upload
- **Build**: Gradle (Kotlin DSL), release signing

## Build

```bash
./gradlew assembleRelease
```

Requirements:

- Android SDK with API level 34
- A `local.properties` file pointing at your SDK location
- The reader SDK jars under `app/libs/` (`YNHAPI-*.jar`) and native `.so` libraries under `app/libs/<abi>/`

## Configuration

All runtime settings live in a single `Set` model serialized to JSON in `SharedPreferences`, and can be exported/imported as a file from the admin panel. Configurable items include the reading backend and algorithm, the per-field card map, branding (logo, colors, texts), media, Wiegand format and byte order, serial port speed, kiosk behavior (status/navigation bars) and cloud upload credentials.

## Notes

This app requires **Device Owner** privileges for full kiosk operation. On a production terminal it is provisioned as the device owner so it can enforce Lock Task Mode and act as the launcher. NFC hardware is required (`android.hardware.nfc`).
