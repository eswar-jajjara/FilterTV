"""Regenerate the FilterTV Android TV artwork (requires Pillow)."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "smarttubetv" / "src" / "stfdroid" / "res" / "mipmap-nodpi"
OUT.mkdir(parents=True, exist_ok=True)
SCALE = 4
RED = "#FF0033"
WHITE = "#FFFFFF"
INK = "#101014"
FONT = Path("C:/Windows/Fonts/arialbd.ttf")


def canvas(width, height, background):
    return Image.new("RGBA", (width * SCALE, height * SCALE), background)


def play_mark(draw, box):
    x0, y0, x1, y1 = [value * SCALE for value in box]
    draw.rounded_rectangle((x0, y0, x1, y1), radius=(y1 - y0) // 4, fill=RED)
    width = x1 - x0
    height = y1 - y0
    draw.polygon(
        [
            (x0 + int(width * 0.43), y0 + int(height * 0.26)),
            (x0 + int(width * 0.43), y0 + int(height * 0.74)),
            (x0 + int(width * 0.72), y0 + int(height * 0.50)),
        ],
        fill=WHITE,
    )


def save(image, name, size):
    image.resize(size, Image.Resampling.LANCZOS).save(OUT / name)


icon = canvas(320, 320, INK)
draw = ImageDraw.Draw(icon)
draw.rounded_rectangle((0, 0, 320 * SCALE - 1, 320 * SCALE - 1), radius=68 * SCALE, fill=INK)
play_mark(draw, (47, 85, 273, 235))
save(icon, "filtertv_icon_art.png", (320, 320))
save(icon, "app_icon_alt.png", (320, 320))

logo = canvas(320, 80, (0, 0, 0, 0))
draw = ImageDraw.Draw(logo)
play_mark(draw, (4, 13, 94, 67))
font = ImageFont.truetype(str(FONT), 43 * SCALE)
draw.text((108 * SCALE, 15 * SCALE), "FilterTV", font=font, fill=WHITE)
save(logo, "filtertv_logo_art.png", (320, 80))

banner = canvas(320, 180, INK)
draw = ImageDraw.Draw(banner)
play_mark(draw, (24, 65, 112, 119))
font = ImageFont.truetype(str(FONT), 34 * SCALE)
draw.text((126 * SCALE, 70 * SCALE), "FilterTV", font=font, fill=WHITE)
save(banner, "filtertv_banner_art.png", (320, 180))
