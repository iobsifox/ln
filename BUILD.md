# Build and Verification - Last Night

This document provides technical instructions on verifying, building, and running test processes on the **Last Night** project.

## ⚙️ Requirements
*   **JDK:** Version 11+
*   **Android SDK level:** Compile/Target SDK `36`, Minimum SDK `24`
*   **Gradle DSL:** Kotlin (`build.gradle.kts`)

---

## 🛠️ Build Commands

All main tasks can be run directly using the Gradle wrapper CLI:

### 1. Compile and Sync Application
Runs standard build processes and validates correct package imports, resource bindings, and syntax safety:
```bash
./gradlew compileDebugSources
```

### 2. Assemble Debug APK
Builds and packages a signed debug application package, ready to be sideloaded onto an emulator or device:
```bash
./gradlew assembleDebug
```
The resulting artifact will be located in:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Assemble Release APK
Assembles optimized release-profile application assets:
```bash
./gradlew assembleRelease
```

---

## 🧪 Testing Verification Guides

The project comes with a comprehensive JUnit, Robolectric, and Roborazzi visual regression testing suite.

### 1. Execute Local Unit Tests
Verifies DNS RFC packet builders, response byte serialization, and endpoint selection mappings:
```bash
./gradlew :app:testDebugUnitTest
```

### 2. Verify Roborazzi Screenshots
Renders visual layouts inside local headless environments to detect aesthetic regression:
```bash
./gradlew :app:verifyRoborazziDebug
```

### 3. Record/Update reference layout screenshots
If you make explicit adjustments to the UI and wish to record updated baseline reference images:
```bash
./gradlew :app:recordRoborazziDebug
```

---

## 📂 Source Code Tree Map

Refined file structure mapping for reference:
```text
LastNightAndroid/
  build.gradle.kts
  settings.gradle.kts
  gradle/libs.versions.toml
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      assets/
        default-settings.json
        vendored-cores.json
      res/
        drawable/
          ic_launcher_background.xml
          ic_launcher_foreground.xml
        values/
          strings.xml
          colors.xml
      java/com/example/
        MainActivity.kt
        LastNightApplication.kt
        data/
          AppSettings.kt
          SettingsRepository.kt
        dns/
          DnsCache.kt
          DnsWire.kt
          DohClient.kt
        proxy/
          HttpProxyServer.kt
          Socks5ProxyServer.kt
        vpn/
          AppRoutingManager.kt
          LastNightVpnService.kt
          TunEngine.kt
        cores/
          CoreAdapter.kt
          CoreManager.kt
          PsiphonAdapter.kt
          SingBoxAdapter.kt
          XrayAdapter.kt
        profiles/
          ProfileManager.kt
        logs/
          LogRepository.kt
```
