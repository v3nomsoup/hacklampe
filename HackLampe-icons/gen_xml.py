#!/usr/bin/env python3
import os, math

ROOT = "/home/claude/HackLampe-icons"
RES = os.path.join(ROOT, "res")
AMBER, DARK, LENS, CONE = "#FFFFB12E", "#FF20243F", "#FFFFE9A8", "#FFFFF4CF"

def rrect(x, y, w, h, r):
    r = min(r, w/2, h/2)
    return (f"M{x+r:.2f},{y:.2f} L{x+w-r:.2f},{y:.2f} Q{x+w:.2f},{y:.2f} {x+w:.2f},{y+r:.2f} "
            f"L{x+w:.2f},{y+h-r:.2f} Q{x+w:.2f},{y+h:.2f} {x+w-r:.2f},{y+h:.2f} "
            f"L{x+r:.2f},{y+h:.2f} Q{x:.2f},{y+h:.2f} {x:.2f},{y+h-r:.2f} "
            f"L{x:.2f},{y+r:.2f} Q{x:.2f},{y:.2f} {x+r:.2f},{y:.2f} Z")

CH1, CH2 = "M38.6,22.1 L47.3,27.9 L56.0,22.1", "M38.6,29.0 L47.3,34.8 L56.0,29.0"
CONE_P = "M61.8,52.2 L85.0,39.5 L85.0,85.9 L61.8,73.1 Z"
RAY1, RAY2 = "M65.9,59.2 L82.1,51.1", "M65.9,66.2 L82.1,75.4"
TAIL = rrect(22.9,58.6,3.5,8.1,1.2); BODY = rrect(26.4,56.3,25.5,12.8,2.9)
SWITCH = rrect(34.5,54.0,7.0,2.9,1.2); HEAD = "M51.9,54.0 L60.1,49.9 L60.1,75.4 L51.9,71.4 Z"
LENS_P = rrect(59.5,51.1,2.3,23.2,1.0)

drawable = os.path.join(RES, "drawable"); os.makedirs(drawable, exist_ok=True)
anydpi = os.path.join(RES, "mipmap-anydpi-v26"); os.makedirs(anydpi, exist_ok=True)
values = os.path.join(RES, "values"); os.makedirs(values, exist_ok=True)

# Background (solid amber)
open(os.path.join(drawable, "ic_launcher_background.xml"), "w").write(
f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{AMBER}" android:pathData="M0,0h108v108h-108z"/>
</vector>
''')

# Foreground (full colour content)
open(os.path.join(drawable, "ic_launcher_foreground.xml"), "w").write(
f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{CONE}" android:fillAlpha="0.65" android:pathData="{CONE_P}"/>
    <path android:fillColor="{DARK}" android:pathData="{TAIL}"/>
    <path android:fillColor="{DARK}" android:pathData="{BODY}"/>
    <path android:fillColor="{DARK}" android:pathData="{SWITCH}"/>
    <path android:fillColor="{DARK}" android:pathData="{HEAD}"/>
    <path android:fillColor="{LENS}" android:pathData="{LENS_P}"/>
    <path android:strokeColor="{DARK}" android:strokeWidth="4.1" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{CH1}"/>
    <path android:strokeColor="{DARK}" android:strokeWidth="4.1" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{CH2}"/>
</vector>
''')

# Monochrome (single colour silhouette for themed icons, Android 13+)
M = "#FF000000"
open(os.path.join(drawable, "ic_launcher_monochrome.xml"), "w").write(
f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{M}" android:pathData="{TAIL}"/>
    <path android:fillColor="{M}" android:pathData="{BODY}"/>
    <path android:fillColor="{M}" android:pathData="{SWITCH}"/>
    <path android:fillColor="{M}" android:pathData="{HEAD}"/>
    <path android:fillColor="{M}" android:pathData="{LENS_P}"/>
    <path android:strokeColor="{M}" android:strokeWidth="1.7" android:strokeLineCap="round" android:pathData="{RAY1}"/>
    <path android:strokeColor="{M}" android:strokeWidth="1.7" android:strokeLineCap="round" android:pathData="{RAY2}"/>
    <path android:strokeColor="{M}" android:strokeWidth="4.1" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{CH1}"/>
    <path android:strokeColor="{M}" android:strokeWidth="4.1" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{CH2}"/>
</vector>
''')

# anydpi-v26 adaptive configs
cfg = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
'''
open(os.path.join(anydpi, "ic_launcher.xml"), "w").write(cfg)
open(os.path.join(anydpi, "ic_launcher_round.xml"), "w").write(cfg)

# optional colors resource
open(os.path.join(values, "ic_launcher_background.xml"), "w").write(
'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFB12E</color>
</resources>
''')

readme = '''# HackLampe — App-Icon (klassisches Adaptive Icon)

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
'''
open(os.path.join(ROOT, "README.md"), "w").write(readme)
print("XMLs + README done")
