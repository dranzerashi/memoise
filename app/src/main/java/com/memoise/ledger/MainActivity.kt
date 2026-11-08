package com.memoise.ledger

import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.memoise.ledger.ui.ledger.EntryViewModel
import com.memoise.ledger.ui.ledger.MemoiseApp
import com.memoise.ledger.ui.theme.MemoiseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val entryViewModel: EntryViewModel by viewModels()
    private val openQuickCapture = mutableStateOf(false)
    private val openEntryId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openQuickCapture.value = intent?.action == ACTION_QUICK_CAPTURE
        openEntryId.value = intent?.takeIf { it.action == ACTION_OPEN_ENTRY }
            ?.getStringExtra(EXTRA_ENTRY_ID)
        enableEdgeToEdge()
        setContent {
            val themePreference by entryViewModel.themePreference.collectAsStateWithLifecycle()
            MemoiseTheme(themePreference = themePreference) {
                MemoiseApp(
                    viewModel = entryViewModel,
                    openQuickCapture = openQuickCapture.value,
                    onQuickCaptureOpened = { openQuickCapture.value = false },
                    openEntryId = openEntryId.value,
                    onEntryOpened = { openEntryId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openQuickCapture.value = intent.action == ACTION_QUICK_CAPTURE
        openEntryId.value = intent.takeIf { it.action == ACTION_OPEN_ENTRY }
            ?.getStringExtra(EXTRA_ENTRY_ID)
    }

    companion object {
        const val ACTION_QUICK_CAPTURE = "com.memoise.ledger.action.QUICK_CAPTURE"
        const val ACTION_OPEN_ENTRY = "com.memoise.ledger.action.OPEN_ENTRY"
        const val EXTRA_ENTRY_ID = "com.memoise.ledger.extra.ENTRY_ID"
    }
}
