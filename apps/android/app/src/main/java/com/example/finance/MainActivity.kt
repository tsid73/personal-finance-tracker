package com.example.finance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import com.example.finance.ui.AppShell
import com.example.finance.ui.theme.PersonalFinanceTrackerTheme
import com.example.finance.util.BiometricAuthHandler
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FinanceApplication
        setContent {
            val scope = rememberCoroutineScope()
            val lifecycleOwner = LocalLifecycleOwner.current
            val isDarkTheme by app.preferenceManager.darkTheme.collectAsState(initial = null)
            val resolvedDarkTheme = isDarkTheme ?: isSystemInDarkTheme()
            val biometricEnabled by app.preferenceManager.biometricEnabled.collectAsState(initial = null)
            
            var isLocked by remember { mutableStateOf<Boolean?>(null) }
            var authAttempted by remember { mutableStateOf(false) }
            var shouldLockOnResume by remember { mutableStateOf(false) }

            // Sync database settings with isLocked state once loaded
            LaunchedEffect(biometricEnabled) {
                if (biometricEnabled != null && isLocked == null) {
                    isLocked = biometricEnabled
                }
            }

            // Lock on app resume (ON_STOP -> ON_RESUME)
            DisposableEffect(lifecycleOwner, biometricEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (biometricEnabled == true) {
                                shouldLockOnResume = true
                            }
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            if (shouldLockOnResume && biometricEnabled == true) {
                                isLocked = true
                                authAttempted = false
                                shouldLockOnResume = false
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (isLocked == true && !authAttempted) {
                authAttempted = true
                if (BiometricAuthHandler.isBiometricAvailable(this)) {
                    BiometricAuthHandler.showBiometricPrompt(
                        activity = this,
                        onSuccess = { isLocked = false },
                        onError = { error ->
                            Toast.makeText(this, "Auth failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    isLocked = false
                }
            }

            PersonalFinanceTrackerTheme(darkTheme = resolvedDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked == true || isLocked == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("App Locked", style = MaterialTheme.typography.headlineMedium)
                                if (isLocked == true) {
                                    Button(onClick = { authAttempted = false }) {
                                        Text("Unlock")
                                    }
                                }
                            }
                        }
                    } else {
                        AppShell(
                            application = app,
                            isDarkTheme = resolvedDarkTheme,
                            onToggleTheme = {
                                scope.launch {
                                    app.preferenceManager.setTheme(!resolvedDarkTheme)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
