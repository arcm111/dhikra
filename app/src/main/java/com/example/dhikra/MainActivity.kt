package com.example.dhikra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import com.example.dhikra.reminder.ReminderScheduler
import com.example.dhikra.ui.DhikraApp
import com.example.dhikra.ui.MainViewModel
import com.example.dhikra.ui.theme.DhikraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen starts with the dark emerald header (or the dark counter), so the status
        // bar always uses light icons; the navigation bar follows the system theme.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            DhikraTheme {
                DhikraApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Handles reminder taps (open a section) and shared/opened program files (import). */
    private fun handleIntent(intent: Intent) {
        intent.getStringExtra(ReminderScheduler.EXTRA_SECTION_ID)?.let(viewModel::selectSection)

        val programUri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.takeIf { it.scheme == "content" }
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        programUri?.let(viewModel::loadImport)
    }
}
