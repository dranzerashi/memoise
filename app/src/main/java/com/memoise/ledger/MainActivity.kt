package com.memoise.ledger

import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.memoise.ledger.ui.ledger.EntryViewModel
import com.memoise.ledger.ui.ledger.MemoiseApp
import com.memoise.ledger.ui.theme.MemoiseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val entryViewModel: EntryViewModel by viewModels()
    private val openQuickCapture = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openQuickCapture.value = intent?.action == ACTION_QUICK_CAPTURE
        enableEdgeToEdge()
        setContent {
            MemoiseTheme {
                MemoiseApp(
                    viewModel = entryViewModel,
                    openQuickCapture = openQuickCapture.value,
                    onQuickCaptureOpened = { openQuickCapture.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openQuickCapture.value = intent.action == ACTION_QUICK_CAPTURE
    }

    companion object {
        const val ACTION_QUICK_CAPTURE = "com.memoise.ledger.action.QUICK_CAPTURE"
    }
}
