package com.example.dhikra.ui.library

import com.example.dhikra.ui.components.CompactTextField
import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Quran
import com.example.dhikra.data.QuranLookup
import com.example.dhikra.data.QuranPassage
import com.example.dhikra.data.QuranRef
import com.example.dhikra.data.Surah
import com.example.dhikra.ui.components.ReaderText
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.theme.ReadingStyles
import com.example.dhikra.ui.uiDigits

/** The bundled Quran text, for the screens that pick passages; null where it can't be read (previews). */
val LocalQuranLookup = staticCompositionLocalOf<QuranLookup?> { null }

/** Ayat shown in the preview of a passage: longer passages are cut, the reader shows them in full. */
private const val PREVIEW_AYAT = 20

/** The passage the picker's fields name, or null while they don't name a valid one. */
fun quranRefOf(sura: Int, from: String, to: String): QuranRef? {
    val first = ArabicText.toLatinDigits(from).trim().toIntOrNull() ?: return null
    val last = ArabicText.toLatinDigits(to).trim().toIntOrNull() ?: return null
    return QuranRef(sura, first, last).takeIf { it.isValid }
}

/** The default title of a passage of [surah]: «سورة الكهف». */
fun quranTitle(resources: Resources, surah: Surah): String =
    resources.getString(R.string.quran_surah_title, surah.name)

/** The default description of [ref]: the whole surah, «الآية ٢٥٥» or «الآيات ١٩٠ - ٢٠٠». */
fun quranRangeDescription(resources: Resources, ref: QuranRef): String = when {
    ref.from == 1 && ref.to == Quran.ayahCount(ref.sura) -> resources.getString(R.string.quran_whole_surah)
    ref.from == ref.to -> resources.getString(R.string.quran_ayah, uiDigits(ref.from))
    else -> resources.getString(
        R.string.quran_ayat, uiDigits(ref.from), uiDigits(ref.to),
    )
}

/**
 * Picks the ayat a Quran entry reads: a surah (searched by name or number), then a range of its
 * ayat, with a preview of their text. [onPick] receives the surah and the two ayah fields as typed;
 * [passage] is the text read for the passage they name, null while [loading] or when unreadable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPassagePicker(
    surahs: List<Surah>,
    sura: Int,
    from: String,
    to: String,
    onPick: (sura: Int, from: String, to: String) -> Unit,
    passage: QuranPassage?,
    loading: Boolean,
    showErrors: Boolean,
    modifier: Modifier = Modifier,
) {
    val selected = surahs.firstOrNull { it.number == sura }
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val key = ArabicText.searchKey(query)
    val matches = remember(surahs, key) {
        if (key.isEmpty()) surahs
        else surahs.filter { ArabicText.searchKey(it.name).contains(key) || it.number.toString().startsWith(key) }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { open ->
                expanded = open
                if (!open) query = ""
            },
        ) {
            CompactTextField(
                value = if (expanded) query else selected?.let { surahLabel(it) }.orEmpty(),
                onValueChange = {
                    query = it
                    expanded = true
                },
                label = { Text(stringResource(R.string.quran_surah)) },
                placeholder = { Text(stringResource(R.string.quran_surah_hint)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = showErrors && selected == null,
                supportingText = when {
                    selected != null -> {
                        { Text(ayahCount(selected.ayahCount)) }
                    }
                    showErrors -> {
                        { Text(stringResource(R.string.quran_surah_error)) }
                    }
                    else -> null
                },
                singleLine = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded && matches.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                    query = ""
                },
            ) {
                matches.forEach { surah ->
                    DropdownMenuItem(
                        text = { Text(surahLabel(surah)) },
                        trailingIcon = {
                            Text(
                                text = ayahCount(surah.ayahCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {
                            expanded = false
                            query = ""
                            // A new surah is read whole until a range is chosen.
                            onPick(surah.number, uiDigits(1), uiDigits(surah.ayahCount))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        if (selected != null) {
            val count = selected.ayahCount
            val first = ArabicText.toLatinDigits(from).trim().toIntOrNull()
            val last = ArabicText.toLatinDigits(to).trim().toIntOrNull()
            val rangeError = stringResource(R.string.quran_range_error, uiDigits(count))
            val fromError = if (first == null || first !in 1..count) rangeError else null
            val toError = when {
                last == null || last !in 1..count -> rangeError
                first != null && last < first -> stringResource(R.string.quran_to_error)
                else -> null
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AyahField(R.string.quran_from, from, fromError.takeIf { showErrors || from.isNotBlank() }, Modifier.weight(1f)) {
                    onPick(sura, it, to)
                }
                AyahField(R.string.quran_to, to, toError.takeIf { showErrors || to.isNotBlank() }, Modifier.weight(1f)) {
                    onPick(sura, from, it)
                }
            }
            FilterChip(
                selected = first == 1 && last == count,
                onClick = { onPick(sura, uiDigits(1), uiDigits(count)) },
                label = { Text(stringResource(R.string.quran_whole_surah)) },
            )
            when {
                fromError != null || toError != null -> Unit
                loading -> Text(
                    text = stringResource(R.string.quran_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                passage == null -> Text(
                    text = stringResource(R.string.quran_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> PassagePreview(passage)
            }
        }
    }
}

/** Where the Quran texts come from, as Tanzil's terms ask: their name, and a link to their site. */
@Composable
fun QuranSourceNote(modifier: Modifier = Modifier) {
    val url = stringResource(R.string.quran_source_url)
    val linkStyle = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    val source = stringResource(R.string.quran_source)
    Text(
        text = buildAnnotatedString {
            append(source)
            append(" • ")
            withLink(LinkAnnotation.Url(url, linkStyle)) { append(url.substringAfter("://")) }
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun surahLabel(surah: Surah): String =
    stringResource(R.string.quran_surah_item, uiDigits(surah.number), surah.name)

@Composable
private fun ayahCount(count: Int): String =
    pluralStringResource(R.plurals.quran_ayah_count, count, uiDigits(count))

@Composable
private fun AyahField(label: Int, value: String, error: String?, modifier: Modifier, onValueChange: (String) -> Unit) {
    CompactTextField(
        value = value,
        onValueChange = { typed -> onValueChange(typed.filter { it.isDigit() }.take(3)) },
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

/** The passage as the reader will show it (its first ayat only when it is long), on the Quran's card. */
@Composable
private fun PassagePreview(passage: QuranPassage) {
    val colors = ItemType.QURAN.colors()
    val shown = passage.ayat.take(PREVIEW_AYAT)
    val text = remember(passage) { ArabicText.withAyahMarkers(QuranPassage(passage.ref, shown).text) }
    Surface(shape = MaterialTheme.shapes.large, color = colors.container, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (passage.ref.opensWithBasmala) {
                Text(
                    text = Quran.BASMALA,
                    style = ReadingStyles.quranOpening(0.85f),
                    color = colors.accent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Laid out like in the reader: pause marks on their words, ayah numbers in rosettes.
            ReaderText(
                text = text,
                style = ReadingStyles.quran(0.85f),
                colors = colors,
                ornamentalAyahMarkers = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (shown.size < passage.ayat.size) {
                Text(
                    text = stringResource(R.string.quran_preview_more, uiDigits(passage.ref.to)),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
