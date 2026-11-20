#!/usr/bin/env python3
"""Adds the starter program «الأذكار اليومية» to the app's built-in programs.

Reads app/src/main/resources/default_program.json and writes it back with:
- the morning & evening adhkar of the open dataset Seen-Arabic/Morning-And-Evening-Adhkar-DB
  (MIT, vendored in adhkar-db/ at commit DATASET_COMMIT) as library entries `me_<order>`, except
  where the library already has the same text (then that entry is used) or the text is Quran
  (then the bundled Quran reference entries are used: Quran text is never copied);
- the subsection «أذكار بعد الصلاة» (BLOCK_ID), read after each of the five prayers;
- the program `"starter"`: the only built-in program, the one a first launch follows.
Every other program, and every library text, group or subsection the starter doesn't read, is
dropped (the detailed program «برنامج المرحلة الثانية» is kept in backup/second-stage-program/).

Re-runnable: what an earlier run added is dropped first, so it can run again on its own output, on
a store copied from the phone, or after the dataset is updated.

Usage: python3 -I scripts/starter/build_starter.py
"""
import json
import re
import sys
from pathlib import Path

DATASET_COMMIT = "29d7623fede52eca835a789025dfda866e8cfe44"
ROOT = Path(__file__).resolve().parents[2]
STORE = ROOT / "app/src/main/resources/default_program.json"
DATASET = Path(__file__).resolve().parent / "adhkar-db/ar.json"

STARTER_ID = "starter"
STARTER_NAME = "الأذكار اليومية"
BLOCK_ID = "block_baad_salat"
ENTRY_PREFIX = "me_"

# Dataset texts that are Quran: the step reads the bundled Quran (entry or group) instead.
QURAN = {
    2: ("entry", "ayat_al_kursi"),       # آية الكرسي
    3: ("entry", "2da8f77b3cc3"),        # آمن الرسول (البقرة ٢٨٥ - ٢٨٦)
    4: ("group", "group_muawwidhat"),    # الإخلاص، الفلق، الناس (×٣ each, one step)
    5: None,                             # read in order 4's step
    6: None,
}

# Type and title of the dataset texts the library doesn't have yet (anything else is an error).
NEW_ENTRIES = {
    1: ("dhikr", "الحمد لله وحده"),
    16: ("dua", "اللهم عافني في بدني، اللهم إني أعوذ بك من الكفر والفقر"),
    29: ("dhikr", "اللهم صل وسلم على نبينا محمد"),
    33: ("dhikr", "أستغفر الله وأتوب إليه"),
}


def norm(text: str) -> str:
    """A text without tashkeel, punctuation or spelling variants, for comparing two wordings."""
    text = re.sub(r"[ؐ-ًؚ-ٰٟۖ-ۭـ]", "", text)
    text = text.replace("صلى الله عليه وسلم", "ﷺ")
    text = re.sub("[أإآٱ]", "ا", text).replace("ى", "ي").replace("ة", "ه").replace("ؤ", "و").replace("ئ", "ي")
    text = re.sub(r"[^ء-ي\s]", " ", text)
    return " ".join(text.split())


def fadl(item: dict) -> str:
    """The virtue of a dataset text, without the ayah reference some start with."""
    return re.sub(r"^\[[^\]]*\]\s*", "", (item.get("fadl") or "").strip())


def main() -> None:
    store = json.loads(STORE.read_text(encoding="utf-8"))
    dataset = sorted(json.loads(DATASET.read_text(encoding="utf-8")), key=lambda x: x["order"])

    # What an earlier run added.
    store["programs"] = [p for p in store["programs"] if p["id"] != STARTER_ID]
    store.setdefault("groups", [])
    store["blocks"] = [b for b in store.get("blocks", []) if b["id"] != BLOCK_ID]
    used = json.dumps([store["programs"], store.get("groups", []), store["blocks"]], ensure_ascii=False)
    store["library"] = [
        e for e in store["library"]
        if not e["id"].startswith(ENTRY_PREFIX) or f'"{e["id"]}"' in used
    ]
    library = {e["id"]: e for e in store["library"]}
    groups = {g["id"]: g for g in store.get("groups", [])}
    by_text = {}
    for entry in store["library"]:
        if "text" in entry:
            by_text.setdefault(norm(entry["text"]), entry["id"])

    # The dataset's texts as library entries.
    steps_of = {}  # dataset order -> step (without id)
    report = []
    for item in dataset:
        order = item["order"]
        note = fadl(item)
        if order in QURAN:
            target = QURAN[order]
            if target is None:
                continue
            kind, ref = target
            known = library if kind == "entry" else groups
            if ref not in known:
                sys.exit(f"order {order}: missing Quran {kind} {ref}")
            ref_type = known[ref]["type"]
            steps_of[order] = {"type": ref_type, kind: ref, "count": 1 if kind == "group" else item["count"], "note": note}
            report.append(f"  {order:>2} quran  -> {kind} {ref}")
            continue
        entry_id = by_text.get(norm(item["content"]))
        if entry_id is not None:
            report.append(f"  {order:>2} reuse  -> {entry_id} ({library[entry_id]['title']})")
        else:
            if order not in NEW_ENTRIES:
                sys.exit(f"order {order}: new text without a title in NEW_ENTRIES: {item['content'][:60]}")
            entry_type, title = NEW_ENTRIES[order]
            entry_id = f"{ENTRY_PREFIX}{order}"
            entry = {
                "id": entry_id,
                "type": entry_type,
                "title": title,
                "description": item["source"].strip().lstrip("،, ").strip(),
                "text": item["content"].strip(),
            }
            store["library"].append(entry)
            library[entry_id] = entry
            by_text[norm(entry["text"])] = entry_id
            report.append(f"  {order:>2} NEW    -> {entry_id} ({title})")
        steps_of[order] = {"type": library[entry_id]["type"], "entry": entry_id, "count": item["count"], "note": note}

    def adhkar(prefix: str, kinds: set) -> list:
        steps = []
        for item in dataset:
            step = steps_of.get(item["order"])
            if step is None or item["type"] not in kinds:
                continue
            steps.append(clean({"id": f"{prefix}_{item['order']}", **step}))
        return steps

    # The subsection read after each prayer.
    for needed in ("istighfar", "adhkar_baad_salam", "ayat_al_kursi"):
        if needed not in library:
            sys.exit(f"missing entry {needed}")
    for needed in ("group_tasbih_salat", "dada18cc2c8e", "group_muawwidhat", "group_tasbih_nawm"):
        if needed not in groups:
            sys.exit(f"missing group {needed}")
    store["blocks"].append({
        "id": BLOCK_ID,
        "title": "أذكار بعد الصلاة",
        "items": [
            {"id": "bs_istighfar", "type": "dhikr", "entry": "istighfar", "count": 3},
            {"id": "bs_salam", "type": "dhikr", "entry": "adhkar_baad_salam"},
            {"id": "bs_tasbih", "type": "dhikr", "group": "group_tasbih_salat",
             "note": "من سبّح الله في دبر كل صلاة ثلاثًا وثلاثين، وحمد الله ثلاثًا وثلاثين، وكبّر الله ثلاثًا "
                     "وثلاثين، فتلك تسعة وتسعون، وقال تمام المئة: لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على "
                     "كل شيء قدير، غُفرت خطاياه وإن كانت مثل زبد البحر. (رواه مسلم)"},
            {"id": "bs_kursi", "type": "quran", "entry": "ayat_al_kursi",
             "note": "من قرأ آية الكرسي دبر كل صلاة مكتوبة لم يمنعه من دخول الجنة إلا أن يموت. (رواه النسائي في الكبرى)"},
            {"id": "bs_muawwidhat", "type": "quran", "group": "dada18cc2c8e",
             "note": "تقرأ مرة واحدة بعد الصلوات، وثلاثاً بعد الفجر والمغرب"},
        ],
    })

    for needed in ("dua_bismika_amutu", "dua_qini", "dua_bismika_rabbi", "2da8f77b3cc3"):
        if needed not in library:
            sys.exit(f"missing entry {needed}")

    def after_prayer(key: str, name: str, prayer: str, reminder: str) -> dict:
        return {
            "id": f"st_{key}", "title": f"بعد صلاة {name}", "icon": "🕌", "reminder": reminder, "prayer": prayer,
            "items": [{"id": f"st_{key}_adhkar", "type": "heading", "block": BLOCK_ID}],
        }

    sections = [
        after_prayer("fajr", "الفجر", "fajr", "05:00"),
        {
            "id": "st_sabah", "title": "أذكار الصباح", "icon": "☀️", "reminder": "05:15", "prayer": "fajr",
            "offset": 15, "note": "وقتها من بعد صلاة الفجر إلى طلوع الشمس",
            "items": adhkar("st_sabah", {0, 1}),
        },
        after_prayer("dhuhr", "الظهر", "dhuhr", "12:00"),
        after_prayer("asr", "العصر", "asr", "15:30"),
        {
            "id": "st_masaa", "title": "أذكار المساء", "icon": "🌙", "reminder": "15:45", "prayer": "asr",
            "offset": 15, "note": "وقتها من بعد صلاة العصر إلى غروب الشمس",
            "items": adhkar("st_masaa", {0, 2}),
        },
        after_prayer("maghrib", "المغرب", "maghrib", "18:00"),
        after_prayer("isha", "العشاء", "isha", "19:30"),
        {
            "id": "st_nawm", "title": "أذكار النوم", "icon": "🌙", "reminder": "22:30",
            "items": [
                {"id": "st_nawm_kursi", "type": "quran", "entry": "ayat_al_kursi",
                 "note": "إذا أويت إلى فراشك فاقرأ آية الكرسي، لن يزال عليك من الله حافظ، ولا يقربك شيطان حتى تصبح. (رواه البخاري)"},
                {"id": "st_nawm_baqara", "type": "quran", "entry": "2da8f77b3cc3",
                 "note": "من قرأ بالآيتين من آخر سورة البقرة في ليلة كفتاه. (متفق عليه)"},
                {"id": "st_nawm_muawwidhat", "type": "quran", "group": "group_muawwidhat",
                 "note": "يجمع كفيه ثم ينفث فيهما ويقرأ المعوذات، ثم يمسح بهما ما استطاع من جسده، يبدأ برأسه "
                         "ووجهه وما أقبل من جسده، يفعل ذلك ثلاث مرات. (رواه البخاري)"},
                {"id": "st_nawm_tasbih", "type": "dhikr", "group": "group_tasbih_nawm",
                 "note": "إذا أخذتما مضاجعكما فكبّرا أربعًا وثلاثين، وسبّحا ثلاثًا وثلاثين، واحمدا ثلاثًا وثلاثين، "
                         "فهو خير لكما من خادم. (متفق عليه)"},
                {"id": "st_nawm_amutu", "type": "dua", "entry": "dua_bismika_amutu"},
                {"id": "st_nawm_qini", "type": "dua", "entry": "dua_qini", "count": 3},
                {"id": "st_nawm_janbi", "type": "dua", "entry": "dua_bismika_rabbi"},
            ],
        },
    ]
    starter = {"id": STARTER_ID, "name": STARTER_NAME, "sections": sections}
    # The starter is the only built-in program, with only what it reads.
    dropped = [p["name"] for p in store["programs"]]
    store["programs"] = [starter]
    store["active"] = STARTER_ID
    before = {k: len(store.get(k, [])) for k in ("library", "groups", "blocks")}
    prune(store)

    STORE.write_text(json.dumps(store, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Dataset {DATASET_COMMIT[:10]}: {len(dataset)} texts")
    print("\n".join(report))
    for section in sections:
        print(f"  {section['title']}: {len(section['items'])} steps")
    if dropped:
        print(f"  dropped programs: {dropped}")
    for k, n in before.items():
        print(f"  {k}: {n} -> {len(store[k])}")
    print(f"Wrote {STORE.relative_to(ROOT)}: programs {[p['id'] for p in store['programs']]}, active {store['active']}")


def prune(store: dict) -> None:
    """Keeps only the texts, groups and subsections the store's programs read."""
    groups = {g["id"]: g for g in store.get("groups", [])}
    blocks = {b["id"]: b for b in store.get("blocks", [])}
    entry_ids, group_ids, block_ids = set(), set(), set()

    def read(steps: list) -> None:
        for step in steps:
            if step.get("entry"):
                entry_ids.add(step["entry"])
            if step.get("group") in groups:
                group_ids.add(step["group"])
                entry_ids.update(item["entry"] for item in groups[step["group"]]["items"])
            if step.get("block") in blocks and step["block"] not in block_ids:
                block_ids.add(step["block"])
                read(blocks[step["block"]]["items"])

    for program in store["programs"]:
        for section in program["sections"]:
            read(section["items"])
    store["library"] = [e for e in store["library"] if e["id"] in entry_ids]
    store["groups"] = [g for g in store.get("groups", []) if g["id"] in group_ids]
    store["blocks"] = [b for b in store.get("blocks", []) if b["id"] in block_ids]
    missing = entry_ids - {e["id"] for e in store["library"]}
    if missing:
        sys.exit(f"steps read missing texts: {sorted(missing)}")


def clean(step: dict) -> dict:
    """A step without empty notes or a count of one."""
    return {k: v for k, v in step.items() if not (k == "note" and not v) and not (k == "count" and v == 1)}


if __name__ == "__main__":
    main()
