package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.NovaCallApp
import com.example.ui.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

    private val dialerViewModel: DialerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDialIntent(intent)
        setContent {
            NovaCallApp(viewModel = dialerViewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    private fun handleDialIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme.equals("tel", ignoreCase = true)) {
            val number = data.schemeSpecificPart.orEmpty()
            if (number.isNotBlank()) {
                dialerViewModel.prefillNumberFromIntent(number)
            }
        }
    }
}
