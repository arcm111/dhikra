package io.github.arcm111.dhikra.ui.components

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ReminderSound
import io.github.arcm111.dhikra.reminder.ReminderScheduler

/** The name of [sound] as the user sees it: «الافتراضي», «الأذان», or the phone sound's title. */
@Composable
fun reminderSoundLabel(sound: ReminderSound): String {
    val context = LocalContext.current
    return when (sound) {
        ReminderSound.Default -> stringResource(R.string.sound_default)
        ReminderSound.Silent -> stringResource(R.string.sound_silent)
        ReminderSound.Azan -> stringResource(R.string.sound_azan)
        is ReminderSound.Device -> remember(sound.uri) { ReminderScheduler.deviceSoundTitle(context, sound.uri) }
            ?: stringResource(R.string.sound_device_fallback)
    }
}

/**
 * Chooses a reminder's sound: the phone's default (what reminders always played), the azan when
 * [allowAzan] (it can be played here first), silence, or any notification sound of the phone
 * through Android's own sound picker. Picking one calls [onSelect]; the caller closes the dialog.
 */
@Composable
fun ReminderSoundDialog(
    current: ReminderSound,
    allowAzan: Boolean,
    onSelect: (ReminderSound) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.sound_title),
) {
    val context = LocalContext.current
    val pickerTitle = stringResource(R.string.sound_title)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (result.resultCode == Activity.RESULT_OK && uri != null) onSelect(ReminderSound.Device(uri.toString()))
    }
    // The azan's preview, stopped when the dialog closes.
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    fun stopPreview() {
        player?.release()
        player = null
    }
    DisposableEffect(Unit) { onDispose { stopPreview() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                SoundOption(
                    label = stringResource(R.string.sound_default),
                    note = stringResource(R.string.sound_default_note),
                    selected = current == ReminderSound.Default,
                    onClick = { onSelect(ReminderSound.Default) },
                )
                if (allowAzan) {
                    SoundOption(
                        label = stringResource(R.string.sound_azan),
                        note = stringResource(R.string.sound_azan_note),
                        selected = current == ReminderSound.Azan,
                        onClick = { onSelect(ReminderSound.Azan) },
                        trailing = {
                            val playing = player != null
                            IconButton(onClick = {
                                if (playing) {
                                    stopPreview()
                                } else {
                                    player = MediaPlayer.create(context, R.raw.azan)?.apply {
                                        setOnCompletionListener { stopPreview() }
                                        start()
                                    }
                                }
                            }) {
                                Icon(
                                    if (playing) StopIcon else Icons.Filled.PlayArrow,
                                    stringResource(if (playing) R.string.sound_stop else R.string.sound_play),
                                )
                            }
                        },
                    )
                }
                SoundOption(
                    label = stringResource(R.string.sound_silent),
                    selected = current == ReminderSound.Silent,
                    onClick = { onSelect(ReminderSound.Silent) },
                )
                SoundOption(
                    label = stringResource(R.string.sound_device),
                    note = (current as? ReminderSound.Device)?.let { reminderSoundLabel(it) },
                    selected = current is ReminderSound.Device,
                    onClick = {
                        stopPreview()
                        picker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, pickerTitle)
                                .putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    (current as? ReminderSound.Device)?.let { Uri.parse(it.uri) },
                                )
                        )
                    },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SoundOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    note: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}
