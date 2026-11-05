package com.example.dhikra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dhikra.ui.theme.EmeraldDeep
import com.example.dhikra.ui.theme.GoldLight

/** The Dhikra brand mark: a crescent on an emerald disc. Used in empty states. */
@Composable
fun DhikraLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, EmeraldDeep))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MoonStarIcon,
            contentDescription = null,
            tint = GoldLight,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}
