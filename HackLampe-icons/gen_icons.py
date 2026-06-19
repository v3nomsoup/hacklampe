#!/usr/bin/env python3
"""Generate the complete HackLampe adaptive-icon asset set."""
import os, math, cairosvg
from PIL import Image, ImageDraw

ROOT = "/home/claude/HackLampe-icons"
RES = os.path.join(ROOT, "res")

AMBER   = "#FFB12E"
DARK    = "#20243F"
LENS    = "#FFE9A8"
CONE    = "#FFF4CF"
RAY     = "#FBE49A"

def rrect(x, y, w, h, r):
    r = min(r, w/2, h/2)
    return (f"M{x+r:.2f},{y:.2f} L{x+w-r:.2f},{y:.2f} Q{x+w:.2f},{y:.2f} {x+w:.2f},{y+r:.2f} "
            f"L{x+w:.2f},{y+h-r:.2f} Q{x+w:.2f},{y+h:.2f} {x+w-r:.2f},{y+h:.2f} "
            f"L{x+r:.2f},{y+h:.2f} Q{x:.2f},{y+h:.2f} {x:.2f},{y+h-r:.2f} "
            f"L{x:.2f},{y+r:.2f} Q{x:.2f},{y:.2f} {x+r:.2f},{y:.2f} Z")

# --- Foreground geometry in a 108-unit adaptive canvas (content inside safe zone) ---
CH1   = "M38.6,22.1 L47.3,27.9 L56.0,22.1"
CH2   = "M38.6,29.0 L47.3,34.8 L56.0,29.0"
CONE_P= "M61.8,52.2 L85.0,39.5 L85.0,85.9 L61.8,73.1 Z"
RAY1  = "M65.9,59.2 L82.1,51.1"
RAY2  = "M65.9,66.2 L82.1,75.4"
TAIL  = rrect(22.9, 58.6, 3.5, 8.1, 1.2)
BODY  = rrect(26.4, 56.3, 25.5, 12.8, 2.9)
SWITCH= rrect(34.5, 54.0, 7.0, 2.9, 1.2)
HEAD  = "M51.9,54.0 L60.1,49.9 L60.1,75.4 L51.9,71.4 Z"
LENS_P= rrect(59.5, 51.1, 2.3, 23.2, 1.0)
CHW   = 4.1   # chevron stroke width

def foreground_svg(include_bg=False, bg_rx=24):
    bg = f'<path d="{rrect(0,0,108,108,bg_rx)}" fill="{AMBER}"/>' if include_bg else ''
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">
{bg}
<path d="{CONE_P}" fill="{CONE}" opacity="0.65"/>
<path d="{RAY1}" stroke="{RAY}" stroke-width="1.7" stroke-linecap="round" fill="none"/>
<path d="{RAY2}" stroke="{RAY}" stroke-width="1.7" stroke-linecap="round" fill="none"/>
<path d="{TAIL}" fill="{DARK}"/>
<path d="{BODY}" fill="{DARK}"/>
<path d="{SWITCH}" fill="{DARK}"/>
<path d="{HEAD}" fill="{DARK}"/>
<path d="{LENS_P}" fill="{LENS}"/>
<path d="{CH1}" stroke="{DARK}" stroke-width="{CHW}" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
<path d="{CH2}" stroke="{DARK}" stroke-width="{CHW}" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
</svg>'''

os.makedirs(ROOT, exist_ok=True)

# 1. Source SVG (full icon, squircle) and Play Store PNG ---------------------
full_svg = foreground_svg(include_bg=True, bg_rx=24)
open(os.path.join(ROOT, "ic_launcher-source.svg"), "w").write(full_svg)
cairosvg.svg2png(bytestring=full_svg.encode(), write_to=os.path.join(ROOT, "playstore-icon.png"),
                 output_width=512, output_height=512)

# preview of foreground only (transparent)
cairosvg.svg2png(bytestring=foreground_svg(False).encode(),
                 write_to=os.path.join(ROOT, "preview-foreground.png"),
                 output_width=512, output_height=512)

# 2. Legacy mipmap PNGs (square + round) -------------------------------------
DENS = {"mdpi":48, "hdpi":72, "xhdpi":96, "xxhdpi":144, "xxxhdpi":192}
for name, px in DENS.items():
    d = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    # square (squircle baked)
    cairosvg.svg2png(bytestring=full_svg.encode(),
                     write_to=os.path.join(d, "ic_launcher.png"),
                     output_width=px, output_height=px)
    # round: render a circular-bg version then mask to circle
    round_svg = foreground_svg(include_bg=True, bg_rx=54)  # rx=54 => full circle
    cairosvg.svg2png(bytestring=round_svg.encode(),
                     write_to=os.path.join(d, "ic_launcher_round.png"),
                     output_width=px, output_height=px)
    img = Image.open(os.path.join(d, "ic_launcher_round.png")).convert("RGBA")
    mask = Image.new("L", (px, px), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, px-1, px-1), fill=255)
    img.putalpha(mask)
    img.save(os.path.join(d, "ic_launcher_round.png"))

print("PNGs done")
