# HackLampe (Chop'n'Light) — Design

**Datum:** 2026-06-17
**Status:** Entwurf, vom Nutzer freizugeben

## Ziel

Eine native Android-App, die die „Hackgeste" wie bei Motorola auf beliebigen
Android-Telefonen nachrüstet: Mit einer doppelten Hackbewegung („Doppel-Chop")
wird die Taschenlampe an- bzw. ausgeschaltet. Die Geste funktioniert auch, wenn
die App nicht offen ist.

- **Name (Deutsch):** HackLampe
- **Anzeigename (Englisch):** Chop'n'Light
- **Paket-ID:** `de.hacklampe.app`
- **Sprache:** Kotlin
- **Min-SDK:** Android 12 (API 31)
- **Compile/Target-SDK:** Android 15 (API 35)

## Wichtige technische Randbedingung

Kontinuierliches Abhören des Beschleunigungssensors im Hintergrund ist auf
modernem Android **nur über einen Foreground-Service mit dauerhafter
Benachrichtigung** möglich. Es gibt keinen zuverlässigen Weg ohne Notification
(recherchiert und bestätigt; der `SIGNIFICANT_MOTION`-Trigger-Sensor ist für
eine schnelle, spezifische Hackbewegung ungeeignet). Die Benachrichtigung ist
daher Teil des Designs, nicht ein Bug.

Steuerung des Service erfolgt über:
- **Quick-Settings-Kachel** (Schnellzugriffsmenü, von oben wischen)
- **Einstellungs-App**

Kein Homescreen-Widget (YAGNI; Kachel deckt den Bedarf).

## Architektur

Kern ist der `GestureService` (Foreground-Service). Drumherum sitzen
Steueroberflächen (Kachel, Activity) und ein gerätefreier, voll testbarer
Erkennungskern (`ChopDetector`).

```
SettingsActivity ─┐
HackTile ─────────┼─(start/stop)─► GestureService ──► SensorManager (Linearbeschleunigung)
                  │                      │
BootReceiver ─────┘                      ├─► ChopDetector (reine Kotlin-Logik)
                                         └─► TorchController ──► CameraManager.setTorchMode()
Prefs (Einstellungen) ◄──── alle
```

## Komponenten

### `ChopDetector` (reine Kotlin-Klasse, keine Android-Abhängigkeit)
- **Aufgabe:** Erkennt aus einem Strom von Sensor-Samples das Doppel-Hack-Muster.
- **Schnittstelle:** `onSample(timestampNanos: Long, magnitude: Float): Boolean`
  gibt `true` zurück, wenn ein Doppel-Chop vollständig erkannt wurde.
  `setSensitivity(level)` passt Schwellwert/Timing an.
- **Abhängigkeiten:** keine. → Voll unit-testbar auf dem Mac.

### `GestureService` (Foreground-Service, Typ `specialUse`/`sensor`)
- **Aufgabe:** Registriert den Linearbeschleunigungssensor, berechnet den
  Beschleunigungsbetrag, füttert den `ChopDetector`, schaltet bei erkanntem
  Doppel-Chop die Taschenlampe. Zeigt die Pflicht-Benachrichtigung mit Status
  und einer Stopp-Aktion.
- **Abhängigkeiten:** `SensorManager`, `ChopDetector`, `TorchController`, `Prefs`.

### `TorchController`
- **Aufgabe:** Kapselt `CameraManager.setTorchMode()`, hält den An/Aus-Zustand,
  registriert ggf. `TorchCallback`, um externe Änderungen mitzubekommen.
- **Abhängigkeiten:** `CameraManager`.

### `HackTile` (`TileService`)
- **Aufgabe:** Quick-Settings-Kachel. Tippen startet/stoppt `GestureService`.
  Kachelzustand (an/aus) spiegelt, ob der Service läuft.

### `SettingsActivity`
- **Aufgabe:** Service an/aus, Empfindlichkeits-Schieberegler, „beim Boot
  starten"-Schalter, Anfordern der nötigen Berechtigungen
  (`POST_NOTIFICATIONS`, ggf. Kamera/Taschenlampe), kurze Erklärung der Geste.

### `BootReceiver`
- **Aufgabe:** Startet den Service nach Geräte-Neustart — nur wenn „beim Boot
  starten" in den Einstellungen aktiviert ist.

### `Prefs`
- **Aufgabe:** Persistiert Einstellungen (Empfindlichkeit, Auto-Start beim Boot,
  zuletzt gewünschter Service-Zustand) via `SharedPreferences`/DataStore.

## Gesten-Algorithmus (Doppel-Hack)

Schwellenwertbasierter Zustandsautomat (kein ML — leichtgewichtig,
batteriefreundlich, testbar):

1. Sensor `TYPE_LINEAR_ACCELERATION` liefert x/y/z ohne Schwerkraft.
   → Betrag `m = sqrt(x²+y²+z²)`.
2. **Ein Chop = Peak:** `m` überschreitet Schwellwert `T` und fällt innerhalb
   eines kurzen Fensters (z.B. ≤ 200 ms) wieder unter eine untere Schranke.
3. **Doppel-Chop:** Zwei Chops innerhalb eines Zeitfensters (Default ca.
   150–600 ms zwischen den Peaks) → auslösen.
4. **Cooldown:** Nach dem Auslösen kurze Sperre (z.B. 800 ms), um Mehrfach-
   und Selbstauslösung zu verhindern.
5. **Empfindlichkeit:** Der Regler verschiebt `T` (niedriger = empfindlicher)
   und ggf. die Zeitfenster. Defaults werden auf dem Gerät feinjustiert.

Die konkreten Zahlenwerte sind Startwerte und werden beim manuellen Gerätetest
kalibriert.

## Berechtigungen & Manifest

- `FOREGROUND_SERVICE` + passender Service-Typ-Permission
  (`FOREGROUND_SERVICE_SPECIAL_USE` oder `_HEALTH`/`_SENSORS` — beim Build final
  festgelegt, abhängig von Play-Policy-Tauglichkeit).
- `POST_NOTIFICATIONS` (Android 13+).
- `RECEIVE_BOOT_COMPLETED` (nur für Auto-Start).
- `HIGH_SAMPLING_RATE_SENSORS` falls höhere Sensor-Rate nötig.
- Taschenlampe: `CameraManager.setTorchMode()` braucht **keine** Kamera-
  Berechtigung (nur Flash-Feature im Manifest deklarieren).

## Toolchain (wird auf dem Mac eingerichtet)

Der Nutzer hat noch nichts installiert. Einzurichten:
- **JDK 17** (Homebrew).
- **Android SDK Command-Line-Tools** + `platform-tools` (`adb`),
  `build-tools`, `platform android-35`.
- **Gradle Wrapper** im Projekt → Build per `./gradlew assembleDebug` ohne
  Android Studio.
- Ergebnis: `app-debug.apk` zum Aufspielen auf das Telefon.

## Test-Strategie

- **Unit-Tests** für `ChopDetector` mit synthetischen/aufgezeichneten Sensor-
  Sequenzen (TDD). Läuft komplett auf dem Mac, kein Gerät nötig.
- **Manueller Gerätetest** für Service, Kachel, Taschenlampe und
  Empfindlichkeits-Kalibrierung auf einem echten Telefon (Emulator hat weder
  echten Beschleunigungssensor noch LED-Taschenlampe).

## Bewusst weggelassen (YAGNI)

- Homescreen-Widget (Kachel reicht).
- ML-basierte Gestenerkennung.
- Konfiguration der Geste über Empfindlichkeit hinaus.
- Play-Store-Release-Signierung/Veröffentlichung.
