package com.memoise.ledger

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoiseTheme {
                MemoiseApp(viewModel = entryViewModel)
            }
        }
    }
}
