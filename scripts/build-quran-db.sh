#!/usr/bin/env bash
# Builds the Quran database bundled with the app from Tanzil's MySQL dump of the Uthmani text.
#
#   scripts/build-quran-db.sh data/quran-uthmani.sql app/src/main/assets/quran.db
#
# Only the rows matching the strict pattern (index, sura, aya, 'text') and the "-- Sura n (name)"
# comments are read: nothing else of the downloaded dump is executed. The text is copied verbatim,
# as Tanzil's terms of use require, and their copyright notice is stored in the `meta` table.
#
# When the output changes, bump QuranDatabase.VERSION so installed copies are replaced.
# Requires sqlite3 (the Android SDK ships one in platform-tools).
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <quran-uthmani.sql> <output.db>" >&2
  exit 2
fi
src=$1
out=$2
command -v sqlite3 >/dev/null || { echo "sqlite3 not found" >&2; exit 1; }

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

row_pattern="^\([0-9]+, [0-9]+, [0-9]+, '[^']*'\)[,;]$"
name_pattern="^-- Sura [0-9]+ \([^')]+\)$"

{
  echo "PRAGMA page_size = 4096;"
  echo "BEGIN;"
  echo "CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL);"
  echo "CREATE TABLE surah (sura INTEGER PRIMARY KEY, name TEXT NOT NULL, ayat INTEGER NOT NULL);"
  echo "CREATE TABLE ayah (sura INTEGER NOT NULL, aya INTEGER NOT NULL, text TEXT NOT NULL,"
  echo "  PRIMARY KEY (sura, aya)) WITHOUT ROWID;"
  grep -E "$name_pattern" "$src" \
    | sed -E "s/^-- Sura ([0-9]+) \((.*)\)$/INSERT INTO surah (sura, name, ayat) VALUES (\1, '\2', 0);/"
  grep -E "$row_pattern" "$src" \
    | sed -E "s/^\(([0-9]+), ([0-9]+), ([0-9]+), ('[^']*')\)[,;]$/INSERT INTO ayah (sura, aya, text) VALUES (\2, \3, \4);/"
  echo "UPDATE surah SET ayat = (SELECT count(*) FROM ayah WHERE ayah.sura = surah.sura);"
  # The copyright block (the dump's '#' lines), quotes doubled, newlines restored by SQL.
  notice=$(grep -E '^#' "$src" | sed -E "s/^#//; s/'/''/g" | tr '\n' '\r')
  echo "INSERT INTO meta (key, value) VALUES ('source', 'Tanzil Quran Text (Uthmani, Version 1.1), https://tanzil.net');"
  echo "INSERT INTO meta (key, value) VALUES ('license', 'Creative Commons Attribution 3.0; verbatim copies only');"
  echo "INSERT INTO meta (key, value) VALUES ('notice', replace('$notice', char(13), char(10)));"
  echo "COMMIT;"
  echo "PRAGMA user_version = 1;"
  echo "VACUUM;"
} > "$work/build.sql"

sqlite3 -bail "$work/quran.db" < "$work/build.sql"

read -r surahs ayat named < <(sqlite3 -separator ' ' "$work/quran.db" \
  "SELECT (SELECT count(*) FROM surah), (SELECT count(*) FROM ayah), (SELECT count(*) FROM surah WHERE ayat > 0);")
if [[ "$surahs" != 114 || "$ayat" != 6236 || "$named" != 114 ]]; then
  echo "unexpected content: $surahs surahs, $ayat ayat, $named surahs with ayat" >&2
  exit 1
fi

mkdir -p "$(dirname "$out")"
chmod 644 "$work/quran.db"
mv "$work/quran.db" "$out"
echo "wrote $out: $surahs surahs, $ayat ayat"
