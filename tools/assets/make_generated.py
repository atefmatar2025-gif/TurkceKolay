#!/usr/bin/env python3
"""Draws pictures for concepts a photo cannot show unambiguously: numbers, colours, weekdays.

Images are 640x400; everything important sits in the central 400x400 square, so both the
square picture-choice cards and the wide vocabulary card show the whole drawing.
Output: app/src/main/assets/images/vocab/<concept>.webp
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "app/src/main/assets/images/vocab")
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
W, H = 640, 400
BG = (248, 247, 242)
TURQ = (20, 163, 163)
ORANGE = (242, 153, 74)
BLUE = (29, 62, 110)
GREY = (220, 218, 210)
S = 3  # supersampling for smooth edges


def canvas():
    im = Image.new("RGB", (W * S, H * S), BG)
    return im, ImageDraw.Draw(im)


def save(im, name):
    im = im.resize((W, H), Image.LANCZOS)
    im.save(os.path.join(OUT, name + ".webp"), quality=90)


def font(px):
    return ImageFont.truetype(FONT, px * S)


def numbers():
    names = ["bir", "iki", "uc", "dort", "bes", "alti", "yedi", "sekiz", "dokuz", "on"]
    for n, name in enumerate(names, start=1):
        im, d = canvas()
        # big digit at the top of the central square
        f = font(150)
        txt = str(n)
        tw = d.textlength(txt, font=f)
        d.text(((W * S - tw) / 2, 20 * S), txt, font=f, fill=BLUE)
        # n balls below, in rows of 5
        r = 22
        gap = 14
        rows = [min(5, n - i) for i in range(0, n, 5)]
        y0 = 230 if len(rows) == 1 else 205
        for ri, count in enumerate(rows):
            total = count * (2 * r) + (count - 1) * gap
            x = (W - total) / 2 + r
            y = y0 + ri * (2 * r + gap) + r
            for _ in range(count):
                d.ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=ORANGE)
                d.ellipse([(x - r * 0.45) * S, (y - r * 0.6) * S, (x - r * 0.05) * S, (y - r * 0.2) * S], fill=(250, 200, 150))
                x += 2 * r + gap
        save(im, name)


def colors():
    palette = {
        "kirmizi": (220, 38, 38), "mavi": (37, 99, 235), "yesil": (22, 163, 74), "sari": (250, 204, 21),
        "siyah": (23, 23, 23), "beyaz": (255, 255, 255), "turuncu": (249, 115, 22), "mor": (126, 34, 206),
    }
    for name, c in palette.items():
        im, d = canvas()
        box = [(W / 2 - 150) * S, 50 * S, (W / 2 + 150) * S, 350 * S]
        d.rounded_rectangle(box, radius=40 * S, fill=c, outline=(200, 198, 190) if name == "beyaz" else None, width=4 * S)
        # paint-drop shine
        d.ellipse([(W / 2 - 110) * S, 80 * S, (W / 2 - 60) * S, 130 * S], fill=tuple(min(255, v + 60) for v in c))
        save(im, name)


def weekdays():
    names = ["pazartesi", "sali", "carsamba", "persembe", "cuma", "cumartesi", "pazar"]
    for i, name in enumerate(names):
        im, d = canvas()
        cell, gap = 46, 6
        total = 7 * cell + 6 * gap
        x0 = (W - total) / 2
        y0 = 150
        f = font(24)
        # Week strip, Monday first (Turkish calendar), drawn left to right like a Turkish calendar.
        for k in range(7):
            x = x0 + k * (cell + gap)
            fill = ORANGE if k == i else (GREY if k < 5 else (205, 230, 228))
            d.rounded_rectangle([x * S, y0 * S, (x + cell) * S, (y0 + cell + 40) * S], radius=10 * S, fill=fill)
            t = str(k + 1)
            tw = d.textlength(t, font=f)
            d.text(((x + cell / 2) * S - tw / 2, (y0 + 28) * S), t, font=f, fill=(255, 255, 255) if k == i else BLUE)
        # calendar rings
        for k in (1, 5):
            x = x0 + k * (cell + gap) + cell / 2
            d.rounded_rectangle([(x - 6) * S, (y0 - 22) * S, (x + 6) * S, (y0 + 10) * S], radius=6 * S, fill=BLUE)
        # marker above the chosen day
        x = x0 + i * (cell + gap) + cell / 2
        d.polygon([((x - 16) * S, 95 * S), ((x + 16) * S, 95 * S), (x * S, 120 * S)], fill=TURQ)
        save(im, name)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    numbers()
    colors()
    weekdays()
    print("generated", len(os.listdir(OUT)))
