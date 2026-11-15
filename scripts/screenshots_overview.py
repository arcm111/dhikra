"""Tiles a folder's numbered screenshots (NN-name.png) into overview.png, six to a row.

Usage: python3 scripts/screenshots_overview.py docs/screenshots/ar [docs/screenshots/en ...]
"""
import sys
from pathlib import Path

from PIL import Image

COLUMNS = 6
TILE_WIDTH = 360
GAP = 14
BACKGROUND = (244, 242, 236)


def overview(folder: Path) -> None:
    shots = sorted(p for p in folder.glob("[0-9][0-9]-*.png"))
    if not shots:
        print(f"{folder}: no screenshots")
        return
    tiles = []
    for path in shots:
        image = Image.open(path).convert("RGB")
        height = round(image.height * TILE_WIDTH / image.width)
        tiles.append(image.resize((TILE_WIDTH, height), Image.LANCZOS))
    tile_height = max(t.height for t in tiles)
    rows = (len(tiles) + COLUMNS - 1) // COLUMNS
    sheet = Image.new(
        "RGB",
        (COLUMNS * TILE_WIDTH + (COLUMNS + 1) * GAP, rows * tile_height + (rows + 1) * GAP),
        BACKGROUND,
    )
    for i, tile in enumerate(tiles):
        row, column = divmod(i, COLUMNS)
        sheet.paste(tile, (GAP + column * (TILE_WIDTH + GAP), GAP + row * (tile_height + GAP)))
    sheet.save(folder / "overview.png", optimize=True)
    print(f"{folder}/overview.png: {len(tiles)} screenshots, {sheet.width}x{sheet.height}")


if __name__ == "__main__":
    for arg in sys.argv[1:]:
        overview(Path(arg))
