package com.example.dhikra.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dhikra.ui.theme.LocalDhikraPalette

/** Flat top (it runs under the status bar), generously rounded bottom. */
private val HeaderShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

/**
 * The branded header shared by every screen: an emerald gradient with a faint geometric star
 * lattice, drawn edge-to-edge behind the status bar and lifted above the page by a soft shadow,
 * so the app bar and the tabs read as their own layer, independent of the scrolling content.
 */
@Composable
fun DhikraHeader(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalDhikraPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = HeaderShape)
            .background(Brush.verticalGradient(listOf(palette.headerStart, palette.headerEnd)))
            .starLattice(Color.White.copy(alpha = 0.05f))
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(bottom = bottomPadding),
        content = content,
    )
}

/**
 * A pill-shaped tab inside [DhikraHeader]: outlined on the emerald gradient, filled once
 * [selected]. Place it in a `selectableGroup()` row.
 */
@Composable
fun HeaderTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalDhikraPalette.current
    val container by animateColorAsState(
        targetValue = if (selected) palette.tabSelected else Color.White.copy(alpha = 0.10f),
        label = "tabContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) palette.onTabSelected else Color.White.copy(alpha = 0.92f),
        label = "tabContent",
    )
    Surface(
        selected = selected,
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = contentColor,
        border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = modifier.semantics { role = Role.Tab },
    ) {
        Row(
            modifier = Modifier.height(40.dp).padding(start = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Transparent app bar colors with white content, for app bars placed inside [DhikraHeader]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun headerAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    navigationIconContentColor = Color.White,
    titleContentColor = Color.White,
    actionIconContentColor = Color.White,
)
