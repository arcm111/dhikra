#!/usr/bin/env python3
"""Builds the city database bundled with the app (assets/cities.db) from GeoNames.

    python3 scripts/build-cities-db.py [--cache build/geonames] [--out app/src/main/assets/cities.db]

Downloads once into the cache (re-used on later runs; delete it to refresh):
  cities15000.zip       every city of 15,000 people or more (and each country's capital)
  countryInfo.txt       countries
  admin1CodesASCII.txt  states / provinces
  alternateNamesV2.zip  names in other languages (~200 MB; only the Arabic ones are kept)

GeoNames data is licensed under Creative Commons Attribution 4.0 (https://www.geonames.org);
the attribution is stored in the `meta` table and shown on the app's About screen.

Tables (names are Arabic when GeoNames has an Arabic name, else the English one):
  country(code, name, name_en, search, capital)       capital -> city.id
  region(id, country, code, name, name_en, search)    first-level divisions (state / province)
  timezone(id, name)                                  IANA zone ids
  city(id, name, name_en, country, region, lat, lon, timezone, population, search)
`search` columns hold the same normalised key as ArabicText.searchKey() in the app (plus the
ASCII spelling), so a query is matched with a plain LIKE.

When the output changes, bump CityDatabase.VERSION so installed copies are replaced.
Standard library only.
"""

import argparse
import io
import os
import re
import sqlite3
import sys
import tempfile
import time
import unicodedata
import urllib.request
import zipfile
from datetime import date

BASE_URL = "https://download.geonames.org/export/dump/"
FILES = ["cities15000.zip", "countryInfo.txt", "admin1CodesASCII.txt", "alternateNamesV2.zip"]
USER_AGENT = "Dhikra-build-cities-db/1.0"
VERSION = 1



def download(cache):
    os.makedirs(cache, exist_ok=True)
    for name in FILES:
        path = os.path.join(cache, name)
        if os.path.exists(path):
            continue
        print(f"downloading {name}…", file=sys.stderr)
        partial = path + ".partial"
        total = None
        for attempt in range(1, 11):
            # A long download can be cut short (with or without an error): it resumes where it
            # stopped, with a Range request, until every byte has arrived.
            have = os.path.getsize(partial) if os.path.exists(partial) else 0
            headers = {"User-Agent": USER_AGENT}
            if have:
                headers["Range"] = f"bytes={have}-"
            try:
                request = urllib.request.Request(BASE_URL + name, headers=headers)
                with urllib.request.urlopen(request, timeout=60) as response:
                    if have and response.status != 206:
                        have = 0  # the server ignored the range: start over
                    length = int(response.headers.get("Content-Length") or -1)
                    if length >= 0:
                        total = have + length
                    with open(partial, "ab" if have else "wb") as out:
                        while chunk := response.read(1 << 20):
                            out.write(chunk)
            except OSError as error:
                print(f"  {error}", file=sys.stderr)
            size = os.path.getsize(partial) if os.path.exists(partial) else 0
            if total is not None and size == total:
                break
            if attempt == 10:
                sys.exit(f"{name}: got {size} of {total} bytes")
            print(f"  {size} of {total} bytes; resuming", file=sys.stderr)
            time.sleep(2 * attempt)
        if name.endswith(".zip") and not zipfile.is_zipfile(partial):
            os.remove(partial)
            sys.exit(f"{name}: not a valid zip file (removed; run again)")
        os.replace(partial, path)


def tsv(lines):
    """GeoNames rows: tab-separated, no quoting, '#' comments."""
    for line in lines:
        if line.startswith("#") or not line.strip():
            continue
        yield line.rstrip("\n").split("\t")


def zipped_lines(path, member):
    with zipfile.ZipFile(path) as archive:
        with archive.open(member) as raw:
            yield from io.TextIOWrapper(raw, encoding="utf-8")


def text_lines(path):
    with open(path, encoding="utf-8") as f:
        yield from f


ARABIC_DIGITS = {ord(c): str(i) for i, c in enumerate("٠١٢٣٤٥٦٧٨٩")}
ARABIC_DIGITS.update({ord(c): str(i) for i, c in enumerate("۰۱۲۳۴۵۶۷۸۹")})
LETTERS = {"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ة": "ه", "ى": "ي", "ؤ": "و", "ئ": "ي"}


BIDI_CONTROLS = dict.fromkeys(map(ord, "\u200e\u200f\u202a\u202b\u202c\u202d\u202e\u2066\u2067\u2068\u2069"))


def clean(name):
    """A name as shown: invisible direction marks (left in some GeoNames names) removed."""
    return re.sub(r"\s+", " ", (name or "").translate(BIDI_CONTROLS)).strip()


def search_key(text):
    """The app's ArabicText.searchKey(): marks and tatweel dropped, letter variants unified."""
    out = []
    for c in text.translate(ARABIC_DIGITS).lower():
        if c == "ـ" or unicodedata.category(c) == "Mn":
            continue
        out.append(LETTERS.get(c, c))
    return re.sub(r"\s+", " ", "".join(out)).strip()


def search_column(*names):
    keys = []
    for name in names:
        key = search_key(name or "")
        if key and key not in keys:
            keys.append(key)
    return "|".join(keys)


def arabic_names(cache, wanted):
    """geonameid -> Arabic name: the preferred one, else a short one, else the first listed."""
    best = {}
    path = os.path.join(cache, "alternateNamesV2.zip")
    for row in tsv(zipped_lines(path, "alternateNamesV2.txt")):
        if len(row) < 8 or row[2] != "ar":
            continue
        gid = int(row[1])
        if gid not in wanted:
            continue
        colloquial, historic = row[6] == "1", row[7] == "1"
        if colloquial or historic:
            continue
        rank = (row[4] == "1") * 2 + (row[5] == "1")
        if gid not in best or rank > best[gid][0]:
            best[gid] = (rank, clean(row[3]))
    return {gid: name for gid, (_, name) in best.items()}


def build(cache, out):
    countries = {}
    for row in tsv(text_lines(os.path.join(cache, "countryInfo.txt"))):
        countries[row[0]] = {"name_en": clean(row[4]), "capital_name": row[5], "gid": int(row[16]) if row[16] else None}

    regions = {}
    for row in tsv(text_lines(os.path.join(cache, "admin1CodesASCII.txt"))):
        country, code = row[0].split(".", 1)
        regions[(country, code)] = {"name_en": clean(row[1]), "gid": int(row[3]) if row[3] else None}

    cities = []
    for row in tsv(zipped_lines(os.path.join(cache, "cities15000.zip"), "cities15000.txt")):
        if row[8] not in countries:
            continue
        cities.append({
            "id": int(row[0]), "name_en": clean(row[1]), "ascii": clean(row[2]), "lat": float(row[4]), "lon": float(row[5]),
            "feature": row[7], "country": row[8], "admin1": row[10], "population": int(row[14] or 0),
            "timezone": row[17],
        })

    wanted = {c["id"] for c in cities}
    wanted |= {c["gid"] for c in countries.values() if c["gid"]}
    wanted |= {r["gid"] for r in regions.values() if r["gid"]}
    print("reading the Arabic names…", file=sys.stderr)
    arabic = arabic_names(cache, wanted)

    # Each country's capital: GeoNames marks it PPLC; with several (or none), the one named in
    # countryInfo, else the most populous.
    by_country = {}
    for city in cities:
        by_country.setdefault(city["country"], []).append(city)
    capitals = {}
    for code, info in countries.items():
        members = by_country.get(code, [])
        named = [c for c in members if info["capital_name"] and c["name_en"] == info["capital_name"]]
        marked = [c for c in members if c["feature"] == "PPLC"]
        pick = [c for c in marked if c in named] or marked or named
        if pick:
            capitals[code] = max(pick, key=lambda c: c["population"])["id"]

    work = tempfile.mkdtemp()
    db_path = os.path.join(work, "cities.db")
    db = sqlite3.connect(db_path)
    db.executescript("""
        PRAGMA page_size = 4096;
        CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);
        CREATE TABLE country (code TEXT PRIMARY KEY, name TEXT NOT NULL, name_en TEXT NOT NULL,
            search TEXT NOT NULL, capital INTEGER);
        CREATE TABLE region (id INTEGER PRIMARY KEY, country TEXT NOT NULL, code TEXT NOT NULL,
            name TEXT NOT NULL, name_en TEXT NOT NULL, search TEXT NOT NULL);
        CREATE TABLE timezone (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE);
        CREATE TABLE city (id INTEGER PRIMARY KEY, name TEXT NOT NULL, name_en TEXT NOT NULL,
            country TEXT NOT NULL, region INTEGER, lat REAL NOT NULL, lon REAL NOT NULL,
            timezone INTEGER NOT NULL, population INTEGER NOT NULL, search TEXT NOT NULL);
    """)
    used_countries = {c["country"] for c in cities}
    for code in sorted(used_countries):
        info = countries[code]
        name = arabic.get(info["gid"]) or info["name_en"]
        db.execute("INSERT INTO country VALUES (?, ?, ?, ?, ?)",
                   (code, name, info["name_en"], search_column(name, info["name_en"]), capitals.get(code)))

    region_ids = {}
    for (country, code), info in sorted(regions.items()):
        if country not in used_countries:
            continue
        name = arabic.get(info["gid"]) or info["name_en"]
        cursor = db.execute("INSERT INTO region (country, code, name, name_en, search) VALUES (?, ?, ?, ?, ?)",
                            (country, code, name, info["name_en"], search_column(name, info["name_en"])))
        region_ids[(country, code)] = cursor.lastrowid

    zone_ids = {}
    for city in sorted(cities, key=lambda c: c["id"]):
        zone = city["timezone"]
        if zone not in zone_ids:
            zone_ids[zone] = db.execute("INSERT INTO timezone (name) VALUES (?)", (zone,)).lastrowid
        name = arabic.get(city["id"]) or city["name_en"]
        db.execute(
            "INSERT INTO city VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (city["id"], name, city["name_en"], city["country"], region_ids.get((city["country"], city["admin1"])),
             round(city["lat"], 5), round(city["lon"], 5), zone_ids[zone], city["population"],
             search_column(name, city["ascii"], city["name_en"])),
        )
    db.execute("CREATE INDEX city_country ON city (country, population)")
    db.execute("CREATE INDEX city_region ON city (region)")
    db.execute("CREATE INDEX region_country ON region (country)")
    db.executemany("INSERT INTO meta VALUES (?, ?)", [
        ("source", "GeoNames (cities15000, countryInfo, admin1CodesASCII, alternateNamesV2), https://www.geonames.org"),
        ("license", "Creative Commons Attribution 4.0 International (CC BY 4.0)"),
        ("built", date.today().isoformat()),
    ])
    db.commit()
    db.execute(f"PRAGMA user_version = {VERSION}")
    db.execute("VACUUM")

    # Sanity checks: what the app relies on.
    count = db.execute("SELECT count(*) FROM city").fetchone()[0]
    arabic_count = db.execute("SELECT count(*) FROM city WHERE name <> name_en").fetchone()[0]
    missing_capitals = [r[0] for r in db.execute(
        "SELECT code FROM country WHERE capital IS NULL AND code IN ('SA','EG','GB','US','PK','ID','TR','MA')")]
    makkah = db.execute("SELECT name FROM city WHERE id = 104515").fetchone()
    db.close()
    if count < 20000 or missing_capitals or not makkah:
        sys.exit(f"unexpected content: {count} cities, capitals missing {missing_capitals}, Makkah {makkah}")

    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    os.chmod(db_path, 0o644)
    os.replace(db_path, out)
    print(f"{out}: {count} cities ({arabic_count} with an Arabic name), "
          f"{len(used_countries)} countries, {len(region_ids)} regions, {len(zone_ids)} time zones, "
          f"{os.path.getsize(out) / 1e6:.1f} MB")


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--cache", default="build/geonames", help="where the GeoNames downloads are kept")
    parser.add_argument("--out", default="app/src/main/assets/cities.db")
    args = parser.parse_args()
    download(args.cache)
    build(args.cache, args.out)


if __name__ == "__main__":
    main()
