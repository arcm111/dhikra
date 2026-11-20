package io.github.arcm111.dhikra.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** The room a text field gives its text: the outlined field's sides, less above and below it. */
private val CompactPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)

/**
 * An outlined text field like Material's [androidx.compose.material3.OutlinedTextField] — same
 * border, floating label, icons and supporting text — with less room above and below its text, for
 * the editors' popups.
 */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = OutlinedTextFieldDefaults.shape,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    val scheme = MaterialTheme.colorScheme
    val ink = if (textStyle.color.isSpecified) textStyle.color else scheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        // Room above for the label, which sits on the border once the field is filled or focused.
        modifier = modifier.then(if (label != null) Modifier.padding(top = 8.dp) else Modifier),
        readOnly = readOnly,
        textStyle = textStyle.copy(color = ink),
        cursorBrush = SolidColor(if (isError) scheme.error else scheme.primary),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else maxLines,
        interactionSource = interaction,
        decorationBox = { field ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = field,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                isError = isError,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                colors = colors,
                contentPadding = CompactPadding,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = isError,
                        interactionSource = interaction,
                        colors = colors,
                        shape = shape,
                    )
                },
            )
        },
    )
}
