package io.github.arcm111.dhikra.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.NoticeKind
import io.github.arcm111.dhikra.data.SectionNotice
import io.github.arcm111.dhikra.ui.theme.LocalDhikraPalette

val NoticeKind.icon: ImageVector
    get() = when (this) {
        NoticeKind.INFO -> InfoIcon
        NoticeKind.WARNING -> WarningIcon
        NoticeKind.IMPORTANT -> ImportantIcon
    }

val NoticeKind.label: Int
    get() = when (this) {
        NoticeKind.INFO -> R.string.notice_info
        NoticeKind.WARNING -> R.string.notice_warning
        NoticeKind.IMPORTANT -> R.string.notice_important
    }

/** A notice's colour: the app's slate blue to know, its golden amber to heed, its purple for what matters most. */
@Composable
fun NoticeKind.accent(): Color {
    val palette = LocalDhikraPalette.current
    return when (this) {
        NoticeKind.INFO -> palette.slate.accent
        NoticeKind.WARNING -> palette.done
        NoticeKind.IMPORTANT -> palette.purple
    }
}

/** A section's message above its steps: its icon and text on a soft tint of its colour. */
@Composable
fun SectionNoticeCard(notice: SectionNotice, modifier: Modifier = Modifier) {
    val accent = notice.kind.accent()
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val kind = stringResource(notice.kind.label)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.10f).compositeOver(surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$kind: ${notice.text}" },
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Icon(notice.kind.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = notice.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
