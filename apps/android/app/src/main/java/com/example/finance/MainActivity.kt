package com.example.finance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            val isDarkTheme by app.preferenceManager.darkTheme.collectAsState(initial = null)
            val resolvedDarkTheme = isDarkTheme ?: isSystemInDarkTheme()
            val biometricEnabled by app.preferenceManager.biometricEnabled.collectAsState(initial = false)
            
            var isLocked by remember { mutableStateOf(biometricEnabled) }
            var authAttempted by remember { mutableStateOf(false) }

            if (isLocked && !authAttempted) {
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
                    if (isLocked) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("App Locked", style = MaterialTheme.typography.headlineMedium)
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
