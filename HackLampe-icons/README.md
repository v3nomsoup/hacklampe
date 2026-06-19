# HackLampe — App-Icon (klassisches Adaptive Icon)

Dunkler Vordergrund (Taschenlampe + Doppel-Hack-Chevrons) auf amberfarbenem
Hintergrund (#FFB12E).

## Einbau in Android Studio

1. Kopiere den Inhalt von `res/` nach `app/src/main/res/` (Ordner zusammenfuehren).
2. Im Manifest sicherstellen:
       android:icon="@mipmap/ic_launcher"
       android:roundIcon="@mipmap/ic_launcher_round"
3. Build → das System maskiert den Vordergrund automatisch auf Kreis, Squircle usw.

## Was ist drin

res/
  drawable/
    ic_launcher_background.xml   amberfarbene Flaeche (Hintergrund-Layer)
    ic_launcher_foreground.xml   Lampe + Chevrons, farbig (Vordergrund-Layer)
    ic_launcher_monochrome.xml   einfarbige Silhouette fuer themed icons (Android 13+)
  mipmap-anydpi-v26/
    ic_launcher.xml              verknuepft die drei Layer (API 26+)
    ic_launcher_round.xml        dito fuer runde Launcher
  mipmap-mdpi … xxxhdpi/
    ic_launcher.png              Fallback fuer API < 26 (Squircle gebacken)
    ic_launcher_round.png        runder Fallback
  values/
    ic_launcher_background.xml   Hintergrundfarbe als Ressource

playstore-icon.png   512×512, fuer den Play-Store-Eintrag
ic_launcher-source.svg  editierbares Vektor-Original
preview-foreground.png  nur der Vordergrund (transparent)

## Hinweise

- Vordergrund-Inhalt sitzt in der zentralen Sicherheitszone (66/108 dp), wird also
  von keiner Maskenform abgeschnitten.
- Die Quick-Settings-Kachel deiner App ist NICHT dieses Icon. Dafuer setzt du in
  der TileService das Tile-Icon per Icon.createWithResource(...) — am besten mit
  einer einfarbigen Variante (du kannst `ic_launcher_monochrome` als Vorlage nehmen
  oder ein eigenes 24dp-Vektor-Icon ableiten), da Kacheln meist nur einfarbig
  dargestellt werden.

## Quick-Settings-Kachel (Tile)

Zwei fertige 24dp-Vektor-Icons liegen unter `res/drawable/`:

- `ic_tile_flashlight.xml`  nur Taschenlampe + Strahl (am klarsten lesbar)
- `ic_tile_hacklampe.xml`   Taschenlampe + Chevrons (mit Hack-Geste)

Beide sind einfarbig weiss mit `android:tint="?attr/colorControlNormal"`, damit das
System sie je nach Kachel-Zustand (aktiv/inaktiv) korrekt einfaerbt.

Im TileService verwenden:

    override fun onStartListening() {
        qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile_flashlight)
        qsTile.state = if (torchOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

Tipp: Fuer den Aktiv-Zustand kannst du dasselbe Icon lassen — Android hebt aktive
Kacheln automatisch farblich hervor. Wenn du moechtest, nimm im Aktiv-Zustand die
Variante mit Chevrons und im Inaktiv-Zustand die schlichte, als visuelles Feedback.
