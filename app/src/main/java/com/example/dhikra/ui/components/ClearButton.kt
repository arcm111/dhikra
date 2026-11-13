package com.example.dhikra.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.dhikra.R

/**
 * A text field's clear button, for its `trailingIcon`: an ✕ that empties the field, shown only while
 * it holds text (null otherwise, so an empty field keeps its full width).
 */
fun clearButton(value: String, onClear: () -> Unit): (@Composable () -> Unit)? =
    if (value.isEmpty()) {
        null
    } else {
        {
            // Small and quiet: it stays out of the text's way.
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clear_text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
