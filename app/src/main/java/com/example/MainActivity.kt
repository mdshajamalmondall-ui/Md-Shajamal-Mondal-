package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NovaCallApp
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutFromNovaCall
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.NovaCallTheme
import com.example.ui.viewmodel.DialerViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private var pendingDialNumber by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureDialIntent(intent)

        setContent {
            val isFirebaseInitialized = remember {
                FirebaseApp.getApps(this@MainActivity).isNotEmpty() ||
                    runCatching { FirebaseApp.initializeApp(this@MainActivity) != null }.getOrDefault(false)
            }

            if (isFirebaseInitialized) {
                val auth = Firebase.auth
                val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
                val credentialManager = remember { CredentialManager.create(this@MainActivity) }
                val scope = rememberCoroutineScope()

                val user = currentUser
                if (user == null) {
                    NovaCallTheme(darkTheme = true) {
                        AuthScreen(onAuthSuccess = {})
                    }
                } else {
                    key(user.uid) {
                        val dialerViewModel: DialerViewModel = viewModel(
                            key = user.uid,
                            factory = DialerViewModel.factory(
                                application = application,
                                currentUserId = user.uid,
                                userDisplayNameHint = user.displayName ?: user.email?.substringBefore("@") ?: "Shajamal User",
                                enableCloudCallerId = true
                            )
                        )

                        LaunchedEffect(pendingDialNumber) {
                            val number = pendingDialNumber
                            if (!number.isNullOrBlank()) {
                                dialerViewModel.prefillNumberFromIntent(number)
                                pendingDialNumber = null
                            }
                        }

                        NovaCallApp(
                            viewModel = dialerViewModel,
                            signedInEmail = user.email,
                            onSignOut = {
                                signOutFromNovaCall(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            } else {
                val dialerViewModel: DialerViewModel = viewModel(
                    key = "local_offline_session",
                    factory = DialerViewModel.factory(
                        application = application,
                        currentUserId = "",
                        userDisplayNameHint = "Shajamal User",
                        enableCloudCallerId = false
                    )
                )

                LaunchedEffect(pendingDialNumber) {
                    val number = pendingDialNumber
                    if (!number.isNullOrBlank()) {
                        dialerViewModel.prefillNumberFromIntent(number)
                        pendingDialNumber = null
                    }
                }

                NovaCallApp(
                    viewModel = dialerViewModel,
                    signedInEmail = null,
                    onSignOut = {}
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureDialIntent(intent)
    }

    private fun captureDialIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme.equals("tel", ignoreCase = true)) {
            val number = data.schemeSpecificPart.orEmpty()
            if (number.isNotBlank()) {
                pendingDialNumber = number
            }
        }
    }
}
