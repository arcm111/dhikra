package io.github.arcm111.dhikra.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.arcm111.dhikra.R

/**
 * The Dhikra brand mark (the launcher icon's layers: a mihrab arch with a crescent and a star over
 * three prayer beads, on dark green) in a disc. Used in empty states.
 */
@Composable
fun DhikraLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    // The adaptive icon layers are 108 units with the visible icon in the central 72: scale them
    // up so that central part fills the disc, as on the launcher.
    val zoom = Modifier.fillMaxSize().graphicsLayer {
        scaleX = 108f / 72f
        scaleY = 108f / 72f
    }
    Box(modifier.size(size).clip(CircleShape)) {
        Image(painterResource(R.drawable.ic_launcher_background), null, zoom, contentScale = ContentScale.Crop)
        Image(painterResource(R.drawable.ic_launcher_foreground), null, zoom)
    }
}
