"""Geometry of the Dhikra mark (108-unit adaptive-icon viewport).

Regenerate the icon after changing it:
    python3 scripts/logo/write_xml.py app/src/main/res            # vector drawables
    python3 -c "import sys; sys.path.insert(0, 'scripts/logo'); import render; render.write_mipmaps('app/src/main/res')"   # legacy webp
"""
import math
# --- geometry in the 108-unit adaptive icon viewport (safe zone: r=33 around 54,54)
LEG_L, LEG_R, SPRING, APEX, BASE = 37.0, 71.0, 50.0, 26.0, 78.0
half = (LEG_R - LEG_L) / 2
rise = SPRING - APEX
R = (half**2 + rise**2) / (2 * half)            # radius of each arc of the pointed arch
STROKE = 5.0
C1, R1 = (54.0, 57.0), 10.0                      # crescent: outer disc
C2, R2 = (58.2, 53.0), 8.4                       # ... minus this disc
STAR, STAR_R = (62.6, 42.6), 3.6                 # four-point star in the crescent's opening
BEADS = [(45.5, 78.0), (54.0, 78.0), (62.5, 78.0)]
BEAD_R = 2.5

def f(x): return f"{x:.2f}".rstrip("0").rstrip(".")

def arch_path():
    return (f"M{f(LEG_L)},{f(BASE)}V{f(SPRING)}A{f(R)},{f(R)} 0 0 1 54,{f(APEX)}"
            f"A{f(R)},{f(R)} 0 0 1 {f(LEG_R)},{f(SPRING)}V{f(BASE)}")

def circle_intersections(c1, r1, c2, r2):
    (x1, y1), (x2, y2) = c1, c2
    d = math.dist(c1, c2)
    a = (r1**2 - r2**2 + d**2) / (2 * d)
    h = math.sqrt(r1**2 - a**2)
    xm, ym = x1 + a * (x2 - x1) / d, y1 + a * (y2 - y1) / d
    return ((xm + h * (y2 - y1) / d, ym - h * (x2 - x1) / d),
            (xm - h * (y2 - y1) / d, ym + h * (x2 - x1) / d))

def crescent_path():
    p, q = circle_intersections(C1, R1, C2, R2)
    # p -> q the long way round the outer disc, then back to p along the inner disc's arc that
    # lies inside the outer one.
    for sweep in (0, 1):
        pass
    return (f"M{f(p[0])},{f(p[1])}A{f(R1)},{f(R1)} 0 1 0 {f(q[0])},{f(q[1])}"
            f"A{f(R2)},{f(R2)} 0 0 1 {f(p[0])},{f(p[1])}Z"), p, q

def star_path():
    x, y = STAR; r = STAR_R; k = r * 0.28
    return (f"M{f(x)},{f(y-r)}L{f(x+k)},{f(y-k)}L{f(x+r)},{f(y)}L{f(x+k)},{f(y+k)}"
            f"L{f(x)},{f(y+r)}L{f(x-k)},{f(y+k)}L{f(x-r)},{f(y)}L{f(x-k)},{f(y-k)}Z")

def bead_path(c):
    x, y = c
    return f"M{f(x-BEAD_R)},{f(y)}a{f(BEAD_R)},{f(BEAD_R)} 0 1 0 {f(2*BEAD_R)},0a{f(BEAD_R)},{f(BEAD_R)} 0 1 0 {f(-2*BEAD_R)},0z"

if __name__ == "__main__":
    print("R", R); print(arch_path()); print(crescent_path()); print(star_path())
    for c in [(LEG_L, BASE), (54, APEX)]:
        print("dist from centre", c, math.dist(c, (54, 54)) + STROKE / 2)
