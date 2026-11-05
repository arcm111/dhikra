package com.example.dhikra.ui.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.example.dhikra.data.ItemType

/**
 * Line icons drawn on the same 24dp grid and with the same stroke weight as the Material icons,
 * so every glyph in the app (menus, buttons, badges) looks like one family.
 */
private fun lineIcon(name: String, vararg pathData: String, autoMirror: Boolean = false): ImageVector {
    // Same 24dp viewport as the Material icons; paths are stroked instead of filled.
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    )
    pathData.forEach { data ->
        builder.addPath(
            pathData = addPathNodes(data),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

/** Three rays above the horizon: a morning (Fajr) section. */
val FajrIcon: ImageVector = lineIcon(
    "DhikraFajr",
    "M3,19.5h18",
    "M7,19.5a5,5 0 0 1 10,0",
    "M12,6.2v2.4",
    "M5.6,9.1l1.7,1.7",
    "M18.4,9.1l-1.7,1.7",
    "M6,22h2.5",
    "M15.5,22H18",
)

/** Crescent with a small star: night / evening sections. */
val MoonStarIcon: ImageVector = lineIcon(
    "DhikraMoonStar",
    "M15.8,3.2a8.8,8.8 0 1 0 5,14.1A7.2,7.2 0 0 1 15.8,3.2z",
    "M18.4,3.1l0.9,1.9 2.1,0.3 -1.5,1.5 0.4,2.1 -1.9,-1 -1.9,1 0.4,-2.1 -1.5,-1.5 2.1,-0.3z",
)

/** Prayer niche with a hanging lamp. */
val PrayerIcon: ImageVector = lineIcon(
    "DhikraPrayer",
    "M5,21V11.5a7,7 0 0 1 14,0V21",
    "M3.2,21h17.6",
    "M12,5.6v1.9",
    "M9.9,7.5h4.2l-0.7,3.2h-2.8z",
    "M10.8,10.7v4.1",
)

/** Sun: daytime sections such as Duha. */
val SunIcon: ImageVector = lineIcon(
    "DhikraSun",
    "M12,16.4a4.4,4.4 0 1 0 0,-8.8 4.4,4.4 0 0 0 0,8.8z",
    "M12,2.4v2",
    "M12,19.6v2",
    "M2.4,12h2",
    "M19.6,12h2",
    "M5.2,5.2l1.4,1.4",
    "M17.4,17.4l1.4,1.4",
    "M5.2,18.8l1.4,-1.4",
    "M17.4,6.6l1.4,-1.4",
)

/** Mosque dome with a door, used for the default section badge. */
val MosqueIcon: ImageVector = lineIcon(
    "DhikraMosque",
    "M6,12.5a6,6 0 0 1 12,0",
    "M12,3.2v3.3",
    "M12,3.2a0.9,0.9 0 1 1 0.1,0",
    "M6,12.5V21",
    "M18,12.5V21",
    "M3.2,21h17.6",
    "M10,21v-4.2a2,2 0 0 1 4,0V21",
)

/** Kaaba silhouette: the pilgrimage section. */
val KaabaIcon: ImageVector = lineIcon(
    "DhikraKaaba",
    "M4.6,8.6L12,5l7.4,3.6v9.8L12,22l-7.4,-3.6z",
    "M4.6,8.6L12,12.2l7.4,-3.6",
    "M12,12.2V22",
    "M4.6,13.6L12,17.2l7.4,-3.6",
)

/** Crescent moon: a generic section. */
val CrescentIcon: ImageVector = lineIcon(
    "DhikraCrescent",
    "M15.4,3.3a8.7,8.7 0 1 0 4.6,14.2A7.6,7.6 0 0 1 15.4,3.3z",
)


/** Filled eight-pointed star (Rub el Hizb), used as the app mark. */
val StarOrnamentIcon: ImageVector = materialIcon("DhikraStar") {
    materialPath {
        moveTo(12f, 2.2f)
        lineTo(15.2f, 8.8f)
        lineTo(21.8f, 12f)
        lineTo(15.2f, 15.2f)
        lineTo(12f, 21.8f)
        lineTo(8.8f, 15.2f)
        lineTo(2.2f, 12f)
        lineTo(8.8f, 8.8f)
        close()
    }
}

/** Open mushaf with ayah marks. */
val QuranIcon: ImageVector = lineIcon(
    "DhikraQuran",
    "M12,6.4C10,4.9 7.4,4.4 4.4,4.7V18c3,-0.3 5.6,0.2 7.6,1.7 2,-1.5 4.6,-2 7.6,-1.7V4.7C16.6,4.4 14,4.9 12,6.4z",
    "M12,6.4V19.7",
    "M6.6,8.6h2.4",
    "M6.6,12.2h2.4",
    "M15,8.6h2.4",
    "M15,12.2h2.4",
)

/** Two cupped hands raised in supplication. */
val PrayingHandsIcon: ImageVector = lineIcon(
    "DhikraDua",
    "M12,21c-3.2,0 -5.6,-2.6 -5.6,-5.8V10a1.4,1.4 0 0 1 2.8,0v4",
    "M12,21c3.2,0 5.6,-2.6 5.6,-5.8V10a1.4,1.4 0 0 0 -2.8,0v4",
    "M9.2,10.6V5.4a1.4,1.4 0 0 1 2.8,0v5",
    "M12,10.4V4.6a1.4,1.4 0 0 1 2.8,0v6",
)

/** Rolled parchment scroll. */
val ScrollIcon: ImageVector = lineIcon(
    "DhikraScroll",
    "M6.4,4.4h9.6a2,2 0 0 1 2,2v11.8a1.6,1.6 0 0 0 3.2,0V7",
    "M6.4,4.4a2,2 0 0 0 -2,2v12.4a1.6,1.6 0 0 0 1.6,1.6h11.2",
    "M8.4,9h6.4",
    "M8.4,12.4h6.4",
    "M8.4,15.8h4",
)

/** Loop of a misbaha (prayer beads). */
val BeadsIcon: ImageVector = lineIcon(
    "DhikraBeads",
    "M12,3.6a8.4,8.4 0 1 0 0.1,0",
    "M12,20.4v2.2",
    "M12,1.4a1.2,1.2 0 1 1 0,2.4 1.2,1.2 0 0 1 0,-2.4",
    "M6.2,5.2a1.2,1.2 0 1 1 0,2.4 1.2,1.2 0 0 1 0,-2.4",
    "M17.8,5.2a1.2,1.2 0 1 1 0,2.4 1.2,1.2 0 0 1 0,-2.4",
    "M3.4,12a1.2,1.2 0 1 1 0,2.4 1.2,1.2 0 0 1 0,-2.4",
    "M20.6,12a1.2,1.2 0 1 1 0,2.4 1.2,1.2 0 0 1 0,-2.4",
)

/** Books side by side, the last one leaning: the library of texts. */
val LibraryIcon: ImageVector = lineIcon(
    "DhikraLibrary",
    "M4.4,4.5h3.6V20H4.4z",
    "M10,6.5h3.6V20H10z",
    "M15.6,8.1l3.4,-0.9 3,11.6 -3.4,0.9z",
    "M4.4,8.2H8",
    "M10,10H13.6",
)

/** Circle with a check mark: a task. */
val CheckCircleIcon: ImageVector = lineIcon(
    "DhikraCheckCircle",
    "M21,12a9,9 0 1 1 -18,0 9,9 0 0 1 18,0z",
    "M7.8,12.2l2.8,2.8 5.6,-5.6",
)

/** Sparkles, used for celebrations ("تقبّل الله منك"). */
val SparkleIcon: ImageVector = lineIcon(
    "DhikraSparkle",
    "M12,3l1.9,5.1 5.1,1.9 -5.1,1.9 -1.9,5.1 -1.9,-5.1 -5.1,-1.9 5.1,-1.9z",
    "M18.6,15.4l0.7,1.9 1.9,0.7 -1.9,0.7 -0.7,1.9 -0.7,-1.9 -1.9,-0.7 1.9,-0.7z",
)

/** Four outward arrows: expand to full screen. */
val ExpandIcon: ImageVector = lineIcon(
    "DhikraExpand",
    "M9,4H4v5",
    "M4,4l6,6",
    "M15,4h5v5",
    "M20,4l-6,6",
    "M9,20H4v-5",
    "M4,20l6,-6",
    "M15,20h5v-5",
    "M20,20l-6,-6",
)

/** Curved arrow going back one step; mirrored in right-to-left layouts. */
val UndoIcon: ImageVector = lineIcon(
    "DhikraUndo",
    "M8.5,14.5L4,10l4.5,-4.5",
    "M4,10h10a5.5,5.5 0 0 1 0,11h-3",
    autoMirror = true,
)

/** A tag/note glyph, used next to user notes. */
val NoteIcon: ImageVector = lineIcon(
    "DhikraNote",
    "M4,5.2A1.6,1.6 0 0 1 5.6,3.6h12.8A1.6,1.6 0 0 1 20,5.2v8.8a1.6,1.6 0 0 1 -1.6,1.6H9.4L4.8,20.2a0.6,0.6 0 0 1 -0.8,-0.5z",
    "M8,8.4h8",
    "M8,11.6h5",
)

/** Icon representing what kind of content an item holds. */
val ItemType.icon: ImageVector
    get() = when (this) {
        ItemType.QURAN -> QuranIcon
        ItemType.DUA -> PrayingHandsIcon
        ItemType.HADITH -> ScrollIcon
        ItemType.DHIKR -> BeadsIcon
        ItemType.TASK -> CheckCircleIcon
        ItemType.HEADING -> StarOrnamentIcon
    }

/**
 * Maps the emoji a user picked for a section (stored in the program file, so it stays portable)
 * onto the matching line icon. Unknown emoji fall back to the mosque mark.
 */
fun sectionIcon(emoji: String): ImageVector = when (emoji) {
    "🌙" -> CrescentIcon
    "☀️", "🌕" -> SunIcon
    "🕌", "🏠", "🛐" -> MosqueIcon
    "🕋" -> KaabaIcon
    "📖" -> QuranIcon
    "📿" -> BeadsIcon
    "🤲" -> PrayingHandsIcon
    "📜" -> ScrollIcon
    "✅" -> CheckCircleIcon
    "🌅", "🌄" -> FajrIcon
    "⭐", "🌟", "✨", "۞" -> MoonStarIcon
    else -> MosqueIcon
}

/** Icons offered by the section editor. The emoji key is what gets saved in the program file. */
val SectionIconChoices: List<Pair<String, ImageVector>> = listOf(
    "🕌" to MosqueIcon,
    "🌅" to FajrIcon,
    "🌙" to MoonStarIcon,
    "☀️" to SunIcon,
    "📖" to QuranIcon,
    "📿" to BeadsIcon,
    "🤲" to PrayingHandsIcon,
    "📜" to ScrollIcon,
    "🕋" to KaabaIcon,
    "✅" to CheckCircleIcon,
)
