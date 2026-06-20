# HackLampe — Privacy Policy / Datenschutzerklärung

**Last updated / Zuletzt aktualisiert: 2026-06-20**

---

## Privacy Policy (English)

> **Short version:** HackLampe collects no personal data, has no internet access, and sends nothing off your device. Everything happens locally on your phone.

### About the app

HackLampe is a flashlight app. It toggles your phone's flashlight (torch) when you perform a double "chop" gesture, which is detected using the device's accelerometer. The app works **completely offline**.

### Data we collect

**None.** HackLampe does not collect, store, or transmit any personal data. It has no internet or network permission, no servers, no analytics, no advertising, no tracking, and no user accounts. Nothing ever leaves your device.

The only data the app stores are your calibration settings (e.g. gesture sensitivity). These are saved **locally on your device only**, using Android's `SharedPreferences`, and are never transmitted anywhere. They are removed when you uninstall the app.

### Permissions and why they are used

| Permission | Why it is used |
| --- | --- |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | To keep listening for the chop gesture while the app is closed. Android requires a persistent notification for this. |
| `POST_NOTIFICATIONS` | To show the persistent status notification that Android requires for the background gesture service. |
| `HIGH_SAMPLING_RATE_SENSORS` | To read the accelerometer at a useful rate so the gesture can be detected reliably. |
| `RECEIVE_BOOT_COMPLETED` | Optional: to auto-start the gesture service after the device reboots. |

**Note:** The flashlight is controlled via `CameraManager.setTorchMode()` and requires **no camera permission**. The accelerometer also requires no permission.

### Sharing with third parties

No data is shared with any third parties. Because the app collects nothing and has no network access, there is nothing to share.

### Children

HackLampe is safe for all ages. It collects no data from anyone, including children.

### Changes to this policy

If this policy changes, the updated version will be published with a new "last updated" date.

### Contact

Questions about this privacy policy? Contact: hello@eckert.rocks

---

## Datenschutzerklärung (Deutsch)

> **Kurzfassung:** HackLampe erfasst keine personenbezogenen Daten, hat keinen Internetzugriff und sendet nichts von deinem Gerät. Alles geschieht lokal auf deinem Telefon.

### Über die App

HackLampe ist eine Taschenlampen-App. Sie schaltet die Taschenlampe (Torch) deines Telefons ein und aus, wenn du eine doppelte „Hack"-Geste ausführst, die über den Beschleunigungssensor des Geräts erkannt wird. Die App funktioniert **vollständig offline**.

### Welche Daten wir erfassen

**Keine.** HackLampe erfasst, speichert oder überträgt keine personenbezogenen Daten. Die App hat keine Internet- oder Netzwerkberechtigung, keine Server, keine Analyse, keine Werbung, kein Tracking und keine Benutzerkonten. Es verlässt niemals etwas dein Gerät.

Die einzigen Daten, die die App speichert, sind deine Kalibrierungseinstellungen (z. B. Gestenempfindlichkeit). Diese werden **ausschließlich lokal auf deinem Gerät** über Androids `SharedPreferences` gespeichert und nirgendwohin übertragen. Sie werden gelöscht, wenn du die App deinstallierst.

### Berechtigungen und ihre Verwendung

| Berechtigung | Wofür sie verwendet wird |
| --- | --- |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Um die Hack-Geste weiter zu erkennen, während die App geschlossen ist. Android verlangt dafür eine dauerhafte Benachrichtigung. |
| `POST_NOTIFICATIONS` | Um die dauerhafte Status-Benachrichtigung anzuzeigen, die Android für den Hintergrund-Gestendienst verlangt. |
| `HIGH_SAMPLING_RATE_SENSORS` | Um den Beschleunigungssensor mit einer brauchbaren Rate auszulesen, damit die Geste zuverlässig erkannt wird. |
| `RECEIVE_BOOT_COMPLETED` | Optional: um den Gestendienst nach einem Neustart des Geräts automatisch zu starten. |

**Hinweis:** Die Taschenlampe wird über `CameraManager.setTorchMode()` gesteuert und benötigt **keine Kamera-Berechtigung**. Der Beschleunigungssensor benötigt ebenfalls keine Berechtigung.

### Weitergabe an Dritte

Es werden keine Daten an Dritte weitergegeben. Da die App nichts erfasst und keinen Netzwerkzugriff hat, gibt es nichts weiterzugeben.

### Kinder

HackLampe ist für jedes Alter geeignet. Die App erfasst von niemandem Daten, auch nicht von Kindern.

### Änderungen dieser Erklärung

Falls sich diese Erklärung ändert, wird die aktualisierte Version mit einem neuen Datum „Zuletzt aktualisiert" veröffentlicht.

### Kontakt

Fragen zu dieser Datenschutzerklärung? Kontakt: hello@eckert.rocks

---

### Disclaimer / Markenhinweis

HackLampe is an independent, unofficial app and is not affiliated with, endorsed by, or sponsored by Motorola or Lenovo. "Motorola" is a trademark of its respective owners and is used here only to describe a similar gesture.

HackLampe ist eine eigenständige, inoffizielle App und steht in keiner Verbindung zu Motorola oder Lenovo, wird von diesen nicht unterstützt oder gesponsert. „Motorola" ist eine Marke der jeweiligen Inhaber und wird hier nur zur Beschreibung einer ähnlichen Geste genannt.
