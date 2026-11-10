package com.example.dhikra.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ProgramFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Shareable program format (`*.dhikra.json`), version 4:
 *
 * ```json
 * {
 *   "format": "dhikra.program",
 *   "version": 4,
 *   "name": "...",
 *   "library": [
 *     { "id": "kahf_1_10", "type": "quran", "title": "...", "description": "...",
 *       "opening": "...", "quran": "18:1-10" },
 *     { "id": "dua_nur", "type": "hadith|dua|dhikr", "title": "...",
 *       "description": "...", "opening": "...", "text": "..." }
 *   ],
 *   "groups": [
 *     { "id": "group_hamd", "type": "dua", "title": "...", "description": "...",
 *       "items": [ { "entry": "dua_awzini" }, { "entry": "dua_ajirni", "count": 7 } ] }
 *   ],
 *   "sections": [
 *     { "id": "...", "title": "...", "icon": "🕌", "reminder": "05:15", "note": "...",
 *       "items": [
 *         { "id": "...", "type": "quran", "entry": "kahf_1_10", "count": 3, "note": "..." },
 *         { "id": "...", "type": "dua", "group": "group_hamd", "note": "..." },
 *         { "id": "...", "type": "task|heading", "title": "...", "text": "...", "note": "..." }
 *       ] }
 *   ]
 * }
 * ```
 * Program steps never embed Quran / hadith / dua / dhikr texts: they reference a library entry
 * or a group of entries by id. Quran entries don't embed their text either: they reference their
 * ayat ("2:255", "3:190-200"), whose text comes from the Quran text bundled with the app, so a
 * file can't carry an altered Quran text. Version 1 files (texts embedded in the steps) are
 * migrated into library entries; version 2 files simply have no groups; Quran entries of version
 * 3 files, typed in as text, stay text entries.
 * Empty fields are omitted; unknown fields are ignored so the format can evolve.
 */
object ProgramJson {
    const val FORMAT = "dhikra.program"
    const val VERSION = 4
    const val FILE_EXTENSION = ".dhikra.json"
    const val MIME_TYPE = "application/json"
    const val MAX_COUNT = 100_000

    /** How far (in minutes) a section's time can be set from its prayer's. */
    const val MAX_OFFSET = 6 * 60
    private const val STORE_FORMAT = "dhikra.store"
    private const val STORE_VERSION = 1

    fun encode(bundle: ProgramBundle): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("name", bundle.program.name)
        put("library", JSONArray().apply { bundle.library.entries.forEach { put(encodeEntry(it)) } })
        if (bundle.library.groups.isNotEmpty()) {
            put("groups", JSONArray().apply { bundle.library.groups.forEach { put(encodeGroup(it)) } })
        }
        put("sections", JSONArray().apply {
            bundle.program.sections.forEach { put(encodeSection(it)) }
        })
    }.toString(2)

    fun decode(json: String): ProgramBundle {
        try {
            val root = JSONObject(json)
            val format = root.optString("format")
            if (format.isNotEmpty() && format != FORMAT) {
                throw ProgramFormatException("Unknown format: $format")
            }
            val sectionsJson = root.optJSONArray("sections")
                ?: throw ProgramFormatException("Missing sections")
            val context = decodeLibrary(root)
            val program = decodeProgram(root.optString("name"), sectionsJson, context)
            return ProgramBundle(program = program, library = context.library)
        } catch (e: JSONException) {
            throw ProgramFormatException("Invalid JSON", e)
        }
    }

    /**
     * The app's own storage of all the user's programs, with the library they share (not a format
     * for sharing: a program is exported on its own with [encode]):
     *
     * ```json
     * { "format": "dhikra.store", "version": 1, "active": "<program id>",
     *   "library": [ ... ], "groups": [ ... ],
     *   "programs": [ { "id": "...", "name": "...", "sections": [ ... ] } ] }
     * ```
     */
    fun encodeStore(store: ProgramStore): String = JSONObject().apply {
        put("format", STORE_FORMAT)
        put("version", STORE_VERSION)
        put("active", store.active.id)
        put("library", JSONArray().apply { store.library.entries.forEach { put(encodeEntry(it)) } })
        if (store.library.groups.isNotEmpty()) {
            put("groups", JSONArray().apply { store.library.groups.forEach { put(encodeGroup(it)) } })
        }
        put("programs", JSONArray().apply {
            store.programs.forEach { program ->
                put(JSONObject().apply {
                    put("id", program.id)
                    put("name", program.name)
                    put("sections", JSONArray().apply { program.sections.forEach { put(encodeSection(it)) } })
                })
            }
        })
    }.toString(2)

    /** Reads [encodeStore]'s output; section and step ids stay unique across all the programs. */
    fun decodeStore(json: String): ProgramStore {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != STORE_FORMAT) throw ProgramFormatException("Not a program store")
            val programsJson = root.optJSONArray("programs") ?: throw ProgramFormatException("Missing programs")
            val context = decodeLibrary(root)
            val programIds = mutableSetOf<String>()
            val programs = (0 until programsJson.length()).map { i ->
                val programJson = programsJson.getJSONObject(i)
                decodeProgram(
                    name = programJson.optString("name"),
                    sectionsJson = programJson.optJSONArray("sections") ?: JSONArray(),
                    context = context,
                    id = uniqueId(programJson.optString("id"), programIds),
                )
            }
            if (programs.isEmpty()) throw ProgramFormatException("No program")
            return ProgramStore(programs, root.optString("active"), context.library)
        } catch (e: JSONException) {
            throw ProgramFormatException("Invalid JSON", e)
        }
    }

    /** The library entries and groups of a program file or of the store. */
    private fun decodeLibrary(root: JSONObject): DecodeContext {
        val entryIds = mutableSetOf<String>()
        val libraryJson = root.optJSONArray("library") ?: JSONArray()
        val entries = (0 until libraryJson.length()).mapNotNull { i ->
            decodeEntry(libraryJson.getJSONObject(i), entryIds)
        }.toMutableList()

        val groupIds = mutableSetOf<String>()
        val groupsJson = root.optJSONArray("groups") ?: JSONArray()
        val groups = (0 until groupsJson.length()).mapNotNull { i ->
            decodeGroup(groupsJson.getJSONObject(i), groupIds, entryIds)
        }
        return DecodeContext(mutableSetOf(), entryIds, entries, groups)
    }

    private fun decodeProgram(
        name: String,
        sectionsJson: JSONArray,
        context: DecodeContext,
        id: String = newId(),
    ): Program {
        val sections = (0 until sectionsJson.length()).map { i ->
            decodeSection(sectionsJson.getJSONObject(i), context)
        }
        return Program(name = name.trim(), sections = sections, id = id)
    }

    /** True when [json] is an older file that should be re-saved in the current format. */
    fun isOutdated(json: String): Boolean =
        runCatching { JSONObject(json).optInt("version", 1) < VERSION }.getOrDefault(false)

    private fun encodeEntry(entry: LibraryEntry) = JSONObject().apply {
        put("id", entry.id)
        put("type", entry.type.key)
        put("title", entry.title)
        putIfNotBlank("description", entry.description)
        putIfNotBlank("opening", entry.opening)
        val quran = entry.quran
        if (quran != null) put("quran", quran.toString()) else put("text", entry.text)
    }

    /**
     * A library entry; a Quran entry with a valid reference gets no text (it is read from the
     * bundled Quran text), any other entry needs one.
     */
    private fun decodeEntry(json: JSONObject, entryIds: MutableSet<String>): LibraryEntry? {
        val type = ItemType.fromKey(json.optString("type")).takeIf { it.isContent } ?: return null
        val quran = if (type == ItemType.QURAN) QuranRef.parse(json.optString("quran")) else null
        val text = if (quran != null) "" else json.optString("text").trim()
        if (quran == null && text.isEmpty()) return null
        return LibraryEntry(
            id = uniqueId(json.optString("id"), entryIds),
            type = type,
            title = json.optString("title").trim().ifEmpty {
                if (quran != null) ArabicText.toArabicDigits(quran.toString()) else titleFrom(text)
            },
            text = text,
            description = json.optString("description").trim(),
            opening = json.optString("opening").trim(),
            quran = quran,
        )
    }

    private fun encodeGroup(group: LibraryGroup) = JSONObject().apply {
        put("id", group.id)
        put("type", group.type.key)
        put("title", group.title)
        putIfNotBlank("description", group.description)
        put("items", JSONArray().apply {
            group.items.forEach { item ->
                put(JSONObject().apply {
                    put("entry", item.entryId)
                    if (item.count > 1) put("count", item.count)
                })
            }
        })
        if (group.isChoice) put("required", group.required)
    }

    /** A group of known entries (each read once at most); groups without a title are dropped. */
    private fun decodeGroup(json: JSONObject, groupIds: MutableSet<String>, entryIds: Set<String>): LibraryGroup? {
        val type = ItemType.fromKey(json.optString("type")).takeIf { it.isContent } ?: return null
        val title = json.optString("title").trim()
        if (title.isEmpty()) return null
        val itemsJson = json.optJSONArray("items") ?: JSONArray()
        val items = (0 until itemsJson.length()).mapNotNull { i ->
            val item = itemsJson.optJSONObject(i) ?: return@mapNotNull null
            val entryId = item.optString("entry").trim().takeIf { it in entryIds } ?: return@mapNotNull null
            GroupItem(entryId, item.optInt("count", 1).coerceIn(1, MAX_COUNT))
        }.distinctBy { it.entryId }
        return LibraryGroup(
            id = uniqueId(json.optString("id"), groupIds),
            type = type,
            title = title,
            description = json.optString("description").trim(),
            items = items,
            // Some of the texts, or (0) all of them.
            required = json.optInt("required", 0).takeIf { it in 1 until items.size } ?: 0,
        )
    }

    private fun encodeSection(section: ProgramSection) = JSONObject().apply {
        put("id", section.id)
        put("title", section.title)
        putIfNotBlank("icon", section.icon)
        section.reminderMinutes?.let { put("reminder", formatTime24(it)) }
        section.anchor?.let { anchor ->
            put("prayer", anchor.prayer.name.lowercase())
            if (anchor.offsetMinutes != 0) put("offset", anchor.offsetMinutes)
        }
        putIfNotBlank("note", section.note)
        if (section.notices.isNotEmpty()) {
            put("notices", JSONArray().apply {
                section.notices.forEach { notice ->
                    put(JSONObject().apply {
                        put("kind", notice.kind.key)
                        put("text", notice.text)
                    })
                }
            })
        }
        put("items", JSONArray().apply { section.items.forEach { put(encodeItem(it)) } })
    }

    private fun encodeItem(item: ProgramItem) = JSONObject().apply {
        put("id", item.id)
        put("type", item.type.key)
        if (item.type.isContent) {
            if (item.isGroup) put("group", item.groupId) else put("entry", item.entryId)
        } else {
            putIfNotBlank("title", item.title)
            putIfNotBlank("text", item.text)
        }
        if (item.count > 1) put("count", item.count)
        putIfNotBlank("note", item.note)
    }

    /**
     * What decoding the steps needs: the ids taken so far and the library read before them. Version 1
     * steps add the texts they embed to [entries].
     */
    private class DecodeContext(
        val usedIds: MutableSet<String>,
        val entryIds: MutableSet<String>,
        val entries: MutableList<LibraryEntry>,
        private val groupList: List<LibraryGroup>,
    ) {
        val groups: Map<String, LibraryGroup> = groupList.associateBy { it.id }
        val library: Library get() = Library(entries.toList(), groupList)
    }

    private fun decodeSection(json: JSONObject, context: DecodeContext): ProgramSection {
        val itemsJson = json.optJSONArray("items") ?: JSONArray()
        return ProgramSection(
            id = uniqueId(json.optString("id"), context.usedIds),
            title = json.optString("title").trim(),
            icon = json.optString("icon").ifBlank { "🕌" },
            reminderMinutes = parseTime24(json.optString("reminder")),
            note = json.optString("note").trim(),
            notices = json.optJSONArray("notices")?.let { array ->
                (0 until array.length()).mapNotNull { i ->
                    val notice = array.optJSONObject(i) ?: return@mapNotNull null
                    val text = notice.optString("text").trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    SectionNotice(NoticeKind.fromKey(notice.optString("kind")), text)
                }
            }.orEmpty(),
            anchor = Prayer.entries.firstOrNull { it.name.equals(json.optString("prayer"), ignoreCase = true) }
                ?.let { SectionAnchor(it, json.optInt("offset", 0).coerceIn(-MAX_OFFSET, MAX_OFFSET)) },
            items = (0 until itemsJson.length()).mapNotNull { i ->
                decodeItem(itemsJson.getJSONObject(i), context)
            },
        )
    }

    private fun decodeItem(json: JSONObject, context: DecodeContext): ProgramItem? {
        val type = ItemType.fromKey(json.optString("type"))
        val id = uniqueId(json.optString("id"), context.usedIds)
        val count = json.optInt("count", 1).coerceIn(1, MAX_COUNT)
        val note = json.optString("note").trim()
        if (!type.isContent) {
            return ProgramItem(
                id = id,
                type = type,
                title = json.optString("title").trim(),
                text = json.optString("text").trim(),
                note = note,
            )
        }
        val groupId = json.optString("group").trim()
        if (groupId.isNotEmpty()) {
            // A group step takes its group's collection; its texts carry their own counts.
            val groupType = context.groups[groupId]?.type ?: type
            return ProgramItem(
                id = id,
                type = groupType,
                note = note,
                groupId = groupId,
            )
        }
        val entryId = json.optString("entry").trim().ifEmpty {
            // Version 1: the text is embedded in the step. Move it into the library.
            migrateEntry(json, type, context.entryIds)?.also { context.entries += it }?.id ?: return null
        }
        return ProgramItem(id = id, type = type, count = count, note = note, entryId = entryId)
    }

    private fun migrateEntry(json: JSONObject, type: ItemType, entryIds: MutableSet<String>): LibraryEntry? {
        val text = json.optString("text").trim()
        if (text.isEmpty()) return null
        val description = listOf("source", "grade", "narrator")
            .map { json.optString(it).trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" • ")
        return LibraryEntry(
            id = uniqueId(json.optString("id"), entryIds),
            type = type,
            title = json.optString("title").trim().ifEmpty { titleFrom(text) },
            text = text,
            description = description,
            opening = json.optString("opening").trim(),
        )
    }

    /** A short title made of the first words of [text], for entries saved without one. */
    fun titleFrom(text: String): String {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        return if (words.size <= 4) words.joinToString(" ") else words.take(4).joinToString(" ") + "…"
    }

    private fun uniqueId(candidate: String, usedIds: MutableSet<String>): String {
        val id = candidate.takeIf { it.isNotBlank() && it !in usedIds } ?: newId()
        usedIds += id
        return id
    }

    private fun JSONObject.putIfNotBlank(key: String, value: String) {
        if (value.isNotBlank()) put(key, value)
    }
}

fun formatTime24(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

fun parseTime24(value: String?): Int? {
    val match = Regex("""^\s*(\d{1,2}):(\d{2})\s*$""").find(value ?: return null) ?: return null
    val h = match.groupValues[1].toInt()
    val m = match.groupValues[2].toInt()
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}
