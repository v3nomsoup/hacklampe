# HackLampe (Chop'n'Light) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eine Android-App bauen, die per Doppel-Hackbewegung die Taschenlampe an-/ausschaltet — auch wenn die App geschlossen ist — gesteuert über eine Quick-Settings-Kachel und eine Einstellungs-App.

**Architecture:** Ein Foreground-Service (`GestureService`) hört den Linearbeschleunigungssensor ab und füttert einen gerätefreien, voll unit-getesteten Erkennungskern (`ChopDetector`). Bei erkanntem Doppel-Chop schaltet ein `TorchController` die LED. Steuerung über `HackTile` (Quick-Settings-Kachel) und `SettingsActivity`; optionaler Auto-Start via `BootReceiver`.

**Tech Stack:** Kotlin, Android SDK (minSdk 31 / compileSdk 35), Gradle (Kotlin DSL) mit Wrapper, JUnit4 für Unit-Tests, Homebrew für die Toolchain auf macOS.

---

## File Structure

```
settings.gradle.kts                 # Gradle-Projektdefinition
build.gradle.kts                    # Root-Build (Plugin-Versionen)
gradle.properties                   # JVM/AndroidX-Flags
gradle/wrapper/gradle-wrapper.properties
gradlew, gradlew.bat                # Gradle Wrapper
local.properties                    # SDK-Pfad (nicht eingecheckt)
app/build.gradle.kts                # App-Modul-Build
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/layout/activity_settings.xml
app/src/main/kotlin/de/hacklampe/app/
    detector/ChopDetector.kt        # reine Erkennungslogik (kein Android)
    torch/TorchController.kt        # CameraManager.setTorchMode-Wrapper
    service/GestureService.kt       # Foreground-Service + Sensor
    tile/HackTile.kt                # Quick-Settings-Kachel
    data/Prefs.kt                   # SharedPreferences-Wrapper
    ui/SettingsActivity.kt          # Einstellungs-Oberfläche
    boot/BootReceiver.kt            # Auto-Start nach Neustart
app/src/test/kotlin/de/hacklampe/app/detector/ChopDetectorTest.kt
```

---

## Task 0: Toolchain auf macOS einrichten

Der Nutzer hat noch keine Android-Entwicklungsumgebung. Diese Task installiert alles. Keine Code-Tests — Verifikation über Versions-Ausgaben.

**Files:** keine (nur System-Setup)

- [ ] **Step 1: Homebrew prüfen**

Run: `brew --version`
Expected: Eine Versionsnummer (z.B. `Homebrew 4.x`). Falls `command not found`, zuerst Homebrew installieren von https://brew.sh und dann fortfahren.

- [ ] **Step 2: JDK 17 installieren**

```bash
brew install --cask temurin@17
```
Expected: Installation läuft durch, endet mit `was successfully installed`.

- [ ] **Step 3: JDK verifizieren**

Run: `/usr/libexec/java_home -v 17 && "$(/usr/libexec/java_home -v 17)/bin/java" -version`
Expected: Ein Pfad und `openjdk version "17.x"`.

- [ ] **Step 4: Android Command-Line-Tools installieren**

```bash
brew install --cask android-commandlinetools
```
Expected: Endet mit `was successfully installed`. Die Tools liegen danach unter `$(brew --prefix)/share/android-commandlinetools`.

- [ ] **Step 5: Umgebungsvariablen für die Shell setzen**

```bash
{
  echo ''
  echo '# Android SDK'
  echo "export ANDROID_HOME=\"$(brew --prefix)/share/android-commandlinetools\""
  echo 'export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"'
  echo "export JAVA_HOME=\"$(/usr/libexec/java_home -v 17)\""
} >> ~/.zshrc
source ~/.zshrc
echo "ANDROID_HOME=$ANDROID_HOME"
```
Expected: Eine Zeile `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools` (oder `/usr/local/...` auf Intel-Macs).

- [ ] **Step 6: SDK-Pakete installieren und Lizenzen akzeptieren**

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```
Expected: `licenses ... accepted` und danach `done` für die Pakete.

- [ ] **Step 7: adb verifizieren**

Run: `adb --version`
Expected: `Android Debug Bridge version 1.0.xx`.

---

## Task 1: Gradle-Android-Projektgerüst

Erzeugt ein lauffähiges, leeres Android-Projekt, das per Kommandozeile baut. Verifikation: erfolgreicher Build.

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Gradle Wrapper über Homebrew-Gradle erzeugen**

```bash
brew install gradle
gradle wrapper --gradle-version 8.9
```
Expected: Erzeugt `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` und `gradle-wrapper.properties`. (Nach diesem Schritt nutzen wir nur noch `./gradlew`.)

- [ ] **Step 2: `settings.gradle.kts` schreiben**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "HackLampe"
include(":app")
```

- [ ] **Step 3: Root `build.gradle.kts` schreiben**

```kotlin
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}
```

- [ ] **Step 4: `gradle.properties` schreiben**

```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 5: `local.properties` schreiben (SDK-Pfad)**

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
cat local.properties
```
Expected: `sdk.dir=/opt/homebrew/share/android-commandlinetools` (oder Intel-Pfad).

- [ ] **Step 6: `app/build.gradle.kts` schreiben**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.hacklampe.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.hacklampe.app"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets["main"].java.srcDirs("src/main/kotlin")
    sourceSets["test"].java.srcDirs("src/test/kotlin")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 7: `app/src/main/res/values/strings.xml` schreiben**

```xml
<resources>
    <string name="app_name">HackLampe</string>
    <string name="notification_channel_name">Gestenerkennung</string>
    <string name="notification_title">HackLampe aktiv</string>
    <string name="notification_text">Doppel-Hack schaltet die Taschenlampe</string>
    <string name="notification_stop">Stoppen</string>
    <string name="tile_label">HackLampe</string>
    <string name="settings_start">Gestenerkennung starten</string>
    <string name="settings_stop">Gestenerkennung stoppen</string>
    <string name="settings_sensitivity">Empfindlichkeit</string>
    <string name="settings_autostart">Beim Neustart automatisch starten</string>
    <string name="settings_hint">Mache zwei schnelle Hackbewegungen, um die Taschenlampe umzuschalten.</string>
</resources>
```

- [ ] **Step 8: Minimales `AndroidManifest.xml` schreiben**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.DayNight">

        <activity
            android:name=".ui.SettingsActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

- [ ] **Step 9: Platzhalter-Activity anlegen, damit das Manifest auflöst**

Datei `app/src/main/kotlin/de/hacklampe/app/ui/SettingsActivity.kt`:
```kotlin
package de.hacklampe.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
```

- [ ] **Step 10: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. Eine APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`. (Beim allerersten Lauf lädt Gradle Abhängigkeiten — das dauert etwas.)

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "scaffold: Gradle Android project skeleton that builds"
```

---

## Task 2: ChopDetector (Erkennungskern, TDD)

Reine Kotlin-Logik ohne Android-Abhängigkeit — vollständig auf dem Mac testbar. Dies ist das Herz der App.

**Schnittstellen-Definition (gilt für alle Schritte):**
- `ChopDetector(sensitivity: Int = 5)` — Empfindlichkeit 1..10 (höher = empfindlicher).
- `fun onSample(timestampNanos: Long, magnitude: Float): Boolean` — füttert ein Sample; gibt `true` zurück, sobald ein **vollständiger Doppel-Chop** erkannt wurde.
- `fun setSensitivity(level: Int)` — passt Schwellwerte zur Laufzeit an.

**Files:**
- Create: `app/src/main/kotlin/de/hacklampe/app/detector/ChopDetector.kt`
- Test: `app/src/test/kotlin/de/hacklampe/app/detector/ChopDetectorTest.kt`

- [ ] **Step 1: Failing test schreiben**

Datei `app/src/test/kotlin/de/hacklampe/app/detector/ChopDetectorTest.kt`:
```kotlin
package de.hacklampe.app.detector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChopDetectorTest {

    private val ms = 1_000_000L // Nanosekunden pro Millisekunde

    // Bei Empfindlichkeit 5 liegt der Peak-Schwellwert ~18.3 m/s², die
    // Wieder-Scharf-Schranke ~7.3 m/s². 30f gilt als Peak, 0f als Ruhe.
    private val peak = 30f
    private val rest = 0f

    @Test
    fun singleChopDoesNotTrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
    }

    @Test
    fun doubleChopWithinWindowTriggers() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))          // Chop 1
        assertFalse(d.onSample(50 * ms, rest))    // wieder scharf
        assertTrue(d.onSample(300 * ms, peak))    // Chop 2 -> auslösen
    }

    @Test
    fun twoChopsTooSlowDoNotTrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
        assertFalse(d.onSample(50 * ms, rest))
        assertFalse(d.onSample(2000 * ms, peak))  // 2 s Abstand > Maximum
    }

    @Test
    fun secondPeakWithoutRearmIsIgnored() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))          // Chop 1, jetzt entschärft
        assertFalse(d.onSample(300 * ms, peak))   // bleibt hoch -> kein Chop 2
    }

    @Test
    fun cooldownSuppressesImmediateRetrigger() {
        val d = ChopDetector(sensitivity = 5)
        assertFalse(d.onSample(0, peak))
        assertFalse(d.onSample(50 * ms, rest))
        assertTrue(d.onSample(300 * ms, peak))    // erster Doppel-Chop
        // Sofort danach: weiterer Doppel-Chop im Cooldown wird unterdrückt
        assertFalse(d.onSample(350 * ms, rest))
        assertFalse(d.onSample(400 * ms, peak))
        assertFalse(d.onSample(450 * ms, rest))
        assertFalse(d.onSample(500 * ms, peak))
    }
}
```

- [ ] **Step 2: Test laufen lassen, Fehlschlag bestätigen**

Run: `./gradlew test`
Expected: FAIL — Kompilierfehler `unresolved reference: ChopDetector` (die Klasse existiert noch nicht).

- [ ] **Step 3: Minimale Implementierung schreiben**

Datei `app/src/main/kotlin/de/hacklampe/app/detector/ChopDetector.kt`:
```kotlin
package de.hacklampe.app.detector

/**
 * Erkennt das Doppel-Hack-Muster aus einem Strom von Beschleunigungs-Beträgen.
 * Reine Logik ohne Android-Abhängigkeit, damit voll unit-testbar.
 *
 * Ablauf:
 *  - Ein "Chop" wird erkannt, wenn der Betrag den Peak-Schwellwert übersteigt,
 *    nachdem er zuvor unter die Ruhe-Schranke gefallen war (steigende Flanke).
 *  - Zwei Chops mit einem Abstand im erlaubten Fenster ergeben einen Doppel-Chop.
 *  - Nach dem Auslösen sperrt ein Cooldown weitere Auslöser kurzzeitig.
 */
class ChopDetector(sensitivity: Int = 5) {

    private var peakThreshold = 0f
    private var restThreshold = 0f

    private val minGapNanos = 120_000_000L  // 120 ms: schneller wäre eine Bewegung
    private val maxGapNanos = 700_000_000L  // 700 ms: langsamer zählt als getrennt
    private val cooldownNanos = 800_000_000L // 800 ms Sperre nach Auslösen

    private var armed = true
    private var hasFirstChop = false
    private var firstChopNanos = 0L
    private var cooldownUntilNanos = Long.MIN_VALUE

    init {
        setSensitivity(sensitivity)
    }

    /** Empfindlichkeit 1 (unempfindlich) .. 10 (sehr empfindlich). */
    fun setSensitivity(level: Int) {
        val l = level.coerceIn(1, 10)
        // level 1 -> 25 m/s², level 10 -> 10 m/s² (linear interpoliert)
        peakThreshold = 25f - (l - 1) * (15f / 9f)
        restThreshold = peakThreshold * 0.4f
    }

    /** Liefert true, sobald ein vollständiger Doppel-Chop erkannt wurde. */
    fun onSample(timestampNanos: Long, magnitude: Float): Boolean {
        if (!armed) {
            if (magnitude < restThreshold) armed = true
            return false
        }
        if (magnitude < peakThreshold) return false
        // Steigende Flanke über den Schwellwert -> ein Chop
        armed = false
        return registerChop(timestampNanos)
    }

    private fun registerChop(now: Long): Boolean {
        if (now < cooldownUntilNanos) {
            hasFirstChop = false
            return false
        }
        if (!hasFirstChop) {
            hasFirstChop = true
            firstChopNanos = now
            return false
        }
        val gap = now - firstChopNanos
        return when {
            gap in minGapNanos..maxGapNanos -> {
                hasFirstChop = false
                cooldownUntilNanos = now + cooldownNanos
                true
            }
            gap > maxGapNanos -> {
                // zu langsam: dieser Chop startet ein neues Paar
                firstChopNanos = now
                false
            }
            else -> false // zu schnell: zweiten Peak ignorieren, ersten behalten
        }
    }
}
```

- [ ] **Step 4: Tests laufen lassen, Erfolg bestätigen**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, alle 5 Tests grün.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/de/hacklampe/app/detector/ChopDetector.kt app/src/test/kotlin/de/hacklampe/app/detector/ChopDetectorTest.kt
git commit -m "feat: ChopDetector double-chop detection with unit tests"
```

---

## Task 3: TorchController

Kapselt das Schalten der Taschenlampe. Braucht keine Kamera-Berechtigung (`setTorchMode`).

**Files:**
- Create: `app/src/main/kotlin/de/hacklampe/app/torch/TorchController.kt`

- [ ] **Step 1: TorchController schreiben**

Datei `app/src/main/kotlin/de/hacklampe/app/torch/TorchController.kt`:
```kotlin
package de.hacklampe.app.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/** Schaltet die LED-Taschenlampe und merkt sich den Zustand. */
class TorchController(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val torchCameraId: String? = cameraManager.cameraIdList.firstOrNull { id ->
        cameraManager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    @Volatile
    private var isOn = false

    fun toggle() = setTorch(!isOn)

    fun setTorch(enabled: Boolean) {
        val id = torchCameraId ?: return
        try {
            cameraManager.setTorchMode(id, enabled)
            isOn = enabled
        } catch (e: CameraAccessException) {
            // Taschenlampe gerade nicht verfügbar (z.B. Kamera in Benutzung)
        }
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/de/hacklampe/app/torch/TorchController.kt
git commit -m "feat: TorchController wrapping CameraManager.setTorchMode"
```

---

## Task 4: Prefs

Persistiert Einstellungen. Wird von Service, Tile, Activity und BootReceiver gebraucht.

**Files:**
- Create: `app/src/main/kotlin/de/hacklampe/app/data/Prefs.kt`

- [ ] **Step 1: Prefs schreiben**

Datei `app/src/main/kotlin/de/hacklampe/app/data/Prefs.kt`:
```kotlin
package de.hacklampe.app.data

import android.content.Context

/** Zentraler Zugriff auf gespeicherte Einstellungen. */
object Prefs {
    private const val FILE = "hacklampe_prefs"
    private const val KEY_SENSITIVITY = "sensitivity"
    private const val KEY_AUTOSTART = "autostart"

    const val DEFAULT_SENSITIVITY = 5

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getSensitivity(context: Context): Int =
        prefs(context).getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

    fun setSensitivity(context: Context, value: Int) =
        prefs(context).edit().putInt(KEY_SENSITIVITY, value).apply()

    fun isAutoStart(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTOSTART, false)

    fun setAutoStart(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_AUTOSTART, value).apply()
}
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/de/hacklampe/app/data/Prefs.kt
git commit -m "feat: Prefs for sensitivity and autostart settings"
```

---

## Task 5: GestureService (Foreground-Service + Sensor)

Verbindet Sensor → `ChopDetector` → `TorchController` und zeigt die Pflicht-Benachrichtigung. Hält einen statischen `isRunning`-Status für Kachel/Activity.

**Files:**
- Create: `app/src/main/kotlin/de/hacklampe/app/service/GestureService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Berechtigungen und Service ins Manifest eintragen**

In `app/src/main/AndroidManifest.xml` direkt nach der `<manifest ...>`-Zeile einfügen:
```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <uses-feature
        android:name="android.hardware.camera.flash"
        android:required="false" />
```

Und innerhalb von `<application>` (vor `</application>`) den Service ergänzen:
```xml
        <service
            android:name=".service.GestureService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="gesture_to_torch" />
        </service>
```

- [ ] **Step 2: GestureService schreiben**

Datei `app/src/main/kotlin/de/hacklampe/app/service/GestureService.kt`:
```kotlin
package de.hacklampe.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.detector.ChopDetector
import de.hacklampe.app.torch.TorchController
import kotlin.math.sqrt

class GestureService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private lateinit var detector: ChopDetector
    private lateinit var torch: TorchController

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        detector = ChopDetector(Prefs.getSensitivity(this))
        torch = TorchController(this)

        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        if (detector.onSample(event.timestamp, magnitude)) {
            torch.toggle()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        isRunning = false
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, GestureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_stop), stopPending)
            .build()
    }

    companion object {
        @Volatile
        var isRunning = false
            private set

        const val ACTION_STOP = "de.hacklampe.app.action.STOP"
        private const val CHANNEL_ID = "hacklampe_gestures"
        private const val NOTIF_ID = 1
    }
}
```

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/de/hacklampe/app/service/GestureService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: GestureService foreground service driving torch from chops"
```

---

## Task 6: HackTile (Quick-Settings-Kachel)

Startet/stoppt den Service aus dem Schnellzugriffsmenü und spiegelt den Zustand.

**Files:**
- Create: `app/src/main/kotlin/de/hacklampe/app/tile/HackTile.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: HackTile schreiben**

Datei `app/src/main/kotlin/de/hacklampe/app/tile/HackTile.kt`:
```kotlin
package de.hacklampe.app.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import de.hacklampe.app.service.GestureService

class HackTile : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        val intent = Intent(this, GestureService::class.java)
        if (GestureService.isRunning) {
            stopService(intent)
        } else {
            ContextCompat.startForegroundService(this, intent)
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (GestureService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
```

- [ ] **Step 2: Tile-Service ins Manifest eintragen**

Innerhalb von `<application>` (vor `</application>`) ergänzen:
```xml
        <service
            android:name=".tile.HackTile"
            android:exported="true"
            android:icon="@android:drawable/ic_menu_view"
            android:label="@string/tile_label"
            android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
            <intent-filter>
                <action android:name="android.service.quicksettings.action.QS_TILE" />
            </intent-filter>
        </service>
```

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/de/hacklampe/app/tile/HackTile.kt app/src/main/AndroidManifest.xml
git commit -m "feat: HackTile quick-settings tile to toggle the service"
```

---

## Task 7: SettingsActivity (Einstellungs-Oberfläche)

Service starten/stoppen, Empfindlichkeit per Schieberegler, Auto-Start-Schalter, Notification-Berechtigung anfragen.

**Files:**
- Create: `app/src/main/res/layout/activity_settings.xml`
- Modify: `app/src/main/kotlin/de/hacklampe/app/ui/SettingsActivity.kt` (ersetzt Platzhalter aus Task 1)

- [ ] **Step 1: Layout schreiben**

Datei `app/src/main/res/layout/activity_settings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_hint"
        android:paddingBottom="24dp" />

    <Button
        android:id="@+id/toggleButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_start" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_sensitivity"
        android:paddingTop="32dp" />

    <SeekBar
        android:id="@+id/sensitivitySeekBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:max="9" />

    <CheckBox
        android:id="@+id/autostartCheckBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_autostart"
        android:paddingTop="32dp" />

</LinearLayout>
```

- [ ] **Step 2: SettingsActivity-Inhalt ersetzen**

Datei `app/src/main/kotlin/de/hacklampe/app/ui/SettingsActivity.kt` komplett ersetzen:
```kotlin
package de.hacklampe.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import de.hacklampe.app.R
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.service.GestureService

class SettingsActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* egal */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        toggleButton = findViewById(R.id.toggleButton)
        val seekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)
        val autostart = findViewById<CheckBox>(R.id.autostartCheckBox)

        // Empfindlichkeit 1..10 auf SeekBar 0..9 abbilden
        seekBar.progress = Prefs.getSensitivity(this) - 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                Prefs.setSensitivity(this@SettingsActivity, progress + 1)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        autostart.isChecked = Prefs.isAutoStart(this)
        autostart.setOnCheckedChangeListener { _, checked ->
            Prefs.setAutoStart(this, checked)
        }

        toggleButton.setOnClickListener { onToggleClicked() }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updateToggleLabel()
    }

    private fun onToggleClicked() {
        val intent = Intent(this, GestureService::class.java)
        if (GestureService.isRunning) {
            stopService(intent)
        } else {
            ContextCompat.startForegroundService(this, intent)
        }
        updateToggleLabel()
    }

    private fun updateToggleLabel() {
        toggleButton.setText(
            if (GestureService.isRunning) R.string.settings_stop else R.string.settings_start
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
```

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/activity_settings.xml app/src/main/kotlin/de/hacklampe/app/ui/SettingsActivity.kt
git commit -m "feat: SettingsActivity with start/stop, sensitivity, autostart"
```

---

## Task 8: BootReceiver (Auto-Start)

Startet den Service nach Neustart, wenn der Auto-Start-Schalter aktiv ist.

**Files:**
- Create: `app/src/main/kotlin/de/hacklampe/app/boot/BootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: BootReceiver schreiben**

Datei `app/src/main/kotlin/de/hacklampe/app/boot/BootReceiver.kt`:
```kotlin
package de.hacklampe.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import de.hacklampe.app.data.Prefs
import de.hacklampe.app.service.GestureService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isAutoStart(context)) {
            ContextCompat.startForegroundService(
                context, Intent(context, GestureService::class.java)
            )
        }
    }
}
```

- [ ] **Step 2: Receiver ins Manifest eintragen**

Innerhalb von `<application>` (vor `</application>`) ergänzen:
```xml
        <receiver
            android:name=".boot.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
```

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/de/hacklampe/app/boot/BootReceiver.kt app/src/main/AndroidManifest.xml
git commit -m "feat: BootReceiver to auto-start service when enabled"
```

---

## Task 9: Gerätetest & Kalibrierung (manuell)

Emulator hat weder echte Hackbewegung noch LED — dieser Test braucht ein echtes Telefon.

**Files:** keine

- [ ] **Step 1: Telefon verbinden (USB-Debugging)**

Am Telefon: Einstellungen → Über das Telefon → 7× auf „Build-Nummer" tippen → Entwickleroptionen → „USB-Debugging" an. Per Kabel anschließen, Dialog am Telefon bestätigen.

Run: `adb devices`
Expected: Eine Zeile mit der Geräte-Seriennummer und `device` (nicht `unauthorized`).

- [ ] **Step 2: App installieren**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: `Success`.

- [ ] **Step 3: App öffnen und starten**

App „HackLampe" öffnen, Notification-Berechtigung erlauben, „Gestenerkennung starten" tippen.
Expected: Eine dauerhafte Benachrichtigung „HackLampe aktiv" erscheint.

- [ ] **Step 4: Geste testen**

Zwei schnelle Hackbewegungen mit dem Telefon machen.
Expected: Taschenlampe geht an. Erneut: geht aus.

- [ ] **Step 5: Empfindlichkeit kalibrieren**

Falls zu unempfindlich (Geste löst selten aus): Schieberegler nach rechts. Falls Fehlauslöser (z.B. beim Gehen): nach links. Jeweils Service kurz stoppen/starten ist nicht nötig — der Service liest die Empfindlichkeit beim nächsten Start; für sofortige Wirkung Service über die Kachel aus- und wieder einschalten. Notiere gute Defaults; falls die Startwerte (Schwellwerte/Zeitfenster in `ChopDetector`) generell schlecht passen, dort anpassen, neu bauen, neu installieren.

- [ ] **Step 6: Kachel testen**

Schnellzugriffsmenü öffnen (von oben wischen) → Kachel bearbeiten → „HackLampe"-Kachel hinzufügen. Kachel tippen.
Expected: Service startet/stoppt, Kachel wird aktiv/inaktiv, Geste funktioniert auch bei geschlossener App.

- [ ] **Step 7: Auto-Start testen (optional)**

Auto-Start in den Einstellungen aktivieren, Telefon neu starten.
Expected: Service läuft nach dem Boot wieder (Benachrichtigung erscheint). Hinweis: Manche Hersteller (Xiaomi, Huawei, Samsung) blockieren Auto-Start — ggf. in den Akku-/Autostart-Einstellungen des Herstellers erlauben.

---

## Self-Review

**Spec-Abdeckung:**
- Doppel-Hack-Erkennung → Task 2 ✓
- Taschenlampe schalten → Task 3 ✓
- Hintergrundbetrieb / Foreground-Service + Notification → Task 5 ✓
- Quick-Settings-Kachel → Task 6 ✓
- Einstellungs-App + Empfindlichkeit + Berechtigungen → Task 7 ✓
- Auto-Start beim Boot (optional) → Task 8 ✓
- Toolchain-Setup (Nutzer hat nichts installiert) → Task 0 ✓
- Min-SDK 31 / compile 35, Kotlin, Gradle-Wrapper → Task 1 ✓
- Unit-Tests auf dem Mac + manueller Gerätetest → Task 2 / Task 9 ✓

**Konsistenz-Check:**
- `ChopDetector(sensitivity)`, `onSample(Long, Float): Boolean`, `setSensitivity(Int)` durchgängig gleich verwendet ✓
- `GestureService.isRunning`, `GestureService.ACTION_STOP` in Tile/Activity korrekt referenziert ✓
- `Prefs.getSensitivity/setSensitivity/isAutoStart/setAutoStart` durchgängig konsistent ✓
- Paket-Pfade `de.hacklampe.app.*` einheitlich ✓

Keine Platzhalter, keine offenen TODOs.
