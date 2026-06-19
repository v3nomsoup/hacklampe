#!/usr/bin/env python3
import os, cairosvg
from PIL import Image

ROOT = "/home/claude/HackLampe-icons"
TILES = os.path.join(ROOT, "res", "drawable")
os.makedirs(TILES, exist_ok=True)
PREV = "/home/claude/tile_preview"; os.makedirs(PREV, exist_ok=True)

def rrect(x, y, w, h, r):
    r = min(r, w/2, h/2)
    return (f"M{x+r:.2f},{y:.2f} L{x+w-r:.2f},{y:.2f} Q{x+w:.2f},{y:.2f} {x+w:.2f},{y+r:.2f} "
            f"L{x+w:.2f},{y+h-r:.2f} Q{x+w:.2f},{y+h:.2f} {x+w-r:.2f},{y+h:.2f} "
            f"L{x+r:.2f},{y+h:.2f} Q{x:.2f},{y+h:.2f} {x:.2f},{y+h-r:.2f} "
            f"L{x:.2f},{y+r:.2f} Q{x:.2f},{y:.2f} {x+r:.2f},{y:.2f} Z")

# shared flashlight (24dp space)
TAIL = rrect(2.6, 14.2, 1.4, 2.8, 0.6)
BODY = rrect(4.0, 13.4, 7.2, 4.4, 1.2)
SW   = rrect(6.6, 12.6, 2.2, 1.0, 0.4)
HEAD = "M11.2,11.9 L13.6,11.0 L13.6,20.2 L11.2,19.3 Z"
LENS = rrect(13.6, 12.2, 0.9, 6.8, 0.4)
B1, B2, B3 = "M15.2,13.9 L19.6,11.8", "M15.4,15.6 L20.2,15.6", "M15.2,17.3 L19.6,19.4"
CH1, CH2 = "M7.5,3.0 L12,5.6 L16.5,3.0", "M7.5,6.2 L12,8.8 L16.5,6.2"

def flashlight(color):
    return (f'<path fill="{color}" d="{TAIL}"/>'
            f'<path fill="{color}" d="{BODY}"/>'
            f'<path fill="{color}" d="{SW}"/>'
            f'<path fill="{color}" d="{HEAD}"/>'
            f'<path fill="{color}" d="{LENS}"/>'
            f'<path stroke="{color}" stroke-width="1.3" stroke-linecap="round" fill="none" d="{B1}"/>'
            f'<path stroke="{color}" stroke-width="1.3" stroke-linecap="round" fill="none" d="{B2}"/>'
            f'<path stroke="{color}" stroke-width="1.3" stroke-linecap="round" fill="none" d="{B3}"/>')

def chevrons(color):
    return (f'<path stroke="{color}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" fill="none" d="{CH1}"/>'
            f'<path stroke="{color}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" fill="none" d="{CH2}"/>')

# Variant A: flashlight only, vertically centred & a touch larger
flA = flashlight("{c}").replace("13.4","13.0").replace("12.6","12.2")  # nudge up slightly
def svgA(c): return f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">{flashlight(c)}</svg>'
def svgB(c): return f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">{chevrons(c)}{flashlight(c)}</svg>'

# Android vector drawables (white => tinted by system)
def vd(paths):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp"\n'
            '    android:viewportWidth="24" android:viewportHeight="24"\n'
            '    android:tint="?attr/colorControlNormal">\n' + paths + '</vector>\n')

def vpath_fill(d): return f'    <path android:fillColor="#FFFFFFFF" android:pathData="{d}"/>\n'
def vpath_stroke(d, w): return (f'    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="{w}" '
                                f'android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{d}"/>\n')

fl_paths = "".join(vpath_fill(p) for p in [TAIL, BODY, SW, HEAD, LENS]) + \
           "".join(vpath_stroke(p, "1.3") for p in [B1, B2, B3])
ch_paths = "".join(vpath_stroke(p, "1.7") for p in [CH1, CH2])

open(os.path.join(TILES, "ic_tile_flashlight.xml"), "w").write(vd(fl_paths))
open(os.path.join(TILES, "ic_tile_hacklampe.xml"), "w").write(vd(ch_paths + fl_paths))
print("tile vectors written")

# previews on dark + light tile backgrounds, rendered at 24 then upscaled
def preview(svg_fn, name):
    for bg, tag in [("#202327", "dark"), ("#E9E6DF", "light")]:
        cssvg = svg_fn("#FFFFFF" if tag == "dark" else "#1C1B1F")
        cairosvg.svg2png(bytestring=cssvg.encode(), write_to=f"{PREV}/{name}_{tag}_24.png",
                         output_width=24, output_height=24, background_color=bg)
        Image.open(f"{PREV}/{name}_{tag}_24.png").resize((144,144), Image.NEAREST)\
            .save(f"{PREV}/{name}_{tag}_x6.png")

preview(svgA, "flashlight")
preview(svgB, "hacklampe")
print("previews written")
