# HackLampe — Play Console: Formular-Antworten & Hinweise

Vorgefertigte Antworten zum Copy-&-Paste für die Einreichung im Google Play Console.
(Die Google-Prüfung liest Englisch — die Begründungstexte sind daher auf Englisch.)

---

## 1. Foreground Service `specialUse` — Begründung (WICHTIG)

Beim Hochladen fragt Play wegen `FOREGROUND_SERVICE_SPECIAL_USE` nach einer Begründung.
Das ist der häufigste Review-Stolperstein. Text zum Einfügen:

> HackLampe continuously monitors the device accelerometer to detect a deliberate
> double "chop" gesture that toggles the flashlight, including while the app is in
> the background or closed — this is the app's core, user-initiated purpose. This
> requires an ongoing foreground service. None of the predefined foreground service
> types apply: the app does not play media, track location, sync data over the
> network, manage a connected device, make phone calls, etc. It only reads the
> motion sensor and toggles the torch. We therefore use the `specialUse` type with
> an accurate, user-visible persistent notification. The service runs only while the
> user has explicitly enabled gesture detection and can be stopped at any time from
> the notification or the Quick Settings tile.

Manifest-Subtyp (bereits gesetzt): `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE = "gesture_to_torch"`.

---

## 2. Datensicherheit / Data safety

- **Erhebt oder teilt die App Nutzerdaten?** → **Nein** (keine Erhebung, keine Weitergabe).
- Begründung: Die App hat **keine `INTERNET`-Berechtigung**, keine Server, keine Analyse, keine Werbung. Einstellungen (Kalibrierung, Empfindlichkeit) liegen ausschließlich lokal (`SharedPreferences`).
- Alle Datenkategorien: **keine Erhebung**.
- Verschlüsselung bei Übertragung / Löschanfragen: **N/A** (es verlässt nichts das Gerät).

---

## 3. Inhaltsfreigabe (IARC-Fragebogen)

- Kategorie der App: **Tools / Dienstprogramme**.
- Gewalt, Sexualität, Schimpfwörter, Drogen, Glücksspiel, Angst/Schock: **alles Nein**.
- Teilt Standort / persönliche Daten: **Nein**.
- Nutzerinteraktion / Online-Funktionen: **Nein**.
- Ergebnis: Freigabe **für alle Altersgruppen** (USK 0 / PEGI 3 / Everyone).

---

## 4. App-Zugriff (App access)

- Ist ein Teil der App eingeschränkt (Login)? → **Nein**, alle Funktionen ohne Anmeldung verfügbar.

---

## 5. Werbung & Zielgruppe

- **Enthält Werbung?** → Nein.
- Zielgruppe/Alter: Da keine Datenerhebung — für alle Altersgruppen wählbar.

---

## 6. Berechtigungen — Kurzbegründung (falls nachgefragt)

| Berechtigung | Zweck |
|---|---|
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Gestenerkennung im Hintergrund/bei geschlossener App (siehe §1) |
| `POST_NOTIFICATIONS` | Pflicht-Statusbenachrichtigung des Foreground-Service |
| `HIGH_SAMPLING_RATE_SENSORS` | Beschleunigungssensor mit ausreichender Rate auslesen |
| `RECEIVE_BOOT_COMPLETED` | optionaler Auto-Start des Dienstes nach Neustart |

Hinweis: Die Taschenlampe wird über `CameraManager.setTorchMode()` geschaltet — **keine Kamera-Berechtigung** nötig. Der Beschleunigungssensor braucht keine Berechtigung.

---

## 7. Checkliste vor dem Release

- [ ] AAB hochladen: `HackLampe-1.0-playstore.aab`
- [ ] Datenschutz-URL eintragen: https://v3nomsoup.github.io/hacklampe/ (GitHub Pages aktivieren)
- [ ] Store-Texte aus `play-store-listing.md` einfügen (6 Sprachen)
- [ ] Icon 512×512: `HackLampe-icons/playstore-icon.png`
- [ ] Feature-Graphic 1024×500: `docs/feature-graphic.png`
- [ ] Screenshots (≥ 2) je Sprache
- [ ] specialUse-Begründung (§1) eintragen
- [ ] Datensicherheit = keine Daten (§2)
- [ ] Inhaltsfreigabe-Fragebogen (§3)
