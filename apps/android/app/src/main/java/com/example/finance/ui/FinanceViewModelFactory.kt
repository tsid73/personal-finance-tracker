package com.example.finance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun <T : ViewModel> financeViewModel(
    modelClass: Class<T>,
    creator: () -> T
): T {
    val factory = remember(modelClass) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(requestedClass: Class<VM>): VM {
                if (!requestedClass.isAssignableFrom(modelClass)) {
                    throw IllegalArgumentException("Unknown ViewModel class: ${requestedClass.name}")
                }
                return creator() as VM
            }
        }
    }
    return viewModel(modelClass = modelClass, factory = factory)
}
