"""Render original vector branding and fictional project illustrations for isolated previews."""

import shutil
import subprocess
import sys
from pathlib import Path

import cairo
import gi

gi.require_version("Rsvg", "2.0")
from gi.repository import Rsvg

ROOT = Path(__file__).resolve().parents[2]


def render(source, target, size):
    width, height = map(int, size.split("x"))
    surface = cairo.ImageSurface(cairo.FORMAT_ARGB32, width, height)
    viewport = Rsvg.Rectangle()
    viewport.x, viewport.y, viewport.width, viewport.height = 0, 0, width, height
    Rsvg.Handle.new_from_file(str(source)).render_document(
        cairo.Context(surface), viewport
    )
    surface.write_to_png(str(target))


def main():
    branding = ROOT / "assets/branding"
    branding.mkdir(parents=True, exist_ok=True)
    svg = """<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 256 256">
<rect width="256" height="256" rx="48" fill="#65425f"/>
<path d="M65 73H180V204H65Z" fill="#f0e4dc"/>
<path d="M81 88H164V189H81Z" fill="none" stroke="#b08ba4" stroke-width="5" stroke-dasharray="8 6"/>
<path d="M166 49L104 153" stroke="#e7bf84" stroke-width="9" stroke-linecap="round"/>
<path d="M171 42L166 51" stroke="#f8f5f1" stroke-width="3" stroke-linecap="round"/>
<circle cx="191" cy="184" r="36" fill="#74536f" stroke="#f8f5f1" stroke-width="6"/>
<path d="M175 183l11 12 23-26" fill="none" stroke="#f8f5f1" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>
</svg>"""
    (branding / "icon.svg").write_text(svg, encoding="utf-8")
    for size in (44, 50, 150, 256, 300, 1024):
        name = {
            44: "Square44x44Logo.png",
            50: "StoreLogo.png",
            150: "Square150x150Logo.png",
            300: "store-icon-300.png",
        }.get(size, f"icon-{size}.png")
        render(branding / "icon.svg", branding / name, f"{size}x{size}")
    subprocess.run(
        [
            "convert",
            str(branding / "icon-256.png"),
            "-define",
            "icon:auto-resize=256,128,64,48,32,16",
            str(branding / "icon.ico"),
        ],
        check=True,
    )
    shutil.copy2(
        branding / "icon-256.png",
        ROOT / "src/main/resources/de/appfabrik/naehplan/icon-256.png",
    )
    subprocess.run([sys.executable, str(ROOT / "scripts/prepare_icons.py")], check=True)
    photos = ROOT / "assets/store_demo/images"
    photos.mkdir(parents=True, exist_ok=True)
    shapes = {
        "lilac": '<path d="M270 210V170Q270 75 400 75Q530 75 530 170V210" fill="none" stroke="#826184" stroke-width="30"/><path d="M230 200H570L600 500H200Z" fill="#b494bb"/><path d="M248 221H550L575 480H225Z" fill="none" stroke="#e2d3e5" stroke-width="8" stroke-dasharray="14 10"/><rect x="330" y="305" width="140" height="125" rx="6" fill="#cab4cf"/>',
        "rose": '<path d="M170 165Q400 130 630 165Q665 320 630 475Q400 510 170 475Q135 320 170 165Z" fill="#c99292"/><path d="M190 185Q400 150 610 185Q640 320 610 455Q400 485 190 455Q160 320 190 185Z" fill="none" stroke="#eed0c5" stroke-width="8" stroke-dasharray="14 10"/><path d="M245 230L555 410M245 410L555 230" stroke="#b07b82" stroke-width="10"/>',
        "sage": '<path d="M320 180V130Q400 65 480 130V180" fill="none" stroke="#61785d" stroke-width="26"/><path d="M315 170H485L510 280L590 500H210L290 280Z" fill="#92aa8c"/><path d="M315 350H485V435H315Z" fill="#bfd0b3"/><path d="M290 280L175 240M510 280L625 240" fill="none" stroke="#61785d" stroke-width="20"/><path d="M235 475H565" stroke="#e4e9d4" stroke-width="8" stroke-dasharray="14 10"/>',
    }
    for name, shape in shapes.items():
        source = photos / f"{name}.svg"
        source.write_text(
            '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600"><rect width="800" height="600" fill="#f4ede4"/><ellipse cx="400" cy="516" rx="245" ry="25" fill="#e3d9ce"/>'
            + shape
            + "</svg>\n",
            encoding="utf-8",
        )
        render(source, photos / f"{name}.png", "800x600")
    subprocess.run(
        [
            "convert",
            str(photos / "lilac.png"),
            "-quality",
            "92",
            str(photos / "lilac.jpg"),
        ],
        check=True,
    )


if __name__ == "__main__":
    main()
