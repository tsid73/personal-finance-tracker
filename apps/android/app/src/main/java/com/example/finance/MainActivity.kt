package com.example.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.finance.ui.AppShell
import com.example.finance.ui.theme.PersonalFinanceTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FinanceApplication
        setContent {
            val scope = rememberCoroutineScope()
            val isDarkTheme by app.preferenceManager.darkTheme.collectAsState(initial = null)
            val resolvedDarkTheme = isDarkTheme ?: isSystemInDarkTheme()
            PersonalFinanceTrackerTheme(darkTheme = resolvedDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
