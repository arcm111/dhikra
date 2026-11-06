import math, sys
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import geom as g

IVORY = (247, 240, 223)
GOLD_TOP, GOLD_BOTTOM = (232, 210, 138), (194, 160, 67)
BG = [(0.0, (31, 94, 67)), (0.55, (23, 75, 53)), (1.0, (14, 49, 34))]

def lerp(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))

def gradient(size, stops, start, end):
    w = h = size
    img = Image.new("RGB", (w, h))
    px = img.load()
    sx, sy = start; ex, ey = end
    dx, dy = ex - sx, ey - sy; L = dx * dx + dy * dy
    for y in range(h):
        for x in range(w):
            t = ((x - sx) * dx + (y - sy) * dy) / L
            t = min(1, max(0, t))
            for i in range(len(stops) - 1):
                if stops[i][0] <= t <= stops[i + 1][0]:
                    u = (t - stops[i][0]) / (stops[i + 1][0] - stops[i][0])
                    px[x, y] = lerp(stops[i][1], stops[i + 1][1], u); break
    return img

def arch_points(s):
    pts = [(g.LEG_L, g.BASE), (g.LEG_L, g.SPRING)]
    cl = (g.LEG_L + g.R, g.SPRING)   # left arc centre
    a0 = math.atan2(g.SPRING - cl[1], g.LEG_L - cl[0]); a1 = math.atan2(g.APEX - cl[1], 54 - cl[0])
    if a1 < a0: a1 += 2 * math.pi
    for i in range(1, 41):
        a = a0 + (a1 - a0) * i / 40; pts.append((cl[0] + g.R * math.cos(a), cl[1] + g.R * math.sin(a)))
    cr = (g.LEG_R - g.R, g.SPRING)
    b0 = math.atan2(g.APEX - cr[1], 54 - cr[0]); b1 = math.atan2(g.SPRING - cr[1], g.LEG_R - cr[0])
    if b1 < b0: b1 += 2 * math.pi
    for i in range(1, 41):
        a = b0 + (b1 - b0) * i / 40; pts.append((cr[0] + g.R * math.cos(a), cr[1] + g.R * math.sin(a)))
    pts.append((g.LEG_R, g.BASE))
    return [(x * s, y * s) for x, y in pts]

def mark_layers(size, mono=False):
    s = size / 108
    arch = Image.new("L", (size, size), 0); d = ImageDraw.Draw(arch)
    pts = arch_points(s); w = g.STROKE * s
    # A round-capped, round-joined stroke: every segment as a thick line, a disc at every vertex.
    for a_, b_ in zip(pts, pts[1:]):
        d.line([a_, b_], fill=255, width=int(round(w)))
    for x, y in pts:
        d.ellipse([x - w / 2, y - w / 2, x + w / 2, y + w / 2], fill=255)
    gold = Image.new("L", (size, size), 0); d = ImageDraw.Draw(gold)
    (x1, y1), r1 = g.C1, g.R1; (x2, y2), r2 = g.C2, g.R2
    d.ellipse([(x1 - r1) * s, (y1 - r1) * s, (x1 + r1) * s, (y1 + r1) * s], fill=255)
    d.ellipse([(x2 - r2) * s, (y2 - r2) * s, (x2 + r2) * s, (y2 + r2) * s], fill=0)
    x, y = g.STAR; r = g.STAR_R; k = r * 0.28
    d.polygon([(a * s, b * s) for a, b in [(x, y - r), (x + k, y - k), (x + r, y), (x + k, y + k), (x, y + r), (x - k, y + k), (x - r, y), (x - k, y - k)]], fill=255)
    for bx, by in g.BEADS:
        d.ellipse([(bx - g.BEAD_R) * s, (by - g.BEAD_R) * s, (bx + g.BEAD_R) * s, (by + g.BEAD_R) * s], fill=255)
    return arch, gold

def icon(size, shape="circle", ss=4):
    S = size * ss
    bg = gradient(S // 4, BG, (8 / 108 * S / 4, 0), (100 / 108 * S / 4, S / 4)).resize((S, S), Image.BICUBIC)
    # soft light from the top-left
    glow = Image.new("L", (S, S), 0); ImageDraw.Draw(glow).ellipse([-0.25 * S, -0.35 * S, 0.75 * S, 0.55 * S], fill=46)
    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.12))
    bg = Image.composite(Image.new("RGB", (S, S), (255, 255, 255)), bg, glow)
    arch, gold = mark_layers(S)
    # thin shadow under the mark for depth
    shadow = ImageChops.lighter(arch, gold).filter(ImageFilter.GaussianBlur(S * 0.012))
    shadow = shadow.point(lambda v: int(v * 0.35))
    bg = Image.composite(Image.new("RGB", (S, S), (4, 20, 13)), bg, ImageChops.offset(shadow, 0, int(S * 0.008)))
    bg = Image.composite(Image.new("RGB", (S, S), IVORY), bg, arch)
    goldfill = gradient(S // 4, [(0, GOLD_TOP), (1, GOLD_BOTTOM)], (0, 36 / 108 * S / 4), (0, 82 / 108 * S / 4)).resize((S, S), Image.BICUBIC)
    bg = Image.composite(goldfill, bg, gold)
    mask = Image.new("L", (S, S), 0); md = ImageDraw.Draw(mask)
    if shape == "circle": md.ellipse([0, 0, S - 1, S - 1], fill=255)
    elif shape == "square": md.rectangle([0, 0, S, S], fill=255)
    else: md.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.23), fill=255)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0)); out.paste(bg, (0, 0), mask)
    return out.resize((size, size), Image.LANCZOS)

if __name__ == "__main__":
    sheet = Image.new("RGBA", (980, 420), (249, 247, 242, 255))
    sheet.alpha_composite(icon(320, "circle"), (20, 50))
    sheet.alpha_composite(icon(320, "squircle"), (360, 50))
    for i, sz in enumerate((96, 48)):
        sheet.alpha_composite(icon(sz, "circle"), (720 + i * 120, 120))
    dark = Image.new("RGBA", (240, 110), (16, 20, 18, 255)); dark.alpha_composite(icon(72, "circle"), (20, 20)); dark.alpha_composite(icon(48, "squircle"), (130, 32))
    sheet.alpha_composite(dark, (720, 260))
    sheet.save(sys.argv[1] if len(sys.argv) > 1 else "preview.png")

def legacy(size, shape):
    """A pre-Android 8 launcher icon: the central 72/108 of the adaptive icon, masked to [shape]."""
    full = icon(round(size * 108 / 72), "square")
    off = (full.width - size) // 2
    crop = full.crop((off, off, off + size, off + size))
    S = size * 4
    mask = Image.new("L", (S, S), 0); md = ImageDraw.Draw(mask)
    if shape == "circle": md.ellipse([0, 0, S - 1, S - 1], fill=255)
    else: md.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.2), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(crop, (0, 0), mask.resize((size, size), Image.LANCZOS))
    return out

def write_mipmaps(res):
    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        legacy(size, "squircle").save(f"{res}/mipmap-{density}/ic_launcher.webp", "WEBP", lossless=True)
        legacy(size, "circle").save(f"{res}/mipmap-{density}/ic_launcher_round.webp", "WEBP", lossless=True)
